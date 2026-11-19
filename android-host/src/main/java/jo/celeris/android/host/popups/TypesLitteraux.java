package jo.celeris.android.host.popups;

/**
 * Le type des LITTÉRAUX du moteur des indices en ligne (inlay hints —
 * l'audit Android Studio du 2026-11-14) : les littéraux seuls y
 * tranchent — entier, décimal, suffixes L/f/d, booléens, chaînes,
 * caractères, tableaux (le type de leur premier élément porté en
 * tableau). Jamais le type d'un identifiant, d'un appel ou d'une
 * expression ne se devine : la syntaxe seule décide.
 */
final class TypesLitteraux {

    private TypesLitteraux() {
    }

    /** Vrai si le texte est un identifiant Java bien formé. */
    static boolean estIdentifiant(String text) {
        if (text.isEmpty()
                || !Character.isJavaIdentifierStart(text.charAt(0))) {
            return false;
        }
        for (int i = 1; i < text.length(); i++) {
            if (!Character.isJavaIdentifierPart(text.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /** Vrai si le caractère peut vivre dans un identifiant. */
    static boolean estIdentifiant(char c) {
        return c == '_' || c == '$' || Character.isLetterOrDigit(c);
    }

    /** Le type du LITTÉRAL — null quand il ne se tranchera pas. */
    static String duLitteral(String value) {
        if (value.equals("true") || value.equals("false")) {
            return ": boolean";
        }
        if (value.equals("null")) {
            // l'IDE de référence ne devine pas le néant
            return null;
        }
        if (estChaine(value)) {
            return ": String";
        }
        if (estCaractere(value)) {
            return ": char";
        }
        if (estTableau(value)) {
            return typeTableau(value);
        }
        return typeNumerique(value);
    }

    /** Vrai si la valeur est une chaîne entre guillemets. */
    static boolean estChaine(String value) {
        return value.length() >= 2 && value.startsWith("\"")
                && value.endsWith("\"");
    }

    /** Vrai si la valeur est un caractère entre quotes. */
    static boolean estCaractere(String value) {
        return value.length() >= 3 && value.startsWith("'")
                && value.endsWith("'");
    }

    /** Vrai si la valeur est un initialiseur de tableau. */
    static boolean estTableau(String value) {
        return value.startsWith("{") && value.endsWith("}");
    }

    /** Le type d'un initialiseur de tableau : le type de son PREMIER
     *  élément porté en tableau — « {1, 2} » lit « : int[] », vide ou
     *  hétérogène ne se devine pas. */
    static String typeTableau(String value) {
        String body = value.substring(1, value.length() - 1).strip();
        if (body.isEmpty()) {
            return null;
        }
        String premier = body.split(",")[0].strip();
        String type = duLitteral(premier);
        return type == null ? null : type + "[]";
    }

    /**
     * Le type du littéral NUMÉRIQUE — int, long, float, double selon
     * la forme et le suffixe ; null quand la valeur n'est pas littérale
     * (identifiant, appel, hexadécimal — jamais deviné).
     */
    static String typeNumerique(String value) {
        char dernier = value.charAt(value.length() - 1);
        String sansSuffix = value;
        if ("fFdDlL".indexOf(dernier) >= 0) {
            sansSuffix = value.substring(0, value.length() - 1);
        }
        boolean entier = sansSuffix.matches("\\d+");
        boolean decimal = sansSuffix.matches("\\d*\\.\\d+");
        if (!entier && !decimal) {
            // identifiant, appel, expression — jamais deviné
            return null;
        }
        if (decimal) {
            return (dernier == 'f' || dernier == 'F') ? ": float"
                    : ": double";
        }
        if (dernier == 'f' || dernier == 'F') {
            return ": float";
        }
        if (dernier == 'd' || dernier == 'D') {
            return ": double";
        }
        if (dernier == 'l' || dernier == 'L') {
            return ": long";
        }
        return ": int";
    }
}
