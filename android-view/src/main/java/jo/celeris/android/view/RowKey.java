package jo.celeris.android.view;

/**
 * Clé du cache de mises en page par rangée.
 *
 * <p>PIEGE-02 : la clé ne signe que des valeurs immuables — la révision
 * de l'instantané, le numéro de rangée, l'identifiant de thème et la
 * spécification de police (ADR-006). Jamais l'état mutable d'un objet
 * partagé (la couleur d'un pinceau, par exemple) n'y entre : deux
 * contenus distincts ne peuvent pas partager une entrée.</p>
 *
 * @param revision la révision de l'instantané du document
 * @param row      le numéro de rangée visuelle
 * @param themeId  l'identifiant du thème
 * @param font     la spécification de police
 */
public record RowKey(long revision, int row, int themeId, FontSpec font) {

    /** Valide la clé. */
    public RowKey {
        if (row < 0) {
            throw new IllegalArgumentException("negative row: " + row);
        }
    }
}
