package jo.celeris.android.widgets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * La feuille du haut de l'ADR-018 (C12) — son premier usage tient la
 * revue d'agent : elle se pose SOUS la barre d'etat (gravite du haut),
 * se cantonne a la largeur de la maquette, se ferme par le retour et
 * par un glissement qui MONTE — le sens inverse de la feuille du bas.
 */
@RunWith(RobolectricTestRunner.class)
public class FeuilleHautTest {

    private Context contexte;

    @Before
    public void preparer() {
        contexte = RuntimeEnvironment.getApplication();
        Settings.Global.putFloat(contexte.getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE, 0f);
    }

    private FrameLayout hotePose(int largeur, int hauteur) {
        FrameLayout hote = new FrameLayout(contexte);
        hote.measure(
                View.MeasureSpec.makeMeasureSpec(largeur,
                        View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(hauteur,
                        View.MeasureSpec.EXACTLY));
        hote.layout(0, 0, largeur, hauteur);
        return hote;
    }

    private void trainerHaut(View poignee, float dy) {
        long t = SystemClock.uptimeMillis();
        poignee.dispatchTouchEvent(MotionEvent.obtain(
                t, t, MotionEvent.ACTION_DOWN, 50f, 20f, 0));
        poignee.dispatchTouchEvent(MotionEvent.obtain(
                t, t + 50, MotionEvent.ACTION_MOVE, 50f, 20f + dy, 0));
        poignee.dispatchTouchEvent(MotionEvent.obtain(
                t, t + 100, MotionEvent.ACTION_UP, 50f, 20f + dy, 0));
    }

    /** La feuille se pose au haut du cadre, centree, voile compris. */
    @Test
    public void laFeuilleSePoseAuHautDuCadre() {
        FrameLayout hote = hotePose(1000, 1800);
        FeuilleHaut feuille = new FeuilleHaut(contexte, PopupPalette.dark());
        feuille.titre("Revue d'agent");
        TextView contenu = new TextView(contexte);
        feuille.contenu(contenu);
        feuille.montrer(hote);
        assertTrue(feuille.estOuverte());
        assertEquals("la gravite du haut se pose",
                Gravity.TOP | Gravity.CENTER_HORIZONTAL,
                ((FrameLayout.LayoutParams) feuille.getLayoutParams()).gravity);
        assertTrue("le voile vit sous la feuille",
                hote.getChildAt(0) == feuille.voileVue());
        hote.measure(
                View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1800, View.MeasureSpec.EXACTLY));
        hote.layout(0, 0, 1000, 1800);
        assertEquals("la feuille se pose au bord haut", 0, feuille.getTop());
        assertTrue("la largeur se cantonne (densite 1 : 760 px)",
                feuille.getWidth() <= 760);
    }

    /** Le retour ferme ; le glissement qui MONTE emporte, celui qui
     *  descend ne fait rien. */
    @Test
    public void leGlissementQuiMonteEmporte() {
        FrameLayout hote = hotePose(1000, 1800);
        FeuilleHaut feuille = new FeuilleHaut(contexte, PopupPalette.dark());
        feuille.contenu(new TextView(contexte));
        feuille.montrer(hote);
        trainerHaut(feuille.poigneeVue(), 60f);
        assertTrue("verser vers le bas n'emporte pas la feuille du haut",
                feuille.estOuverte());
        assertEquals("la feuille reste a son repos", 0f,
                feuille.getTranslationY(), 0f);

        trainerHaut(feuille.poigneeVue(), -300f);
        assertFalse("le glissement qui monte emporte", feuille.estOuverte());
    }

    /** Le retour se consomme une fois, puis la feuille laisse passer. */
    @Test
    public void leRetourFermeEtPasse() {
        FrameLayout hote = hotePose(1000, 1800);
        FeuilleHaut feuille = new FeuilleHaut(contexte, PopupPalette.dark());
        feuille.contenu(new TextView(contexte));
        feuille.montrer(hote);
        assertTrue(feuille.surRetour());
        assertFalse(feuille.estOuverte());
        assertFalse(feuille.surRetour());
    }
}
