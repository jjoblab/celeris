package jo.celeris.android.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;

import java.time.Duration;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import jo.celeris.core.text.SelRange;

/**
 * Le double-tap (le signal du propriétaire du 2026-11-13 : « ajouter
 * aussi le double clic dessus pour afficher le popups couleur picker »)
 * : le second tap rapproché rend son offset à l'hôte — son verdict
 * CONSUMME le geste (la cascade du tap simple ne suit pas : le curseur
 * ne bouge pas, le clavier ne s'arme pas) ou le laisse passer (le tap
 * retombe dans la cascade). Le tap trop écarté n'est jamais un double.
 *
 * <p>Banc réel (ADR-013) : de vrais {@link MotionEvent} à la vue posée,
 * l'horloge avance au looper principal.</p>
 */
@RunWith(RobolectricTestRunner.class)
public class DoubleTapViewTest {

    private static final String CONTENU = "fond #FF5722";

    /** Le double-tap consommé : le verdict part, le curseur tient. */
    @Test
    public void doubleTapConsommeLeGeste() {
        EditorTestRig banc = EditorTestRig.sur(
                RuntimeEnvironment.getApplication(), CONTENU);
        EditorView vue = banc.vue();
        int[] verdicts = new int[1];
        int[] appels = new int[1];
        vue.setSurDoubleTap(offset -> {
            appels[0]++;
            verdicts[0] = offset;
            return true;
        });
        float y = vue.metrics.lineHeight() / 2f;
        long t = SystemClock.uptimeMillis();
        // premier tap au cœur du littéral : la cascade du simple pose le curseur
        vue.onTouchEvent(EditorTestRig.obtenir(t, t,
                MotionEvent.ACTION_DOWN, xDe(banc, 9), y));
        vue.onTouchEvent(EditorTestRig.obtenir(t, t + 16L,
                MotionEvent.ACTION_UP, xDe(banc, 9), y));
        SelRange apresPremier = vue.session().selection().ranges().get(0);
        assertEquals(9, apresPremier.caret());
        // second tap rapproché : l'hôte consomme, la cascade ne suit pas
        vue.onTouchEvent(EditorTestRig.obtenir(t + 100L, t + 100L,
                MotionEvent.ACTION_DOWN, xDe(banc, 10), y));
        vue.onTouchEvent(EditorTestRig.obtenir(t + 100L, t + 116L,
                MotionEvent.ACTION_UP, xDe(banc, 10), y));
        assertEquals("le verdict part une fois", 1, appels[0]);
        assertTrue("l'offset rendu vise le littéral",
                verdicts[0] >= 9 && verdicts[0] <= 11);
        assertEquals("le curseur ne bouge pas — le geste est consommé",
                9, vue.session().selection().ranges().get(0).caret());
    }

    /** Le double-tap non consommé : le tap retombe dans la cascade. */
    @Test
    public void doubleTapNonConsommeRetombeDansLaCascade() {
        EditorTestRig banc = EditorTestRig.sur(
                RuntimeEnvironment.getApplication(), CONTENU);
        EditorView vue = banc.vue();
        vue.setSurDoubleTap(offset -> false);
        float y = vue.metrics.lineHeight() / 2f;
        long t = SystemClock.uptimeMillis();
        vue.onTouchEvent(EditorTestRig.obtenir(t, t,
                MotionEvent.ACTION_DOWN, xDe(banc, 9), y));
        vue.onTouchEvent(EditorTestRig.obtenir(t, t + 16L,
                MotionEvent.ACTION_UP, xDe(banc, 9), y));
        vue.onTouchEvent(EditorTestRig.obtenir(t + 100L, t + 100L,
                MotionEvent.ACTION_DOWN, xDe(banc, 10), y));
        vue.onTouchEvent(EditorTestRig.obtenir(t + 100L, t + 116L,
                MotionEvent.ACTION_UP, xDe(banc, 10), y));
        assertEquals("le curseur suit le second tap — la cascade a joué",
                10, vue.session().selection().ranges().get(0).caret());
    }

    /** Le tap trop écarté n'est pas un double : le verdict ne part jamais. */
    @Test
    public void tapTropEcarteNestPasUnDouble() {
        EditorTestRig banc = EditorTestRig.sur(
                RuntimeEnvironment.getApplication(), CONTENU);
        EditorView vue = banc.vue();
        int[] appels = new int[1];
        vue.setSurDoubleTap(offset -> {
            appels[0]++;
            return true;
        });
        float y = vue.metrics.lineHeight() / 2f;
        long t = SystemClock.uptimeMillis();
        vue.onTouchEvent(EditorTestRig.obtenir(t, t,
                MotionEvent.ACTION_DOWN, xDe(banc, 9), y));
        vue.onTouchEvent(EditorTestRig.obtenir(t, t + 16L,
                MotionEvent.ACTION_UP, xDe(banc, 9), y));
        // un demi-seconde plus tard : deux taps simples, jamais un double
        vue.onTouchEvent(EditorTestRig.obtenir(t + 500L, t + 500L,
                MotionEvent.ACTION_DOWN, xDe(banc, 10), y));
        vue.onTouchEvent(EditorTestRig.obtenir(t + 500L, t + 516L,
                MotionEvent.ACTION_UP, xDe(banc, 10), y));
        assertEquals("aucun verdict ne part", 0, appels[0]);
        assertEquals("le curseur suit le second tap simple",
                10, vue.session().selection().ranges().get(0).caret());
        // le looper reste propre pour les bancs suivants
        org.robolectric.Shadows.shadowOf(Looper.getMainLooper())
                .idleFor(Duration.ofMillis(50L));
    }

    /**
     * L'abscisse du bord gauche du caractère à l'offset donné — le quart
     * gauche du glyphe, jamais son centre (PIEGE-06).
     */
    private static float xDe(EditorTestRig banc, int offset) {
        EditorView vue = banc.vue();
        vue.gutterWidth();
        return vue.largeurGouttiere + offset * vue.metrics.charWidth() + 1f;
    }
}
