package jo.celeris.android.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import jo.celeris.core.text.SelRange;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;

/**
 * Le point d'arrêt vit à l'APPUIS LONG de gouttière (D-04, la reprise
 * du design — le constat du propriétaire) : l'appui long dans la
 * gouttière rend la LIGNE visée à l'hôte — c'est lui qui pose, retire
 * ou désactive l'arrêt — sans toucher au curseur ni au clavier ; le
 * mot ne s'y sélectionne pas, la pilule ne s'y offre pas. L'appui long
 * dans le TEXTE garde son comportement : le mot se sélectionne, la
 * documentation peut s'ouvrir — aucune ligne n'y part.
 */
@RunWith(RobolectricTestRunner.class)
public class ArretAppuiLongTest {

    private static final String CONTENU = "alpha beta\ngamma delta\n";

    /**
     * L'appui long dans la gouttière rend la ligne visée à l'hôte — le
     * point d'arrêt s'y pose, le mot ne s'y sélectionne pas.
     */
    @Test
    public void lAppuiLongDeGouttiereRendLaLigne() {
        EditorTestRig banc = EditorTestRig.sur(
                RuntimeEnvironment.getApplication(), CONTENU);
        EditorView vue = banc.vue();
        List<Integer> lignes = new ArrayList<>();
        vue.setSurBasculeArret(lignes::add);
        banc.image(); // la gouttière prend sa largeur à la première image
        // l'appui long au cœur de la gouttière, rangée 1 — la ligne 1
        float y = 1.5f * vue.metrics.lineHeight();
        float x = vue.largeurGouttiere / 2f;
        long t = SystemClock.uptimeMillis();
        vue.onTouchEvent(EditorTestRig.obtenir(t, t,
                MotionEvent.ACTION_DOWN, x, y));
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(
                Duration.ofMillis(900));
        assertEquals("la ligne visée part à l'hôte (D-04)", List.of(1), lignes);
        SelRange plage = vue.session().selection().ranges().get(0);
        assertTrue("le mot sous le doigt ne se sélectionne pas",
                plage.isEmpty());
        vue.onTouchEvent(EditorTestRig.obtenir(t, SystemClock.uptimeMillis(),
                MotionEvent.ACTION_UP, x, y));
    }

    /**
     * L'appui long dans le TEXTE garde son comportement : le mot se
     * sélectionne — aucune ligne d'arrêt ne part.
     */
    @Test
    public void lAppuiLongDansLeTexteNeBasculeRien() {
        EditorTestRig banc = EditorTestRig.sur(
                RuntimeEnvironment.getApplication(), CONTENU);
        EditorView vue = banc.vue();
        List<Integer> lignes = new ArrayList<>();
        vue.setSurBasculeArret(lignes::add);
        banc.image(); // la gouttière prend sa largeur à la première image
        // l'appui long au cœur de « gamma » (rangée 1, colonnes 0..4)
        float y = 1.5f * vue.metrics.lineHeight();
        float x = vue.largeurGouttiere + 2f * vue.metrics.charWidth();
        long t = SystemClock.uptimeMillis();
        vue.onTouchEvent(EditorTestRig.obtenir(t, t,
                MotionEvent.ACTION_DOWN, x, y));
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(
                Duration.ofMillis(900));
        assertTrue("aucune ligne d'arrêt ne part du texte",
                lignes.isEmpty());
        SelRange plage = vue.session().selection().ranges().get(0);
        assertEquals("le mot « gamma » se sélectionne comme avant",
                CONTENU.indexOf("gamma"), plage.start());
        assertEquals(CONTENU.indexOf("gamma") + 5, plage.end());
        vue.onTouchEvent(EditorTestRig.obtenir(t, SystemClock.uptimeMillis(),
                MotionEvent.ACTION_UP, x, y));
    }
}
