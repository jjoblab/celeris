package jo.celeris.android.host.popups;

import jo.celeris.android.widgets.CompletionListView;
import jo.celeris.android.widgets.PopupTokens;

/**
 * Le CAP de la liste de complétion (l'audit du propriétaire du
 * 2026-11-14 : les popups ne cachent pas la frappe) : la hauteur de la
 * liste suit la PLACE DISPONIBLE sous le caret — elle se ramasse au
 * lieu de chasser la ligne tapée ; la place manque au-dessus aussi ?
 * le plafond du dessous borne le geste. Pur et testable sans appareil.
 */
final class CapCompletion {

    private CapCompletion() {
    }

    /**
     * Le nombre de rangées à montrer : au plus le plafond du kit, au
     * moins son plancher — la place sous le caret décide, la place au
     * dessus rattrape quand la liste se rabat.
     *
     * @param hautCaret     le haut du rectangle du curseur, en pixels
     * @param basCaret      le bas du rectangle du curseur, en pixels
     * @param hauteurCouche la hauteur de la couche, en pixels
     * @param margePx       la respiration des ancres aux bords, en pixels
     * @param hauteurRangee la hauteur d'une rangée de complétion, en pixels
     * @return le cap de rangées montrées
     */
    static int de(int hautCaret, int basCaret, int hauteurCouche,
            int margePx, int hauteurRangee) {
        if (hauteurRangee <= 0) {
            return CompletionListView.MAX_MONTRES;
        }
        int placeSous = hauteurCouche - margePx - basCaret;
        int cap = placeSous / hauteurRangee;
        if (cap < CompletionListView.MAX_MONTRES) {
            // la place manque au-dessus aussi ? le plafond du dessous
            // borne le geste — la liste se rabat sans jamais manger la
            // ligne tapée
            int placeDessus = hautCaret - 2 * margePx;
            cap = Math.max(cap, Math.min(CompletionListView.MAX_MONTRES,
                    placeDessus / hauteurRangee));
        }
        return Math.max(CompletionListView.MIN_MONTRES,
                Math.min(cap, CompletionListView.MAX_MONTRES));
    }

    /** La hauteur d'une rangée de complétion, en pixels — la densité
     *  de l'appareil porte le jeton du kit. */
    static float hauteurRangee(float densite) {
        return PopupTokens.HAUTEUR_RANGEE_LISTE_DP * densite;
    }
}
