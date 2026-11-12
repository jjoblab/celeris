package jo.celeris.demo;

import android.view.View;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.android.widgets.IntentionsMenuView;
import jo.celeris.android.widgets.PopupController;
import jo.celeris.android.widgets.PopupLayer;
import jo.celeris.android.widgets.PopupPalette;
import jo.celeris.android.widgets.PopupPlacer;
import jo.celeris.android.widgets.TouchBarView;

/**
 * L'atelier de la barre d'application (le redressement du
 * propriétaire, le design du dépôt de départ) : le menu de
 * DÉBORDEMENT de la toolbar ne vit plus au menu système — la CARTE du
 * kit s'y ouvre, la même que le menu d'intentions (rangées cliquables
 * du verre, fermeture au choix), pendue sous la puce au bord droit.
 * La BARRE DE TOUCHES de code vit au bas de la scène, AU-DESSUS DU
 * CLAVIER (le dépôt de départ, 4.5.0) : Tab, commentaire,
 * déplacements de lignes, duplication — l'hôte y pose ses touches.
 *
 * <p>La carte s'ouvre sur le genre INTENTIONS_MENU de la machine : un
 * menu remplace la complétion, deux menus ne tiennent jamais — la
 * carte est PARTAGÉE avec le menu d'intentions (une carte, tous les
 * menus, le motif du dépôt de départ). La touchbar vit sur le genre
 * TOUCH_BAR : ni la frappe ni le défilement ne la ferment, le
 * clavier la remonte avec l'IME (l'activité bord à bord).</p>
 */
final class AtelierBarreToolbar {

    /** La respiration des ancres aux bords de la couche, en dp. */
    private static final float MARGE_DP = 8f;

    private final PopupController controleur;
    private final PopupLayer couche;
    private final IntentionsMenuView carte;
    private final TouchBarView barreTouches;
    private final int margePx;

    /**
     * Construit l'atelier sur la couche des popups.
     *
     * @param controleur la machine à états des popups
     * @param couche     la couche porte-popups posée sur l'éditeur
     * @param carte      la carte partagée du kit — le menu d'intentions
     * @param palette    la palette dérivée du thème courant
     */
    AtelierBarreToolbar(PopupController controleur, PopupLayer couche,
            IntentionsMenuView carte, PopupPalette palette) {
        this.controleur = controleur;
        this.couche = couche;
        this.carte = carte;
        this.barreTouches = new TouchBarView(couche.getContext(), palette);
        this.margePx = Math.round(MARGE_DP
                * couche.getResources().getDisplayMetrics().density);
        // la touchbar se REPOSE au changement de layout : la rotation
        // la recolle au bas de la scène — l'observateur global de la
        // couche y veille, sans toucher aux écouteurs de l'éditeur
        couche.getViewTreeObserver().addOnGlobalLayoutListener(
                this::reposerBarre);
    }

    /**
     * Ouvre (ou ré-ancre) la carte de débordement sous la puce de la
     * toolbar : les actions repliées y courent en rangées cliquables,
     * la carte se referme sur le choix AVANT de l'exécuter.
     *
     * @param actions les actions repliées derrière la puce
     */
    void ouvrirDebordement(List<IntentionsMenuView.Item> actions) {
        if (actions.isEmpty()) {
            return;
        }
        controleur.open(PopupController.PopupKind.INTENTIONS_MENU);
        carte.setActions(items(actions));
        PopupPlacer.Cadre taille = tailleMesuree(carte);
        // la carte pend sous la puce : le long du bord DROIT, en tête
        // de scène — la puce de la toolbar vit au-dessus à droite
        int x = Math.max(margePx,
                couche.getWidth() - margePx - taille.largeur());
        int y = margePx;
        couche.show(PopupController.PopupKind.INTENTIONS_MENU, carte, x, y);
    }

    /** Ferme la carte — la couche suit. */
    void fermerDebordement() {
        if (controleur.close(PopupController.PopupKind.INTENTIONS_MENU)) {
            couche.dismiss(PopupController.PopupKind.INTENTIONS_MENU);
        }
    }

    /**
     * Pose la barre de touches au bas de la scène — AU-DESSUS DU
     * CLAVIER : l'hôte y apporte ses touches, la barre vit avec le
     * document.
     *
     * @param touches les touches de code, dans l'ordre d'affichage
     */
    void poserBarreTouches(List<TouchBarView.Touche> touches) {
        barreTouches.setActions(touches);
        controleur.open(PopupController.PopupKind.TOUCH_BAR);
        reposerBarre();
    }

    /** La barre de touches — l'hôte y rafraîchit les états activés. */
    TouchBarView barreTouches() {
        return barreTouches;
    }

    /** La palette tourne : la barre de touches la suit. */
    void surPalette(PopupPalette palette) {
        barreTouches.setPalette(palette);
    }

    /** Recolle la barre au bas de la scène — au layout global. */
    private void reposerBarre() {
        if (!controleur.isOpen(PopupController.PopupKind.TOUCH_BAR)
                || barreTouches.getMeasuredWidth() <= 0) {
            return;
        }
        PopupPlacer.Cadre taille = new PopupPlacer.Cadre(0, 0,
                barreTouches.getMeasuredWidth(),
                barreTouches.getMeasuredHeight());
        int x = Math.max(margePx,
                (couche.getWidth() - taille.largeur()) / 2);
        int y = Math.max(margePx,
                couche.getHeight() - taille.hauteur() - margePx);
        couche.show(PopupController.PopupKind.TOUCH_BAR, barreTouches, x, y);
    }

    /** Les items de la carte : chaque action court APRÈS la fermeture. */
    private List<IntentionsMenuView.Item> items(
            List<IntentionsMenuView.Item> actions) {
        List<IntentionsMenuView.Item> items = new ArrayList<>(actions.size());
        for (IntentionsMenuView.Item item : actions) {
            items.add(new IntentionsMenuView.Item(item.label(), () -> {
                fermerDebordement();
                item.action().run();
            }));
        }
        return items;
    }

    /** La taille mesurée d'une vue — l'ancre y anticipe la pose. */
    private PopupPlacer.Cadre tailleMesuree(View vue) {
        vue.measure(View.MeasureSpec.makeMeasureSpec(0,
                        View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0,
                        View.MeasureSpec.UNSPECIFIED));
        return new PopupPlacer.Cadre(0, 0, vue.getMeasuredWidth(),
                vue.getMeasuredHeight());
    }
}
