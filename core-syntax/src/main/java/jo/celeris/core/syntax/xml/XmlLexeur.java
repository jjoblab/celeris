package jo.celeris.core.syntax.xml;

import java.util.ArrayList;
import java.util.List;
import jo.celeris.core.syntax.LanguageLexer;
import jo.celeris.core.syntax.LexState;
import jo.celeris.core.syntax.LexedLine;
import jo.celeris.core.syntax.Token;
import jo.celeris.core.syntax.TokenKind;

/**
 * Lexeur XML (ADR-004) : la balise ouvrante porte son nom
 * ({@code <produit}) puis ses attributs — identificateur, signe égale,
 * chaîne — jusqu'au chevron de ponctuation ; la fermeture nommée
 * ({@code </produit>}) et l'auto-fermeture ({@code />}) sont des crochets
 * fermants appariés à l'ouverte — l'appariement et la profondeur suivent.
 * Le prologue est un jeton de documentation, le texte un jeton de chaîne
 * (entités comprises), la section CDATA un crochet ouvrant suivi de son
 * contenu puis de {@code ]]>}. Les commentaires, sections CDATA et balises
 * ouvertes traversent les lignes par l'état transporté ; le lexique
 * reprend après une fermeture en milieu de ligne. Une valeur d'attribut
 * non fermée est un jeton ERROR, jamais une exception.
 *
 * <p>Usage à fil unique (ADR-007) : le moteur appelle {@link #lex} ligne
 * après ligne, séquentiellement — l'état de sortie de la dernière ligne
 * vit dans les champs privés, réarmés à chaque appel.</p>
 */
final class XmlLexeur implements LanguageLexer {

    /** États transportés d'une ligne à la suivante. */
    enum State implements LexState {
        /** Dans le texte, entre les balises. */
        TEXTE,
        /** Dans une balise ouverte, entre les attributs. */
        BALISE,
        /** Dans un commentaire. */
        COMMENTAIRE,
        /** Dans une section CDATA. */
        CDATA
    }

    private boolean dansBalise;
    private State exitState = State.TEXTE;

    @Override
    public LexState initialState() {
        return State.TEXTE;
    }

    @Override
    public LexedLine lex(CharSequence text, LexState input) {
        exitState = State.TEXTE;
        dansBalise = input == State.BALISE;
        List<Token> tokens = new ArrayList<>();
        int reprise = 0;
        if (input == State.COMMENTAIRE) {
            reprise = lexDansCommentaire(text, tokens);
        } else if (input == State.CDATA) {
            reprise = lexDansCdata(text, tokens);
        }
        int n = text.length();
        int i = reprise;
        while (i < n) {
            i = lexUn(text, tokens, i, n);
        }
        if (exitState == State.TEXTE) {
            exitState = dansBalise ? State.BALISE : State.TEXTE;
        }
        return new LexedLine(tokens.toArray(new Token[0]), exitState);
    }

    /** Renvoie la position de reprise : le lexique continue après la fermeture. */
    private int lexDansCommentaire(CharSequence text, List<Token> tokens) {
        int fermeture = find(text, "-->", 0);
        int end = fermeture < 0 ? text.length() : fermeture + 3;
        tokens.add(new Token(TokenKind.COMMENT, 0, end));
        exitState = fermeture < 0 ? State.COMMENTAIRE : State.TEXTE;
        return end;
    }

    /** Renvoie la position de reprise : le lexique continue après la fermeture. */
    private int lexDansCdata(CharSequence text, List<Token> tokens) {
        int fermeture = find(text, "]]>", 0);
        if (fermeture < 0) {
            if (text.length() > 0) {
                tokens.add(new Token(TokenKind.STRING, 0, text.length()));
            }
            exitState = State.CDATA;
            return text.length();
        }
        if (fermeture > 0) {
            tokens.add(new Token(TokenKind.STRING, 0, fermeture));
        }
        tokens.add(new Token(TokenKind.CLOSE_BRACKET, fermeture, fermeture + 3));
        exitState = State.TEXTE;
        return fermeture + 3;
    }

    private int lexUn(CharSequence text, List<Token> tokens, int i, int n) {
        char c = text.charAt(i);
        if (Character.isWhitespace(c)) {
            return i + 1;
        }
        if (dansBalise) {
            return lexAttribut(text, tokens, i, n);
        }
        if (c == '<') {
            return lexBalise(text, tokens, i, n);
        }
        return lexText(text, tokens, i, n);
    }

    /** Le prologue et le doctype sont de la documentation. */
    private int lexBalise(CharSequence text, List<Token> tokens, int i, int n) {
        int apres = i + 1;
        if (apres < n && text.charAt(apres) == '?') {
            return lexPrologue(text, tokens, i, n);
        }
        if (apres < n && text.charAt(apres) == '!') {
            return lexExclamation(text, tokens, i, n);
        }
        if (apres < n && text.charAt(apres) == '/') {
            return lexFermeture(text, tokens, i, n);
        }
        int end = apres;
        while (end < n && estNomChar(text.charAt(end))) {
            end++;
        }
        if (end == apres) {
            tokens.add(new Token(TokenKind.OPERATOR, i, i + 1));
            return i + 1;
        }
        tokens.add(new Token(TokenKind.OPEN_BRACKET, i, end));
        dansBalise = true;
        return end;
    }

