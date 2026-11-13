package jo.celeris.android.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;

import java.time.Duration;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import static org.robolectric.Shadows.shadowOf;

import jo.celeris.core.text.SelRange;

/**
 * La sélection par appui long (M7.5, redressement — le constat du
 * propriétaire) : le simple appui pose le curseur et n'étend RIEN au
 * dragage, l'appui long sélectionne le mot sous le doigt et RETIRE le
 * clavier, le dragage qui suit étend la sélection — le mot de départ
 * toujours englobé, l'ancre figée à la sortie du mot (PIEGE-03).
 * Le redressement du 2026-11-13 : AUCUN rectangle plein ne couvre le
 * mot sélectionné — la pilule « ajouter un curseur » s'est retirée,
 * la barre de sélection du dépôt de départ l'offre à la place.
 *
 * <p>Banc réel (ADR-013) : de vrais {@link MotionEvent} à la vue
 * posée, l'appui long part du détecteur de la plateforme — le temps
 * avance au looper principal, comme sur l'appareil.</p>
 */
@RunWith(RobolectricTestRunner.class)
public class LongPressSelectionViewTest {

    private static final String CONTENU = "alpha beta";

    /**
     * L'appui long sélectionne le mot et retire le clavier ; le dragage
     * étend la sélection au-delà du mot, le retour dans le mot ne le
     * ronge pas.
     */
    @Test
    public void appuiLongSelectionneLeMotEtLeDragageEtend() {
        EditorTestRig banc = EditorTestRig.sur(
                RuntimeEnvironment.getApplication(), CONTENU);
        EditorView vue = banc.vue();
        float y = vue.metrics.lineHeight() / 2f;
        long t = SystemClock.uptimeMillis();
        // le doigt se pose au cœur de « alpha » (offsets 0..4)
        vue.onTouchEvent(EditorTestRig.obtenir(t, t,
                MotionEvent.ACTION_DOWN, xDe(banc, 2), y));
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(900));
        SelRange plage = vue.session().selection().ranges().get(0);
        assertEquals("le mot « alpha » commence à la sélection",
                0, plage.start());
        assertEquals("le mot « alpha » entier est sélectionné",
                5, plage.end());
        assertFalse("l'appui long retire le clavier",
                vue.curseurClavier.clavierDemande());
        // le dragage étend vers « beta » : le doigt glisse à droite
        glisser(banc, t, xDe(banc, 2), y, xDe(banc, 8));
        plage = vue.session().selection().ranges().get(0);
        assertEquals("l'ancre reste le début du mot (PIEGE-03)",
                0, plage.start());
        assertTrue("la sélection s'étend au-delà du mot",
                plage.end() >= 8);
        // le retour dans le mot ne le ronge pas
        glisser(banc, t, xDe(banc, 8), y, xDe(banc, 1));
        plage = vue.session().selection().ranges().get(0);
        assertEquals(0, plage.start());
        assertEquals("le mot de départ reste englobé", 5, plage.end());
        vue.onTouchEvent(EditorTestRig.obtenir(t, SystemClock.uptimeMillis(),
                MotionEvent.ACTION_UP, xDe(banc, 1), y));
    }

    /** Le simple appui pose le curseur ; le dragage qui suit ne sélectionne rien. */
    @Test
    public void simpleAppuiPuisDragageNeSelectionneRien() {
        EditorTestRig banc = EditorTestRig.sur(
                RuntimeEnvironment.getApplication(), CONTENU);
        EditorView vue = banc.vue();
        float y = vue.metrics.lineHeight() / 2f;
        long t = SystemClock.uptimeMillis();
        vue.onTouchEvent(EditorTestRig.obtenir(t, t,
                MotionEvent.ACTION_DOWN, xDe(banc, 2), y));
        glisser(banc, t, xDe(banc, 2), y, xDe(banc, 8));
        SelRange plage = vue.session().selection().ranges().get(0);
        assertTrue("le dragage d'un simple appui ne sélectionne pas",
                plage.isEmpty());
        vue.onTouchEvent(EditorTestRig.obtenir(t, SystemClock.uptimeMillis(),
                MotionEvent.ACTION_UP, xDe(banc, 8), y));
    }

    /**
     * Le redressement du 2026-11-13 : l'appui long ne peint RIEN sur le
     * mot — le chrome de la vue reste nu au-dessus de la sélection,
     * c'est la BARRE du dépôt de départ (posée par l'hôte) qui s'y
     * offre ; l'appui qui suit ailleurs pose le curseur à son point.
     */
    @Test
    public void appuiLongNePeintAucunRectangleSurLeMot() {
        EditorTestRig banc = EditorTestRig.sur(
                RuntimeEnvironment.getApplication(), CONTENU);
        EditorView vue = banc.vue();
        float y = vue.metrics.lineHeight() / 2f;
        long t = SystemClock.uptimeMillis();
        vue.onTouchEvent(EditorTestRig.obtenir(t, t,
                MotionEvent.ACTION_DOWN, xDe(banc, 2), y));
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(900));
        SelRange plage = vue.session().selection().ranges().get(0);
        assertEquals("le mot « alpha » entier est sélectionné",
                5, plage.end());
        // l'appui qui suit ailleurs pose le curseur à son point — la
        // cascade du tap vit comme avant, la pilule ne s'y interpose
        t = SystemClock.uptimeMillis();
        vue.onTouchEvent(EditorTestRig.obtenir(t, t,
                MotionEvent.ACTION_DOWN, xDe(banc, 8), y));
        vue.onTouchEvent(EditorTestRig.obtenir(t, t + 16L,
                MotionEvent.ACTION_UP, xDe(banc, 8), y));
        SelRange apres = vue.session().selection().ranges().get(0);
        assertTrue("le tap après l'appui long pose un curseur",
                apres.isEmpty());
        assertEquals("le curseur vit au point du tap", 8, apres.caret());
    }

    /** L'appui long sur le blanc entre deux mots ne sélectionne rien. */
    @Test
    public void appuiLongSurLeBlancNeSelectionneRien() {
        EditorTestRig banc = EditorTestRig.sur(
                RuntimeEnvironment.getApplication(), CONTENU);
        EditorView vue = banc.vue();
        float y = vue.metrics.lineHeight() / 2f;
        long t = SystemClock.uptimeMillis();
        vue.onTouchEvent(EditorTestRig.obtenir(t, t,
                MotionEvent.ACTION_DOWN, xDe(banc, 5), y));
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(900));
        SelRange plage = vue.session().selection().ranges().get(0);
        assertTrue("le blanc entre deux mots ne se sélectionne pas",
                plage.isEmpty());
    }

    /** Glisse le doigt vers la cible, par pas de deux pixels, un MOVE par cran. */
    private static void glisser(EditorTestRig banc, long t,
            float departX, float y, float cibleX) {
        EditorView vue = banc.vue();
        long maintenant = SystemClock.uptimeMillis();
        float courant = departX;
        float pas = Math.signum(cibleX - departX) * 2f;
        while (Math.abs(cibleX - courant) > Math.abs(pas)) {
            courant += pas;
            maintenant += 16L;
            vue.onTouchEvent(EditorTestRig.obtenir(t, maintenant,
                    MotionEvent.ACTION_MOVE, courant, y));
        }
        maintenant += 16L;
        vue.onTouchEvent(EditorTestRig.obtenir(t, maintenant,
                MotionEvent.ACTION_MOVE, cibleX, y));
    }

    /**
     * L'abscisse du bord gauche du caractère à l'offset donné — le quart
     * gauche du glyphe, jamais son centre (PIEGE-06). L'état d'image se
     * prépare D'ABORD : la première touche y passe par le fantôme, la
     * gouttière y devient réelle — les coordonnées se calculent sur la
     * MÊME gouttière que le test de visée.
     */
    private static float xDe(EditorTestRig banc, int offset) {
        EditorView vue = banc.vue();
        vue.gutterWidth();
        return vue.largeurGouttiere + offset * vue.metrics.charWidth() + 1f;
    }
}
