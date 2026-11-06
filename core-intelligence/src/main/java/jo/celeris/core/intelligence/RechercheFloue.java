package jo.celeris.core.intelligence;

/**
 * La recherche floue de symboles (B-13) : l'aller à symbole hors ligne
 * tape quelques lettres, l'index répond dans l'ordre — identique,
 * préfixe, début de mot interne, sous-chaîne, sous-suite —
 * insensible à la casse, jamais au-delà.
 */
public final class RechercheFloue {

    /** L'identique, insensible à la casse. */
    public static final int SCORE_IDENTIQUE = 100;

    /** La requête ouvre le nom. */
    public static final int SCORE_PREFIXE = 80;

    /** La requête ouvre un mot interne — camelCase ou après un souligné. */
    public static final int SCORE_DEBUT_MOT = 60;

    /** La requête vit à l'intérieur d'un mot. */
    public static final int SCORE_SOUS_CHAINE = 40;

    /** Les caractères de la requête, dans l'ordre, pas forcément voisins. */
    public static final int SCORE_SOUS_SUITE = 20;

    private RechercheFloue() {
    }

    /**
     * Vrai quand le nom répond à la requête.
     *
     * @param nom     le nom candidat
     * @param requete les lettres tapées, vide correspond à tout
     * @return vrai si le nom mérite d'être montré
     */
    public static boolean correspond(String nom, String requete) {
        return score(nom, requete) >= 0;
    }

    /**
     * Le score du nom pour la requête — mieux vaut plus haut.
     *
     * @param nom     le nom candidat
     * @param requete les lettres tapées
     * @return le score, -1 quand le nom ne répond pas, 0 pour la
     *         requête muette
     */
    public static int score(String nom, String requete) {
        if (requete.isEmpty()) {
            return 0;
        }
        String n = nom.toLowerCase(java.util.Locale.ROOT);
        String r = requete.toLowerCase(java.util.Locale.ROOT);
        if (n.length() < r.length()) {
            return -1;
        }
        if (n.equals(r)) {
            return SCORE_IDENTIQUE;
        }
        int position = n.indexOf(r);
        if (position == 0) {
            return SCORE_PREFIXE;
        }
        if (position > 0) {
            return debutDeMot(nom, position) ? SCORE_DEBUT_MOT : SCORE_SOUS_CHAINE;
        }
        return sousSuite(n, r) ? SCORE_SOUS_SUITE : -1;
    }

    /** Vrai si la position ouvre un mot : début, après un souligné, ou majuscule après minuscule. */
    private static boolean debutDeMot(String nom, int position) {
        if (position == 0) {
            return true;
        }
        char avant = nom.charAt(position - 1);
        char ici = nom.charAt(position);
        return avant == '_' || avant == '$'
                || (Character.isLowerCase(avant) && Character.isUpperCase(ici));
    }

    /** Vrai si les caractères de la requête vivent dans le nom, dans l'ordre. */
    private static boolean sousSuite(String nom, String requete) {
        int curseur = 0;
        for (int i = 0; i < requete.length(); i++) {
            char cherche = requete.charAt(i);
            int trouve = nom.indexOf(cherche, curseur);
            if (trouve < 0) {
                return false;
            }
            curseur = trouve + 1;
        }
        return true;
    }
}
