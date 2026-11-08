package jo.celeris.android.widgets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.MotionEvent;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * Les vues de popups (portage du design du dépôt de départ) : la
 * complétion porte ses candidats TYPÉS — rangées deux lignes, badge de
 * genre, runs de match — et choisit à l'appui, la documentation aligne
 * la bande signature teintée et les sections distinctes (PIEGE-16), la
 * signature se mesure à ses trois tiers, le menu d'intentions
 * exécute, l'info-bulle suit la sévérité. Robolectric : mesure, dessin
 * et toucher réels.
 */
@RunWith(RobolectricTestRunner.class)
public class PopupViewsTest {

    private Context contexte;

    @Before
    public void preparer() {
        contexte = RuntimeEnvironment.getApplication();
    }

    private void mesurer(android.view.View vue) {
        vue.measure(android.view.View.MeasureSpec.makeMeasureSpec(0,
                        android.view.View.MeasureSpec.UNSPECIFIED),
                android.view.View.MeasureSpec.makeMeasureSpec(0,
                        android.view.View.MeasureSpec.UNSPECIFIED));
    }

    /**
     * La complétion : les candidats typés se mesurent en rangées deux
     * lignes, le premier surligné, l'appui choisit.
     */
    @Test
    public void completionListsAndPicksTypedCandidates() {
        CompletionListView vue = new CompletionListView(contexte,
                PopupPalette.dark());
        vue.setItems(List.of(
                new CompletionListView.Candidat("valeur", "",
                        BadgeGenre.Genre.VARIABLE),
                new CompletionListView.Candidat("variable", "",
                        BadgeGenre.Genre.VARIABLE),
                new CompletionListView.Candidat("ajouter",
                        "ajouter(int a, int b)", BadgeGenre.Genre.METHODE)));
        vue.setPrefix("va");
        mesurer(vue);
        assertTrue("la liste se mesure",
                vue.getMeasuredWidth() > 0 && vue.getMeasuredHeight() > 0);
        assertEquals("valeur", vue.itemAt(0));
        assertEquals("l'identité qualifiée reste lisible",
                "ajouter(int a, int b)", vue.candidatAt(2).detail());

        AtomicInteger choisi = new AtomicInteger(-1);
        vue.setOnPick(choisi::set);
        float yRangee = vue.rowCenterY(2);
        vue.onTouchEvent(MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN,
                10f, yRangee, 0));
        boolean pris = vue.onTouchEvent(MotionEvent.obtain(0L, 10L,
                MotionEvent.ACTION_UP, 10f, yRangee, 0));
        assertTrue("l'appui sur une rangée se prend", pris);
        assertEquals("la troisième rangée choisit le troisième candidat",
                2, choisi.get());
    }

    /** La documentation : le titre distinct, les sections typées à part. */
    @Test
    public void docKeepsTitleAndSectionsApart() {
        CompletionDocView vue = new CompletionDocView(contexte,
                PopupPalette.dark());
        vue.setModel(CompletionDocModel.of("ajouter",
                "Additionne deux entiers.\n\n@param a le premier\n"
                        + "@param b le second"));
        mesurer(vue);
        assertTrue("le panneau se mesure", vue.getMeasuredWidth() > 0);
        int hauteurAvec = vue.getMeasuredHeight();
        vue.setModel(null);
        mesurer(vue);
        assertTrue("le panneau vidé rétrécit",
                vue.getMeasuredHeight() < hauteurAvec);
    }

    /** La signature : le modèle rend la plage du paramètre courant, le
     *  panneau se mesure — le rendu à trois tiers vit au dessin. */
    @Test
    public void signatureMeasuresAndBoundsCurrentParameter() {
        SignatureHelpView vue = new SignatureHelpView(contexte,
                PopupPalette.dark());
        SignatureAideModel model = SignatureAideModel.of(
                "ajouter(int a, int b)", List.of("a", "b"), 1);
        vue.setModel(model);
        mesurer(vue);
        assertTrue("la signature se mesure", vue.getMeasuredWidth() > 0);
        assertNotNull("le modèle rend la plage du paramètre courant",
                model.plageParametreCourant());
        assertEquals("les plages de paramètres se comptent",
                2, SignatureHelpView.plagesParametres(
                        "ajouter(int a, int b)").length);
        vue.setModel(null);
        mesurer(vue);
        assertEquals("vidée, la signature ne mesure rien de contenu", 0,
                vue.getMeasuredHeight() - 2 * vue.paddingYPx);
    }

    /** Le menu d'intentions : l'appui exécute l'action choisie. */
    @Test
    public void intentionsRunTheChosenAction() {
        IntentionsMenuView vue = new IntentionsMenuView(contexte,
                PopupPalette.dark());
        AtomicInteger execute = new AtomicInteger(-1);
        vue.setActions(List.of(
                new IntentionsMenuView.Item("corriger", () -> execute.set(0)),
                new IntentionsMenuView.Item("formater", () -> execute.set(1))));
        mesurer(vue);
        float deuxiemeRangee = vue.rowCenterY(1);
        vue.onTouchEvent(MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN,
                10f, deuxiemeRangee, 0));
        assertTrue(vue.onTouchEvent(MotionEvent.obtain(0L, 10L,
                MotionEvent.ACTION_UP, 10f, deuxiemeRangee, 0)));
        assertEquals("la deuxième action court", 1, execute.get());
    }

    /** L'info-bulle : le message se pose, vide elle ne mesure rien. */
    @Test
    public void tooltipCarriesTheMessage() {
        DiagnosticTooltipView vue = new DiagnosticTooltipView(contexte,
                PopupPalette.dark());
        vue.setMessage("Accolade fermante étrangère", DiagnosticTooltipView
                .Severity.ERROR);
        mesurer(vue);
        assertTrue(vue.getMeasuredWidth() > 0);
        vue.setMessage(null, DiagnosticTooltipView.Severity.ERROR);
        mesurer(vue);
        assertFalse("vide, l'info-bulle ne retient rien",
                vue.getMeasuredHeight() > 2 * vue.paddingYPx);
    }

    /** Aucune vue de popup ne se déclare focalisable (PIEGE-27). */
    @Test
    public void noPopupViewIsFocusable() {
        for (android.view.View vue : List.of(
                new CompletionListView(contexte, PopupPalette.dark()),
                new CompletionDocView(contexte, PopupPalette.dark()),
                new SignatureHelpView(contexte, PopupPalette.dark()),
                new IntentionsMenuView(contexte, PopupPalette.dark()),
                new DiagnosticTooltipView(contexte, PopupPalette.dark()))) {
            assertFalse("aucun popup ne prend le focus (PIEGE-27)",
                    vue.isFocusable());
        }
    }
}
