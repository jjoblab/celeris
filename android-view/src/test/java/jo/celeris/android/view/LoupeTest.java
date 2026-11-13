package jo.celeris.android.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;

import java.time.Duration;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;

/**
 * La loupe du dragage (D-16 — le redressement du propriétaire
 * 2026-11-13 : « ajoute la fonctionnalité de magnifier, quand on fait
 * longclick et on commence à draguer ») : le zoom, le suivi amorti et
 * snappé, et la conduite réelle — l'appui long arme le dragage, la
 * loupe naît AU PREMIER DÉPLACEMENT du dragage, le geste levé la
 * referme. Les BORNES de la bulle vivent dans
 * {@code MagnifierGeometryTest} (la source unique des bornes).
 *
 * <p>Banc réel (ADR-013) : de vrais {@link MotionEvent} à la vue posée,
 * l'horloge avance au looper principal, le rendu natif photographie la
 * frame — la bulle y rend vraiment.</p>
 */
@RunWith(RobolectricTestRunner.class)
public class LoupeTest {

    private static final String CONTENU = "alpha beta";

    /** Le centre x de la bulle ne sort jamais des bords horizontaux. */
    @Test
    public void borneCxContientLaBulleDansLaVue() {
        // vue large : le doigt borne librement, la bulle tient
        assertEquals(500f, Loupe.borneCx(500f, 2000f, 100f), 1e-6f);
        // doigt au bord gauche : la bulle s'y colle (le centre vaut le rayon)
        assertEquals(100f, Loupe.borneCx(0f, 2000f, 100f), 1e-6f);
        // doigt au bord droit : la bulle s'y colle
        assertEquals(1900f, Loupe.borneCx(2000f, 2000f, 100f), 1e-6f);
        // vue plus étroite que la bulle : son centre
        assertEquals(75f, Loupe.borneCx(0f, 150f, 100f), 1e-6f);
    }

    /** Le centre y de la bulle : AU-DESSUS si la place suffit, sinon
     *  au-dessous, sinon borné — jamais coupée par un bord. */
    @Test
    public void resoudreCyGardeLaBulleVisible() {
        // au milieu de la vue : au-dessus (1,8 rayon au-dessus du doigt)
        assertEquals(400f - 180f, Loupe.resoudreCy(400f, 2000f, 100f), 1e-6f);
        // doigt trop haut : la bulle passe SOUS le doigt
        assertEquals(50f + 180f, Loupe.resoudreCy(50f, 2000f, 100f), 1e-6f);
        // vue serrée : ni au-dessus ni au-dessous — borné dans ce qui reste
        assertEquals(100f, Loupe.resoudreCy(50f, 240f, 100f), 1e-6f);
        // vue plus petite que la bulle : son centre
        assertEquals(75f, Loupe.resoudreCy(50f, 150f, 100f), 1e-6f);
    }

    /** Le zoom se règle, le plancher tient — la loupe ne rétrécit jamais. */
    @Test
    public void leZoomSeRegleAvecUnPlancher() {
        Loupe loupe = new Loupe();
        assertEquals(Loupe.ZOOM_DEFAUT, loupe.zoom(), 1e-6f);
        loupe.setZoom(3f);
        assertEquals(3f, loupe.zoom(), 1e-6f);
        loupe.setZoom(0.5f);
        assertEquals(1f, loupe.zoom(), 1e-6f);
    }

    /** La position suit : y snappé à la rangée réglée, x amorti à α=0,55. */
    @Test
    public void laPositionSuitLeDoigtAmortieEtSnappee() {
        Loupe loupe = new Loupe();
        assertFalse(loupe.active());
        // le premier suivi pose la position SANS amortir (départ net)
        loupe.suivre(100f, 40f);
        assertTrue(loupe.active());
        assertEquals(100f, loupe.x(), 1e-6f);
        assertEquals(40f, loupe.y(), 1e-6f);
        // le suivi d'après amortit x : 100 + (200-100)*0,55 = 155
        loupe.suivre(200f, 60f);
        assertEquals(155f, loupe.x(), 1e-6f);
        // y prend TOUJOURS le centre de rangée donné, sans mémoire
        assertEquals(60f, loupe.y(), 1e-6f);
        // la fermeture referme la loupe
        loupe.cesser();
        assertFalse(loupe.active());
    }

    /**
     * La conduite réelle : l'appui long arme le dragage, la loupe naît
     * au premier déplacement du dragage et suit, le geste levé la
     * referme — la photographie de la frame se rend (rendu natif).
     */
    @Test
    public void laLoupeNaitAuDragageDeLappuiLongEtSeReferme() {
        EditorTestRig banc = EditorTestRig.sur(
                RuntimeEnvironment.getApplication(), CONTENU);
        EditorView vue = banc.vue();
        float y = vue.metrics.lineHeight() / 2f;
        long t = SystemClock.uptimeMillis();
        // l'appui long : pas encore de loupe — la sélection seule se pose
        vue.onTouchEvent(EditorTestRig.obtenir(t, t,
                MotionEvent.ACTION_DOWN, xDe(banc, 2), y));
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(
                Duration.ofMillis(900));
        assertFalse("l'appui long seul n'ouvre pas la loupe",
                vue.loupe().active());
        // le premier déplacement du dragage : la loupe naît
        glisser(banc, t, xDe(banc, 2), y, xDe(banc, 6));
        assertTrue("le dragage de l'appui long ouvre la loupe",
                vue.loupe().active());
        assertTrue("la loupe vit au-dessus du doigt",
                vue.loupe().y() < y + vue.metrics.lineHeight());
        // la photographie de la frame se rend (rendu natif Skia)
        assertNotNull("la toile de la loupe se rend",
                vue.loupe().toileDe(vue.getWidth(), vue.getHeight()));
        // le geste se lève : la loupe se referme
        vue.onTouchEvent(EditorTestRig.obtenir(t, SystemClock.uptimeMillis(),
                MotionEvent.ACTION_UP, xDe(banc, 6), y));
        assertFalse("le doigt levé referme la loupe",
                vue.loupe().active());
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
     * gauche du glyphe, jamais son centre (PIEGE-06).
     */
    private static float xDe(EditorTestRig banc, int offset) {
        EditorView vue = banc.vue();
        vue.gutterWidth();
        return vue.largeurGouttiere + offset * vue.metrics.charWidth() + 1f;
    }
}
