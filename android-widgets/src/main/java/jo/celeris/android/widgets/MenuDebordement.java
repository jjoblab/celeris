package jo.celeris.android.widgets;

import java.util.List;

/**
 * Le menu de débordement de la barre de sélection (le design du kit du
 * dépôt de départ, le motif de la plateforme) : le tap sur la puce
 * « ⋯ » ouvre la CARTE À ÉTAGES du kit — les actions repliées s'y
 * groupent par SECTIONS CONTEXTUELLES (l'audit du propriétaire du
 * 2026-11-14 : « je veux le séparer en étages ou colonnes qui
 * deviennent visibles contextuellement comme les go to, l'option
 * usages, rename, etc. ») : l'étage vide ne se montre jamais, la
 * visibilité naît de la donnée. Le menu ne survit jamais à sa barre
 * (la machine à états l'emporte avec elle) ; le choix d'une action la
 * referme AVANT de l'exécuter, l'en-tête de section ne se prend jamais.
 *
 * <p>Le montage tient en deux temps : {@link #ouvrir} sous la puce de
 * la barre (les étages y courent), {@link #fermer} sur le choix ou la
 * fermeture de la barre — la machine à états tient le reste, l'hôte
 * n'y code rien. Sans étage vivant, le menu ne se pose pas.</p>
 */
public final class MenuDebordement {

    private final PopupManager popups;
    private final MenuEtages carte;
    private final int margePx;

    /**
     * Construit le menu de débordement sur le coordinateur des popups :
     * la carte naît liée au genre débordement, la palette courante la
     * vêt. L'ancienne carte à section unique vit maintenant par
     * étages — la vue du kit suit.
     *
     * @param popups  le coordinateur des popups de la scène
     * @param palette la palette courante des popups
     * @param margePx la respiration des ancres aux bords de la couche
     */
    public MenuDebordement(PopupManager popups, PopupPalette palette,
            int margePx) {
        this.popups = popups;
        this.carte = new MenuEtages(popups.couche().getContext(), palette);
        popups.lier(PopupController.PopupKind.SELECTION_OVERFLOW, carte);
        this.margePx = margePx;
        // le choix referme la carte AVANT d'exécuter l'action — le
        // débordement ne survit jamais au choix
        carte.setOnPick(rang -> {
            MenuEtages.Item item = carte.itemAt(rang);
            if (item != null) {
                fermer();
                item.action().run();
            }
        });
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
     * découvert dans les deux poses. La carte trop haute se RAMASSE
     * au plafond du kit : jamais un menu qui mange la couche.
     *
     * @param barre        la barre de sélection porteuse, déjà posée
     * @param basSelection le bas du rectangle de la sélection, en
     *                     pixels de couche — la pose du menu y épargne
     *                     le caret
     * @param etages       les étages d'actions, vides pour ne rien poser
     */
    public void ouvrir(SelectionBarView barre, int basSelection,
            List<MenuEtages.Etage> etages) {
        carte.setEtages(etages);
        if (!carte.aActions()) {
            return;
        }
        int x = barre.getLeft() + barre.ancrePuceDebordementX();
        PopupPlacer.Cadre placee = PopupAnchor.belowSelectionOrAboveBar(x,
                basSelection, barre.getTop(), taille(), coucheLargeur(),
                coucheHauteur(), margePx);
        popups.montrer(PopupController.PopupKind.SELECTION_OVERFLOW,
                placee.x(), placee.y());
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
