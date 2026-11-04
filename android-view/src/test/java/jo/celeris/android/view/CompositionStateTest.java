package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.SelRange;

import org.junit.jupiter.api.Test;

/**
 * Tests de l'état de composition IME : la machine à états calcule les
 * plans d'édition (remplacer la plage composée, poser le curseur selon
 * la convention de la plateforme), aligne les régions de composition
 * aux frontières de clusters de graphèmes et retire le dernier cluster
 * au retour arrière — jamais une demi-paire de substitution (piège
 * Unicode du registre).
 *
 * <p>Semantique du curseur vérifiée sur les sources de la plateforme
 * (niveau 36) : position positive relative à la fin du texte moins un,
 * position nulle ou négative relative au début.</p>
 */
class CompositionStateTest {

    /** L'emoji famille « homme-femme » : cinq unités, un seul cluster. */
    private static final String FAMILLE = "\ud83d\udc68\u200d\ud83d\udc69";

    /** La composition démarre sur la sélection : elle la remplace. */
    @Test
    void composing_startsOnSelectionAndReplacesIt() {
        CompositionState etat = new CompositionState();
        Document document = Document.of("abcdef");

        CompositionState.Plan plan =
                etat.planComposing(document, new SelRange(2, 5), "xy", 1);

        assertEquals(2, plan.replaceStart());
        assertEquals(5, plan.replaceEnd());
        assertEquals("xy", plan.insertion());
        assertEquals(4, plan.caret());
        assertTrue(plan.composingAfter());
        assertEquals(2, plan.spanStart());
        assertEquals(4, plan.spanEnd());
    }

    /**
     * Chaque frappe composée remplace la composition précédente, jamais
     * un append : la plage composée gagne sur la sélection courante.
     */
    @Test
    void composing_successiveCallsReplaceTheSpanNotAppend() {
        CompositionState etat = new CompositionState();
        Document document = Document.of("abcdef");
        etat.adopt(etat.planComposing(document, new SelRange(2, 5), "xy", 1));
        Document apres = document.replace(2, 5, "xy");

        CompositionState.Plan second =
                etat.planComposing(apres, SelRange.caretAt(4), "xyz", 1);

        assertEquals(2, second.replaceStart());
        assertEquals(4, second.replaceEnd());
        assertEquals("xyz", second.insertion());
        assertEquals(5, second.caret());
    }

    /** Curseur positif : relatif à la fin du texte composé, moins un. */
    @Test
    void composing_positiveCursorIsRelativeToEndMinusOne() {
        CompositionState etat = new CompositionState();
        Document document = Document.of("abcdef");

        // fin = 2 + 3 = 5 ; position 1 -> 5 ; position 2 -> 6.
        assertEquals(5, etat.planComposing(document, SelRange.caretAt(2), "abc", 1).caret());
        assertEquals(6, etat.planComposing(document, SelRange.caretAt(2), "abc", 2).caret());
    }

    /** Curseur nul ou négatif : relatif au début du texte composé. */
    @Test
    void composing_nonPositiveCursorIsRelativeToStart() {
        CompositionState etat = new CompositionState();
        Document document = Document.of("abcdef");

        assertEquals(2, etat.planComposing(document, SelRange.caretAt(2), "abc", 0).caret());
        assertEquals(1, etat.planComposing(document, SelRange.caretAt(2), "abc", -1).caret());
    }

    /** Le curseur ne sort jamais du document : borné à la nouvelle longueur. */
    @Test
    void composing_cursorIsClampedToTheDocument() {
        CompositionState etat = new CompositionState();
        Document document = Document.of("abcdef");

        // Nouvelle longueur 6 - 3 + 3 = 6 ; position 9 -> 6.
        CompositionState.Plan plan =
                etat.planComposing(document, new SelRange(0, 3), "abc", 9);
        assertEquals(6, plan.caret());
    }

    /** Le commit remplace la composition puis la clôt. */
    @Test
    void commit_replacesTheSpanAndClosesComposition() {
        CompositionState etat = new CompositionState();
        Document document = Document.of("abcdef");
        etat.adopt(etat.planComposing(document, new SelRange(2, 5), "xy", 1));
        Document apres = document.replace(2, 5, "xy");

        CompositionState.Plan plan = etat.planCommit(apres, SelRange.caretAt(4), "Z", 1);

        assertEquals(2, plan.replaceStart());
        assertEquals(4, plan.replaceEnd());
        assertFalse(plan.composingAfter());
        assertEquals(3, plan.caret());
    }

    /** Le commit sans composition ouverte insère à la sélection. */
    @Test
    void commit_withoutCompositionInsertsAtCaret() {
        CompositionState etat = new CompositionState();
        Document document = Document.of("abcdef");

        CompositionState.Plan plan = etat.planCommit(document, SelRange.caretAt(3), "Q", 1);

        assertEquals(3, plan.replaceStart());
        assertEquals(3, plan.replaceEnd());
        assertEquals(4, plan.caret());
    }

