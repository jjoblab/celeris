package jo.celeris.core.syntax;

import java.util.List;
import java.util.Set;

/**
 * Lexeur Java (ADR-004) : mots réservés, littéraux de toutes bases, chaînes
 * et caractères, textes en bloc multi-lignes (état transporté), commentaires
 * de ligne, de bloc et de documentation (le DOC se distingue du bloc par
 * l'ouverture «&#x2f;** »), annotations, opérateurs contre ponctuation,
 * crochets appariés. La convention de casse distingue lexicalement les
 * types (majuscule initiale non suivie d'un point) et les constantes (tout
 * en majuscules) — un segment qualifié ({@code Map.}Entrée) reste un
 * identificateur ; une chaîne non fermée est un jeton ERROR, jamais une
 * exception.
 *
 * <p>Les jetons d'une ligne se soudent : chaque jeton commence là où finit
 * le précédent — les blancs appartiennent au jeton qui les suit, le rendu
 * n'a jamais de trou à peindre. Le premier jeton garde son début réel.</p>
 *
 * <p>Usage à fil unique (ADR-007) : le moteur appelle {@link #lex} ligne
 * après ligne, séquentiellement — l'état de sortie de la dernière ligne
 * vit dans le champ privé, réarmé à chaque appel.</p>
 */
final class JavaLexeur implements LanguageLexer {

    /** États transportés d'une ligne à la suivante. */
    enum Etat implements LexState {
        /** Hors de toute construction multi-lignes. */
        NORMAL,
        /** Dans un commentaire de bloc. */
        BLOC,
        /** Dans un commentaire de documentation. */
        DOC,
        /** Dans un texte en bloc (triple guillemet). */
        BLOC_TEXTE
    }

