package jo.celeris.android.widgets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * La carte des usages du symbole (catégorie C de l'inventaire — le
 * signal du propriétaire du 2026-11-13, la première surface
 * réveillée) : l'en-tête annonce et ne se prend jamais, l'occurrence
 * se prend au doigt et à la validation déléguée (le MÊME chemin — le
 * Runnable de l'hôte seul décide), la flèche se borne aux occurrences,
 * la page décline — les rangées vivent toutes à découvert.
 */
@RunWith(RobolectricTestRunner.class)
public class ReferencesViewTest {

    private Context contexte;

    @Before
    public void preparer() {
        contexte = RuntimeEnvironment.getApplication();
    }

    private void mesurer(View vue) {
        vue.measure(View.MeasureSpec.makeMeasureSpec(0,
                        View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0,
                        View.MeasureSpec.UNSPECIFIED));
    }

    /** L'en-tête annonce, les occurrences suivent : la carte se
     *  mesure à son contenu — l'en-tête ET les occurrences. */
    @Test
    public void lenteteAnnonceEtLesOccurrencesMesurent() {
        ReferencesView vue = new ReferencesView(contexte, PopupPalette.dark());
        vue.setTitre("3 usages de « total »");
        vue.setOccurrences(List.of(
                new ReferencesView.Occurrence("L1 · int total = 0;", () -> { }),
                new ReferencesView.Occurrence("L2 · total += 1;", () -> { }),
                new ReferencesView.Occurrence("L4 · return total;", () -> { })));
        mesurer(vue);
        assertEquals(3, vue.nombre());
        assertEquals("L2 · total += 1;", vue.occurrenceAt(1).libelle());
        assertNull("l'occurrence hors bornes n'existe pas",
                vue.occurrenceAt(7));
        assertTrue("la carte tient ses rangées", vue.getMeasuredHeight() > 0);
        vue.setTitre(null);
        assertEquals("le titre retiré n'emporte pas les occurrences", 3,
                vue.nombre());
    }

    /** L'appui CHOISIT l'occurrence : le doigt pose puis relâche —
     *  l'en-tête ne se prend jamais, l'occurrence sous le doigt saute. */
    @Test
    public void lappuiChoisitLoccurrencePasLentete() {
        AtomicInteger courses = new AtomicInteger(-1);
        ReferencesView vue = new ReferencesView(contexte, PopupPalette.dark());
        vue.setTitre("2 usages");
        vue.setOccurrences(List.of(
                new ReferencesView.Occurrence("L1", () -> courses.set(1)),
                new ReferencesView.Occurrence("L2", () -> courses.set(2))));
        mesurer(vue);
        long t = android.os.SystemClock.uptimeMillis();
        // l'en-tête d'abord : le relâchement dessus ne choisit rien
        float entete = vue.rowCenterY(0) + vue.paddingYPx;
        vue.onTouchEvent(MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN,
                10f, entete, 0));
        vue.onTouchEvent(MotionEvent.obtain(t, t + 16L, MotionEvent.ACTION_UP,
                10f, entete, 0));
        assertEquals("l'en-tête ne se prend jamais", -1, courses.get());
        // la première occurrence suit l'en-tête : son rang compte le décalage
        float premiere = vue.rowCenterY(1) + vue.paddingYPx;
        vue.onTouchEvent(MotionEvent.obtain(t, t + 32L, MotionEvent.ACTION_DOWN,
                10f, premiere, 0));
        vue.onTouchEvent(MotionEvent.obtain(t, t + 48L, MotionEvent.ACTION_UP,
                10f, premiere, 0));
        assertEquals("le relâchement choisit la première occurrence", 1,
                courses.get());
    }

    /** La flèche déléguée se borne aux occurrences : la surbrillance
     *  part de la tête, s'arrête au bas, remonte bornée — la
     *  validation exécute le saut, la page décline, la carte vide
     *  décline tout. */
    @Test
    public void laFlecheDelegueeSeBorneAuxOccurrences() {
        AtomicInteger courses = new AtomicInteger(-1);
        ReferencesView vue = new ReferencesView(contexte, PopupPalette.dark());
        vue.setTitre("2 usages");
        vue.setOccurrences(List.of(
                new ReferencesView.Occurrence("L1", () -> courses.set(1)),
                new ReferencesView.Occurrence("L2", () -> courses.set(2))));
        assertFalse("la page décline — les rangées vivent à découvert",
                vue.surPage(1));
        assertTrue(vue.surFleche(1));
        assertEquals("la flèche surligne la première occurrence", 0,
                vue.surbrillance());
        assertTrue(vue.surFleche(1));
        assertEquals("la flèche descend", 1, vue.surbrillance());
        assertTrue(vue.surFleche(1));
        assertEquals("la flèche au bas s'arrête", 1, vue.surbrillance());
        assertTrue(vue.surFleche(-2));
        assertEquals("la flèche remonte bornée", 0, vue.surbrillance());
        assertTrue("la validation exécute l'occurrence surlignée",
                vue.surValidation());
        assertEquals(1, courses.get());
        vue.setOccurrences(List.of());
        assertFalse("la carte vide décline la flèche", vue.surFleche(1));
        assertFalse("la carte vide décline la validation",
                vue.surValidation());
    }

    /** L'audit Android Studio : l'IDE de référence montre les usages
     *  de tous les fichiers — l'occurrence ÉTRANGÈRE (au saut nul) vit
     *  muette : ni le doigt ni la flèche ne la prennent, la flèche la
     *  saute. */
    @Test
    public void letrangereVitMueEtJamaisPrise() {
        AtomicInteger courses = new AtomicInteger(-1);
        ReferencesView vue = new ReferencesView(contexte, PopupPalette.dark());
        vue.setTitre("3 usages");
        vue.setOccurrences(List.of(
                new ReferencesView.Occurrence("L1 · locale", () -> courses.set(1)),
                new ReferencesView.Occurrence("autre.kt · L5 · hors document", null),
                new ReferencesView.Occurrence("L7 · locale", () -> courses.set(2))));
        mesurer(vue);
        long t = android.os.SystemClock.uptimeMillis();
        // l'étrangère vit derrière la locale : son rang compte l'en-tête
        float etrangere = vue.rowCenterY(2) + vue.paddingYPx;
        vue.onTouchEvent(MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN,
                10f, etrangere, 0));
        vue.onTouchEvent(MotionEvent.obtain(t, t + 16L, MotionEvent.ACTION_UP,
                10f, etrangere, 0));
        assertEquals("l'étrangère muette ne se prend jamais", -1, courses.get());
        // la flèche saute l'étrangère : la première prenable, puis la suivante
        assertTrue(vue.surFleche(1));
        assertEquals("la flèche part de la première locale", 0,
                vue.surbrillance());
        assertTrue(vue.surFleche(1));
        assertEquals("la flèche saute l'étrangère muette", 2,
                vue.surbrillance());
        assertTrue("la validation exécute la locale surlignée",
                vue.surValidation());
        assertEquals(2, courses.get());
        // une carte d'étrangères seules décline la flèche
        vue.setOccurrences(List.of(
                new ReferencesView.Occurrence("a.kt · L1", null)));
        vue.setTitre(null);
        assertFalse("sans prenable, la flèche décline", vue.surFleche(1));
    }
}
