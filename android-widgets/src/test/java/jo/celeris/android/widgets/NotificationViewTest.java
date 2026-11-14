package jo.celeris.android.widgets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
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
 * la dernière annonce). L'audit Android Studio du 2026-11-14 y pose
 * les LIENS : les actions de la bulle de l'IDE de référence suivent
 * le message derrière un filet, la prise d'un lien referme la bulle
 * puis fait courir son action (l'ordre de l'IDE : la bulle cède
 * d'abord — une action qui re-pose n'est jamais tuée par sa propre
 * fermeture).
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

    /** L'audit Android Studio : les LIENS suivent le message derrière
     * un filet — la prise du lien referme la bulle puis fait courir son
     * action, la prise du titre referme seule. */
    @Test
    public void lesLiensSuiventLeMessageEtLaPriseLesReferme() {
        List<String> trace = new ArrayList<>();
        NotificationView vue = new NotificationView(contexte,
                PopupPalette.dark());
        vue.setAnnonce("Lint", "3 fixed", List.of(
                new NotificationView.Action("Annuler",
                        () -> trace.add("action"))));
        assertEquals("le titre, le message et le lien", 3, vue.rowCount());
        vue.setSurPrise(() -> trace.add("prise"));
        mesurer(vue);
        long t = android.os.SystemClock.uptimeMillis();
        // la prise du LIEN : la bulle cède d'abord, l'action court ensuite
        float lien = vue.rowCenterY(2) + vue.paddingYPx;
        vue.onTouchEvent(MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN,
                10f, lien, 0));
        vue.onTouchEvent(MotionEvent.obtain(t, t + 16L, MotionEvent.ACTION_UP,
                10f, lien, 0));
        assertEquals("la prise du lien referme puis exécute",
                List.of("prise", "action"), trace);
        // la prise du TITRE : la bulle referme seule, l'action ne court pas
        trace.clear();
        float titre = vue.rowCenterY(0) + vue.paddingYPx;
        vue.onTouchEvent(MotionEvent.obtain(t, t + 32L,
                MotionEvent.ACTION_DOWN, 10f, titre, 0));
        vue.onTouchEvent(MotionEvent.obtain(t, t + 48L,
                MotionEvent.ACTION_UP, 10f, titre, 0));
        assertEquals("la prise du titre referme seule",
                List.of("prise"), trace);
    }

    /** La parité au simulateur : la JAUGE coule — armée à 1, épuisée à
     *  0, la carte compte la zone de la jauge et sa légende. */
    @Test
    public void laJaugeCouleDeUnAZero() {
        NotificationView vue = new NotificationView(contexte,
                PopupPalette.dark());
        vue.setAnnonce("Lint", "3 fixed");
        mesurer(vue);
        int hauteurSansHorloge = vue.getMeasuredHeight();
        assertEquals("désarmée, la jauge ne promet rien", 1f,
                vue.progression(), 0f);
        vue.armer(1000L, () -> { });
        mesurer(vue);
        assertTrue("armée, la carte compte la jauge et sa légende",
                vue.getMeasuredHeight() > hauteurSansHorloge);
        assertEquals("armée, la jauge part pleine", 1f,
                vue.progression(), 0.0001f);
        ShadowLooper shadow = ShadowLooper.shadowMainLooper();
        shadow.runToEndOfTasks();
        assertEquals("épuisée, la jauge tombe à zéro", 0f,
                vue.progression(), 0.0001f);
        assertEquals("le reste échu", 0L, vue.resteMs());
        // le désarmement coupe la promesse : la jauge ne se trace plus
        vue.desarmer();
        assertEquals("désarmée, la jauge ne promet plus rien", 1f,
                vue.progression(), 0f);
    }

    /** La légende du compte à rebours — pure : au dixième près, la
     *  virgule du terrain, jamais de négatif. */
    @Test
    public void laLegendeDuReboursAuDixieme() {
        assertEquals("l'horloge la referme — 4,0 s",
                NotificationView.phraseRebours(4000L));
        assertEquals("l'horloge la referme — 1,2 s",
                NotificationView.phraseRebours(1234L));
        assertEquals("l'horloge la referme — 0,1 s",
                NotificationView.phraseRebours(99L));
        assertEquals("l'horloge la referme — 0,0 s",
                NotificationView.phraseRebours(0L));
        assertEquals("jamais de négatif",
                "l'horloge la referme — 0,0 s",
                NotificationView.phraseRebours(-500L));
    }
}
