package jo.celeris.android.view;

/**
 * Couche de gouttière : les numéros de lignes, alignés à droite dans le
 * champ de la gouttière (D-01 — numéros absolus).
 *
 * <p>La gouttière défile verticalement avec le contenu (chaque numéro
 * suit sa rangée) mais jamais horizontalement : son abscisse ne dépend
 * pas du défilement horizontal. Les numéros passent par la projection
 * (PIEGE-04) — la rangée d'un pli porte le numéro de son en-tête — et
 * les étiquettes mémoïsées n'allouent rien en régime établi
 * (PIEGE-15).</p>
 */
public final class GutterPainter extends PainterLayer {

    /** La respiration par défaut de chaque côté du champ de numéros, en pixels. */
    public static final float MARGE_PAR_DEFAUT = 8f;

    private final GutterNumbers numeros = new GutterNumbers();
    private final float marge;

    /**
     * Construit la couche de gouttière à respiration par défaut.
     *
     * @param brushes la fabrique de pinceaux (la couche prend le sien)
     */
    public GutterPainter(BrushFactory brushes) {
        this(brushes, MARGE_PAR_DEFAUT);
    }

    /**
     * Construit la couche de gouttière à respiration donnée.
     *
     * @param brushes la fabrique de pinceaux (la couche prend le sien)
     * @param padding la respiration de chaque côté du champ, en pixels
     */
    public GutterPainter(BrushFactory brushes, float padding) {
        super(brushes);
        this.marge = padding;
    }

    @Override
    protected int baseColor(FrameState frame) {
        return frame.theme().gutterTextColor();
    }

    @Override
    protected void onPaint(DrawSurface surface, FrameState frame) {
        EditorMetrics metrics = frame.metrics();
        float charWidth = metrics.charWidth();
        float largeurGouttiere = frame.gutterWidth();
        int premiere = frame.firstRow();
        int derniere = premiere + frame.visibleRows();
        float baseY = metrics.rowBaseline(premiere) - frame.scrollY();
        float hauteur = metrics.lineHeight();
        for (int rangee = premiere; rangee < derniere; rangee++) {
            int numero = frame.projection().firstLineOfRow(rangee) + 1;
            String etiquette = numeros.label(numero);
            float x = GutterGeometry.numberX(etiquette.length(), largeurGouttiere,
                    charWidth, marge);
            float y = baseY + (rangee - premiere) * hauteur;
            surface.drawText(etiquette, x, y, brush());
        }
    }
}
