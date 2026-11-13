package jo.celeris.android.host.popups;

import jo.celeris.android.widgets.NotificationView;
import jo.celeris.android.widgets.PopupAnchor;
import jo.celeris.android.widgets.PopupController;
import jo.celeris.android.widgets.PopupManager;
import jo.celeris.android.widgets.PopupPalette;
import jo.celeris.android.widgets.PopupPlacer;

/**
 * Les notifications de l'éditeur : la carte éphémère du coin
 * bas-droit — l'hôte y publie ce qui ne touche pas le texte
 * (sauvegarde, serveur de langage, agent). La carte naît liée à son
 * genre, l'horloge (4 000 ms) referme seule, la ré-pose ré-arme SANS
 * mourir du retard de l'ancienne.
 */
final class Notifications {

    private final PopupManager popups;
    private final NotificationView vue;
    private final int margePx;

    /**
     * Construit les notifications sur le coordinateur des popups : la
     * carte naît liée à son genre, la palette courante la vêt.
     *
     * @param popups  le coordinateur des popups de la scène
     * @param margePx la respiration des ancres aux bords de la couche
     * @param palette la palette courante des popups
     */
    Notifications(PopupManager popups, int margePx, PopupPalette palette) {
        this.popups = popups;
        this.margePx = margePx;
        this.vue = new NotificationView(popups.couche().getContext(),
                palette);
        popups.lier(PopupController.PopupKind.NOTIFICATION, vue);
    }

    /** La palette tourne : la carte de notification la suit. */
    void surPalette(PopupPalette palette) {
        vue.setPalette(palette);
    }

    /**
     * Publie une notification : le titre et le message gagnent la
     * carte au coin bas-droit, l'horloge referme seule — quatre
     * secondes, la carte meurt.
     *
     * @param titre   le titre de l'avis
     * @param message le message de l'avis, ses lignes suivent
     */
    void notifier(String titre, String message) {
        vue.setMessage(titre, message, this::fermer);
        PopupPlacer.Cadre placee = PopupAnchor.bottomRightCorner(
                popups.taille(PopupController.PopupKind.NOTIFICATION),
                popups.couche().getWidth(),
                popups.couche().getHeight(), margePx);
        popups.montrer(PopupController.PopupKind.NOTIFICATION,
                placee.x(), placee.y());
    }

    /** Ferme la notification — l'horloge en vol se retire avec elle. */
    void fermer() {
        vue.desarmer();
        popups.fermer(PopupController.PopupKind.NOTIFICATION);
    }
}
