package jo.celeris.android.view;

/**
 * Couche de texte : trace les rangées visibles, mises en page servies
 * par le cache.
 *
 * <p>En régime établi (PIEGE-15), chaque rangée visible sort du cache
 * sans allocation — la source n'est consultée qu'au premier affichage
 * d'une rangée, et le dépôt qui s'ensuit est le seul moment
 * d'allocation du chemin. Les textes tracés sont les instances mêmes du
 * cache : la preuve d'identité du piège s'y appuie.</p>
 */
public final class TextPainter extends PainterLayer {

    private final RowLayoutCache cache;
    private final RowSource source;

    /**
     * Construit la couche de texte.
     *
     * @param brushes la fabrique de pinceaux (la couche prend le sien)
     * @param cache   le cache de mises en page par rangée
     * @param source  la source des rangées manquantes au cache
     */
    public TextPainter(BrushFactory brushes, RowLayoutCache cache, RowSource source) {
        super(brushes);
        this.cache = cache;
        this.source = source;
    }

    @Override
    protected int baseColor(FrameState frame) {
        return frame.theme().baseTextColor();
    }

    @Override
    protected void onPaint(DrawSurface surface, FrameState frame) {
        EditorMetrics metrics = frame.metrics();
        FontSpec police = metrics.font();
        int theme = frame.theme().id();
        long revision = frame.revision();
        int colonnes = frame.projection().columns();
        int premiere = frame.firstRow();
        int derniere = premiere + frame.visibleRows();
        float baseY = metrics.rowBaseline(premiere) - frame.scrollY();
        float hauteur = metrics.lineHeight();
        float bordGauche = frame.gutterWidth() - frame.scrollX();
        for (int rangee = premiere; rangee < derniere; rangee++) {
            RowLayout layout = cache.find(revision, rangee, theme, police, colonnes);
            if (layout == null) {
                RowKey cle = new RowKey(revision, rangee, theme, police, colonnes);
                layout = source.layoutOf(cle);
                cache.store(cle, layout);
            }
            surface.drawText(layout.text(), bordGauche,
                    baseY + (rangee - premiere) * hauteur, brush());
        }
    }
}
