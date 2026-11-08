package jo.celeris.android.view;

import jo.celeris.core.text.SelRange;

/**
 * Gestuelle de glissement de sélection tactile.
 *
 * <p>PIEGE-03 : dans le dépôt de départ, l'ancre relue à chaque MOVE
 * sautait quand le doigt franchissait l'autre borne. Ici, l'ancre est
 * figée à l'ARMEMENT — l'appui long qui démarre la sélection (M7.5 :
 * le simple appui pose le curseur, il ne sélectionne pas) — et ne
 * bouge plus de toute la gestuelle : le curseur seul suit le doigt, la
 * plage s'inverse proprement au besoin.</p>
 *
 * <p>L'armement sur mot (appui long) garde le mot sélectionné pendant
 * l'extension : la plage rendue ENGLOBE le mot de l'armement, le
 * doigt qui revient dessus ne le ronge pas — la sélection ne rétrécit
 * jamais sous le mot de départ.</p>
 */
public final class SelectionDrag {

    private boolean actif;
    private int ancre;
    private int motDebut;
    private int motFin;

    /**
     * Fige l'ancre au point d'appui, sans mot englobant.
     *
     * @param offset l'offset d'appui dans le document
     */
    public void down(int offset) {
        if (offset < 0) {
            throw new IllegalArgumentException("negative offset: " + offset);
        }
        actif = true;
        ancre = offset;
        motDebut = offset;
        motFin = offset;
    }

    /**
     * Fige l'ancre à un bord du mot sélectionné par l'appui long : la
     * gestuelle étend la sélection SANS jamais perdre le mot.
     *
     * @param ancre     le bord qui reste figé (début ou fin du mot)
     * @param motDebut  le début du mot de l'appui long
     * @param motFin    la fin exclusive du mot de l'appui long
     */
    public void down(int ancre, int motDebut, int motFin) {
        if (ancre < 0 || motDebut < 0 || motFin < motDebut) {
            throw new IllegalArgumentException("invalid word bounds: "
                    + motDebut + ".." + motFin + " anchor " + ancre);
        }
        actif = true;
        this.ancre = ancre;
        this.motDebut = motDebut;
        this.motFin = motFin;
    }

    /**
     * Étend la sélection vers la position du doigt, l'ancre restant
     * figée et le mot de l'armement restant englobé — la plage rendue
     * porte l'ancre figée en ancre, le bord effectif du doigt en
     * curseur.
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
        if (motFin > motDebut) {
            if (ancre <= motDebut) {
                // ancre au début du mot : le curseur n'entre jamais
                // dans le mot, il s'arrête à sa fin au plus près
                return new SelRange(ancre, Math.max(offset, motFin));
            }
            // ancre à la fin du mot : le curseur n'entre jamais dans
            // le mot, il s'arrête à son début au plus près
            return new SelRange(ancre, Math.min(offset, motDebut));
        }
        return new SelRange(ancre, offset);
    }

    /** Termine la gestuelle (UP) : l'ancre sera figée au prochain armement. */
    public void up() {
        actif = false;
        motDebut = 0;
        motFin = 0;
    }

    /** Vrai pendant la gestuelle, entre l'armement et le UP. */
    public boolean isActive() {
        return actif;
    }
}
