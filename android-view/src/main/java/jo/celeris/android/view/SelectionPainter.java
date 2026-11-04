package jo.celeris.android.view;

import java.util.List;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.SelRange;

/**
 * Couche de chrome de sélection : trace le fond des plages sélectionnées
 * pour chaque rangée visible.
 *
 * <p>La conversion rangée → ligne passe par la projection (PIEGE-04) ;
 * l'intersection plage ↔ rangée se calcule en primitives — aucune
 * allocation dans la boucle (PIEGE-15).</p>
 */
public final class SelectionPainter extends PainterLayer {

    /**
     * Construit la couche de sélection.
     *
     * @param brushes la fabrique de pinceaux (la couche prend le sien)
     */
    public SelectionPainter(BrushFactory brushes) {
        super(brushes);
    }

    @Override
    protected int baseColor(FrameState frame) {
        return frame.theme().selectionColor();
    }

    @Override
    protected void onPaint(DrawSurface surface, FrameState frame) {
        List<SelRange> plages = frame.selection().ranges();
        for (int i = 0; i < plages.size(); i++) {
            peindrePlage(surface, frame, plages.get(i));
        }
    }

    private void peindrePlage(DrawSurface surface, FrameState frame, SelRange plage) {
        if (plage.isEmpty()) {
            return;
        }
        Document document = frame.document();
        EditorMetrics metrics = frame.metrics();
        int premiere = frame.firstRow();
        int derniere = premiere + frame.visibleRows();
        for (int rangee = premiere; rangee < derniere; rangee++) {
            int ligne = frame.projection().firstLineOfRow(rangee);
            int debutRangee = document.lineStart(ligne);
            int finRangee = document.lineEnd(ligne);
            int debut = Math.max(plage.start(), debutRangee);
            int fin = Math.min(plage.end(), finRangee);
            if (fin <= debut) {
                continue;
            }
            float gauche = (debut - debutRangee) * metrics.charWidth() - frame.scrollX();
            float haut = metrics.rowTop(rangee) - frame.scrollY();
            float droite = (fin - debutRangee) * metrics.charWidth() - frame.scrollX();
            float bas = haut + metrics.lineHeight();
            surface.drawRect(gauche, haut, droite, bas, brush());
        }
    }
}
