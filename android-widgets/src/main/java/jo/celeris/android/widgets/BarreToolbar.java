package jo.celeris.android.widgets;

import android.view.ViewTreeObserver;

import java.util.ArrayList;
import java.util.List;

/**
 * La barre d'application de la scène d'édition (le design du dépôt de
 * départ) : le menu de DÉBORDEMENT de la toolbar ne vit pas au menu
 * système — la CARTE du kit s'y ouvre, pendue sous la puce au bord
 * droit. La BARRE DE TOUCHES de code vit au bas de la scène,
 * AU-DESSUS DU CLAVIER (le dépôt de départ, 4.5.0) : Tab,
 * commentaire, déplacements de lignes, duplication — l'hôte y pose
 * ses touches.
 *
 * <p>La carte vit son PROPRE genre ({@code TOOLBAR_OVERFLOW}) — deux
 * menus ne tiennent jamais ; l'observateur du layout global se garde
 * RETIRABLE ({@link #detacher()}), jamais une référence de méthode ne
 * s'y perd.</p>
 */
public final class BarreToolbar {

    /** La respiration des ancres aux bords de la couche, en dp. */
    private static final float MARGE_DP = 8f;

    private final PopupManager popups;
    private final IntentionsMenuView carte;
    /** Le titre de la section du débordement — l'état NavMenu de la carte. */
    private final String section;
    private final TouchBarView barreTouches;
    private final int margePx;
    /** L'observateur du layout global — gardé, jamais perdu, retirable. */
    private final ViewTreeObserver.OnGlobalLayoutListener observateur;

    /**
     * Construit la barre d'application sur le coordinateur des popups :
     * la carte naît liée au genre débordement de toolbar, la barre de
     * touches au sien.
     *
     * @param popups  le coordinateur des popups de la scène
     * @param palette la palette dérivée du thème courant
     * @param section le titre de section du débordement
     */
    public BarreToolbar(PopupManager popups, PopupPalette palette,
            String section) {
        this.popups = popups;
        this.carte = new IntentionsMenuView(popups.couche().getContext(),
                palette);
        popups.lier(PopupController.PopupKind.TOOLBAR_OVERFLOW, carte);
        this.section = section;
        this.barreTouches = new TouchBarView(popups.couche().getContext(),
                palette);
        popups.lier(PopupController.PopupKind.TOUCH_BAR, barreTouches);
        this.margePx = Math.round(MARGE_DP
                * popups.couche().getResources().getDisplayMetrics().density);
        // la touchbar se REPOSE au changement de layout : la rotation
        // la recolle au bas de la scène — l'observateur garde SA
        // référence, jamais la perte ne se paie au détachement
        observateur = this::reposerBarre;
        popups.couche().getViewTreeObserver()
                .addOnGlobalLayoutListener(observateur);
    }

    /**
     * Ouvre (ou ré-ancre) la carte de débordement sous la puce de la
     * toolbar : les actions repliées y courent en rangées cliquables,
     * la carte se referme sur le choix AVANT de l'exécuter.
     *
     * @param actions les actions repliées derrière la puce
     */
    public void ouvrirDebordement(List<IntentionsMenuView.Item> actions) {
        if (actions.isEmpty()) {
            return;
        }
        // la carte prend son état NavMenu : la section du débordement
        carte.setSection(section);
        carte.setActions(items(actions));
        PopupPlacer.Cadre taille = popups.taille(
                PopupController.PopupKind.TOOLBAR_OVERFLOW);
        // la carte pend sous la puce : le long du bord DROIT, en tête
        // de scène — la puce de la toolbar vit au-dessus à droite
        int x = Math.max(margePx,
                popups.couche().getWidth() - margePx - taille.largeur());
        int y = margePx;
        popups.montrer(PopupController.PopupKind.TOOLBAR_OVERFLOW, x, y);
    }

    /** Ferme la carte — la couche suit. */
    public void fermerDebordement() {
        popups.fermer(PopupController.PopupKind.TOOLBAR_OVERFLOW);
    }

    /**
     * Pose la barre de touches au bas de la scène — AU-DESSUS DU
     * CLAVIER : l'hôte y apporte ses touches, la barre vit avec le
     * document.
     *
     * @param touches les touches de code, dans l'ordre d'affichage
     */
    public void poserBarreTouches(List<TouchBarView.Touche> touches) {
        barreTouches.setActions(touches);
        popups.controleur().open(PopupController.PopupKind.TOUCH_BAR);
        reposerBarre();
    }

    /** La barre de touches — l'hôte y rafraîchit les états activés. */
    public TouchBarView barreTouches() {
        return barreTouches;
    }

    /** La palette tourne : la barre de touches la suit. */
    public void surPalette(PopupPalette palette) {
        barreTouches.setPalette(palette);
        carte.setPalette(palette);
    }

    /**
     * Le détachement de fin de vie : l'observateur du layout se retire
     * — jamais la fuite ne se paie.
     */
    public void detacher() {
        popups.couche().getViewTreeObserver()
                .removeOnGlobalLayoutListener(observateur);
    }

    /** Recolle la barre au bas de la scène — au layout global. */
    private void reposerBarre() {
        if (!popups.estOuvert(PopupController.PopupKind.TOUCH_BAR)
                || barreTouches.getMeasuredWidth() <= 0) {
            return;
        }
        PopupPlacer.Cadre taille = new PopupPlacer.Cadre(0, 0,
                barreTouches.getMeasuredWidth(),
                barreTouches.getMeasuredHeight());
        int x = Math.max(margePx,
                (popups.couche().getWidth() - taille.largeur()) / 2);
        int y = Math.max(margePx,
                popups.couche().getHeight() - taille.hauteur() - margePx);
        popups.montrer(PopupController.PopupKind.TOUCH_BAR, x, y);
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
}
