package jo.celeris.android.view;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;

/**
 * Couche de chrome de la BANDE DE LIGNE COURANTE (le retour terrain du
 * 2026-11-19 : « la ligne où se trouve le curseur s'illumine avec une
 * couleur de fond ») : la rangée qui porte le caret principal se teinte
 * d'un voile de l'encre de base — un curseur réduit SEUL, jamais pendant
 * une sélection (la sélection parle déjà), et toute la hauteur de la
 * rangée visuelle, retours à la ligne compris.
 *
 * <p>La bande se trace SOUS les guides, la sélection et le texte :
 * elle éclaire, elle ne lit pas. La couleur vit dans le canal alpha —
 * un voile de structure, l'encre du texte reste seule à parler (la
 * même convention que les guides d'indentation).</p>
 */
public final class CurrentLineBand extends PainterLayer {

    /**
     * L'alpha du voile — une lumière discrète : la ligne se voit sans
     * éblouir, le texte reste roi.
     */
    private static final int ALPHA = 0x18;

    /**
     * Construit la couche de la bande de ligne courante.
     *
     * @param brushes la fabrique de pinceaux (la couche prend le sien)
     */
    public CurrentLineBand(BrushFactory brushes) {
        super(brushes);
    }

    @Override
    protected int baseColor(FrameState frame) {
        // le voile de l'encre de base : la bande éclaire la ligne du
        // caret sans la colorer d'une autre encre
        return (frame.theme().baseTextColor() & 0x00FFFFFF) | (ALPHA << 24);
    }

    @Override
    protected void onPaint(DrawSurface surface, FrameState frame) {
        MultiSelection selection = frame.selection();
        java.util.List<SelRange> ranges = selection.ranges();
        if (ranges.size() != 1 || !ranges.get(0).isEmpty()) {
            // un curseur réduit SEUL : la sélection a sa propre bande
            return;
        }
        int caret = ranges.get(0).start();
        Document document = frame.document();
        if (caret > document.length()) {
            return;
        }
        int line = document.lineForOffset(caret);
        VisualProjection projection = frame.projection();
        int rangnee = projection.rowOfLine(line);
        if (rangnee < frame.firstRow()
                || rangnee >= frame.firstRow() + frame.visibleRows()) {
            return;
        }
        EditorMetrics metrics = frame.metrics();
        float haut = metrics.rowTop(rangnee) - frame.scrollY();
        float bas = haut + metrics.lineHeight();
        surface.drawRect(frame.gutterWidth(), haut,
                Math.max(frame.gutterWidth() + 1f, frame.windowWidth()), bas,
                brush());
    }
}
