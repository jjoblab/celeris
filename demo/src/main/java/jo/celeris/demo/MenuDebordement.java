package jo.celeris.demo;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.android.widgets.IntentionsMenuView;
import jo.celeris.android.widgets.PopupAnchor;
import jo.celeris.android.widgets.PopupController;
import jo.celeris.android.widgets.PopupLayer;
import jo.celeris.android.widgets.PopupPlacer;
import jo.celeris.android.widgets.SelectionBarView;

/**
 * Le menu de débordement de la barre de sélection (M8.6 — le design du
 * kit du dépôt de départ, le motif de la plateforme) : le tap sur la
 * puce « ⋯ » ouvre la CARTE du kit en son état NavMenu — une SECTION
 * en petites capitales muettes (« MORE » au dépôt de départ) annonce
 * les actions repliées, rangées cliquables du même verre que le menu
 * d'intentions. Le menu ne survit jamais à sa barre (la machine à
 * états l'emporte avec elle) ; le choix d'une action la referme AVANT
 * de l'exécuter, l'en-tête de section ne se prend jamais.
 */
final class MenuDebordement {

    private final PopupController controleur;
    private final IntentionsMenuView menu;
    private final PopupLayer couche;
    private final int margePx;

    /**
     * Construit le menu de débordement sur la couche des popups.
     *
     * @param controleur la machine à états des popups
     * @param menu       la carte partagée du kit — le menu d'intentions
     * @param couche     la couche porte-popups posée sur l'éditeur
     * @param margePx    la respiration des ancres aux bords de la couche
     * @param section    le titre de section du débordement (« MORE » au
     *                   dépôt de départ) — l'état NavMenu de la carte
     */
    MenuDebordement(PopupController controleur, IntentionsMenuView menu,
            PopupLayer couche, int margePx, String section) {
        this.controleur = controleur;
        this.menu = menu;
        this.couche = couche;
        this.margePx = margePx;
        this.section = section;
    }

    /** Le titre de la section du débordement — l'état NavMenu. */
    private final String section;

    /**
     * Ouvre (ou ré-ancre) le menu sous la puce de la barre : les
     * actions repliées y courent, le menu se referme sur le choix.
     *
     * @param barre   la barre de sélection porteuse, déjà posée
     * @param actions les actions repliées derrière la puce
     */
    void ouvrir(SelectionBarView barre, List<SelectionBarView.Item> actions) {
        if (actions.isEmpty()) {
            return;
        }
        controleur.open(PopupController.PopupKind.SELECTION_OVERFLOW);
        // la section d'abord (l'état NavMenu de la carte), les actions
        // ensuite — la carte partagée s'y re-pose entière
        menu.setSection(section);
        menu.setActions(items(actions));
        int x = barre.getLeft() + barre.ancrePuceDebordementX();
        PopupPlacer.Cadre placee = PopupAnchor.belowOrAboveCaret(x,
                barre.getBottom(), taille(), couche.getWidth(),
                couche.getHeight(), margePx);
        couche.show(PopupController.PopupKind.SELECTION_OVERFLOW, menu,
                placee.x(), placee.y());
    }

    /** Les items du menu : chaque action court APRÈS la fermeture du menu. */
    private List<IntentionsMenuView.Item> items(
            List<SelectionBarView.Item> actions) {
        List<IntentionsMenuView.Item> items = new ArrayList<>(actions.size());
        for (SelectionBarView.Item item : actions) {
            items.add(new IntentionsMenuView.Item(item.label(), () -> {
                fermer();
                item.action().run();
            }));
        }
        return items;
    }

    /** Ferme le menu — la couche suit, la barre reste. */
    void fermer() {
        if (controleur.close(PopupController.PopupKind.SELECTION_OVERFLOW)) {
            couche.dismiss(PopupController.PopupKind.SELECTION_OVERFLOW);
        }
    }

    /** La taille mesurée de la carte — l'ancre y anticipe la pose. */
    private PopupPlacer.Cadre taille() {
        menu.measure(android.view.View.MeasureSpec.makeMeasureSpec(0,
                        android.view.View.MeasureSpec.UNSPECIFIED),
                android.view.View.MeasureSpec.makeMeasureSpec(0,
                        android.view.View.MeasureSpec.UNSPECIFIED));
        return new PopupPlacer.Cadre(0, 0, menu.getMeasuredWidth(),
                menu.getMeasuredHeight());
    }
}