    private static int lexPrologue(CharSequence text, List<Token> tokens, int i, int n) {
        int fermeture = find(text, "?>", i);
        int end = fermeture < 0 ? n : fermeture + 2;
        tokens.add(new Token(TokenKind.DOC, i, end));
        return end;
    }

    /** Commentaire ou section CDATA — le doctype retombe sur la documentation. */
    private int lexExclamation(CharSequence text, List<Token> tokens, int i, int n) {
        if (commence(text, i, "<!--")) {
            int fermeture = find(text, "-->", i + 4);
            int end = fermeture < 0 ? n : fermeture + 3;
            tokens.add(new Token(TokenKind.COMMENT, i, end));
            exitState = fermeture < 0 ? State.COMMENTAIRE : State.TEXTE;
            return end;
        }
        if (commence(text, i, "<![CDATA[")) {
            tokens.add(new Token(TokenKind.OPEN_BRACKET, i, i + 9));
            return lexCdataOuvert(text, tokens, i + 9, n);
        }
        int fermeture = find(text, ">", i);
        int end = fermeture < 0 ? n : fermeture + 1;
        tokens.add(new Token(TokenKind.DOC, i, end));
        return end;
    }

    private int lexCdataOuvert(CharSequence text, List<Token> tokens, int depuis, int n) {
        int fermeture = find(text, "]]>", depuis);
        if (fermeture < 0) {
            if (depuis < n) {
                tokens.add(new Token(TokenKind.STRING, depuis, n));
            }
            exitState = State.CDATA;
            return n;
        }
        if (fermeture > depuis) {
            tokens.add(new Token(TokenKind.STRING, depuis, fermeture));
        }
        tokens.add(new Token(TokenKind.CLOSE_BRACKET, fermeture, fermeture + 3));
        exitState = State.TEXTE;
        return fermeture + 3;
    }

    private int lexFermeture(CharSequence text, List<Token> tokens, int i, int n) {
        int start = i + 2;
        int end = start;
        while (end < n && estNomChar(text.charAt(end))) {
            end++;
        }
        int apresNom = end;
        while (apresNom < n && Character.isWhitespace(text.charAt(apresNom))) {
            apresNom++;
        }
        if (apresNom < n && text.charAt(apresNom) == '>') {
            tokens.add(new Token(TokenKind.CLOSE_BRACKET, i, apresNom + 1));
            return apresNom + 1;
        }
        tokens.add(new Token(TokenKind.CLOSE_BRACKET, i, Math.max(end, i + 2)));
        return Math.max(end, i + 2);
    }

    /** Dans la balise : chevron, auto-fermeture, égale, valeur ou nom d'attribut. */
    private int lexAttribut(CharSequence text, List<Token> tokens, int i, int n) {
        char c = text.charAt(i);
        if (c == '>') {
            dansBalise = false;
            tokens.add(new Token(TokenKind.PUNCTUATION, i, i + 1));
            return i + 1;
        }
        if (c == '/' && i + 1 < n && text.charAt(i + 1) == '>') {
            dansBalise = false;
            tokens.add(new Token(TokenKind.CLOSE_BRACKET, i, i + 2));
            return i + 2;
        }
        if (c == '=') {
            tokens.add(new Token(TokenKind.OPERATOR, i, i + 1));
            return i + 1;
        }
        if (c == '"') {
            return lexValue(text, tokens, i, n);
        }
        int end = i;
        while (end < n && estNomChar(text.charAt(end))) {
            end++;
        }
        if (end == i) {
            tokens.add(new Token(TokenKind.ERROR, i, i + 1));
            return i + 1;
        }
        tokens.add(new Token(TokenKind.IDENTIFIER, i, end));
        return end;
    }

    /** La valeur d'attribut jusqu'au guillemet fermant — l'antislash
     * n'échappe rien en XML, seul «&#x2f;&…;&#x2f;» est une entité : le
     * chemin Windows se ferme comme toute valeur ; non fermée =
     * ERROR. */
    private int lexValue(CharSequence text, List<Token> tokens, int i, int n) {
        int end = i + 1;
        while (end < n) {
            char c = text.charAt(end);
            if (c == '"') {
                tokens.add(new Token(TokenKind.STRING, i, end + 1));
                return end + 1;
            }
            end++;
        }
        tokens.add(new Token(TokenKind.ERROR, i, n));
        return n;
    }

    /** Le texte jusqu'au «&#x2f;» ouvrant suivant — entités comprises. */
    private int lexText(CharSequence text, List<Token> tokens, int i, int n) {
        int end = i;
        while (end < n && text.charAt(end) != '<') {
            end++;
        }
        tokens.add(new Token(TokenKind.STRING, i, end));
        return end;
    }

    private static boolean estNomChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.' || c == ':';
    }

    private static boolean commence(CharSequence text, int i, String pattern) {
        if (i + pattern.length() > text.length()) {
            return false;
        }
        for (int j = 0; j < pattern.length(); j++) {
            if (text.charAt(i + j) != pattern.charAt(j)) {
                return false;
            }
        }
        return true;
    }

    private static int find(CharSequence text, String pattern, int depuis) {
        for (int i = depuis; i + pattern.length() <= text.length(); i++) {
            boolean egale = true;
            for (int j = 0; j < pattern.length(); j++) {
                if (text.charAt(i + j) != pattern.charAt(j)) {
                    egale = false;
                    break;
                }
            }
            if (egale) {
                return i;
            }
        }
        return -1;
    }
}
