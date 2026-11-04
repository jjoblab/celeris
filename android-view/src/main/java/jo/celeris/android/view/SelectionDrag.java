package jo.celeris.android.view;

import jo.celeris.core.text.SelRange;

/**
 * Gestuelle de glissement de sélection tactile.
 *
 * <p>PIEGE-03 : dans le dépôt de départ, l'ancre relue à chaque MOVE
 * sautait quand le doigt franchissait l'autre borne. Ici, l'ancre est
 * figée au DOWN et ne bouge plus de toute la gestuelle : le curseur
 * seul suit le doigt, la plage s'inverse proprement au besoin.</p>
 */
public final class SelectionDrag {

    private boolean actif;
    private int ancre;

    /**
     * Fige l'ancre au point d'appui (DOWN).
     *
     * @param offset l'offset d'appui dans le document
     */
    public void down(int offset) {
        if (offset < 0) {
            throw new IllegalArgumentException("negative offset: " + offset);
        }
        actif = true;
        ancre = offset;
    }

    /**
     * Étend la sélection vers la position du doigt, l'ancre restant figée.
     *
     * @param offset l'offset courant du doigt dans le document
     * @return la plage ancre-curseur courante
     */
    public SelRange dragTo(int offset) {
        if (!actif) {
            throw new IllegalStateException("drag not started: down() required");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("negative offset: " + offset);
        }
        return new SelRange(ancre, offset);
    }

    /** Termine la gestuelle (UP) : l'ancre sera figée au prochain DOWN. */
    public void up() {
        actif = false;
    }

    /** Vrai pendant la gestuelle, entre le DOWN et le UP. */
    public boolean isActive() {
        return actif;
    }
}
