package jo.celeris.android.host.popups;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.android.view.EditorView;
import jo.celeris.android.view.EditorSettingsPanel;
import jo.celeris.android.widgets.PopupAnchor;
import jo.celeris.android.widgets.PopupController;
import jo.celeris.android.widgets.PopupManager;
import jo.celeris.android.widgets.PopupPalette;
import jo.celeris.android.widgets.PopupPlacer;
import jo.celeris.android.widgets.SettingsSheet;

/**
 * Le satellite des réglages de l'éditeur (l'audit du propriétaire du
 * 2026-11-14 : « ajoute un système central editor settings pour
 * pouvoir activer désactiver, etc. certaines fonctionnalités ») : la
 * carte des commutateurs se pose au coin bas-droit — l'annonce des
 * notifications vit au même mur, jamais l'un sur l'autre (la carte
 * referme l'annonce à sa pose, l'annonce reprend son quart à la
 * fermeture). La table centrale ({@link EditorSettingsPanel}) reste le
 * point de vérité : la carte y LIT ses portes, la bascule y RETOURNE
 * — les libellés seuls vivent ici.
 */
final class SettingsPanel {

    private final EditorView editor;
    private final PopupManager popups;
    private final int margePx;
    private final SettingsSheet carte;

    /**
     * Construit le satellite des réglages sur le coordinateur des
     * popups : la carte naît liée à son genre.
     *
     * @param editor la vue d'éditeur ouverte (sa table de réglages)
     * @param popups  le coordinateur des popups de la scène
     * @param margePx la respiration des ancres aux bords, en pixels
     * @param palette la palette courante des popups
     */
    SettingsPanel(EditorView editor, PopupManager popups, int margePx,
            PopupPalette palette) {
        this.editor = editor;
        this.popups = popups;
        this.margePx = margePx;
        this.carte = new SettingsSheet(popups.couche().getContext(),
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
     *                 {@link EditorSettingsPanel.Porte} de la table
     */
    void open(List<String> libelles) {
        EditorSettingsPanel table = editor.settingsPanel();
        EditorSettingsPanel.Porte[] portes = EditorSettingsPanel.Porte.values();
        List<SettingsSheet.Interrupteur> interrupteurs =
                new ArrayList<>(portes.length);
        for (int i = 0; i < portes.length && i < libelles.size(); i++) {
            EditorSettingsPanel.Porte porte = portes[i];
            interrupteurs.add(new SettingsSheet.Interrupteur(
                    libelles.get(i), IconeDePorte.of(porte),
                    table.ouverte(porte),
                    () -> table.toggle(porte)));
        }
        carte.setInterrupteurs(interrupteurs);
        PopupPlacer.Cadre placee = PopupAnchor.bottomRightCorner(
                popups.size(PopupController.PopupKind.SETTINGS_MENU),
                popups.couche().getWidth(),
                popups.couche().getHeight(), margePx);
        popups.show(PopupController.PopupKind.SETTINGS_MENU,
                placee.x(), placee.y());
    }

    /** Re-pose la carte ouverte — les bascules s'y relisent, l'état
     *  changé se voit aussitôt. */
    void refresh(List<String> libelles) {
        if (popups.estOuvert(PopupController.PopupKind.SETTINGS_MENU)) {
            open(libelles);
        }
    }

    /** Ferme la carte des réglages. */
    void close() {
        popups.close(PopupController.PopupKind.SETTINGS_MENU);
    }
}
