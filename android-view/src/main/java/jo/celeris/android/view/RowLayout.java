package jo.celeris.android.view;

/**
 * Mise en page d'une rangée visuelle : la tranche d'une ligne du document
 * que la rangée porte, avec sa largeur rendue.
 *
 * <p>Sans retour à la ligne ni pli, une rangée porte une ligne entière
 * (début 0, fin la longueur de la ligne) ; le retour à la ligne souple
 * par offsets (A-18, variante code) et les plis affinent la projection
 * — la rangée reste la seule unité de rendu et de cache.</p>
 *
 * @param line  le numéro de la première ligne du document portée
 * @param start le début de la tranche dans la ligne (colonne)
 * @param end   la fin exclusive de la tranche dans la ligne (colonne)
 * @param text  le texte tracé par la rangée
 * @param width la largeur rendue du texte, en pixels
 */
public record RowLayout(int line, int start, int end, String text, float width) {

    /** Valide la mise en page. */
    public RowLayout {
        if (line < 0) {
            throw new IllegalArgumentException("negative line: " + line);
        }
        if (start < 0 || end < start) {
            throw new IllegalArgumentException(
                    "invalid span: start=" + start + ", end=" + end);
        }
    }
}
