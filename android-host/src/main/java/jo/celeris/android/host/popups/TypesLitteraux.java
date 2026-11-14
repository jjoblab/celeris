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
    static boolean estIdentifiant(String texte) {
        if (texte.isEmpty()
                || !Character.isJavaIdentifierStart(texte.charAt(0))) {
            return false;
        }
        for (int i = 1; i < texte.length(); i++) {
            if (!Character.isJavaIdentifierPart(texte.charAt(i))) {
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
    static String duLitteral(String valeur) {
        if (valeur.equals("true") || valeur.equals("false")) {
            return ": boolean";
        }
        if (valeur.equals("null")) {
            // l'IDE de référence ne devine pas le néant
            return null;
        }
        if (estChaine(valeur)) {
            return ": String";
        }
        if (estCaractere(valeur)) {
            return ": char";
        }
        if (estTableau(valeur)) {
            return typeTableau(valeur);
        }
        return typeNumerique(valeur);
    }

    /** Vrai si la valeur est une chaîne entre guillemets. */
    static boolean estChaine(String valeur) {
        return valeur.length() >= 2 && valeur.startsWith("\"")
                && valeur.endsWith("\"");
    }

    /** Vrai si la valeur est un caractère entre quotes. */
    static boolean estCaractere(String valeur) {
        return valeur.length() >= 3 && valeur.startsWith("'")
                && valeur.endsWith("'");
    }

    /** Vrai si la valeur est un initialiseur de tableau. */
    static boolean estTableau(String valeur) {
        return valeur.startsWith("{") && valeur.endsWith("}");
    }

    /** Le type d'un initialiseur de tableau : le type de son PREMIER
     *  élément porté en tableau — « {1, 2} » lit « : int[] », vide ou
     *  hétérogène ne se devine pas. */
    static String typeTableau(String valeur) {
        String corps = valeur.substring(1, valeur.length() - 1).strip();
        if (corps.isEmpty()) {
            return null;
        }
        String premier = corps.split(",")[0].strip();
        String type = duLitteral(premier);
        return type == null ? null : type + "[]";
    }

    /**
     * Le type du littéral NUMÉRIQUE — int, long, float, double selon
     * la forme et le suffixe ; null quand la valeur n'est pas littérale
     * (identifiant, appel, hexadécimal — jamais deviné).
     */
    static String typeNumerique(String valeur) {
        char dernier = valeur.charAt(valeur.length() - 1);
        String sansSuffix = valeur;
        if ("fFdDlL".indexOf(dernier) >= 0) {
            sansSuffix = valeur.substring(0, valeur.length() - 1);
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
