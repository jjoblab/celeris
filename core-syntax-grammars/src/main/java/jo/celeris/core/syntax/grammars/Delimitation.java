package jo.celeris.core.syntax.grammars;

/**
 * L'analyse de délimitation d'un groupe (garde catastrophique,
 * ADR-014) : un groupe est délimité quand son contenu commence par un
 * atome obligatoire — littéral échappé, caractère ordinaire ou classe
 * explicite sans intervalle ni négation — qu'aucun autre atome du
 * contenu ne peut avaler. Les itérations se séparent alors sans
 * ambiguïté, le retour arrière reste polynomial.
 */
final class Delimitation {

    /** Les métacaractères hors classe — un délimiteur ne peut pas en être un. */
    private static final String METAS = "\\^$.[]|()*+?{}-";

    private Delimitation() {
    }

    /**
     * Vrai quand le groupe est délimité : son contenu commence par un
     * atome obligatoire qu'aucun autre atome du contenu ne peut avaler.
     *
     * @param motif        la source du motif
     * @param contenuDebut le début du contenu du groupe
     * @param contenuFin   la fin du contenu du groupe
     * @return vrai si le groupe est délimité
     */
    static boolean delimite(String motif, int contenuDebut, int contenuFin) {
        int atome = debutAtome(motif, contenuDebut, contenuFin);
        if (atome < 0) {
            return false;
        }
        int finAtome = finAtome(motif, atome, contenuFin);
        if (finAtome < 0) {
            return false;
        }
        Alphabet alphabet = new Alphabet(motif, atome, finAtome);
        return !avale(motif, finAtome, contenuFin, alphabet);
    }

    /**
     * La position de l'atome initial — après le modificateur de groupe
     * ({@code ?:}, {@code ?<nom>}) ; -1 pour un look-around, sa largeur
     * nulle ne délimite rien.
     */
    private static int debutAtome(String motif, int contenuDebut, int contenuFin) {
        return apresModificateur(motif, contenuDebut, contenuFin);
    }

    /**
     * La fin de l'atome à la position donnée, délimiteur obligatoire —
     * ou -1 : échappement de classe large, classe niée ou à intervalles,
     * atome quantifié, métacaractère.
     */
    private static int finAtome(String motif, int atome, int contenuFin) {
        char c = motif.charAt(atome);
        int fin;
        if (c == '\\') {
            if (atome + 1 >= contenuFin || Character.isLetterOrDigit(motif.charAt(atome + 1))) {
                return -1;
            }
            fin = atome + 2;
        } else if (c == '[') {
            fin = motif.indexOf(']', atome + 1) + 1;
            if (fin <= 0 || fin > contenuFin || classeEtendue(motif, atome)) {
                return -1;
            }
        } else if (METAS.indexOf(c) >= 0 || Character.isWhitespace(c)) {
            return -1;
        } else {
            fin = atome + 1;
        }
        if (fin >= contenuFin || estQuantificateur(motif.charAt(fin))) {
            return -1;
        }
        return fin;
    }

    /**
     * La position après le modificateur de groupe ({@code ?:},
     * {@code ?<nom>}, {@code ?'nom'}) ; -1 pour un look-around — sa
     * largeur nulle ne délimite rien.
     */
    private static int apresModificateur(String motif, int debut, int fin) {
        if (debut >= fin || motif.charAt(debut) != '?') {
            return debut;
        }
        if (debut + 1 >= fin) {
            return -1;
        }
        char c = motif.charAt(debut + 1);
        if (lookaround(motif, debut, c)) {
            return -1;
        }
        if (c == '<') {
            return apresNom(motif, debut + 2, fin, '>');
        }
        if (c == '\'') {
            return apresNom(motif, debut + 2, fin, '\'');
        }
        int i = debut + 1;
        while (i < fin && motif.charAt(i) != ':' && motif.charAt(i) != ')') {
            i++;
        }
        return i + 1;
    }

    /** Vrai pour les look-arounds et le define : largeur nulle, jamais un atome. */
    private static boolean lookaround(String motif, int debut, char c) {
        if (c == '=' || c == '!' || c == '#') {
            return true;
        }
        return c == '<' && debut + 2 < motif.length()
                && (motif.charAt(debut + 2) == '=' || motif.charAt(debut + 2) == '!');
    }

    /** La position après le nom d'un groupe nommé, closeur donné. */
    private static int apresNom(String motif, int debut, int fin, char closeur) {
        for (int i = debut; i < fin; i++) {
            if (motif.charAt(i) == closeur) {
                return i + 1;
            }
        }
        return -1;
    }

    /** Vrai pour une classe niée, à intervalles ou à échappements. */
    private static boolean classeEtendue(String motif, int crochets) {
        for (int i = crochets + 1; i < motif.length() && motif.charAt(i) != ']'; i++) {
            char c = motif.charAt(i);
            if (c == '^' || c == '\\' || c == '-') {
                return true;
            }
            if (i + 1 < motif.length() && motif.charAt(i + 1) == '-') {
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
    private static boolean avale(String motif, int depuis, int fin, Alphabet alphabet) {
        boolean classe = false;
        boolean negation = false;
        int i = depuis;
        while (i < fin) {
            char c = motif.charAt(i);
            if (c == '\\') {
                if (alphabet.couvreEchappement(motif.charAt(i + 1))) {
                    return true;
                }
                i += 2;
                continue;
            }
            if (classe) {
                if (c == ']') {
                    classe = false;
                    negation = false;
                } else if (negation || elementAvale(motif, i, fin, alphabet)) {
                    return true;
                }
                i++;
                continue;
            }
            if (c == '[') {
                classe = true;
                negation = i + 1 < fin && motif.charAt(i + 1) == '^';
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
    private static boolean elementAvale(String motif, int i, int fin, Alphabet alphabet) {
        char c = motif.charAt(i);
        boolean intervalle = i + 2 < fin && motif.charAt(i + 1) == '-'
                && motif.charAt(i + 2) != ']';
        if (intervalle) {
            return alphabet.couvreIntervalle(c, motif.charAt(i + 2));
        }
        return alphabet.contient(c);
    }
}
