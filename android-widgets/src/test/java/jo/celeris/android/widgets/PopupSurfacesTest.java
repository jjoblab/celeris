package jo.celeris.android.widgets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
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
 * Les surfaces restantes des popups (M7.5, §7, reprise) : le fil
 * d'Ariane tronque sa TÊTE en ellipse, la barre de sélection exécute
 * sa pastille visée — la puce de débordement prévient l'hôte, le menu
 * vit le même design —, la bande d'erreurs saute au diagnostic du cran
 * le plus proche — et aucune ne prend le focus (PIEGE-27).
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
                vue.getMeasuredHeight() > 2 * vue.paddingYPx);
    }

    /** La barre de sélection : l'appui sur une pastille l'exécute —
     *  chaque libellé respire de 12 dp, 2 dp les séparent (le design
     *  du dépôt de départ). */
    @Test
    public void selectionBarRunsTheChosenChip() {
        SelectionBarView vue = new SelectionBarView(contexte,
                PopupPalette.dark());
        AtomicInteger execute = new AtomicInteger(-1);
        vue.setActions(List.of(
                new SelectionBarView.Item("Couper", () -> execute.set(0)),
                new SelectionBarView.Item("Copier", () -> execute.set(1)),
                new SelectionBarView.Item("Coller", () -> execute.set(2))),
                List.of());
        mesurer(vue);
        assertTrue("la barre se mesure", vue.getMeasuredWidth() > 0);
        // le cœur de la DEUXIÈME pastille : sa respiration comprise
        float xDeuxieme = vue.paddingPx + 12f
                + vue.textPaint().measureText("Couper") + 12f + 2f + 12f
                + vue.textPaint().measureText("Copier") / 2f;
        float y = vue.centerY();
        vue.onTouchEvent(MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN,
                xDeuxieme, y, 0));
        assertTrue(vue.onTouchEvent(MotionEvent.obtain(0L, 10L,
                MotionEvent.ACTION_UP, xDeuxieme, y, 0)));
        assertEquals("la deuxième pastille court", 1, execute.get());
    }

    /**
     * La barre de sélection et sa PUCE DE DÉBORDEMENT (le kit du dépôt
     * de départ, le motif de la plateforme) : le débordement plié
     * agrandit la barre de la puce, l'appui dessus prévient l'hôte —
     * les actions repliées courent au MENU, pas à la barre.
     */
    @Test
    public void selectionBarOverflowChipOpensTheHostMenu() {
        SelectionBarView vue = new SelectionBarView(contexte,
                PopupPalette.dark());
        vue.setActions(List.of(
                new SelectionBarView.Item("Couper", () -> { }),
                new SelectionBarView.Item("Copier", () -> { })),
                List.of(new SelectionBarView.Item("Tout", () -> { })));
        assertFalse("sans débordement la puce manque",
                sansDebordement(contexte));
        assertTrue("avec débordement la puce se montre", vue.aDebordement());
        mesurer(vue);
        int largeurSansPuce = vue.getMeasuredWidth();
        assertTrue("la puce s'ancre à droite du contenu",
                vue.puceDebordementCentreX() > 0f
                        && vue.puceDebordementCentreX() < largeurSansPuce);
        AtomicInteger ouvert = new AtomicInteger(0);
        vue.setOnOverflow(ouvert::incrementAndGet);
        float x = vue.paddingPx + vue.puceDebordementCentreX();
        float y = vue.centerY();
        vue.onTouchEvent(MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN,
                x, y, 0));
        assertTrue("l'appui sur la puce se prend",
                vue.onTouchEvent(MotionEvent.obtain(0L, 10L,
                        MotionEvent.ACTION_UP, x, y, 0)));
        assertEquals("l'hôte est prévenu une fois", 1, ouvert.get());
    }

    /** Une barre sans débordement ne montre pas la puce. */
    private static boolean sansDebordement(Context contexte) {
        SelectionBarView vue = new SelectionBarView(contexte,
                PopupPalette.dark());
        vue.setActions(List.of(new SelectionBarView.Item("Couper", () -> { })),
                List.of());
        return vue.aDebordement();
    }

    /** Le « i » de documentation du dépôt de départ : l'appui prévient
     *  l'hôte — l'icône vit à droite du filet, au groupe icônes. */
    @Test
    public void selectionBarDocsIconServesTheHost() {
        SelectionBarView vue = new SelectionBarView(contexte,
                PopupPalette.dark());
        AtomicInteger ouvert = new AtomicInteger(0);
        vue.setActions(
                List.of(new SelectionBarView.Item("Copier", () -> { })),
                List.of());
        vue.setOnDocs(ouvert::incrementAndGet);
        mesurer(vue);
        // le cœur de l'icône : 16 dp du bord droit du contenu (icône de 32 dp)
        float x = vue.getMeasuredWidth() - vue.paddingPx - 16f;
        float y = vue.centerY();
        vue.onTouchEvent(MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN,
                x, y, 0));
        assertTrue("l'appui sur le « i » se prend",
                vue.onTouchEvent(MotionEvent.obtain(0L, 10L,
                        MotionEvent.ACTION_UP, x, y, 0)));
        assertEquals("l'hôte sert sa documentation une fois", 1, ouvert.get());
    }

    /** La pilule resserrée (l'audit de compacité du propriétaire du
     *  2026-11-14) : contenu de 26 dp (14 dp de texte, 2 × 6 dp de
     *  respiration), capsule au rayon de sa hauteur. */
    @Test
    public void selectionBarPorteLaPiluleDuDepot() {
        SelectionBarView vue = new SelectionBarView(contexte,
                PopupPalette.dark());
        vue.setActions(
                List.of(new SelectionBarView.Item("Copier", () -> { })),
                List.of());
        mesurer(vue);
        assertEquals("le contenu respire la pilule : 26 dp",
                26, vue.getMeasuredHeight() - 2 * vue.paddingYPx);
        vue.layout(0, 0, vue.getMeasuredWidth(), vue.getMeasuredHeight());
        assertEquals("la capsule prend le rayon de sa hauteur",
                vue.getHeight() / 2f, vue.rayonCartePx(), 0.5f);
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

    /** C9 — la minimap : la silhouette se pose, l'appui et le glissement
     *  sautent à la ligne visée, la fenêtre visible borne ses crans. */
    @Test
    public void minimapJumpsToTheTouchedLine() {
        StringBuilder contenu = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            if (i > 0) {
                contenu.append('\n');
            }
            contenu.append("ligne ").append(i);
        }
        Document document = TextSession.of(contenu.toString(), () -> 0L)
                .document();
        MinimapModel carte = MinimapModel.of(document, 100);
        MinimapView vue = new MinimapView(contexte, PopupPalette.dark());
        vue.setModel(carte, document.lineCount());
        vue.setSpanPx(1000);
        mesurer(vue);
        vue.layout(0, 0, vue.getMeasuredWidth(), vue.getMeasuredHeight());
        // la carte honnête : cent rangees à 2,5 dp (densité 1) — elle ne
        // s'étire pas sur l'étendue posée
        assertEquals("la carte courte prend sa hauteur honnête", 250,
                vue.getMeasuredHeight());
        AtomicInteger ligne = new AtomicInteger(-1);
        vue.setOnJump(ligne::set);
        // le haut de carte vise la tête du document
        vue.onTouchEvent(MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN,
                2f, 5f, 0));
        assertTrue(vue.onTouchEvent(MotionEvent.obtain(0L, 10L,
                MotionEvent.ACTION_UP, 2f, 5f, 0)));
        assertTrue("l'appui saute vers la tête",
                ligne.get() >= 0 && ligne.get() <= 2);
        // le glissement saute aussi — le doigt tient la carte
        vue.onTouchEvent(MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN,
                2f, 5f, 0));
        assertTrue(vue.onTouchEvent(MotionEvent.obtain(0L, 10L,
                MotionEvent.ACTION_MOVE, 2f, 995f, 0)));
        assertTrue("le glissement saute vers le pied",
                ligne.get() >= 97);
        // la fenêtre visible : les crans se bornent
        vue.setViewport(50, 10, 100);
        assertTrue("la fenêtre se pose sans casser",
                vue.getMeasuredHeight() > 0);
    }

    /** La re-monte déplace le popup SANS le re-naître : même
     *  instance, même couche, la position suit — le dragage de
     *  sélection y garde la barre identique à l'appui long qui l'a
     *  posée. */
    @Test
    public void remonterDeplaceSansReposer() {
        PopupLayer couche = new PopupLayer(contexte);
        SelectionBarView barre = new SelectionBarView(contexte,
                PopupPalette.dark());
        barre.setActions(List.of(new SelectionBarView.Item("copier", () -> { })),
                List.of());
        couche.show(PopupController.PopupKind.SELECTION_BAR, barre, 10, 20);
        int enfantsAvant = couche.getChildCount();
        View enfant = couche.popupOf(PopupController.PopupKind.SELECTION_BAR);
        couche.remonter(PopupController.PopupKind.SELECTION_BAR, 30, 40);
        assertTrue("le popup reste montré",
                couche.isShowing(PopupController.PopupKind.SELECTION_BAR));
        assertSame("la MÊME instance vit encore dans la couche",
                enfant, couche.popupOf(PopupController.PopupKind.SELECTION_BAR));
        assertEquals("l'enfant ne re-naît pas d'une re-monte",
                enfantsAvant, couche.getChildCount());
        assertEquals(30, enfant.getLeft());
        assertEquals(40, enfant.getTop());
        // la re-monte d'un popup absent reste muette — rien ne se pose
        couche.remonter(PopupController.PopupKind.BREADCRUMB, 5, 5);
        assertFalse("rien ne se pose sur un popup absent",
                couche.isShowing(PopupController.PopupKind.BREADCRUMB));
    }

    /** C9 — le défilement rapide : le pouce suit l'état, le glissement
     *  rend la fraction visée, tout tient : le pouce se tait. */
    @Test
    public void fastScrollerDragsToTheTargetFraction() {
        FastScrollerView vue = new FastScrollerView(contexte,
                PopupPalette.dark());
        vue.setEtat(0f, 0.5f);
        vue.setSpanPx(1000);
        mesurer(vue);
        vue.layout(0, 0, vue.getMeasuredWidth(), vue.getMeasuredHeight());
        assertEquals("le pouce prend l'étendue posée", 1000,
                vue.getMeasuredHeight());
        java.util.concurrent.atomic.AtomicReference<Double> fraction =
                new java.util.concurrent.atomic.AtomicReference<>(-1.0);
        vue.setOnScroll(fraction::set);
        // le doigt au milieu : le HAUT du pouce suit, la fraction vise le milieu
        vue.onTouchEvent(MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN,
                2f, 500f, 0));
        assertTrue("le glissement est pris", vue.onTouchEvent(
                MotionEvent.obtain(0L, 10L, MotionEvent.ACTION_MOVE,
                        2f, 500f, 0)));
        assertTrue("le glissement rend une fraction bornée",
                fraction.get() >= 0.0 && fraction.get() <= 1.0);
        // le pied : la fraction vise la fin
        vue.onTouchEvent(MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN,
                2f, 995f, 0));
        vue.onTouchEvent(MotionEvent.obtain(0L, 20L, MotionEvent.ACTION_MOVE,
                2f, 995f, 0));
        assertTrue("le pied vise la fin", fraction.get() > 0.8);
        // le relèvement rend la main
        assertTrue(vue.onTouchEvent(MotionEvent.obtain(0L, 30L,
                MotionEvent.ACTION_UP, 2f, 995f, 0)));
        // tout tient : le pouce se tait — l'état reste posé
        vue.setEtat(0.5f, 1f);
        assertTrue("tout tient, l'état reste", vue.getMeasuredHeight() > 0);
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
                new ErrorBandView(contexte, PopupPalette.dark()),
                new MinimapView(contexte, PopupPalette.dark()),
                new FastScrollerView(contexte, PopupPalette.dark()))) {
            assertFalse("aucune surface ne prend le focus (PIEGE-27)",
                    vue.isFocusable());
        }
    }
}
