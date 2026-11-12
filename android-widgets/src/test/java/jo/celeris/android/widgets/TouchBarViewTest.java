package jo.celeris.android.widgets;

import static org.junit.Assert.assertEquals;
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
 * La barre de touches de code (le portage du dépôt de départ, 4.5.0) :
 * la rangée se mesure à ses touches — cible tactile de 44 dp chacune,
 * l'écart de 6 dp —, l'appui CHOISIT (le chemin du doigt et celui de
 * TalkBack ne font qu'un), la touche MUETTE rejette le doigt, la
 * hauteur tient à 44 dp. Le genre TOUCH_BAR vit avec le document :
 * ni la frappe ni le défilement ne la ferment.
 */
@RunWith(RobolectricTestRunner.class)
public class TouchBarViewTest {

    private Context contexte;

    @Before
    public void preparer() {
        contexte = RuntimeEnvironment.getApplication();
    }

    private void mesurer(android.view.View vue) {
        vue.measure(android.view.View.MeasureSpec.makeMeasureSpec(0,
                        android.view.View.MeasureSpec.UNSPECIFIED),
                android.view.View.MeasureSpec.makeMeasureSpec(0,
                        android.view.View.MeasureSpec.UNSPECIFIED));
    }

    private void toucher(TouchBarView vue, int action, float x, float y) {
        long t = android.os.SystemClock.uptimeMillis();
        vue.onTouchEvent(MotionEvent.obtain(t, t + 16L, action, x, y, 0));
    }

    /** La rangée se mesure : chaque touche au moins 44 dp, l'écart de 6 dp. */
    @Test
    public void laRangeeSeMesureASesTouches() {
        TouchBarView vue = new TouchBarView(contexte, PopupPalette.dark());
        vue.setActions(List.of(
                new TouchBarView.Touche("Tab", null, () -> { }),
                new TouchBarView.Touche("Commenter", null, () -> { })));
        mesurer(vue);
        float densite = contexte.getResources().getDisplayMetrics().density;
        float cible = 44f * densite;
        float ecart = 6f * densite;
        // deux touches : les largeurs de texte + respirations, plus
        // l'écart — au moins deux cibles et un écart, la hauteur 44 dp
        assertTrue("la rangée tient deux cibles au moins",
                vue.getMeasuredWidth() >= 2f * cible + ecart);
        assertTrue("la hauteur tient à 44 dp (le dépôt de départ)",
                vue.getMeasuredHeight() >= Math.round(44f * densite));
    }

    /** L'appui choisit — le chemin tactile et celui de TalkBack ne font qu'un. */
    @Test
    public void lappuiChoisitLaTouche() {
        AtomicInteger courses = new AtomicInteger();
        TouchBarView vue = new TouchBarView(contexte, PopupPalette.dark());
        vue.setActions(List.of(
                new TouchBarView.Touche("Tab", null, () -> { }),
                new TouchBarView.Touche("Commenter", null,
                        courses::incrementAndGet)));
        mesurer(vue);
        float densite = contexte.getResources().getDisplayMetrics().density;
        float padding = vue.getMeasuredWidth();
        // le doigt pose puis relâche au cœur de la seconde touche : sa
        // largeur vit après la première — vise le tiers du milieu
        float x = vue.getMeasuredWidth() * 0.75f;
        toucher(vue, MotionEvent.ACTION_DOWN, x, vue.getMeasuredHeight() / 2f);
        toucher(vue, MotionEvent.ACTION_UP, x, vue.getMeasuredHeight() / 2f);
        assertEquals("l'appui choisit la touche visée", 1, courses.get());
    }

    /** La touche muette rejette le doigt — l'état activé décide. */
    @Test
    public void laToucheMuetteRejetteLeDoigt() {
        AtomicInteger courses = new AtomicInteger();
        TouchBarView vue = new TouchBarView(contexte, PopupPalette.dark());
        vue.setActions(List.of(
                new TouchBarView.Touche("Muette", () -> false,
                        courses::incrementAndGet)));
        mesurer(vue);
        float x = vue.getMeasuredWidth() * 0.5f;
        toucher(vue, MotionEvent.ACTION_DOWN, x, vue.getMeasuredHeight() / 2f);
        toucher(vue, MotionEvent.ACTION_UP, x, vue.getMeasuredHeight() / 2f);
        assertEquals("la touche muette ne court jamais", 0, courses.get());
    }

    /** Le genre TOUCH_BAR vit avec le document : ni la frappe ni le
     *  défilement ne la ferment. */
    @Test
    public void leGenreVitAvecLeDocument() {
        PopupController controleur = new PopupController();
        assertTrue(controleur.open(PopupController.PopupKind.TOUCH_BAR));
        controleur.onKeystroke();
        assertTrue("la frappe ne ferme pas la barre de touches",
                controleur.isOpen(PopupController.PopupKind.TOUCH_BAR));
        controleur.onScroll();
        assertTrue("le défilement ne ferme pas la barre de touches",
                controleur.isOpen(PopupController.PopupKind.TOUCH_BAR));
    }
}
