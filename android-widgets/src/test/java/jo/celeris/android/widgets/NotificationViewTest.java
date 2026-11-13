package jo.celeris.android.widgets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLooper;

/**
 * La carte de notification (catégorie G de l'inventaire — la forme
 * restait à décider, la bulle éphémère l'emporte) : l'annonce se pose
 * en TITRE et CORPS, le tap LA PREND la referme (le Runnable de
 * l'hôte seul décide), l'HORLOGE la referme à son heure — la re-pose
 * réarme l'horloge, jamais deux courses (la queue du fil ne sert que
 * la dernière annonce).
 */
@RunWith(RobolectricTestRunner.class)
public class NotificationViewTest {

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

    /** L'annonce se pose : le titre au palier titre, le message au
     *  corps — la carte se mesure à son contenu. */
    @Test
    public void lannonceSePoseTitreEtMessage() {
        NotificationView vue = new NotificationView(contexte,
                PopupPalette.dark());
        vue.setAnnonce("Lint", "3 fixed");
        mesurer(vue);
        assertEquals("le titre et le message", 2, vue.rowCount());
        assertEquals("Lint", vue.titre());
        assertTrue("la carte tient ses rangées", vue.getMeasuredHeight() > 0);
        // la pose suivante remplace : jamais quatre rangées
        vue.setAnnonce("Debug", "Paused at line 5");
        assertEquals("Debug", vue.titre());
        assertEquals("la pose suivante remplace", 2, vue.rowCount());
    }

    /** Le tap LA PREND la referme : le relâchement sur la bulle
     *  prévient l'hôte — l'annonce ne survit pas au geste qui la lit. */
    @Test
    public void leTapLaPrendLaReferme() {
        AtomicInteger prises = new AtomicInteger(0);
        NotificationView vue = new NotificationView(contexte,
                PopupPalette.dark());
        vue.setAnnonce("Lint", "3 fixed");
        vue.setSurPrise(() -> prises.incrementAndGet());
        mesurer(vue);
        long t = android.os.SystemClock.uptimeMillis();
        float centre = vue.rowCenterY(0) + vue.paddingYPx;
        vue.onTouchEvent(MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN,
                10f, centre, 0));
        vue.onTouchEvent(MotionEvent.obtain(t, t + 16L, MotionEvent.ACTION_UP,
                10f, centre, 0));
        assertEquals("le relâchement sur la bulle la prend", 1, prises.get());
    }

    /** L'horloge la referme à son heure : la course part à l'armement,
     *  la re-pose la réarme — jamais deux courses, la queue du fil ne
     *  sert que la dernière annonce. */
    @Test
    public void lhorlogeLaRefermeEtSeRearme() {
        AtomicInteger courses = new AtomicInteger(0);
        NotificationView vue = new NotificationView(contexte,
                PopupPalette.dark());
        vue.setAnnonce("Lint", "3 fixed");
        vue.armer(1000L, courses::incrementAndGet);
        // la re-pose réarme : la première course se dissout
        vue.setAnnonce("Debug", "Paused at line 5");
        vue.armer(1000L, courses::incrementAndGet);
        ShadowLooper shadow = ShadowLooper.shadowMainLooper();
        shadow.runToEndOfTasks();
        assertEquals("l'horloge ne sert que la dernière annonce", 1,
                courses.get());
        // le désarmement coupe l'horloge : la course ne part jamais
        vue.armer(1000L, courses::incrementAndGet);
        vue.desarmer();
        shadow.runToEndOfTasks();
        assertEquals("l'horloge désarmée ne court plus", 1, courses.get());
    }
}
