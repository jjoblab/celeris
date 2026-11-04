package jo.celeris.android.view;

/**
 * Géométrie de la loupe (tranche tactile) : la loupe flotte
 * AU-DESSUS du doigt qui glisse, à distance fixe, et son centre reste
 * dans la fenêtre — le contenu agrandi se trace sur l'appareil.
 */
public final class MagnifierGeometry {

    private MagnifierGeometry() {
    }

    /**
     * L'ordonnée du centre de la loupe : trois quarts de diamètre
     * au-dessus du doigt — jamais dessous, le doigt ne se cache jamais.
     *
     * @param touchY   l'ordonnée du doigt, en pixels
     * @param diameter le diamètre de la loupe, en pixels
     * @return l'ordonnée du centre de la loupe
     */
    public static float centerY(float touchY, float diameter) {
        return touchY - 3f * diameter / 4f;
    }

    /**
     * L'abscisse du centre de la loupe : le doigt la guide, les bords
     * de la fenêtre la retiennent.
     *
     * @param touchX        l'abscisse du doigt, en pixels
     * @param viewportWidth la largeur de la fenêtre, en pixels
     * @param diameter      le diamètre de la loupe, en pixels
     * @return l'abscisse du centre de la loupe, bornée à la fenêtre
     */
    public static float centerX(float touchX, float viewportWidth, float diameter) {
        float demi = diameter / 2f;
        return Math.max(demi, Math.min(touchX, viewportWidth - demi));
    }
}
