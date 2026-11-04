package jo.celeris.android.view;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.core.text.Document;

/**
 * L'arbre d'accessibilité virtuel (G-05) : l'éditeur s'expose comme un
 * nœud éditable dont les enfants virtuels sont les RANGÉES VISIBLES —
 * texte de la tranche et bornes à l'écran. Jamais le document entier
 * ne se sert (G-01) : la fenêtre seule, la demande seule.
 *
 * <p>La construction est pure : la colle Android sert ces rangées
 * comme enfants virtuels de la vue, le lecteur d'écran les lit une à
 * une — « lecture par lignes et plages », le critère G-05 de la
 * matrice.</p>
 */
public final class VirtualRows {

    private VirtualRows() {
    }

    /**
     * Une rangée virtuelle : son numéro, le texte de sa tranche et ses
     * bornes à l'écran.
     *
     * @param row    le numéro de rangée visuelle
     * @param text   le texte de la tranche portée
     * @param left   le bord gauche à l'écran, en pixels
     * @param top    le bord supérieur à l'écran, en pixels
     * @param right  le bord droit à l'écran, en pixels
     * @param bottom le bord inférieur à l'écran, en pixels
     */
    public record Row(int row, String text, float left, float top, float right,
            float bottom) {
    }

    /**
     * Les rangées virtuelles de la fenêtre visible.
     *
     * @param document     l'instantané de document affiché
     * @param projection   la projection rangées ↔ lignes courante
     * @param metrics      les métriques d'éditeur courantes
     * @param firstRow     la première rangée visible
     * @param visibleRows  le nombre de rangées visibles
     * @param gutterWidth  la largeur de gouttière, en pixels
     * @param scrollX      le défilement horizontal, en pixels
     * @param scrollY      le défilement vertical, en pixels
     * @return les rangées visibles, texte et bornes, dans l'ordre
     */
    public static List<Row> visible(Document document,
            VisualProjection projection, EditorMetrics metrics, int firstRow,
            int visibleRows, float gutterWidth, float scrollX, float scrollY) {
        List<Row> rangees = new ArrayList<>(Math.max(0, visibleRows));
        for (int rangee = firstRow;
                rangee < firstRow + visibleRows && rangee < projection.rowCount();
                rangee++) {
            int ligne = projection.firstLineOfRow(rangee);
            int debutLigne = document.lineStart(ligne);
            int debut = projection.spanStartAtRow(rangee);
            int fin = Math.min(projection.spanEndAtRow(rangee),
                    document.lineEnd(ligne) - debutLigne);
            if (fin < debut) {
                fin = debut;
            }
            String texte = document.subText(debutLigne + debut,
                    debutLigne + fin);
            float gauche = gutterWidth - scrollX;
            float haut = metrics.rowTop(rangee) - scrollY;
            rangees.add(new Row(rangee, texte, gauche, haut,
                    gauche + texte.length() * metrics.charWidth(),
                    haut + metrics.lineHeight()));
        }
        return rangees;
    }
}
