package jo.celeris.android.view;

/**
 * Pilule « ajouter un curseur » (A-01, tactile) : l'appui long l'arme à
 * la position du doigt ; tant qu'elle est armée, chaque appui ailleurs
 * ajoute un curseur à la position visée, l'appui sur la pilule la
 * désarme.
 *
 * <p>Pure et testable sans appareil : la pilule ne porte que sa
 * géométrie et son état — la mutation de la sélection vit chez
 * l'hôte.</p>
 */
public final class CursorPill {

    /** La demi-largeur de la pilule, en pixels. */
    public static final float HALF_WIDTH = 36f;

    /** La hauteur de la pilule, en pixels. */
    public static final float HEIGHT = 36f;

    /** La prise sous le doigt, en pixels. */
    public static final float GRIP_BELOW = 8f;

    private boolean armee;
    private float x;
    private float y;

    /**
     * Arme la pilule à la position donnée.
     *
     * @param x l'abscisse du doigt, en pixels
     * @param y l'ordonnée du doigt, en pixels
     */
    public void show(float x, float y) {
        this.armee = true;
        this.x = x;
        this.y = y;
    }

    /** Désarme la pilule. */
    public void hide() {
        armee = false;
    }

    /** Vrai si la pilule est armée (visible et active). */
    public boolean isArmed() {
        return armee;
    }

    /** L'abscisse du doigt qui tient la pilule. */
    public float x() {
        return x;
    }

    /** L'ordonnée du doigt qui tient la pilule. */
    public float y() {
        return y;
    }

    /** Le bord gauche de la pilule. */
    public float left() {
        return x - HALF_WIDTH;
    }

    /** Le bord droit de la pilule. */
    public float right() {
        return x + HALF_WIDTH;
    }

    /** Le bord supérieur de la pilule. */
    public float top() {
        return bottom() - HEIGHT;
    }

    /** Le bord inférieur de la pilule. */
    public float bottom() {
        return y + GRIP_BELOW;
    }

    /**
     * Vrai si le point donné tombe sur la pilule (armée seulement).
     *
     * @param px l'abscisse testée, en pixels
     * @param py l'ordonnée testée, en pixels
     * @return vrai si le point touche la pilule
     */
    public boolean contains(float px, float py) {
        return armee && px >= left() && px <= right() && py >= top() && py <= bottom();
    }
}
