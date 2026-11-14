package jo.celeris.android.widgets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.MotionEvent;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * Le menu de débordement de la barre de sélection (déménagé du
 * terrain de démonstration au kit, décision du propriétaire du
 * 2026-11-14) : la carte naît liée à SON genre, les actions vides ne
 * posent rien, l'ouverture pose la carte sous la puce — et le CHOIX
 * la referme AVANT d'exécuter l'action (le débordement ne survit
 * jamais au choix, l'en-tête de section ne se prend jamais).
 */
@RunWith(RobolectricTestRunner.class)
public class MenuDebordementTest {

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

    /** La carte naît liée à son genre — fermée jusqu'à la puce. */
    @Test
    public void laCarteVitLieeASonGenre() {
        PopupManager popups = manager();
        new MenuDebordement(popups, PopupPalette.dark(), 8, "MORE");
        assertTrue("la carte du genre vit liée au débordement",
                popups.vue(PopupController.PopupKind.SELECTION_OVERFLOW)
                        instanceof IntentionsMenuView);
        assertFalse("liée ne veut pas dire ouverte",
                popups.estOuvert(
                        PopupController.PopupKind.SELECTION_OVERFLOW));
    }

    /** Sans action repliée, le menu ne se pose pas. */
    @Test
    public void ouvrirVideNePoseRien() {
        PopupManager popups = manager();
        MenuDebordement menu = new MenuDebordement(popups,
                PopupPalette.dark(), 8, "MORE");
        SelectionBarView barre = new SelectionBarView(contexte,
                PopupPalette.dark());
        menu.ouvrir(barre, 120, List.of());
        assertFalse("sans action, la carte ne se pose pas",
                popups.estOuvert(
                        PopupController.PopupKind.SELECTION_OVERFLOW));
    }

    /** Le CHOIX referme la carte AVANT d'exécuter l'action — la
     *  section ne se prend jamais, l'action court après la fermeture. */
    @Test
    public void leChoixRefermeAvantDexecuter() {
        PopupManager popups = manager();
        MenuDebordement menu = new MenuDebordement(popups,
                PopupPalette.dark(), 8, "MORE");
        AtomicInteger passe = new AtomicInteger();
        SelectionBarView barre = new SelectionBarView(contexte,
                PopupPalette.dark());
        // en production, la puce vit sur une barre DÉJÀ posée — la
        // machine n'ouvre le débordement que sur sa barre ouverte ;
        // le menu se pose SOUS LA SÉLECTION (bas 96), jamais sur le
        // caret (l'audit du propriétaire du 2026-11-14)
        popups.controleur().open(PopupController.PopupKind.SELECTION_BAR);
        menu.ouvrir(barre, 96, List.of(new SelectionBarView.Item("Dupliquer",
                jo.celeris.android.widgets.IconeAction.DUPLIQUER,
                passe::incrementAndGet)));
        assertTrue("l'ouverture pose la carte sous la puce",
                popups.estOuvert(
                        PopupController.PopupKind.SELECTION_OVERFLOW));
        IntentionsMenuView carte = (IntentionsMenuView) popups.vue(
                PopupController.PopupKind.SELECTION_OVERFLOW);
        assertTrue("le menu vit SOUS le bas de la sélection — le caret demeure à découvert",
                carte.getTop() >= 96);
        // la rangée 1 : la première action, après l'en-tête de section
        float yRangee = carte.rowCenterY(1);
        carte.onTouchEvent(MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN,
                10f, yRangee, 0));
        assertTrue("l'appui sur une action se prend",
                carte.onTouchEvent(MotionEvent.obtain(0L, 10L,
                        MotionEvent.ACTION_UP, 10f, yRangee, 0)));
        assertEquals("l'action court une fois", 1, passe.get());
        assertFalse("la carte s'est refermée avant l'exécution",
                popups.estOuvert(
                        PopupController.PopupKind.SELECTION_OVERFLOW));
    }
}
