package jo.celeris.android.view;

/**
 * Clé du cache de mises en page par rangée.
 *
 * <p>PIEGE-02 : la clé ne signe que des valeurs immuables — la révision
 * de l'instantané, le numéro de rangée, l'identifiant de thème, la
 * spécification de police et le budget de colonnes du retour à la ligne
 * (ADR-006). Jamais l'état mutable d'un objet partagé (la couleur d'un
 * pinceau, par exemple) n'y entre : deux contenus distincts ne peuvent
 * pas partager une entrée — ni deux budgets de coupure, la même rangée
 * sous un autre budget est une autre tranche.</p>
 *
 * @param revision la révision de l'instantané du document
 * @param row      le numéro de rangée visuelle
 * @param themeId  l'identifiant du thème
 * @param font     la spécification de police
 * @param columns  le budget de colonnes du retour à la ligne (0 sans)
 */
public record RowKey(long revision, int row, int themeId, FontSpec font, int columns) {

    /** Valide la clé. */
    public RowKey {
        if (row < 0) {
            throw new IllegalArgumentException("negative row: " + row);
        }
        if (columns < 0) {
            throw new IllegalArgumentException("negative columns: " + columns);
        }
    }

    /**
     * Clé sans retour à la ligne : budget nul, la rangée porte la ligne
     * entière.
     *
     * @param revision la révision de l'instantané du document
     * @param row      le numéro de rangée visuelle
     * @param themeId  l'identifiant du thème
     * @param font     la spécification de police
     */
    public RowKey(long revision, int row, int themeId, FontSpec font) {
        this(revision, row, themeId, font, 0);
    }
}
