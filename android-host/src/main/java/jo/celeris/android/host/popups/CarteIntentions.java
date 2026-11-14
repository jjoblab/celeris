package jo.celeris.android.host.popups;

import android.graphics.Rect;

import java.util.List;
import java.util.function.Supplier;

import jo.celeris.android.view.EditorView;
import jo.celeris.android.widgets.IntentionsMenuView;
import jo.celeris.android.widgets.PopupAnchor;
import jo.celeris.android.widgets.PopupController;
import jo.celeris.android.widgets.PopupManager;
import jo.celeris.android.widgets.PopupPalette;
import jo.celeris.android.widgets.PopupPlacer;

/**
 * Le menu d'intentions au curseur (le satellite de la carte — la pose
 * déménage du corps de l'orchestrateur au satellite, 2026-11-14) : les
 * actions posées par l'hôte ({@link PopupsEditeur#configurerIntentions})
 * se proposent au caret, sans AUCUNE section — la carte la laisse au
 * débordement des barres. PIEGE-27 : jamais de focus — la navigation
 * clavier s'y reçoit DÉLÉGUÉE (le manager route).
 */
final class CarteIntentions {

    private final EditorView editeur;
    private final PopupManager popups;
    private final int margePx;
    private final IntentionsMenuView carte;
    /** Les intentions posées par l'hôte — vives au déclenchement. */
    private final Supplier<List<IntentionsMenuView.Item>> intentions;

    /**
     * Construit la carte d'intentions sur le coordinateur des popups :
     * la carte naît liée à son genre, le patron des trois temps (lier,
     * déclencher, déléguer — USAGE §9).
     *
     * @param editeur    la vue d'éditeur ouverte
     * @param popups     le coordinateur des popups de la scène
     * @param margePx    la respiration des ancres aux bords, en pixels
     * @param palette    la palette courante des popups
     * @param intentions les intentions de l'hôte, vives
     */
    CarteIntentions(EditorView editeur, PopupManager popups, int margePx,
            PopupPalette palette,
            Supplier<List<IntentionsMenuView.Item>> intentions) {
        this.editeur = editeur;
        this.popups = popups;
        this.margePx = margePx;
        this.intentions = intentions;
        this.carte = new IntentionsMenuView(popups.couche().getContext(),
                palette);
        popups.lier(PopupController.PopupKind.INTENTIONS_MENU, carte);
    }

    /** La palette tourne : la carte d'intentions la suit. */
    void surPalette(PopupPalette palette) {
        carte.setPalette(palette);
    }

    /**
     * Ouvre le menu au curseur : les intentions posées s'y proposent —
     * sans AUCUNE section, la carte la laisse au débordement des
     * barres. Sans intention, le menu ne se pose pas.
     */
    void ouvrir() {
        List<IntentionsMenuView.Item> items = intentions.get();
        if (items.isEmpty()) {
            return;
        }
        carte.setSection(null);
        carte.setActions(items);
        Rect curseur = editeur.caretBounds();
        PopupPlacer.Cadre placee = PopupAnchor.belowOrAboveCaret(
                curseur.left, curseur.bottom,
                popups.taille(PopupController.PopupKind.INTENTIONS_MENU),
                popups.couche().getWidth(),
                popups.couche().getHeight(), margePx);
        popups.montrer(PopupController.PopupKind.INTENTIONS_MENU,
                placee.x(), placee.y());
    }
}