    private static final Set<String> MOTS_RESERVES = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch",
            "char", "class", "const", "continue", "default", "do", "double",
            "else", "enum", "extends", "final", "finally", "float", "for",
            "goto", "if", "implements", "import", "instanceof", "int",
            "interface", "long", "native", "new", "package", "private",
            "protected", "public", "record", "return", "sealed", "short",
            "static", "strictfp", "super", "switch", "synchronized", "this",
            "throw", "throws", "transient", "try", "var", "void",
            "volatile", "while", "yield", "true", "false", "null",
            "module", "open", "requires", "transitive", "exports", "opens",
            "to", "uses", "provides", "with", "permits", "non");

    private Etat etatSortie = Etat.NORMAL;

    @Override
    public LexState initialState() {
        return Etat.NORMAL;
    }

    @Override
    public LexedLine lex(CharSequence texte, LexState entree) {
        etatSortie = Etat.NORMAL;
        List<Token> jetons = new java.util.ArrayList<>();
        int reprise = 0;
        if (entree == Etat.BLOC || entree == Etat.DOC) {
            reprise = lexDansCommentaire(texte, jetons, entree == Etat.DOC);
        } else if (entree == Etat.BLOC_TEXTE) {
            reprise = lexDansTexte(texte, jetons);
        }
        int n = texte.length();
        int i = reprise;
        while (i < n) {
            i = lexUn(texte, jetons, i, n);
        }
        return new LexedLine(souder(jetons.toArray(new Token[0])), etatSortie);
    }

    /** Renvoie la position de reprise : le lexique continue après la fermeture. */
    private int lexDansCommentaire(CharSequence texte, List<Token> jetons, boolean doc) {
        int fermeture = trouver(texte, "*/", 0);
        int fin = fermeture < 0 ? texte.length() : fermeture + 2;
        jetons.add(new Token(doc ? TokenKind.DOC : TokenKind.COMMENT, 0, fin));
        etatSortie = fermeture < 0 ? (doc ? Etat.DOC : Etat.BLOC) : Etat.NORMAL;
        return fin;
    }

    /** Renvoie la position de reprise : le lexique continue après la fermeture. */
    private int lexDansTexte(CharSequence texte, List<Token> jetons) {
        int fermeture = trouver(texte, "\"\"\"", 0);
        if (fermeture < 0) {
            jetons.add(new Token(TokenKind.STRING, 0, texte.length()));
            etatSortie = Etat.BLOC_TEXTE;
            return texte.length();
        }
        if (fermeture > 0) {
            jetons.add(new Token(TokenKind.STRING, 0, fermeture));
        }
        jetons.add(new Token(TokenKind.CLOSE_BRACKET, fermeture, fermeture + 3));
        etatSortie = Etat.NORMAL;
        return fermeture + 3;
    }

    /** Soude les jetons : le jeton suivant commence à la fin du précédent. */
    private static Token[] souder(Token[] jetons) {
        for (int j = 1; j < jetons.length; j++) {
            Token precedent = jetons[j - 1];
            Token courant = jetons[j];
            if (courant.start() > precedent.end()) {
                jetons[j] = new Token(courant.kind(), precedent.end(), courant.end());
            }
        }
        return jetons;
    }

    private int lexUn(CharSequence texte, List<Token> jetons, int i, int n) {
        char c = texte.charAt(i);
        if (Character.isWhitespace(c)) {
            return i + 1;
        }
        if (c == '/') {
            int apres = lexCommentaire(texte, jetons, i, n);
            if (apres >= 0) {
                return apres;
            }
        }
        if (c == '"' || c == '\'') {
            return lexDelimite(texte, jetons, i, n, c);
        }
        if (c == '@') {
            return lexAnnotation(texte, jetons, i, n);
        }
        if (JavaNombres.ouvre(c, texte, i, n)) {
            return JavaNombres.lex(texte, jetons, i, n);
        }
        if (Character.isJavaIdentifierStart(c)) {
            return lexMot(texte, jetons, i, n);
        }
        return JavaSignes.lex(texte, jetons, i, n);
    }

    /** Renvoie -1 quand ce n'est PAS un commentaire (le '/' retombera sur l'opérateur). */
    private int lexCommentaire(CharSequence texte, List<Token> jetons, int i, int n) {
        if (i + 1 >= n) {
            return -1;
        }
        char suivant = texte.charAt(i + 1);
        if (suivant == '/') {
            jetons.add(new Token(TokenKind.COMMENT, i, n));
            return n;
        }
        if (suivant == '*') {
            boolean doc = i + 2 < n && texte.charAt(i + 2) == '*'
                    && !(i + 3 < n && texte.charAt(i + 3) == '/');
            int fermeture = trouver(texte, "*/", i + 2);
            int fin = fermeture < 0 ? n : fermeture + 2;
            jetons.add(new Token(doc ? TokenKind.DOC : TokenKind.COMMENT, i, fin));
            etatSortie = fermeture < 0 ? (doc ? Etat.DOC : Etat.BLOC) : Etat.NORMAL;
            return fin;
        }
        return -1;
    }

    private int lexDelimite(CharSequence texte, List<Token> jetons, int i, int n,
                            char delimiteur) {
        boolean texteEnBloc = delimiteur == '"' && i + 2 < n
                && texte.charAt(i + 1) == '"' && texte.charAt(i + 2) == '"';
        if (texteEnBloc) {
            return lexTexteEnBloc(texte, jetons, i, n);
        }
        int fin = i + 1;
        while (fin < n) {
            char c = texte.charAt(fin);
            if (c == '\\') {
                fin += 2;
                continue;
            }
            if (c == delimiteur) {
                jetons.add(new Token(TokenKind.STRING, i, fin + 1));
                return fin + 1;
            }
            fin++;
        }
        jetons.add(new Token(TokenKind.ERROR, i, n));
        return n;
    }

    /** L'ouverture d'un texte en bloc est un crochet ouvrant apparié. */
    private int lexTexteEnBloc(CharSequence texte, List<Token> jetons, int i, int n) {
        jetons.add(new Token(TokenKind.OPEN_BRACKET, i, i + 3));
        int fermeture = trouver(texte, "\"\"\"", i + 3);
        if (fermeture < 0) {
            if (i + 3 < n) {
                jetons.add(new Token(TokenKind.STRING, i + 3, n));
            }
            etatSortie = Etat.BLOC_TEXTE;
            return n;
        }
        if (fermeture > i + 3) {
            jetons.add(new Token(TokenKind.STRING, i + 3, fermeture));
        }
        jetons.add(new Token(TokenKind.CLOSE_BRACKET, fermeture, fermeture + 3));
        etatSortie = Etat.NORMAL;
        return fermeture + 3;
    }

    private static int lexAnnotation(CharSequence texte, List<Token> jetons, int i, int n) {
        int fin = i + 1;
        while (fin < n && Character.isJavaIdentifierPart(texte.charAt(fin))) {
            fin++;
        }
        if (fin == i + 1) {
            jetons.add(new Token(TokenKind.OPERATOR, i, fin));
            return fin;
        }
        jetons.add(new Token(TokenKind.ANNOTATION, i, fin));
        return fin;
    }

    private static int lexMot(CharSequence texte, List<Token> jetons, int i, int n) {
        int fin = i + 1;
        while (fin < n && Character.isJavaIdentifierPart(texte.charAt(fin))) {
            fin++;
        }
        String mot = texte.toString().substring(i, fin);
        boolean segmentQualifie = fin < n && texte.charAt(fin) == '.';
        jetons.add(new Token(classer(mot, segmentQualifie), i, fin));
        return fin;
    }

    private static TokenKind classer(String mot, boolean segmentQualifie) {
        if (MOTS_RESERVES.contains(mot)) {
            return TokenKind.KEYWORD;
        }
        boolean majusculeInitial = !mot.isEmpty() && Character.isUpperCase(mot.charAt(0));
        if (majusculeInitial && mot.length() > 1
                && mot.equals(mot.toUpperCase(java.util.Locale.ROOT))) {
            return TokenKind.CONSTANT;
        }
        if (majusculeInitial && !segmentQualifie) {
            return TokenKind.TYPE;
        }
        return TokenKind.IDENTIFIER;
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
