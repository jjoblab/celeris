package jo.celeris.android.widgets;

import java.util.ArrayList;
import java.util.List;

/**
 * Le menu de débordement de la barre de sélection (le design du kit du
 * dépôt de départ, le motif de la plateforme) : le tap sur la puce
 * « ⋯ » ouvre la CARTE du kit en son état NavMenu — une SECTION en
 * petites capitales muettes (« MORE » au dépôt de départ) annonce les
 * actions repliées, rangées cliquables du même verre que le menu
 * d'intentions. Le menu ne survit jamais à sa barre (la machine à états
 * l'emporte avec elle) ; le choix d'une action la referme AVANT de
 * l'exécuter, l'en-tête de section ne se prend jamais.
 *
 * <p>Le montage tient en deux temps : {@link #ouvrir} sous la puce de
 * la barre (les actions repliées y courent), {@link #fermer} sur le
 * choix ou la fermeture de la barre — la machine à états tient le
 * reste, l'hôte n'y code rien.</p>
 */
public final class MenuDebordement {

    private final PopupManager popups;
    private final IntentionsMenuView carte;
    private final int margePx;

    /** Le titre de la section du débordement — l'état NavMenu. */
    private final String section;

    /**
     * Construit le menu de débordement sur le coordinateur des popups :
     * la carte naît liée au genre débordement, la palette courante la
     * vêt.
     *
     * @param popups  le coordinateur des popups de la scène
     * @param palette la palette courante des popups
     * @param margePx la respiration des ancres aux bords de la couche
     * @param section le titre de section du débordement (« MORE » au
     *                dépôt de départ) — l'état NavMenu de la carte
     */
    public MenuDebordement(PopupManager popups, PopupPalette palette,
            int margePx, String section) {
        this.popups = popups;
        this.carte = new IntentionsMenuView(popups.couche().getContext(),
                palette);
        popups.lier(PopupController.PopupKind.SELECTION_OVERFLOW, carte);
        this.margePx = margePx;
        this.section = section;
    }

    /** La palette tourne : la carte du débordement la suit. */
    public void surPalette(PopupPalette palette) {
        carte.setPalette(palette);
    }

    /**
     * Ouvre (ou ré-ancre) le menu sous la SÉLECTION, jamais sur elle
     * — l'audit du propriétaire du 2026-11-14 : le menu ne marche pas
     * sur le caret. La place manque au dessous ? le menu se repose
     * AU-DESSUS DE LA BARRE — la bande de la sélection demeure à
     * découvert dans les deux poses, les actions repliées y courent
     * en rangées cliquables, le menu se referme sur le choix.
     *
     * @param barre       la barre de sélection porteuse, déjà posée
     * @param basSelection le bas du rectangle de la sélection, en
     *                    pixels de couche — la pose du menu y épargne
     *                    le caret
     * @param actions     les actions repliées derrière la puce
     */
    public void ouvrir(SelectionBarView barre, int basSelection,
            List<SelectionBarView.Item> actions) {
        if (actions.isEmpty()) {
            return;
        }
        // la section d'abord (l'état NavMenu de la carte), les actions
        // ensuite — la carte s'y re-pose entière
        carte.setSection(section);
        carte.setActions(items(actions));
        int x = barre.getLeft() + barre.ancrePuceDebordementX();
        PopupPlacer.Cadre placee = PopupAnchor.belowSelectionOrAboveBar(x,
                basSelection, barre.getTop(), taille(), coucheLargeur(),
                coucheHauteur(), margePx);
        popups.montrer(PopupController.PopupKind.SELECTION_OVERFLOW,
                placee.x(), placee.y());
    }

    /** Les items du menu : chaque action court APRÈS la fermeture du menu —
     *  le pictogramme de l'action suit sa rangée. */
    private List<IntentionsMenuView.Item> items(
            List<SelectionBarView.Item> actions) {
        List<IntentionsMenuView.Item> items = new ArrayList<>(actions.size());
        for (SelectionBarView.Item item : actions) {
            items.add(new IntentionsMenuView.Item(item.label(),
                    item.picto(), () -> {
                fermer();
                item.action().run();
            }));
        }
        return items;
    }

    /** Ferme le menu — la couche suit, la barre reste. */
    public void fermer() {
        popups.fermer(PopupController.PopupKind.SELECTION_OVERFLOW);
    }

    /** La taille mesurée de la carte — l'ancre y anticipe la pose. */
    private PopupPlacer.Cadre taille() {
        return popups.taille(PopupController.PopupKind.SELECTION_OVERFLOW);
    }

    private int coucheLargeur() {
        return popups.couche().getWidth();
    }

    private int coucheHauteur() {
        return popups.couche().getHeight();
    }
}
