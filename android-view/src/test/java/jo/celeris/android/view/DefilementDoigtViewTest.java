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
 * Le défilement au doigt et le clavier (redressement demandé par le
 * propriétaire) : « je ne peux pas défiler horizontal et vertical sans
 * que le clavier ne devienne visible ». La discipline du dépôt de
 * départ reprend ses droits : le DOIGT POSÉ n'ouvre RIEN — ni clavier,
 * ni curseur — ; le geste qui dépasse le seuil de tap devient un
 * DÉFILEMENT (les deux axes, un doigt comme deux) ; le relâchement
 * SANS mouvement est un TAP, lui seul pose le curseur et ouvre le
 * clavier.
 *
 * <p>Banc réel (ADR-013) : de vrais {@link MotionEvent} à la vue posée,
 * le temps avance au looper principal — le détecteur d'appui long de la
 * plateforme s'y déclenche comme sur l'appareil.</p>
 */
@RunWith(RobolectricTestRunner.class)
public class DefilementDoigtViewTest {

    /** Le seuil de tap : au-delà, le geste est un défilement (px). */
    private static final float SEUIL_TAP = 24f;

    /** Un contenu long : le défilement vertical a de la matière. */
    private static final String CONTENU_LONG = "ligne\n".repeat(300);

    /**
     * Le panorama à DEUX doigts ne montre pas le clavier : le premier
     * doigt posé n'arme plus l'IME — le constat exact du propriétaire.
     */
    @Test
    public void panoramiqueDeuxDoigtsSansClavier() {
        EditorTestRig banc = EditorTestRig.sur(
                RuntimeEnvironment.getApplication(), CONTENU_LONG);
        EditorView vue = banc.vue();
        banc.image();
        long t = SystemClock.uptimeMillis();
        float x = 500f;
        // le premier doigt se pose, le second suit : le panoramique s'arme
        vue.onTouchEvent(EditorTestRig.obtenir(t, t,
                MotionEvent.ACTION_DOWN, x, 1500f));
        vue.onTouchEvent(EditorTestRig.deuxDoigts(t + 16L,
                MotionEvent.ACTION_POINTER_DOWN | (1
                        << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                x, 1500f, x + 100f, 1600f));
        for (int cran = 1; cran <= 10; cran++) {
            vue.onTouchEvent(EditorTestRig.deuxDoigts(t + 16L * (cran + 1),
                    MotionEvent.ACTION_MOVE,
                    x, 1500f - 40f * cran, x + 100f, 1600f - 40f * cran));
        }
        assertTrue("le panoramique défile verticalement", vue.scrollY > 0f);
        assertFalse("le panoramique n'ouvre pas le clavier",
                vue.curseurClavier.clavierDemande());
        vue.onTouchEvent(EditorTestRig.obtenir(t, SystemClock.uptimeMillis(),
                MotionEvent.ACTION_UP, x, 1100f));
    }

    /**
     * Le glissement À UN doigt au-delà du seuil défile sans montrer le
     * clavier — le geste naturel de lecture, les deux axes compris.
     */
    @Test
    public void glissementUnDoigtDefleSansClavier() {
        EditorTestRig banc = EditorTestRig.sur(
                RuntimeEnvironment.getApplication(), CONTENU_LONG);
        EditorView vue = banc.vue();
        banc.image();
        long t = SystemClock.uptimeMillis();
        float x = vue.largeurGouttiere + 40f;
        // le doigt monte : le contenu suit, le défilement vertical s'engage
        vue.onTouchEvent(EditorTestRig.obtenir(t, t,
                MotionEvent.ACTION_DOWN, x, 1500f));
        for (int cran = 1; cran <= 20; cran++) {
            vue.onTouchEvent(EditorTestRig.obtenir(t, t + 16L * cran,
                    MotionEvent.ACTION_MOVE, x, 1500f - 20f * cran));
        }
        assertTrue("le glissement vertical défile", vue.scrollY > 0f);
        assertFalse("le glissement n'ouvre pas le clavier",
                vue.curseurClavier.clavierDemande());
        // le doigt glisse ensuite vers la gauche : l'axe horizontal suit
        for (int cran = 1; cran <= 20; cran++) {
            vue.onTouchEvent(EditorTestRig.obtenir(t, t + 320L + 16L * cran,
                    MotionEvent.ACTION_MOVE, x - 15f * cran, 1100f));
        }
        vue.onTouchEvent(EditorTestRig.obtenir(t, SystemClock.uptimeMillis(),
                MotionEvent.ACTION_UP, x - 300f, 1100f));
        assertFalse("le relâchement du glissement n'ouvre pas le clavier",
                vue.curseurClavier.clavierDemande());
    }

    /**
     * Le glissement horizontal à un doigt défile l'axe X : une longue
     * ligne dépasse la fenêtre, le doigt qui tire à gauche révèle la
     * droite.
     */
    @Test
    public void glissementHorizontalDefleAxeX() {
        String longueLigne = "alpha ".repeat(400);
        EditorTestRig banc = EditorTestRig.sur(
                RuntimeEnvironment.getApplication(), longueLigne);
        EditorView vue = banc.vue();
        banc.image();
        long t = SystemClock.uptimeMillis();
        float y = vue.metrics.lineHeight() / 2f;
        float x = vue.largeurGouttiere + 600f;
        vue.onTouchEvent(EditorTestRig.obtenir(t, t,
                MotionEvent.ACTION_DOWN, x, y));
        for (int cran = 1; cran <= 20; cran++) {
            vue.onTouchEvent(EditorTestRig.obtenir(t, t + 16L * cran,
                    MotionEvent.ACTION_MOVE, x - 20f * cran, y));
        }
        assertTrue("le glissement horizontal défile l'axe X",
                vue.scrollX > 0f);
        assertFalse("aucun clavier dans le geste horizontal",
                vue.curseurClavier.clavierDemande());
        vue.onTouchEvent(EditorTestRig.obtenir(t, SystemClock.uptimeMillis(),
                MotionEvent.ACTION_UP, x - 400f, y));
    }

    /**
     * Le TAP — doigt posé puis levé SANS mouvement — pose le curseur et
     * ouvre le clavier : c'est le seul geste qui l'arme.
     */
    @Test
    public void tapPoseCurseurEtOuvreClavier() {
        EditorTestRig banc = EditorTestRig.sur(
                RuntimeEnvironment.getApplication(), "alpha beta");
        EditorView vue = banc.vue();
        float y = vue.metrics.lineHeight() / 2f;
        long t = SystemClock.uptimeMillis();
        float x = xDe(banc, 2);
        vue.onTouchEvent(EditorTestRig.obtenir(t, t,
                MotionEvent.ACTION_DOWN, x, y));
        assertFalse("le doigt posé n'arme pas encore le clavier",
                vue.curseurClavier.clavierDemande());
        vue.onTouchEvent(EditorTestRig.obtenir(t, t + 50L,
                MotionEvent.ACTION_UP, x, y));
        assertTrue("le tap ouvre le clavier",
                vue.curseurClavier.clavierDemande());
        SelRange plage = vue.session().selection().ranges().get(0);
        assertEquals("le tap pose le curseur au point touché",
                2, plage.caret());
    }

    /**
     * Le petit jitter du doigt — sous le seuil de tap — reste un tap :
     * la main tremble, le clavier s'ouvre quand même.
     */
    @Test
    public void jitterSousLeSeuilResteUnTap() {
        EditorTestRig banc = EditorTestRig.sur(
                RuntimeEnvironment.getApplication(), "alpha beta");
        EditorView vue = banc.vue();
        float y = vue.metrics.lineHeight() / 2f;
        long t = SystemClock.uptimeMillis();
        float x = xDe(banc, 2);
        vue.onTouchEvent(EditorTestRig.obtenir(t, t,
                MotionEvent.ACTION_DOWN, x, y));
        vue.onTouchEvent(EditorTestRig.obtenir(t, t + 16L,
                MotionEvent.ACTION_MOVE, x + SEUIL_TAP / 2f, y + 2f));
        vue.onTouchEvent(EditorTestRig.obtenir(t, t + 50L,
                MotionEvent.ACTION_UP, x + SEUIL_TAP / 2f, y + 2f));
        assertTrue("le jitter sous le seuil reste un tap",
                vue.curseurClavier.clavierDemande());
    }

    /**
     * Le relâchement de l'appui long ne rouvre PAS le clavier : la
     * sélection au doigt survit au doigt levé.
     */
    @Test
    public void relachementAppuiLongNeRouvrePasLeClavier() {
        EditorTestRig banc = EditorTestRig.sur(
                RuntimeEnvironment.getApplication(), "alpha beta");
        EditorView vue = banc.vue();
        float y = vue.metrics.lineHeight() / 2f;
        long t = SystemClock.uptimeMillis();
        float x = xDe(banc, 2);
        vue.onTouchEvent(EditorTestRig.obtenir(t, t,
                MotionEvent.ACTION_DOWN, x, y));
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(900));
        vue.onTouchEvent(EditorTestRig.obtenir(t, SystemClock.uptimeMillis(),
                MotionEvent.ACTION_UP, x, y));
        assertFalse("le relâchement de l'appui long garde le clavier caché",
                vue.curseurClavier.clavierDemande());
        SelRange plage = vue.session().selection().ranges().get(0);
        assertEquals("la sélection de l'appui long survit",
                0, plage.start());
    }

    /**
     * L'abscisse du bord gauche du caractère à l'offset donné — la
     * gouttière y devient réelle d'abord (PIEGE-06).
     */
    private static float xDe(EditorTestRig banc, int offset) {
        EditorView vue = banc.vue();
        vue.gutterWidth();
        return vue.largeurGouttiere + offset * vue.metrics.charWidth() + 1f;
    }
}
