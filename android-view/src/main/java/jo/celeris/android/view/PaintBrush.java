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

    private final Paint paint;
    private final AndroidFontRuler regle;

    /**
     * Construit le pinceau sur la peinture donnée.
     *
     * @param paint la peinture Android sous-jacente
     * @param regle    la règle porteuse de la table des familles
     *                 (PIEGE-22 : le tracé lit la même table que la
     *                 mesure)
     */
    public PaintBrush(Paint paint, AndroidFontRuler regle) {
        this.paint = paint;
        this.regle = regle;
    }

    /** La peinture Android sous-jacente (accès package). */
    Paint paint() {
        return paint;
    }

    @Override
    public void setColor(int argb) {
        paint.setColor(argb);
    }

    @Override
    public int color() {
        return paint.getColor();
    }

    /**
     * Pose la police du tracé (PIEGE-22) : la même table de familles
     * que la règle de mesure — « mono » rend la police à chasse fixe,
     * la famille posée par l'hôte rend sa police (D-14) — à la
     * taille exacte des métriques, en pixels : ce que la règle a
     * mesuré est ce que la plateforme trace.
     */
    @Override
    public void setFont(FontSpec font) {
        paint.setTypeface(regle.policeDe(font.family()));
        paint.setTextSize(font.size());
    }

    /** L'épaisseur des traits — les chevrons de la gouttière (M7.5). */
    @Override
    public void setStrokeWidth(float pixels) {
        paint.setStrokeWidth(Math.max(1f, pixels));
    }

    /**
     * Les attributs d'une plage à pile de portées (H-07) : la graisse
     * et le soulignement par drapeaux, l'italique par l'inclinaison
     * du tracé — tout se repose à chaque plage, rien ne se propage.
     */
    @Override
    public void setStyle(boolean bold, boolean italic, boolean underline) {
        paint.setFakeBoldText(bold);
        paint.setTextSkewX(italic ? -0.25f : 0f);
        paint.setUnderlineText(underline);
    }
}
