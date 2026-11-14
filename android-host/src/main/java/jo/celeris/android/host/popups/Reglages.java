package jo.celeris.android.host.popups;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.android.view.EditorView;
import jo.celeris.android.view.ReglagesEditeur;
import jo.celeris.android.widgets.PopupAnchor;
import jo.celeris.android.widgets.PopupController;
import jo.celeris.android.widgets.PopupManager;
import jo.celeris.android.widgets.PopupPalette;
import jo.celeris.android.widgets.PopupPlacer;
import jo.celeris.android.widgets.ReglagesView;

/**
 * Le satellite des réglages de l'éditeur (l'audit du propriétaire du
 * 2026-11-14 : « ajoute un système central editor settings pour
 * pouvoir activer désactiver, etc. certaines fonctionnalités ») : la
 * carte des commutateurs se pose au coin bas-droit — l'annonce des
 * notifications vit au même mur, jamais l'un sur l'autre (la carte
 * referme l'annonce à sa pose, l'annonce reprend son quart à la
 * fermeture). La table centrale ({@link ReglagesEditeur}) reste le
 * point de vérité : la carte y LIT ses portes, la bascule y RETOURNE
 * — les libellés seuls vivent ici.
 */
final class Reglages {

    private final EditorView editeur;
    private final PopupManager popups;
    private final int margePx;
    private final ReglagesView carte;

    /**
     * Construit le satellite des réglages sur le coordinateur des
     * popups : la carte naît liée à son genre.
     *
     * @param editeur la vue d'éditeur ouverte (sa table de réglages)
     * @param popups  le coordinateur des popups de la scène
     * @param margePx la respiration des ancres aux bords, en pixels
     * @param palette la palette courante des popups
     */
    Reglages(EditorView editeur, PopupManager popups, int margePx,
            PopupPalette palette) {
        this.editeur = editeur;
        this.popups = popups;
        this.margePx = margePx;
        this.carte = new ReglagesView(popups.couche().getContext(),
                palette);
        popups.lier(PopupController.PopupKind.SETTINGS_MENU, carte);
    }

    /** La palette tourne : la carte la suit. */
    void surPalette(PopupPalette palette) {
        carte.setPalette(palette);
    }

    /**
     * Ouvre (ou re-pose) la carte des réglages : les interrupteurs se
     * relisent de la TABLE CENTRALE — chaque bascule y retourne, la
     * carte se re-pose entière (l'état lu se voit aussitôt). La carte
     * se pose au coin bas-droit, bornée aux murs.
     *
     * @param libelles les libellés des portes, dans l'ordre des
     *                 {@link ReglagesEditeur.Porte} de la table
     */
    void ouvrir(List<String> libelles) {
        ReglagesEditeur table = editeur.reglages();
        ReglagesEditeur.Porte[] portes = ReglagesEditeur.Porte.values();
        List<ReglagesView.Interrupteur> interrupteurs =
                new ArrayList<>(portes.length);
        for (int i = 0; i < portes.length && i < libelles.size(); i++) {
            ReglagesEditeur.Porte porte = portes[i];
            interrupteurs.add(new ReglagesView.Interrupteur(
                    libelles.get(i), IconeDePorte.de(porte),
                    table.ouverte(porte),
                    () -> table.basculer(porte)));
        }
        carte.setInterrupteurs(interrupteurs);
        PopupPlacer.Cadre placee = PopupAnchor.bottomRightCorner(
                popups.taille(PopupController.PopupKind.SETTINGS_MENU),
                popups.couche().getWidth(),
                popups.couche().getHeight(), margePx);
        popups.montrer(PopupController.PopupKind.SETTINGS_MENU,
                placee.x(), placee.y());
    }

    /** Re-pose la carte ouverte — les bascules s'y relisent, l'état
     *  changé se voit aussitôt. */
    void rafraichir(List<String> libelles) {
        if (popups.estOuvert(PopupController.PopupKind.SETTINGS_MENU)) {
            ouvrir(libelles);
        }
    }

    /** Ferme la carte des réglages. */
    void fermer() {
        popups.fermer(PopupController.PopupKind.SETTINGS_MENU);
    }
}
