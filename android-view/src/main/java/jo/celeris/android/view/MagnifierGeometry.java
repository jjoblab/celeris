package jo.celeris.android.view;

/**
 * Géométrie de la loupe du dragage (D-16 — la source unique des bornes,
 * la sémantique du dépôt de départ) : la bulle ne sort JAMAIS de la
 * fenêtre — AU-DESSUS du doigt si la place y suffit, SINON sous le
 * doigt, SINON bornée dans ce qui reste de la vue.
 *
 * <p>Le centre x suit le doigt, retenu aux bords de la fenêtre ; une
 * vue plus étroite que la bulle prend son centre. Pure et sans
 * appareil : l'état (amortissement, snap) vit dans {@link Loupe}, le
 * tracé y photographie la frame.</p>
 */
public final class MagnifierGeometry {

    /** L'écart vertical de la bulle au doigt, en rayons (le dépôt de départ : 1,8). */
    private static final float ECART_RAYONS = 1.8f;

    private MagnifierGeometry() {
    }

    /**
     * L'abscisse du centre de la loupe : le doigt la guide, les bords de
     * la fenêtre la retiennent — la bulle ne sort JAMAIS des bords
     * horizontaux ; une fenêtre plus étroite que la bulle prend son
     * centre.
     *
     * @param touchX        l'abscisse du doigt, en pixels
     * @param viewportWidth la largeur de la fenêtre, en pixels
     * @param diameter      le diamètre de la loupe, en pixels
     * @return l'abscisse du centre de la loupe, bornée à la fenêtre
     */
    public static float centerX(float touchX, float viewportWidth,
            float diameter) {
        float demi = diameter / 2f;
        if (viewportWidth <= diameter) {
            return viewportWidth / 2f;
        }
        return Math.max(demi, Math.min(touchX, viewportWidth - demi));
    }

    /**
     * L'ordonnée du centre de la loupe : AU-DESSUS du doigt si la place
     * y suffit (1,8 rayon au-dessus), SINON sous le doigt si la place y
     * suffit, SINON bornée dans ce qui reste de la vue — une vue plus
     * petite que la bulle prend son centre. La bulle reste entièrement
     * visible quel que soit le bord approché (le dépôt de départ :
     * {@code resolveMagnifierCy}).
     *
     * @param fingerY      l'ordonnée du doigt, en pixels
     * @param viewHeight   la hauteur de la vue, en pixels
     * @param radius       le rayon de la bulle, en pixels
     * @return l'ordonnée du centre de la bulle, jamais coupée
     */
    public static float resoudreCy(float fingerY, float viewHeight,
            float radius) {
        if (viewHeight <= 2f * radius) {
            return viewHeight / 2f;
        }
        float auDessus = fingerY - radius * ECART_RAYONS;
        if (auDessus >= radius) {
            return auDessus;
        }
        float auDessous = fingerY + radius * ECART_RAYONS;
        if (auDessous <= viewHeight - radius) {
            return auDessous;
        }
        return Math.min(viewHeight - radius, Math.max(radius, fingerY));
    }
}
