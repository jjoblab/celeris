package jo.celeris.android.widgets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import jo.celeris.core.text.TextSession;
import jo.celeris.core.text.Document;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * Les surfaces restantes des dix popups (M7.5, §7, reprise) : le fil
 * d'Ariane tronque sa TÊTE en ellipse, la barre de sélection exécute
 * sa pastille visée, la bande d'erreurs saute au diagnostic du cran le
 * plus proche — et aucune ne prend le focus (PIEGE-27).
 */
@RunWith(RobolectricTestRunner.class)
public class PopupSurfacesTest {

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

    /** Le fil d'Ariane : une rangée, la tête élaguée quand la largeur manque. */
    @Test
    public void breadcrumbTruncatesTheHead() {
        BreadcrumbView vue = new BreadcrumbView(contexte, PopupPalette.dark());
        List<String> chemin = List.of("Exemple", "corps", "calculer");
        vue.setPath(chemin, 10_000);
        mesurer(vue);
        int largeurComplete = vue.getMeasuredWidth();
        assertTrue("le chemin complet se mesure", largeurComplete > 0);
        // Une largeur d'étroit : la tête tombe, l'ellipse la signale.
        vue.setPath(chemin, largeurComplete / 2);
        mesurer(vue);
        assertTrue("le chemin tronqué rétrécit",
                vue.getMeasuredWidth() <= largeurComplete);
        // Hors de tout symbole : le bandeau se vide.
        vue.setPath(List.of(), 10_000);
        mesurer(vue);
        assertFalse("vide, le fil ne retient rien",
                vue.getMeasuredHeight() > 2 * vue.paddingPx);
    }

    /** La barre de sélection : l'appui sur une pastille l'exécute. */
    @Test
    public void selectionBarRunsTheChosenChip() {
        SelectionBarView vue = new SelectionBarView(contexte,
                PopupPalette.dark());
        AtomicInteger execute = new AtomicInteger(-1);
        vue.setActions(List.of(
                new SelectionBarView.Item("Couper", () -> execute.set(0)),
                new SelectionBarView.Item("Copier", () -> execute.set(1)),
                new SelectionBarView.Item("Coller", () -> execute.set(2))));
        mesurer(vue);
        assertTrue("la barre se mesure", vue.getMeasuredWidth() > 0);
        float xDeuxieme = vue.paddingPx
                + vue.textPaint().measureText("Couper") + 5f;
        float y = vue.centerY();
        vue.onTouchEvent(MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN,
                xDeuxieme, y, 0));
        assertTrue(vue.onTouchEvent(MotionEvent.obtain(0L, 10L,
                MotionEvent.ACTION_UP, xDeuxieme, y, 0)));
        assertEquals("la deuxième pastille court", 1, execute.get());
    }

    /** La bande d'erreurs : marque au cran, appui → saut à l'offset. */
    @Test
    public void errorBandJumpsToTheNearestMark() {
        Document document = TextSession.of(
                "a {\nb {\nc\n\n", () -> 0L).document();
        List<jo.celeris.core.syntax.SyntaxIssue> diagnostics = List.of(
                nouvelleIssue(4, 1), nouvelleIssue(9, 1));
        ErrorBandModel model = ErrorBandModel.of(diagnostics, document, 100);
        ErrorBandView vue = new ErrorBandView(contexte, PopupPalette.dark());
        vue.setModel(model, 100);
        vue.setSpanPx(1000);
        mesurer(vue);
        vue.layout(0, 0, vue.getMeasuredWidth(), vue.getMeasuredHeight());
        assertEquals("la bande prend l'étendue posée", 1000,
                vue.getMeasuredHeight());
        AtomicInteger saut = new AtomicInteger(-1);
        vue.setOnJump(saut::set);
        // Le haut de bande vise la première marque.
        vue.onTouchEvent(MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN,
                2f, 5f, 0));
        assertTrue(vue.onTouchEvent(MotionEvent.obtain(0L, 10L,
                MotionEvent.ACTION_UP, 2f, 5f, 0)));
        assertTrue("l'appui saute à un offset de diagnostic",
                saut.get() == 4 || saut.get() == 9);
        // Le bas de bande vise la dernière.
        vue.onTouchEvent(MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN,
                2f, 995f, 0));
        assertTrue(vue.onTouchEvent(MotionEvent.obtain(0L, 20L,
                MotionEvent.ACTION_UP, 2f, 995f, 0)));
        assertEquals("le bas vise la marque basse", 9, saut.get());
    }

    private static jo.celeris.core.syntax.SyntaxIssue nouvelleIssue(
            int offset, int longueur) {
        return new jo.celeris.core.syntax.SyntaxIssue(offset, longueur,
                "exemple de diagnostic");
    }

    /** Aucune surface restante ne se déclare focalisable (PIEGE-27). */
    @Test
    public void noRemainingSurfaceIsFocusable() {
        for (View vue : List.of(
                new BreadcrumbView(contexte, PopupPalette.dark()),
                new SelectionBarView(contexte, PopupPalette.dark()),
                new ErrorBandView(contexte, PopupPalette.dark()))) {
            assertFalse("aucune surface ne prend le focus (PIEGE-27)",
                    vue.isFocusable());
        }
    }
}
