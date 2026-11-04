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
            float y = baseY + (rangee - premiere) * hauteur;
            if (layout.runs().isEmpty()) {
                // Chemin rapide : une rangée purement gauche-à-droite
                // se trace d'un seul tenant, l'instance du cache.
                surface.drawText(layout.text(), bordGauche, y, brush());
            } else {
                tracerSegments(surface, layout, bordGauche, y,
                        metrics.charWidth());
            }
        }
    }

    /**
     * Trace la rangée segment par segment, de gauche à droite dans
     * l'ordre visuel (ADR-008) : le texte logique ne bouge jamais, la
     * position de tracé avance — aucune allocation, la variante à
     * bornes de la surface sert le segment tel quel.
     */
    private void tracerSegments(DrawSurface surface, RowLayout layout,
            float bordGauche, float y, float largeurCar) {
        String texte = layout.text();
        float x = bordGauche;
        for (VisualRuns.Run segment : layout.runs()) {
            surface.drawText(texte, segment.start(), segment.end(), x, y, brush());
            x += (segment.end() - segment.start()) * largeurCar;
        }
    }
}
