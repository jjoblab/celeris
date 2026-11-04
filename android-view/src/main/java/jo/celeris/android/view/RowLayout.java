package jo.celeris.android.view;

import java.util.List;

/**
 * Mise en page d'une rangée visuelle : la tranche d'une ligne du document
 * que la rangée porte, avec sa largeur rendue et ses segments visuels
 * bidirectionnels (ADR-008).
 *
 * <p>Sans retour à la ligne ni pli, une rangée porte une ligne entière
 * (début 0, fin la longueur de la ligne) ; le retour à la ligne souple
 * par offsets (A-18, variante code) et les plis affinent la projection
 * — la rangée reste la seule unité de rendu et de cache. Les segments
 * vidés signalent une rangée purement gauche-à-droite : le chemin
 * rapide du peintre, un seul tracé.</p>
 *
 * @param line  le numéro de la première ligne du document portée
 * @param start le début de la tranche dans la ligne (colonne)
 * @param end   la fin exclusive de la tranche dans la ligne (colonne)
 * @param text  le texte tracé par la rangée
 * @param width la largeur rendue du texte, en pixels
 * @param runs  les segments visuels de gauche à droite, vides si la
 *              rangée est purement gauche-à-droite
 */
public record RowLayout(int line, int start, int end, String text, float width,
        List<VisualRuns.Run> runs) {

    /** Valide la mise en page. */
    public RowLayout {
        if (line < 0) {
            throw new IllegalArgumentException("negative line: " + line);
        }
        if (start < 0 || end < start) {
            throw new IllegalArgumentException(
                    "invalid span: start=" + start + ", end=" + end);
        }
        if (runs == null) {
            throw new IllegalArgumentException("null runs");
        }
    }

    /**
     * Une rangée sans directionnalité : les segments vidés signalent le
     * chemin rapide du peintre.
     *
     * @param line  le numéro de la première ligne du document portée
     * @param start le début de la tranche dans la ligne (colonne)
     * @param end   la fin exclusive de la tranche dans la ligne (colonne)
     * @param text  le texte tracé par la rangée
     * @param width la largeur rendue du texte, en pixels
     */
    public RowLayout(int line, int start, int end, String text, float width) {
        this(line, start, end, text, width, List.of());
    }
}
