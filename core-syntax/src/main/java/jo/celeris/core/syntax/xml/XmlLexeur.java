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
    enum Etat implements LexState {
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
    private Etat etatSortie = Etat.TEXTE;

    @Override
    public LexState initialState() {
        return Etat.TEXTE;
    }

    @Override
    public LexedLine lex(CharSequence texte, LexState entree) {
        etatSortie = Etat.TEXTE;
        dansBalise = entree == Etat.BALISE;
        List<Token> jetons = new ArrayList<>();
        int reprise = 0;
        if (entree == Etat.COMMENTAIRE) {
            reprise = lexDansCommentaire(texte, jetons);
        } else if (entree == Etat.CDATA) {
            reprise = lexDansCdata(texte, jetons);
        }
        int n = texte.length();
        int i = reprise;
        while (i < n) {
            i = lexUn(texte, jetons, i, n);
        }
        if (etatSortie == Etat.TEXTE) {
            etatSortie = dansBalise ? Etat.BALISE : Etat.TEXTE;
        }
        return new LexedLine(jetons.toArray(new Token[0]), etatSortie);
    }

    /** Renvoie la position de reprise : le lexique continue après la fermeture. */
    private int lexDansCommentaire(CharSequence texte, List<Token> jetons) {
        int fermeture = trouver(texte, "-->", 0);
        int fin = fermeture < 0 ? texte.length() : fermeture + 3;
        jetons.add(new Token(TokenKind.COMMENT, 0, fin));
        etatSortie = fermeture < 0 ? Etat.COMMENTAIRE : Etat.TEXTE;
        return fin;
    }

    /** Renvoie la position de reprise : le lexique continue après la fermeture. */
    private int lexDansCdata(CharSequence texte, List<Token> jetons) {
        int fermeture = trouver(texte, "]]>", 0);
        if (fermeture < 0) {
            if (texte.length() > 0) {
                jetons.add(new Token(TokenKind.STRING, 0, texte.length()));
            }
            etatSortie = Etat.CDATA;
            return texte.length();
        }
        if (fermeture > 0) {
            jetons.add(new Token(TokenKind.STRING, 0, fermeture));
        }
        jetons.add(new Token(TokenKind.CLOSE_BRACKET, fermeture, fermeture + 3));
        etatSortie = Etat.TEXTE;
        return fermeture + 3;
    }

    private int lexUn(CharSequence texte, List<Token> jetons, int i, int n) {
        char c = texte.charAt(i);
        if (Character.isWhitespace(c)) {
            return i + 1;
        }
        if (dansBalise) {
            return lexAttribut(texte, jetons, i, n);
        }
        if (c == '<') {
            return lexBalise(texte, jetons, i, n);
        }
        return lexTexte(texte, jetons, i, n);
    }

    /** Le prologue et le doctype sont de la documentation. */
    private int lexBalise(CharSequence texte, List<Token> jetons, int i, int n) {
        int apres = i + 1;
        if (apres < n && texte.charAt(apres) == '?') {
            return lexPrologue(texte, jetons, i, n);
        }
        if (apres < n && texte.charAt(apres) == '!') {
            return lexExclamation(texte, jetons, i, n);
        }
        if (apres < n && texte.charAt(apres) == '/') {
            return lexFermeture(texte, jetons, i, n);
        }
        int fin = apres;
        while (fin < n && estNomChar(texte.charAt(fin))) {
            fin++;
        }
        if (fin == apres) {
            jetons.add(new Token(TokenKind.OPERATOR, i, i + 1));
            return i + 1;
        }
        jetons.add(new Token(TokenKind.OPEN_BRACKET, i, fin));
        dansBalise = true;
        return fin;
    }

    private static int lexPrologue(CharSequence texte, List<Token> jetons, int i, int n) {
        int fermeture = trouver(texte, "?>", i);
        int fin = fermeture < 0 ? n : fermeture + 2;
        jetons.add(new Token(TokenKind.DOC, i, fin));
        return fin;
    }

    /** Commentaire ou section CDATA — le doctype retombe sur la documentation. */
    private int lexExclamation(CharSequence texte, List<Token> jetons, int i, int n) {
        if (commence(texte, i, "<!--")) {
            int fermeture = trouver(texte, "-->", i + 4);
            int fin = fermeture < 0 ? n : fermeture + 3;
            jetons.add(new Token(TokenKind.COMMENT, i, fin));
            etatSortie = fermeture < 0 ? Etat.COMMENTAIRE : Etat.TEXTE;
            return fin;
        }
        if (commence(texte, i, "<![CDATA[")) {
            jetons.add(new Token(TokenKind.OPEN_BRACKET, i, i + 9));
            return lexCdataOuvert(texte, jetons, i + 9, n);
        }
        int fermeture = trouver(texte, ">", i);
        int fin = fermeture < 0 ? n : fermeture + 1;
        jetons.add(new Token(TokenKind.DOC, i, fin));
        return fin;
    }

    private int lexCdataOuvert(CharSequence texte, List<Token> jetons, int depuis, int n) {
        int fermeture = trouver(texte, "]]>", depuis);
        if (fermeture < 0) {
            if (depuis < n) {
                jetons.add(new Token(TokenKind.STRING, depuis, n));
            }
            etatSortie = Etat.CDATA;
            return n;
        }
        if (fermeture > depuis) {
            jetons.add(new Token(TokenKind.STRING, depuis, fermeture));
        }
        jetons.add(new Token(TokenKind.CLOSE_BRACKET, fermeture, fermeture + 3));
        etatSortie = Etat.TEXTE;
        return fermeture + 3;
    }

    private int lexFermeture(CharSequence texte, List<Token> jetons, int i, int n) {
        int debut = i + 2;
        int fin = debut;
        while (fin < n && estNomChar(texte.charAt(fin))) {
            fin++;
        }
        int apresNom = fin;
        while (apresNom < n && Character.isWhitespace(texte.charAt(apresNom))) {
            apresNom++;
        }
        if (apresNom < n && texte.charAt(apresNom) == '>') {
            jetons.add(new Token(TokenKind.CLOSE_BRACKET, i, apresNom + 1));
            return apresNom + 1;
        }
        jetons.add(new Token(TokenKind.CLOSE_BRACKET, i, Math.max(fin, i + 2)));
        return Math.max(fin, i + 2);
    }

    /** Dans la balise : chevron, auto-fermeture, égale, valeur ou nom d'attribut. */
    private int lexAttribut(CharSequence texte, List<Token> jetons, int i, int n) {
        char c = texte.charAt(i);
        if (c == '>') {
            dansBalise = false;
            jetons.add(new Token(TokenKind.PUNCTUATION, i, i + 1));
            return i + 1;
        }
        if (c == '/' && i + 1 < n && texte.charAt(i + 1) == '>') {
            dansBalise = false;
            jetons.add(new Token(TokenKind.CLOSE_BRACKET, i, i + 2));
            return i + 2;
        }
        if (c == '=') {
            jetons.add(new Token(TokenKind.OPERATOR, i, i + 1));
            return i + 1;
        }
        if (c == '"') {
            return lexValeur(texte, jetons, i, n);
        }
        int fin = i;
        while (fin < n && estNomChar(texte.charAt(fin))) {
            fin++;
        }
        if (fin == i) {
            jetons.add(new Token(TokenKind.ERROR, i, i + 1));
            return i + 1;
        }
        jetons.add(new Token(TokenKind.IDENTIFIER, i, fin));
        return fin;
    }

    /** La valeur d'attribut jusqu'au guillemet fermant — l'antislash
     * n'échappe rien en XML, seul «&#x2f;&…;&#x2f;» est une entité : le
     * chemin Windows se ferme comme toute valeur ; non fermée =
     * ERROR. */
    private int lexValeur(CharSequence texte, List<Token> jetons, int i, int n) {
        int fin = i + 1;
        while (fin < n) {
            char c = texte.charAt(fin);
            if (c == '"') {
                jetons.add(new Token(TokenKind.STRING, i, fin + 1));
                return fin + 1;
            }
            fin++;
        }
        jetons.add(new Token(TokenKind.ERROR, i, n));
        return n;
    }

    /** Le texte jusqu'au «&#x2f;» ouvrant suivant — entités comprises. */
    private int lexTexte(CharSequence texte, List<Token> jetons, int i, int n) {
        int fin = i;
        while (fin < n && texte.charAt(fin) != '<') {
            fin++;
        }
        jetons.add(new Token(TokenKind.STRING, i, fin));
        return fin;
    }

    private static boolean estNomChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.' || c == ':';
    }

    private static boolean commence(CharSequence texte, int i, String motif) {
        if (i + motif.length() > texte.length()) {
            return false;
        }
        for (int j = 0; j < motif.length(); j++) {
            if (texte.charAt(i + j) != motif.charAt(j)) {
                return false;
            }
        }
        return true;
    }

    private static int trouver(CharSequence texte, String motif, int depuis) {
        for (int i = depuis; i + motif.length() <= texte.length(); i++) {
            boolean egale = true;
            for (int j = 0; j < motif.length(); j++) {
                if (texte.charAt(i + j) != motif.charAt(j)) {
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
