package jo.celeris.android.widgets;

import static org.junit.Assert.assertEquals;
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
 * La carte des détails d'exception (catégorie E de l'inventaire — la
 * surface dormait, la trace se réveille) : le type et le message ne se
 * prennent jamais, le cadre CONNU se prend au doigt et son saut court,
 * le cadre étranger vit muet — la carte se mesure à son contenu.
 */
@RunWith(RobolectricTestRunner.class)
public class ExceptionViewTest {

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

    /** Le type et le message annoncent, les cadres suivent : la carte
     *  se mesure à son contenu, le cadre hors bornes n'existe pas. */
    @Test
    public void leTypeAnnonceEtLesCadresMesurent() {
        ExceptionView vue = new ExceptionView(contexte, PopupPalette.dark());
        vue.setException("java.lang.Exception", "l'échec simulé");
        vue.setCadres(List.of(
                new ExceptionView.Cadre("L12 · Service.inscrire", () -> { }),
                new ExceptionView.Cadre("L7 · Autre.aide", null)));
        mesurer(vue);
        assertEquals(2, vue.nombre());
        assertEquals("L12 · Service.inscrire", vue.cadreAt(0).libelle());
        assertNull("le cadre hors bornes n'existe pas", vue.cadreAt(7));
        assertTrue("la carte tient ses rangées", vue.getMeasuredHeight() > 0);
    }

    /** L'appui CHOISIT le cadre prenant : le doigt pose puis relâche —
     *  le type ne se prend jamais, le cadre muet non plus, le cadre
     *  sous le doigt saute. */
    @Test
    public void lappuiChoisitLeCadrePrenantPasLEntete() {
        AtomicInteger courses = new AtomicInteger(-1);
        ExceptionView vue = new ExceptionView(contexte, PopupPalette.dark());
        vue.setException("java.lang.Exception", "l'échec simulé");
        vue.setCadres(List.of(
                new ExceptionView.Cadre("L12 · Service.inscrire",
                        () -> courses.set(12)),
                new ExceptionView.Cadre("L7 · Autre.aide", null)));
        mesurer(vue);
        long t = android.os.SystemClock.uptimeMillis();
        // le type d'abord : le relâchement dessus ne choisit rien
        float entete = vue.rowCenterY(0) + vue.paddingYPx;
        vue.onTouchEvent(MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN,
                10f, entete, 0));
        vue.onTouchEvent(MotionEvent.obtain(t, t + 16L, MotionEvent.ACTION_UP,
                10f, entete, 0));
        assertEquals("le type ne se prend jamais", -1, courses.get());
        // le cadre muet suit le prenant : il ne saute pas non plus
        float muet = vue.rowCenterY(3) + vue.paddingYPx;
        vue.onTouchEvent(MotionEvent.obtain(t, t + 32L,
                MotionEvent.ACTION_DOWN, 10f, muet, 0));
        vue.onTouchEvent(MotionEvent.obtain(t, t + 48L,
                MotionEvent.ACTION_UP, 10f, muet, 0));
        assertEquals("le cadre étranger vit muet", -1, courses.get());
        // le cadre prenant suit le message : le relâchement saute
        float prenant = vue.rowCenterY(2) + vue.paddingYPx;
        vue.onTouchEvent(MotionEvent.obtain(t, t + 64L,
                MotionEvent.ACTION_DOWN, 10f, prenant, 0));
        vue.onTouchEvent(MotionEvent.obtain(t, t + 80L,
                MotionEvent.ACTION_UP, 10f, prenant, 0));
        assertEquals("le relâchement saute au cadre prenant", 12,
                courses.get());
    }
}
