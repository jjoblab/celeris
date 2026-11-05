package jo.celeris.android.view;

import java.util.List;

/**
 * Mise en page d'une rangée visuelle : la tranche d'une ligne du document
 * que la rangée porte, avec sa largeur rendue, ses segments visuels
 * bidirectionnels (ADR-008) et ses plages colorées (B-01).
 *
 * <p>Sans retour à la ligne ni pli, une rangée porte une ligne entière
 * (début 0, fin la longueur de la ligne) ; le retour à la ligne souple
 * par offsets (A-18, variante code) et les plis affinent la projection
 * — la rangée reste la seule unité de rendu et de cache. Les segments
 * vidés signalent une rangée purement gauche-à-droite : le chemin
 * rapide du peintre, un seul tracé ; les plages vidées signalent une
 * rangée sans coloration — l'encre de base seule.</p>
 *
 * @param line  le numéro de la première ligne du document portée
 * @param start le début de la tranche dans la ligne (colonne)
 * @param end   la fin exclusive de la tranche dans la ligne (colonne)
 * @param text  le texte tracé par la rangée
 * @param width la largeur rendue du texte, en pixels
 * @param runs  les segments visuels de gauche à droite, vides si la
 *              rangée est purement gauche-à-droite
 * @param spans les plages colorées relatives au texte, couvrantes et
 *              sans trou, vides si la rangée reste à l'encre de base
 */
public record RowLayout(int line, int start, int end, String text, float width,
        List<VisualRuns.Run> runs, List<ColoredSpan> spans) {

    /** Valide la mise en page. */
    public RowLayout {
        if (line < 0) {
            throw new IllegalArgumentException("negative line: " + line);
        }
        if (start < 0 || end < start) {
            throw new IllegalArgumentException(
                    "invalid span: start=" + start + ", end=" + end);
        }
        if (runs == null || spans == null) {
            throw new IllegalArgumentException("null runs or spans");
        }
    }

    /**
     * Une rangée sans directionnalité ni coloration : les segments et
     * les plages vidés signalent les chemins rapides du peintre.
     *
     * @param line  le numéro de la première ligne du document portée
     * @param start le début de la tranche dans la ligne (colonne)
     * @param end   la fin exclusive de la tranche dans la ligne (colonne)
     * @param text  le texte tracé par la rangée
     * @param width la largeur rendue du texte, en pixels
     */
    public RowLayout(int line, int start, int end, String text, float width) {
        this(line, start, end, text, width, List.of(), List.of());
    }

    /**
     * Une rangée sans coloration : les plages vidées signalent l'encre
     * de base seule.
     *
     * @param line  le numéro de la première ligne du document portée
     * @param start le début de la tranche dans la ligne (colonne)
     * @param end   la fin exclusive de la tranche dans la ligne (colonne)
     * @param text  le texte tracé par la rangée
     * @param width la largeur rendue du texte, en pixels
     * @param runs  les segments visuels de gauche à droite, vides si la
     *              rangée est purement gauche-à-droite
     */
    public RowLayout(int line, int start, int end, String text, float width,
            List<VisualRuns.Run> runs) {
        this(line, start, end, text, width, runs, List.of());
    }
}
