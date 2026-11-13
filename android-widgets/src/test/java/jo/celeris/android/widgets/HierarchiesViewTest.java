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
 * La carte des hiérarchies de types et d'appels (catégorie C de
 * l'inventaire — les moteurs vivaient, la surface dormait) : l'arbre
 * se rend en rangées INDENTÉES par profondeur, les sections annoncent
 * (supertypes, sous-types, appelants — jamais prises), la frontière
 * introuvable se lit muette et ne se prend pas, le choix d'un sommet
 * SAUTE — le MÊME chemin au doigt et à la validation déléguée (les
 * flèches se bornent aux sommets prenants, la page décline).
 */
@RunWith(RobolectricTestRunner.class)
public class HierarchiesViewTest {

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

    /** Un sommet prenable : son libellé, sa profondeur, son saut —
     *  null pour l'introuvable (la frontière hors projet). */
    private static HierarchiesView.Sommet sommet(String libelle, int profondeur,
            Runnable saut) {
        return new HierarchiesView.Sommet(libelle, profondeur, saut);
    }

    /** L'arbre de types se rend : l'en-tête, les sections et l'arbre
     *  se mesurent — la carte grandit de ses rangées, le doute clôt. */
    @Test
    public void larbreDeTypesSeRendEnSections() {
        HierarchiesView vue = new HierarchiesView(contexte, PopupPalette.dark());
        vue.setArbre("Hiérarchie de « Animal »", List.of(
                new HierarchiesView.Section("SUPERTYPES", List.of(
                        sommet("Animal", 0, () -> { }),
                        sommet("Vivant", 1, () -> { }))),
                new HierarchiesView.Section("SOUS-TYPES", List.of(
                        sommet("Chien", 1, () -> { }))),
                new HierarchiesView.Section("HORS PROJET", List.of(
                        new HierarchiesView.Sommet("Serializable", 0, null)))),
                null);
        mesurer(vue);
        assertTrue("la carte tient ses rangées", vue.getMeasuredHeight() > 0);
        assertEquals("l'en-tête, la section des supertypes, ses deux"
                + " sommets, la section des sous-types, son sommet, la"
                + " section de la frontière et l'introuvable", 8,
                vue.rowCount());
        assertEquals("Animal", vue.sommetAt(0).libelle());
        assertNull("le sommet hors bornes n'existe pas", vue.sommetAt(9));
        vue.setArbre(null, List.of(), null);
        assertEquals("l'arbre vide se rend sans rangées", 0, vue.rowCount());
    }

    /** Le choix d'un sommet SAUTE — le Runnable de l'hôte seul décide ;
     *  la section ne se prend jamais (le doigt sur elle ne choisit rien). */
    @Test
    public void leChoixDunSommetSautePasLaSection() {
        AtomicInteger courses = new AtomicInteger(-1);
        HierarchiesView vue = new HierarchiesView(contexte, PopupPalette.dark());
        vue.setArbre("Hiérarchie d'appels de « mesurer »", List.of(
                new HierarchiesView.Section("APPELANTS", List.of(
                        sommet("mesurer", 0, () -> courses.set(0)),
                        sommet("ajouter", 1, () -> courses.set(1)),
                        sommet("Vivant", 2, () -> courses.set(2))))),
                "des sites d'appels ne se résolvent pas");
        mesurer(vue);
        long t = android.os.SystemClock.uptimeMillis();
        // la section d'abord : le relâchement dessus ne choisit rien
        float section = vue.rowCenterY(1) + vue.paddingYPx;
        vue.onTouchEvent(MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN,
                10f, section, 0));
        vue.onTouchEvent(MotionEvent.obtain(t, t + 16L, MotionEvent.ACTION_UP,
                10f, section, 0));
        assertEquals("la section ne se prend jamais", -1, courses.get());
        // le sommet de profondeur 2 suit la section : le doigt y vise son centre
        float cible = vue.rowCenterY(4) + vue.paddingYPx;
        vue.onTouchEvent(MotionEvent.obtain(t, t + 32L, MotionEvent.ACTION_DOWN,
                10f, cible, 0));
        vue.onTouchEvent(MotionEvent.obtain(t, t + 48L, MotionEvent.ACTION_UP,
                10f, cible, 0));
        assertEquals("le relâchement saute le sommet visé", 2, courses.get());
    }

    /** La flèche déléguée se borne aux sommets PRENANTS : la
     *  surbrillance part de la tête, saute l'introuvable, s'arrête au
     *  bas, remonte bornée — la validation exécute le saut du sommet
     *  surligné, la page décline, la carte vide décline tout. */
    @Test
    public void laFlècheSeBorneAuxSommetsPrenants() {
        AtomicInteger courses = new AtomicInteger(-1);
        HierarchiesView vue = new HierarchiesView(contexte, PopupPalette.dark());
        vue.setArbre("Hiérarchie d'appels", List.of(
                new HierarchiesView.Section("APPELANTS", List.of(
                        sommet("mesurer", 0, () -> courses.set(0)),
                        sommet("ajouter", 1, () -> courses.set(1)),
                        // l'introuvable : muet, jamais pris
                        new HierarchiesView.Sommet("Hors", 1, null)))),
                "des sites d'appels ne se résolvent pas");
        assertFalse("la page décline — les rangées vivent à découvert",
                vue.surPage(1));
        assertTrue(vue.surFleche(1));
        assertEquals("la flèche surligne le premier prenable", 0,
                vue.surbrillance());
        assertTrue(vue.surFleche(1));
        assertEquals("la flèche descend prenable", 1, vue.surbrillance());
        assertTrue(vue.surFleche(1));
        assertEquals("la flèche au bas s'arrête", 1, vue.surbrillance());
        assertTrue(vue.surFleche(-2));
        assertEquals("la flèche remonte bornée", 0, vue.surbrillance());
        assertTrue("la validation exécute le saut surligné", vue.surValidation());
        assertEquals(0, courses.get());
        vue.setArbre(null, List.of(), null);
        assertFalse("la carte vide décline la flèche", vue.surFleche(1));
        assertFalse("la carte vide décline la validation",
                vue.surValidation());
    }
}
