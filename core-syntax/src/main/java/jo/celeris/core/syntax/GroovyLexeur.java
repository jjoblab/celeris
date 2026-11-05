package jo.celeris.core.syntax;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Lexeur Groovy (ADR-004, dialecte Gradle — C-07) : mots réservés,
 * gabarits — les guillemets internes aux accolades {@code ${…}} ne
 * clôturent pas —, chaînes triples multi-lignes par état transporté
 * (l'ouverture triple est un crochet ouvrant apparié à sa fermeture),
 * commentaires de bloc NICHÉS par état à profondeur, documentation
 * distinguée du bloc, annotations, opérateurs propres, convention de
 * casse (majuscule initiale = type, tout en majuscules = constante, un
 * segment qualifié reste un identificateur). Une chaîne non fermée est
 * un jeton ERROR, jamais une exception.
 *
 * <p>Usage à fil unique (ADR-007) : le moteur appelle {@link #lex} ligne
 * après ligne, séquentiellement — les états de sortie de la dernière
 * ligne vivent dans les champs privés, réarmés à chaque appel. Les
 * chaînes slashy et dollar-slashy restent hors périmètre : le script
 * de construction Gradle ne les emploie pas, leur absence est une
 * limite documentée, pas une erreur.</p>
 */
final class GroovyLexeur implements LanguageLexer {

    /** États transportés simples d'une ligne à la suivante. */
    enum Etat implements LexState {
        /** Hors de toute construction multi-lignes. */
        NORMAL
    }

    /** Dans un commentaire de bloc niché, à la profondeur donnée. */
    record DansBloc(int profondeur, boolean documentation) implements LexState {
    }

    /** Dans une chaîne triple du délimiteur donné (apostrophe ou guillemet). */
    record DansTriple(char delimiteur) implements LexState {
    }

    private static final Set<String> MOTS_RESERVES = Set.of(
            "abstract", "as", "assert", "boolean", "break", "byte", "case",
            "catch", "char", "class", "const", "continue", "def", "default",
            "do", "double", "else", "enum", "extends", "false", "final",
            "finally", "float", "for", "goto", "if", "implements", "import",
            "in", "instanceof", "int", "interface", "it", "long", "native",
            "new", "null", "package", "private", "protected", "public",
            "return", "short", "static", "strictfp", "super", "switch",
            "synchronized", "this", "throw", "throws", "trait", "transient",
            "true", "try", "void", "volatile", "while");

    private LexState etatSortie = Etat.NORMAL;

    @Override
    public LexState initialState() {
        return Etat.NORMAL;
    }

    @Override
    public LexedLine lex(CharSequence texte, LexState entree) {
        etatSortie = Etat.NORMAL;
        List<Token> jetons = new ArrayList<>();
        int reprise = 0;
        if (entree instanceof DansBloc bloc) {
            reprise = lexDansBloc(texte, jetons, bloc.profondeur(),
                    bloc.documentation());
        } else if (entree instanceof DansTriple triple) {
            reprise = lexDansTriple(texte, jetons, triple.delimiteur());
        }
        int n = texte.length();
        int i = reprise;
        while (i < n) {
            i = lexUn(texte, jetons, i, n);
        }
        return new LexedLine(jetons.toArray(new Token[0]), etatSortie);
    }

    /** Le commentaire niché continue ; reprise à la fermeture complète. */
    private int lexDansBloc(CharSequence texte, List<Token> jetons, int profondeur,
                            boolean documentation) {
        int n = texte.length();
        int i = 0;
        int profondeurCourante = profondeur;
        int debut = 0;
        TokenKind sorte = documentation ? TokenKind.DOC : TokenKind.COMMENT;
        while (i < n) {
            if (commence(texte, i, "/*")) {
                profondeurCourante++;
                i += 2;
                continue;
            }
            if (commence(texte, i, "*/")) {
                profondeurCourante--;
                i += 2;
                if (profondeurCourante <= 0) {
                    jetons.add(new Token(sorte, debut, i));
                    etatSortie = Etat.NORMAL;
                    return i;
                }
                continue;
            }
            i++;
        }
        jetons.add(new Token(sorte, debut, n));
        etatSortie = new DansBloc(profondeurCourante, documentation);
        return n;
    }

    /** La chaîne triple continue ; reprise après la fermeture de son délimiteur. */
    private int lexDansTriple(CharSequence texte, List<Token> jetons, char delimiteur) {
        int n = texte.length();
        String fermeture = tripleDe(delimiteur);
        int fin = trouver(texte, fermeture, 0);
        if (fin < 0) {
            if (n > 0) {
                jetons.add(new Token(TokenKind.STRING, 0, n));
            }
            etatSortie = new DansTriple(delimiteur);
            return n;
        }
        if (fin > 0) {
            jetons.add(new Token(TokenKind.STRING, 0, fin));
        }
        jetons.add(new Token(TokenKind.CLOSE_BRACKET, fin, fin + 3));
        etatSortie = Etat.NORMAL;
        return fin + 3;
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
        return GroovySignes.lex(texte, jetons, i, n);
    }

    /** Renvoie -1 quand ce n'est PAS un commentaire ; le niché porte sa profondeur. */
    private int lexCommentaire(CharSequence texte, List<Token> jetons, int i, int n) {
        if (i + 1 >= n) {
            return -1;
        }
        char suivant = texte.charAt(i + 1);
        if (suivant == '/') {
            jetons.add(new Token(TokenKind.COMMENT, i, n));
            return n;
        }
        if (suivant != '*') {
            return -1;
        }
        boolean doc = i + 2 < n && texte.charAt(i + 2) == '*'
                && !(i + 3 < n && texte.charAt(i + 3) == '/');
        int profondeur = 1;
        int j = i + 2;
        while (j < n && profondeur > 0) {
            if (commence(texte, j, "/*")) {
                profondeur++;
                j += 2;
                continue;
            }
            if (commence(texte, j, "*/")) {
                profondeur--;
                j += 2;
                continue;
            }
            j++;
        }
        if (profondeur == 0) {
            jetons.add(new Token(doc ? TokenKind.DOC : TokenKind.COMMENT, i, j));
            etatSortie = Etat.NORMAL;
            return j;
        }
        jetons.add(new Token(doc ? TokenKind.DOC : TokenKind.COMMENT, i, n));
        etatSortie = new DansBloc(profondeur, doc);
        return n;
    }

    /** Chaîne délimitée : triple ouvrante en crochet, gabarit à accolades sinon. */
    private int lexDelimite(CharSequence texte, List<Token> jetons, int i, int n,
                            char delimiteur) {
        if (i + 2 < n && texte.charAt(i + 1) == delimiteur
                && texte.charAt(i + 2) == delimiteur) {
            return lexTriple(texte, jetons, i, n, delimiteur);
        }
        return lexLigne(texte, jetons, i, n, delimiteur);
    }

    /**
     * La chaîne délimitée d'une ligne : échappements et, pour les
     * guillemets, gabarit {@code ${…}} — les guillemets internes n'y
     * clôturent pas (l'état vit dans {@link Gabarit}).
     */
    private int lexLigne(CharSequence texte, List<Token> jetons, int i, int n,
                         char delimiteur) {
        Gabarit gabarit = new Gabarit(delimiteur);
        int fin = i + 1;
        while (fin < n) {
            char c = texte.charAt(fin);
            if (c == '\\') {
                fin += 2;
                continue;
            }
            if (gabarit.interieur()) {
                fin = gabarit.avancer(c, fin);
                continue;
            }
            if (c == delimiteur) {
                jetons.add(new Token(TokenKind.STRING, i, fin + 1));
                return fin + 1;
            }
            if (gabarit.ouvre(texte, fin, n)) {
                fin = gabarit.entrer(fin);
                continue;
            }
            fin++;
        }
        jetons.add(new Token(TokenKind.ERROR, i, n));
        return n;
    }

    /**
     * L'état du gabarit {@code ${…}} d'une chaîne de guillemets :
     * l'accolade ouvrante entre, la fermante sort, les imbriquées se
     * comptent — les guillemets internes ne clôturent pas.
     */
    private static final class Gabarit {

        private final char delimiteur;
        private int accolades;

        Gabarit(char delimiteur) {
            this.delimiteur = delimiteur;
        }

        boolean interieur() {
            return delimiteur == '"' && accolades > 0;
        }

        boolean ouvre(CharSequence texte, int fin, int n) {
            return delimiteur == '"' && fin + 1 < n
                    && texte.charAt(fin) == '$' && texte.charAt(fin + 1) == '{';
        }

        int entrer(int fin) {
            accolades++;
            return fin + 2;
        }

        int avancer(char c, int fin) {
            if (c == '{') {
                accolades++;
            } else if (c == '}') {
                accolades--;
            }
            return fin + 1;
        }
    }

    /** La chaîne triple ouvre un crochet apparié à sa fermeture. */
    private int lexTriple(CharSequence texte, List<Token> jetons, int i, int n,
                          char delimiteur) {
        jetons.add(new Token(TokenKind.OPEN_BRACKET, i, i + 3));
        int fermeture = trouver(texte, tripleDe(delimiteur), i + 3);
        if (fermeture < 0) {
            if (i + 3 < n) {
                jetons.add(new Token(TokenKind.STRING, i + 3, n));
            }
            etatSortie = new DansTriple(delimiteur);
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

    private static String tripleDe(char delimiteur) {
        return delimiteur == '"' ? "\"\"\"" : "'''";
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
