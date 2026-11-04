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

    private final CompositionState etat;

    /**
     * Construit la couche de composition.
     *
     * @param brushes la fabrique de pinceaux (la couche prend le sien)
     * @param etat    l'état de composition lu à chaque image
     */
    public ComposingPainter(BrushFactory brushes, CompositionState etat) {
        super(brushes);
        this.etat = etat;
    }

    @Override
    protected int baseColor(FrameState frame) {
        return frame.theme().baseTextColor();
    }

    @Override
    protected void onPaint(DrawSurface surface, FrameState frame) {
        if (!etat.isComposing()) {
            return;
        }
        int debut = etat.composingStart();
        int fin = etat.composingEnd();
        if (fin <= debut) {
            return;
        }
        Document document = frame.document();
        for (int rangee = frame.firstRow();
                rangee < frame.firstRow() + frame.visibleRows(); rangee++) {
            soulignerRangee(surface, frame, document, debut, fin, rangee);
        }
    }

    /** Souligne l'intersection de la plage composée avec SA tranche. */
    private void soulignerRangee(DrawSurface surface, FrameState frame,
            Document document, int debut, int fin, int rangee) {
        VisualProjection projection = frame.projection();
        int ligne = projection.firstLineOfRow(rangee);
        int debutRangee = document.lineStart(ligne);
        int colonneDebut = Math.max(debut, debutRangee) - debutRangee;
        int colonneFin = Math.min(fin, document.lineEnd(ligne)) - debutRangee;
        int debutTranche = projection.spanStartAtRow(rangee);
        colonneDebut = Math.max(colonneDebut, debutTranche);
        colonneFin = Math.min(colonneFin, projection.spanEndAtRow(rangee));
        if (colonneFin <= colonneDebut) {
            return;
        }
        float gauche = frame.gutterWidth()
                + (colonneDebut - debutTranche) * frame.metrics().charWidth()
                - frame.scrollX();
        float droite = frame.gutterWidth()
                + (colonneFin - debutTranche) * frame.metrics().charWidth()
                - frame.scrollX();
        float base = frame.metrics().rowBaseline(rangee) - frame.scrollY();
        surface.drawRect(gauche, base, droite,
                base + frame.metrics().descent(), brush());
    }
}
