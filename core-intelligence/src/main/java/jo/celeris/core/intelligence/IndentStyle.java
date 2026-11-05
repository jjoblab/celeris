package jo.celeris.core.intelligence;

/**
 * Les options de style d'indentation (M5, A-06) : largeur du cran et
 * nature du remplissage. Le style ne dérive jamais du document — il
 * appartient à l'hôte de le choisir, la bibliothèque le respecte.
 *
 * @param width la largeur du cran, en colonnes (une tabulation compte
 *              pour {@code width} colonnes)
 * @param tabs  vrai pour tabuler, faux pour des espaces
 */
public record IndentStyle(int width, boolean tabs) {

    /** Le style canonique : quatre espaces. */
    public static final IndentStyle ESPACES_4 = new IndentStyle(4, false);

    /**
     * Un style à espaces.
     *
     * @param width la largeur du cran, au moins 1
     * @return le style
     * @throws IllegalArgumentException si {@code width < 1}
     */
    public static IndentStyle spaces(int width) {
        return new IndentStyle(width, false);
    }

    /**
     * Un style à tabulations.
     *
     * @param width la largeur visée du cran, au moins 1
     * @return le style
     * @throws IllegalArgumentException si {@code width < 1}
     */
    public static IndentStyle tabs(int width) {
        return new IndentStyle(width, true);
    }

    /**
     * Constructeur canonique : la largeur est strictement positive.
     */
    public IndentStyle {
        if (width < 1) {
            throw new IllegalArgumentException("width must be >= 1: " + width);
        }
    }

    /**
     * La chaîne d'indentation d'un niveau.
     *
     * @param level le niveau, ramené à zéro s'il est négatif
     * @return les blancs du niveau
     */
    public String indent(int level) {
        int borne = Math.max(0, level);
        if (borne == 0) {
            return "";
        }
        return tabs ? "\t".repeat(borne) : " ".repeat(borne * width);
    }
}
