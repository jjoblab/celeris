package jo.celeris.core.syntax.grammars;

/**
 * L'analyse de délimitation d'un groupe (garde catastrophique,
 * ADR-014) : un groupe est délimité quand son contenu commence par un
 * atome obligatoire — littéral échappé, caractère ordinaire ou classe
 * explicite sans intervalle ni négation — qu'aucun autre atome du
 * contenu ne peut avaler. Les itérations se séparent alors sans
 * ambiguïté, le retour arrière reste polynomial.
 */
final class Delimiter {

    /** Les métacaractères hors classe — un délimiteur ne peut pas en être un. */
    private static final String METAS = "\\^$.[]|()*+?{}-";

    private Delimiter() {
    }

    /**
     * Vrai quand le groupe est délimité : son contenu commence par un
     * atome obligatoire qu'aucun autre atome du contenu ne peut avaler.
     *
     * @param pattern        la source du motif
     * @param contenuDebut le début du contenu du groupe
     * @param contenuFin   la fin du contenu du groupe
     * @return vrai si le groupe est délimité
     */
    static boolean delimite(String pattern, int contenuDebut, int contenuFin) {
        int atome = debutAtome(pattern, contenuDebut, contenuFin);
        if (atome < 0) {
            return false;
        }
        int finAtome = finAtome(pattern, atome, contenuFin);
        if (finAtome < 0) {
            return false;
        }
        Alphabet alphabet = new Alphabet(pattern, atome, finAtome);
        return !avale(pattern, finAtome, contenuFin, alphabet);
    }

    /**
     * La position de l'atome initial — après le modificateur de groupe
     * ({@code ?:}, {@code ?<nom>}) ; -1 pour un look-around, sa largeur
     * nulle ne délimite rien.
     */
    private static int debutAtome(String pattern, int contenuDebut, int contenuFin) {
        return apresModificateur(pattern, contenuDebut, contenuFin);
    }

    /**
     * La fin de l'atome à la position donnée, délimiteur obligatoire —
     * ou -1 : échappement de classe large, classe niée ou à intervalles,
     * atome quantifié, métacaractère.
     */
    private static int finAtome(String pattern, int atome, int contenuFin) {
        char c = pattern.charAt(atome);
        int end;
        if (c == '\\') {
            if (atome + 1 >= contenuFin || Character.isLetterOrDigit(pattern.charAt(atome + 1))) {
                return -1;
            }
            end = atome + 2;
        } else if (c == '[') {
            end = pattern.indexOf(']', atome + 1) + 1;
            if (end <= 0 || end > contenuFin || classeEtendue(pattern, atome)) {
                return -1;
            }
        } else if (METAS.indexOf(c) >= 0 || Character.isWhitespace(c)) {
            return -1;
        } else {
            end = atome + 1;
        }
        if (end >= contenuFin || estQuantificateur(pattern.charAt(end))) {
            return -1;
        }
        return end;
    }

    /**
     * La position après le modificateur de groupe ({@code ?:},
     * {@code ?<nom>}, {@code ?'nom'}) ; -1 pour un look-around — sa
     * largeur nulle ne délimite rien.
     */
    private static int apresModificateur(String pattern, int start, int end) {
        if (start >= end || pattern.charAt(start) != '?') {
            return start;
        }
        if (start + 1 >= end) {
            return -1;
        }
        char c = pattern.charAt(start + 1);
        if (lookaround(pattern, start, c)) {
            return -1;
        }
        if (c == '<') {
            return apresNom(pattern, start + 2, end, '>');
        }
        if (c == '\'') {
            return apresNom(pattern, start + 2, end, '\'');
        }
        int i = start + 1;
        while (i < end && pattern.charAt(i) != ':' && pattern.charAt(i) != ')') {
            i++;
        }
        return i + 1;
    }

    /** Vrai pour les look-arounds et le define : largeur nulle, jamais un atome. */
    private static boolean lookaround(String pattern, int start, char c) {
        if (c == '=' || c == '!' || c == '#') {
            return true;
        }
        return c == '<' && start + 2 < pattern.length()
                && (pattern.charAt(start + 2) == '=' || pattern.charAt(start + 2) == '!');
    }

    /** La position après le nom d'un groupe nommé, closeur donné. */
    private static int apresNom(String pattern, int start, int end, char closeur) {
        for (int i = start; i < end; i++) {
            if (pattern.charAt(i) == closeur) {
                return i + 1;
            }
        }
        return -1;
    }

    /** Vrai pour une classe niée, à intervalles ou à échappements. */
    private static boolean classeEtendue(String pattern, int crochets) {
        for (int i = crochets + 1; i < pattern.length() && pattern.charAt(i) != ']'; i++) {
            char c = pattern.charAt(i);
            if (c == '^' || c == '\\' || c == '-') {
                return true;
            }
            if (i + 1 < pattern.length() && pattern.charAt(i + 1) == '-') {
                return true;
            }
        }
        return false;
    }

    /** Vrai si le caractère est un quantificateur (l'atome serait répété ou optionnel). */
    private static boolean estQuantificateur(char c) {
        return c == '*' || c == '+' || c == '?' || c == '{';
    }

    /**
     * Vrai si un atome du contenu après le délimiteur peut avaler un de
     * ses caractères : le point quelconque, un littéral égal, une
     * classe niée, une classe explicite ou large qui le couvre.
     */
    private static boolean avale(String pattern, int depuis, int end, Alphabet alphabet) {
        boolean classe = false;
        boolean negation = false;
        int i = depuis;
        while (i < end) {
            char c = pattern.charAt(i);
            if (c == '\\') {
                if (alphabet.couvreEchappement(pattern.charAt(i + 1))) {
                    return true;
                }
                i += 2;
                continue;
            }
            if (classe) {
                if (c == ']') {
                    classe = false;
                    negation = false;
                } else if (negation || elementAvale(pattern, i, end, alphabet)) {
                    return true;
                }
                i++;
                continue;
            }
            if (c == '[') {
                classe = true;
                negation = i + 1 < end && pattern.charAt(i + 1) == '^';
                if (negation) {
                    i++;
                }
            } else if (c == '.') {
                return true;
            }
            i++;
        }
        return false;
    }

    /** Vrai si l'élément d'une classe (position courante) couvre le délimiteur. */
    private static boolean elementAvale(String pattern, int i, int end, Alphabet alphabet) {
        char c = pattern.charAt(i);
        boolean intervalle = i + 2 < end && pattern.charAt(i + 1) == '-'
                && pattern.charAt(i + 2) != ']';
        if (intervalle) {
            return alphabet.couvreIntervalle(c, pattern.charAt(i + 2));
        }
        return alphabet.contient(c);
    }
}
