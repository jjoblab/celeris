package jo.celeris.demo;

import jo.celeris.android.widgets.NotificationView;
import jo.celeris.android.widgets.PopupAnchor;
import jo.celeris.android.widgets.PopupController;
import jo.celeris.android.widgets.PopupManager;
import jo.celeris.android.widgets.PopupPalette;
import jo.celeris.android.widgets.PopupPlacer;

/**
 * L'atelier des notifications de la démonstration (catégorie G de
 * l'inventaire — la forme restait à décider, la bulle éphémère
 * l'emporte) : l'annonce se pose au coin bas-droit de la couche,
 * l'HORLOGE de la carte la referme à son heure — la re-pose réarme
 * l'horloge, jamais deux courses. Le tap qui la prend la referme ;
 * les régimes l'épargnent (frappe, défilement, geste extérieur —
 * l'annonce ne décrit pas le texte), le voile modal la coupe. Les
 * événements réels de la démonstration y passent : les correctifs
 * appliqués, la pause de débogage.
 *
 * <p>La frontière tient : la carte ne connaît NI l'éditeur, NI le
 * coordinateur — l'atelier arme l'horloge et referme le genre à
 * l'expiration.</p>
 */
final class AtelierNotifications {

    /** La durée de vie d'une annonce, en millisecondes — le quart de
     *  l'annonce : le temps de la lire, jamais assez pour lasser. */
    private static final long DUREE_ANNONCE_MS = 4000L;

    private final PopupManager popups;
    private final int margePx;
    private final NotificationView carte;

    /**
     * Construit l'atelier des notifications sur le coordinateur des
     * popups : la carte naît liée à son genre, le patron des trois
     * temps (lier, déclencher, déléguer — USAGE §9).
     *
     * @param popups  le coordinateur des popups de la scène
     * @param margePx la respiration de l'ancre aux murs, en pixels
     * @param palette la palette courante des popups
     */
    AtelierNotifications(PopupManager popups, int margePx,
            PopupPalette palette) {
        this.popups = popups;
        this.margePx = margePx;
        this.carte = new NotificationView(popups.couche().getContext(),
                palette);
        popups.lier(PopupController.PopupKind.NOTIFICATION, carte);
        carte.setSurPrise(this::refermer);
    }

    /** La palette tourne : la bulle la suit. */
    void surPalette(PopupPalette palette) {
        carte.setPalette(palette);
    }

    /**
     * Annonce à l'usager : la bulle se pose au coin bas-droit, l'horloge
     * s'arme (la course d'avant se dissout — la queue du fil ne sert
     * que la dernière annonce). Le message vide n'annonce rien.
     *
     * @param titre   le titre de l'annonce
     * @param message le message de l'annonce
     */
    void annoncer(String titre, String message) {
        if (titre.isBlank() || message.isBlank()) {
            return;
        }
        carte.setAnnonce(titre, message);
        carte.armer(DUREE_ANNONCE_MS, this::refermer);
        PopupPlacer.Cadre placee = PopupAnchor.bottomRightCorner(
                popups.taille(PopupController.PopupKind.NOTIFICATION),
                popups.couche().getWidth(),
                popups.couche().getHeight(), margePx);
        popups.montrer(PopupController.PopupKind.NOTIFICATION,
                placee.x(), placee.y());
    }

    /** Referme l'annonce — l'horloge se coupe, le genre se ferme. */
    private void refermer() {
        carte.desarmer();
        popups.fermer(PopupController.PopupKind.NOTIFICATION);
    }

}
