package jo.celeris.android.widgets;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * La barre d'application du kit (déménagée du terrain de démonstration,
 * décision du propriétaire du 2026-11-14) : la carte de débordement et
 * la barre de touches naissent liées à leurs genres, la puce pose la
 * carte au coin de scène — les actions vides n'en posent aucune —, la
 * barre de touches vit posée par l'hôte, et le détachement retire
 * l'observateur du layout sans rien laisser fuir.
 */
@RunWith(RobolectricTestRunner.class)
public class BarreToolbarTest {

    private Context contexte;

    @Before
    public void preparer() {
        contexte = RuntimeEnvironment.getApplication();
    }

    /** Le coordinateur d'essai : couche mesurée 300 × 200. */
    private PopupManager manager() {
        PopupLayer couche = new PopupLayer(contexte);
        couche.layout(0, 0, 300, 200);
        return new PopupManager(new PopupController(), couche);
    }

    /** Les deux vues naissent liées à leurs genres — fermées
     *  jusqu'à la puce et jusqu'à la pose de l'hôte. */
    @Test
    public void lesVuesViventLieesALeursGenres() {
        PopupManager popups = manager();
        new BarreToolbar(popups, PopupPalette.dark(), "MORE");
        assertTrue("la carte du débordement vit liée",
                popups.vue(PopupController.PopupKind.TOOLBAR_OVERFLOW)
                        instanceof IntentionsMenuView);
        assertTrue("la barre de touches vit liée",
                popups.vue(PopupController.PopupKind.TOUCH_BAR)
                        instanceof TouchBarView);
        assertFalse("liée ne veut pas dire ouverte",
                popups.estOuvert(
                        PopupController.PopupKind.TOOLBAR_OVERFLOW));
        assertFalse(popups.estOuvert(
                PopupController.PopupKind.TOUCH_BAR));
    }

    /** La puce pose la carte au coin — les actions vides n'en posent
     *  aucune, la fermeture la rend. */
    @Test
    public void laPucePoseLaCarteAuCoin() {
        PopupManager popups = manager();
        BarreToolbar barre = new BarreToolbar(popups, PopupPalette.dark(),
                "MORE");
        barre.ouvrirDebordement(List.of());
        assertFalse("sans action, la carte ne se pose pas",
                popups.estOuvert(
                        PopupController.PopupKind.TOOLBAR_OVERFLOW));
        barre.ouvrirDebordement(List.of(new IntentionsMenuView.Item(
                "Réglages", () -> { })));
        assertTrue("la puce pose la carte",
                popups.estOuvert(
                        PopupController.PopupKind.TOOLBAR_OVERFLOW));
        barre.fermerDebordement();
        assertFalse("la fermeture rend la carte",
                popups.estOuvert(
                        PopupController.PopupKind.TOOLBAR_OVERFLOW));
    }

    /** La barre de touches vit posée par l'hôte — ses touches y
     *  courent, le détachement ne la referme pas. */
    @Test
    public void laBarreDeTouchesVitPoseeParLhote() {
        PopupManager popups = manager();
        BarreToolbar barre = new BarreToolbar(popups, PopupPalette.dark(),
                "MORE");
        barre.poserBarreTouches(List.of(new TouchBarView.Touche("Tab",
                null, () -> { })));
        assertTrue("la pose ouvre le genre touchbar",
                popups.estOuvert(PopupController.PopupKind.TOUCH_BAR));
        assertSame("la barre posée vit sous son genre",
                barre.barreTouches(),
                popups.vue(PopupController.PopupKind.TOUCH_BAR));
        barre.detacher();
        assertTrue("le détachement ne referme pas la barre posée",
                popups.estOuvert(PopupController.PopupKind.TOUCH_BAR));
    }
}
