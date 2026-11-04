package jo.celeris.android.view;

import android.graphics.Paint;

/**
 * Pinceau de texte sur peinture Android : l'adaptateur concret de
 * {@link TextBrush}.
 *
 * <p>La peinture est possédée par la couche via sa fabrique (PIEGE-01) ;
 * la surface de dessin y accède par le package, jamais par un transtypage
 * public.</p>
 */
public final class PaintBrush implements TextBrush {

    private final Paint peinture;

    /**
     * Construit le pinceau sur la peinture donnée.
     *
     * @param peinture la peinture Android sous-jacente
     */
    public PaintBrush(Paint peinture) {
        this.peinture = peinture;
    }

    /** La peinture Android sous-jacente (accès package). */
    Paint peinture() {
        return peinture;
    }

    @Override
    public void setColor(int argb) {
        peinture.setColor(argb);
    }

    @Override
    public int color() {
        return peinture.getColor();
    }
}