    /** Le commit consomme une sélection non vide. */
    @Test
    void commit_consumesTheSelection() {
        CompositionState etat = new CompositionState();
        Document document = Document.of("abcdef");

        CompositionState.Plan plan = etat.planCommit(document, new SelRange(2, 5), "Q", 1);

        assertEquals(2, plan.replaceStart());
        assertEquals(5, plan.replaceEnd());
    }

    /**
     * La région de composition s'aligne aux clusters : une borne posée
     * au milieu d'un emoji ne l'ampute jamais — la région absorbe le
     * cluster entier.
     */
    @Test
    void region_snapsToWholeClusters() {
        CompositionState etat = new CompositionState();
        Document document = Document.of("a\ud83d\ude00b");

        // L'emoji occupe 1..3 ; la région demandée le coupe en deux.
        CompositionState.Plan plan = etat.planRegion(document, 2, 3);

        assertFalse(plan.hasReplacement(), "la région ne remplace rien");
        assertTrue(plan.composingAfter());
        assertEquals(1, plan.spanStart());
        assertEquals(3, plan.spanEnd());
        assertEquals(-1, plan.caret(), "la région ne déplace pas le curseur");
    }

    /** Région inversée : les bornes s'échangent, puis se bornent au document. */
    @Test
    void region_reversedIsSwappedAndClamped() {
        CompositionState etat = new CompositionState();
        Document document = Document.of("abcdefghij");

        CompositionState.Plan inversee = etat.planRegion(document, 5, 2);
        assertEquals(2, inversee.spanStart());
        assertEquals(5, inversee.spanEnd());

        CompositionState.Plan horsBornes = etat.planRegion(document, -3, 99);
        assertEquals(0, horsBornes.spanStart());
        assertEquals(10, horsBornes.spanEnd());
    }

    /** Une famille d'emoji liée par sélecteur forme UN cluster : la région l'absorbe. */
    @Test
    void region_absorbsZwjFamily() {
        CompositionState etat = new CompositionState();
        Document document = Document.of("a" + FAMILLE + "b");

        // La famille occupe 1..6 ; la région demandée coupe au milieu.
        CompositionState.Plan plan = etat.planRegion(document, 3, 4);

        assertEquals(1, plan.spanStart());
        assertEquals(6, plan.spanEnd());
    }

    /** Le retour arrière dans la composition retire le DERNIER cluster entier. */
    @Test
    void backspace_removesTheLastWholeCluster() {
        CompositionState etat = new CompositionState();
        Document document = Document.of("xy" + FAMILLE + "c");
        etat.adopt(etat.planRegion(document, 2, 8));

        CompositionState.Plan premier = etat.planClusterBackspace(document);
        assertEquals(7, premier.replaceStart());
        assertEquals(8, premier.replaceEnd());
        assertEquals("", premier.insertion());
        assertEquals(7, premier.caret());
        Document apres = document.replace(7, 8, "");
        etat.adopt(premier);

        CompositionState.Plan second = etat.planClusterBackspace(apres);
        // La famille entière forme le dernier cluster : elle part d'un bloc.
        assertEquals(2, second.replaceStart());
        assertEquals(7, second.replaceEnd());
    }

    /** Retour arrière sur une composition vide : pas de plan, la voie clavier suit. */
    @Test
    void backspace_onEmptySpanYieldsNoPlan() {
        CompositionState etat = new CompositionState();
        Document document = Document.of("abc");
        etat.adopt(etat.planRegion(document, 1, 1));

        assertNull(etat.planClusterBackspace(document));
    }

    /** Clôturer la composition ne touche pas au texte : l'état seul se vide. */
    @Test
    void finish_keepsTextAndClearsState() {
        CompositionState etat = new CompositionState();
        Document document = Document.of("abcdef");
        etat.adopt(etat.planComposing(document, new SelRange(2, 5), "xy", 1));
        assertTrue(etat.isComposing());

        etat.finish();

        assertFalse(etat.isComposing());
        assertEquals(0, etat.composingStart());
        assertEquals(0, etat.composingEnd());
    }

    /** Une composition vide reste une composition : la plage est posée, vide. */
    @Test
    void composing_emptyTextKeepsAnEmptySpan() {
        CompositionState etat = new CompositionState();
        Document document = Document.of("abcdef");
        etat.adopt(etat.planRegion(document, 2, 4));

        CompositionState.Plan plan = etat.planComposing(document, SelRange.caretAt(3), "", 1);

        assertEquals(2, plan.replaceStart());
        assertEquals(4, plan.replaceEnd());
        assertTrue(plan.composingAfter());
        assertEquals(2, plan.spanStart());
        assertEquals(2, plan.spanEnd());
    }
}
