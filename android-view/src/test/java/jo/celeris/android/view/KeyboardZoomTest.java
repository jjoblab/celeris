package jo.celeris.android.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * Les raccourcis de bureau du zoom (M7.5, §4) : Ctrl + plus / moins /
 * zéro et la molette Ctrl — chaque raccourci marche d'un palier exact,
 * et l'écouteur de taille prévient l'hôte à chaque changement (D-15).
 */
@RunWith(RobolectricTestRunner.class)
public class KeyboardZoomTest {

    private static KeyEvent touche(int code) {
        return new KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, code, 0,
                KeyEvent.META_CTRL_ON);
    }

    /** Ctrl + plus marche d'un palier, jamais deux. */
    @Test
    public void ctrlPlusZoomsInExactlyOneStep() {
        EditorTestRig banc = EditorTestRig.sur(
                RuntimeEnvironment.getApplication(), "int x = 1;");
        EditorView vue = banc.vue();
        assertEquals(14f, vue.policeLogique.size(), 1e-6f);
        vue.onKeyDown(KeyEvent.KEYCODE_PLUS, touche(KeyEvent.KEYCODE_PLUS));
        assertEquals("Ctrl + plus monte au palier voisin, un cran exact",
                16f, vue.policeLogique.size(), 1e-6f);
        vue.onKeyDown(KeyEvent.KEYCODE_PLUS, touche(KeyEvent.KEYCODE_PLUS));
        assertEquals(18f, vue.policeLogique.size(), 1e-6f);
    }

    /** Ctrl + moins descend, Ctrl + zéro revient au défaut. */
    @Test
    public void ctrlMinusAndZeroFollowTheLadder() {
        EditorTestRig banc = EditorTestRig.sur(
                RuntimeEnvironment.getApplication(), "int x = 1;");
        EditorView vue = banc.vue();
        vue.setFont(new FontSpec("mono", 20f));
        vue.onKeyDown(KeyEvent.KEYCODE_MINUS, touche(KeyEvent.KEYCODE_MINUS));
        assertEquals("Ctrl + moins descend au palier voisin",
                18f, vue.policeLogique.size(), 1e-6f);
        vue.onKeyDown(KeyEvent.KEYCODE_0, touche(KeyEvent.KEYCODE_0));
        assertEquals("Ctrl + zéro revient à la police par défaut",
                EditorView.POLICE_PAR_DEFAUT.size(), vue.policeLogique.size(),
                1e-6f);
    }

    /** L'écouteur de taille prévient chaque changement, pincement compris. */
    @Test
    public void fontSizeListenerFiresOnEveryChange() {
        EditorTestRig banc = EditorTestRig.sur(
                RuntimeEnvironment.getApplication(), "int x = 1;");
        EditorView vue = banc.vue();
        List<Float> tailles = new ArrayList<>();
        vue.setOnFontSizeChanged(tailles::add);
        vue.onKeyDown(KeyEvent.KEYCODE_PLUS, touche(KeyEvent.KEYCODE_PLUS));
        banc.pincer(540f, 960f, 120f, 520f, 15);
        assertEquals("chaque changement prévient l'écouteur",
                List.of(16f, 18f, 20f, 24f, 28f), tailles);
    }

    /** La molette avec Ctrl zoome d'un palier, sans Ctrl elle ne consomme pas. */
    @Test
    public void ctrlWheelZoomsOneStepPerNotch() {
        EditorTestRig banc = EditorTestRig.sur(
                RuntimeEnvironment.getApplication(), "int x = 1;");
        EditorView vue = banc.vue();
        assertEquals(true, vue.onGenericMotionEvent(molette(1f, true)));
        assertEquals("un cran de molette Ctrl monte d'un palier",
                16f, vue.policeLogique.size(), 1e-6f);
        assertEquals("sans Ctrl, la molette ne se consomme pas",
                false, vue.onGenericMotionEvent(molette(1f, false)));
        assertEquals(16f, vue.policeLogique.size(), 1e-6f);
        assertEquals(true, vue.onGenericMotionEvent(molette(-1f, true)));
        assertEquals("un cran vers le bas descend d'un palier",
                14f, vue.policeLogique.size(), 1e-6f);
    }

    /** Les touches de zoom par défaut sont distinctes, sans conflit. */
    @Test
    public void defaultZoomBindingsAreDistinct() {
        KeyBindings table = KeyBindings.defaults();
        KeyBinding plus = table.bindingOf(EditorAction.ZOOM_IN).orElseThrow();
        KeyBinding minus = table.bindingOf(EditorAction.ZOOM_OUT).orElseThrow();
        KeyBinding zero = table.bindingOf(EditorAction.ZOOM_RESET).orElseThrow();
        assertNotEquals("les touches de zoom ne se chevauchent pas", plus, minus);
        assertNotEquals(plus, zero);
        assertNotEquals(minus, zero);
        assertEquals("la touche de zoom reste servie à la même action",
                EditorAction.ZOOM_IN, table.actionOf(plus));
    }

    private static MotionEvent molette(float cran, boolean avecCtrl) {
        MotionEvent.PointerProperties[] doigts =
                new MotionEvent.PointerProperties[1];
        doigts[0] = new MotionEvent.PointerProperties();
        doigts[0].id = 0;
        doigts[0].toolType = MotionEvent.TOOL_TYPE_MOUSE;
        MotionEvent.PointerCoords[] positions =
                new MotionEvent.PointerCoords[1];
        positions[0] = new MotionEvent.PointerCoords();
        positions[0].x = 540f;
        positions[0].y = 960f;
        positions[0].setAxisValue(MotionEvent.AXIS_VSCROLL, cran);
        return MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_SCROLL, 1,
                doigts, positions, avecCtrl ? KeyEvent.META_CTRL_ON : 0, 0,
                1f, 1f, 0, 0, InputDevice.SOURCE_MOUSE, 0);
    }
}
