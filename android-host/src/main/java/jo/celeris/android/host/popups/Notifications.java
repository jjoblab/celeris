package jo.celeris.android.host.popups;

import java.util.List;

import jo.celeris.android.widgets.NotificationView;
import jo.celeris.android.widgets.PopupAnchor;
import jo.celeris.android.widgets.PopupController;
import jo.celeris.android.widgets.PopupManager;
import jo.celeris.android.widgets.PopupPalette;
import jo.celeris.android.widgets.PopupPlacer;

/**
 * Les notifications de l'éditeur : l'annonce éphémère du coin
 * bas-droit — l'hôte y publie ce qui ne touche pas le texte
 * (sauvegarde, serveur de langage, agent). La carte naît liée à son
 * genre, l'HORLOGE la referme à son heure (4 000 ms — le quart de
 * l'annonce : le temps de la lire, jamais assez pour lasser) ; la
 * re-pose ré-arme SANS mourir du retard de l'ancienne (la course
 * d'avant se dissout d'abord, la queue du fil ne sert que la dernière
 * annonce). Le tap qui la PREND la referme ; les régimes l'épargnent
 * (frappe, défilement, geste extérieur — l'annonce ne décrit pas le
 * texte), le voile modal la coupe.
 */
final class Notifications {

    /** La durée de vie d'une annonce, en millisecondes — le quart de
     *  l'annonce : le temps de la lire, jamais assez pour lasser. */
    private static final long DUREE_ANNONCE_MS = 4000L;

    private final PopupManager popups;
    private final NotificationView carte;
    private final int margePx;

    /**
     * Construit les notifications sur le coordinateur des popups : la
     * carte naît liée à son genre, la palette courante la vêt, le tap
     * qui la prend la referme.
     *
     * @param popups  le coordinateur des popups de la scène
     * @param margePx la respiration des ancres aux bords, en pixels
     * @param palette la palette courante des popups
     */
    Notifications(PopupManager popups, int margePx, PopupPalette palette) {
        this.popups = popups;
        this.margePx = margePx;
        this.carte = new NotificationView(popups.couche().getContext(),
                palette);
        popups.lier(PopupController.PopupKind.NOTIFICATION, carte);
        carte.setSurPrise(this::fermer);
    }

    /** La palette tourne : la carte de notification la suit. */
    void surPalette(PopupPalette palette) {
        carte.setPalette(palette);
    }

    /**
     * Publie une notification : le titre et le message gagnent la
     * carte au coin bas-droit, l'horloge s'arme (la course d'avant se
     * dissout — jamais deux courses). Le message vide n'annonce rien.
     *
     * @param titre   le titre de l'annonce
     * @param message le message de l'annonce, ses lignes suivent
     */
    void notifier(String titre, String message) {
        notifier(titre, message, List.of());
    }

    /**
     * Publie une notification avec ses liens — l'audit Android Studio
     * du 2026-11-14 : la bulle de l'IDE de référence porte ses actions
     * en hyperliens derrière le message, la prise d'un lien referme la
     * bulle puis la fait courir.
     *
     * @param titre   le titre de l'annonce
     * @param message le message de l'annonce, ses lignes suivent
     * @param actions les liens de la bulle, vides pour aucun
     */
    void notifier(String titre, String message,
            List<NotificationView.Action> actions) {
        if (titre.isBlank() || message.isBlank()) {
            return;
        }
        carte.setAnnonce(titre, message, actions);
        carte.armer(DUREE_ANNONCE_MS, this::fermer);
        PopupPlacer.Cadre placee = PopupAnchor.bottomRightCorner(
                popups.taille(PopupController.PopupKind.NOTIFICATION),
                popups.couche().getWidth(),
                popups.couche().getHeight(), margePx);
        popups.montrer(PopupController.PopupKind.NOTIFICATION,
                placee.x(), placee.y());
    }

    /** Referme l'annonce — l'horloge se coupe, le genre se ferme. */
    void fermer() {
        carte.desarmer();
        popups.fermer(PopupController.PopupKind.NOTIFICATION);
    }
}
