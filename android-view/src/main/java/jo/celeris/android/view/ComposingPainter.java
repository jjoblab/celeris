package jo.celeris.android.view;

import jo.celeris.core.text.Document;

/**
 * Couche de composition IME : souligne la plage composée, rangée par
 * rangée, dans la bande de descente de chaque rangée.
 *
 * <p>PIEGE-04 : le soulignement suit les tranches — il ne déborde
 * jamais de sa rangée, ni à gauche ni à droite de la coupure (A-18).
 * L'état de composition appartient à la vue, la couche le lit à
 * l'image : sans composition active ni plage non vide, rien ne se
 * trace.</p>
 */
public final class ComposingPainter extends PainterLayer {

    private final CompositionState state;

    /**
     * Construit la couche de composition.
     *
     * @param brushes la fabrique de pinceaux (la couche prend le sien)
     * @param state    l'état de composition lu à chaque image
     */
    public ComposingPainter(BrushFactory brushes, CompositionState state) {
        super(brushes);
        this.state = state;
    }

    @Override
    protected int baseColor(FrameState frame) {
        return frame.theme().baseTextColor();
    }

    @Override
    protected void onPaint(DrawSurface surface, FrameState frame) {
        if (!state.isComposing()) {
            return;
        }
        int start = state.composingStart();
        int end = state.composingEnd();
        if (end <= start) {
            return;
        }
        Document document = frame.document();
        for (int rangee = frame.firstRow();
                rangee < frame.firstRow() + frame.visibleRows(); rangee++) {
            underlineRow(surface, frame, document, start, end, rangee);
        }
    }

    /** Souligne l'intersection de la plage composée avec SA tranche. */
    private void underlineRow(DrawSurface surface, FrameState frame,
            Document document, int start, int end, int rangee) {
        VisualProjection projection = frame.projection();
        int line = projection.firstLineOfRow(rangee);
        int rowStart = document.lineStart(line);
        int colonneDebut = Math.max(start, rowStart) - rowStart;
        int colonneFin = Math.min(end, document.lineEnd(line)) - rowStart;
        int debutTranche = projection.spanStartAtRow(rangee);
        // B-05 : la rangée de suite démarre au retrait de sa ligne.
        float indent = projection.indentAtRow(rangee)
                * frame.metrics().charWidth();
        colonneDebut = Math.max(colonneDebut, debutTranche);
        colonneFin = Math.min(colonneFin, projection.spanEndAtRow(rangee));
        if (colonneFin <= colonneDebut) {
            return;
        }
        float gauche = frame.gutterWidth()
                + indent + (colonneDebut - debutTranche) * frame.metrics().charWidth()
                - frame.scrollX();
        float droite = frame.gutterWidth()
                + indent + (colonneFin - debutTranche) * frame.metrics().charWidth()
                - frame.scrollX();
        float base = frame.metrics().rowBaseline(rangee) - frame.scrollY();
        surface.drawRect(gauche, base, droite,
                base + frame.metrics().descent(), brush());
    }
}
