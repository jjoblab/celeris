package jo.celeris.core.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextSession;
import jo.celeris.core.text.Transaction;

/**
 * La fermeture automatique des paires et des citations (A-05, M5) : le
 * moteur de décision produit un plan — transaction par la voie unique,
 * sélection d'après explicite — que la session applique. La paire fermante
 * est insérée avec le curseur au milieu, la sélection est entourée, le
 * fermant déjà présent n'est pas doublé : la frappe saute par-dessus.
 */
class FrappePairesTest {

    private final TypingRules regles = TypingRules.langage("java");

    /** L'ouvrant frappé sur un curseur vide insère la paire, curseur au milieu. */
    @Test
    void ouvrantInsereLaPaireCurseurAuMilieu() {
        Document doc = Document.of("ab");
        Optional<EditPlan> plan = SmartTyping.insert(doc,
                MultiSelection.caretsAt(1), "(", regles);
        assertTrue(plan.isPresent());
        EditPlan geste = plan.get();
        assertEquals(1, geste.transaction().edits().size());
        // rien n'est retiré, l'insertion porte la paire complète
        assertTrue(geste.transaction().edits().get(0).removed().isEmpty());
        assertEquals("()", geste.transaction().edits().get(0).inserted());
        assertEquals(List.of(2), geste.selection().carets());
    }

    /** La citation frappée insère la paire de citations, curseur au milieu. */
    @Test
    void citationInsereLaPaire() {
        Document doc = Document.of("String x = ;");
        Optional<EditPlan> plan = SmartTyping.insert(doc,
                MultiSelection.caretsAt(11), "\"", regles);
        assertTrue(plan.isPresent());
        assertEquals("\"\"", plan.get().transaction().edits().get(0).inserted());
        assertEquals(List.of(12), plan.get().selection().carets());
    }

    /** Le fermant déjà présent n'est pas doublé : la frappe saute par-dessus. */
    @Test
    void fermantDejaPresentEstSaute() {
        Document doc = Document.of("f()");
        Optional<EditPlan> plan = SmartTyping.insert(doc,
                MultiSelection.caretsAt(2), ")", regles);
        assertTrue(plan.isPresent());
        EditPlan geste = plan.get();
        assertTrue(geste.transaction().isEmpty());
        assertEquals(List.of(3), geste.selection().carets());
    }

    /** Le fermant sans contexte s'insère simplement. */
    @Test
    void fermantSansContexteSinsere() {
        Document doc = Document.of("f(");
        Optional<EditPlan> plan = SmartTyping.insert(doc,
                MultiSelection.caretsAt(2), ")", regles);
        assertTrue(plan.isPresent());
        assertEquals(")", plan.get().transaction().edits().get(0).inserted());
        assertEquals(List.of(3), plan.get().selection().carets());
    }

    /** L'ouvrant frappé sur une sélection l'entoure, le contenu reste sélectionné. */
    @Test
    void ouvrantSurSelectionEntoure() {
        Document doc = Document.of("int x;");
        Optional<EditPlan> plan = SmartTyping.insert(doc,
                MultiSelection.of(new SelRange(4, 5)), "(", regles);
        assertTrue(plan.isPresent());
        EditPlan geste = plan.get();
        assertEquals("(x)", geste.transaction().edits().get(0).inserted());
        // le contenu reste sélectionné, prêt à être frappé par-dessus
        List<SelRange> apres = geste.selection().ranges();
        assertEquals(1, apres.size());
        assertEquals(5, apres.get(0).start());
        assertEquals(6, apres.get(0).end());
    }

    /** La multi-frappe : chaque curseur reçoit sa paire, dans un seul plan. */
    @Test
    void multiCurseursChacunSaPaire() {
        Document doc = Document.of("a\nb");
        Optional<EditPlan> plan = SmartTyping.insert(doc,
                MultiSelection.caretsAt(1, 3), "{", regles);
        assertTrue(plan.isPresent());
        EditPlan geste = plan.get();
        assertEquals(2, geste.transaction().edits().size());
        // premier curseur : paire à l'offset 1 ; second : delta d'une paire
        assertEquals(1, geste.transaction().edits().get(0).start());
        assertEquals("{}", geste.transaction().edits().get(0).inserted());
        assertEquals(4, geste.transaction().edits().get(1).start());
        assertEquals("{}", geste.transaction().edits().get(1).inserted());
        assertEquals(List.of(2, 5), geste.selection().carets());
    }

    /** Le plan s'applique par la voie unique et s'annule en une étape. */
    @Test
    void lePlanSAppliqueEtSAnnule() {
        TextSession session = TextSession.of("ab", () -> 0L);
        session.setSelection(MultiSelection.caretsAt(1));
        Optional<EditPlan> plan = SmartTyping.insert(session.document(),
                session.selection(), "(", regles);
        assertTrue(plan.isPresent());
        assertTrue(session.apply(plan.get().transaction(), plan.get().selection()));
        assertEquals("a()b", session.text());
        assertEquals(List.of(2), session.selection().carets());
        assertTrue(session.undo());
        assertEquals("ab", session.text());
        assertEquals(List.of(1), session.selection().carets());
    }

    /** Un caractère sans règle retombe sur l'insertion simple. */
    @Test
    void caractereOrdinaireInsertionSimple() {
        Document doc = Document.of("ab");
        Optional<EditPlan> plan = SmartTyping.insert(doc,
                MultiSelection.caretsAt(1), "x", regles);
        assertTrue(plan.isPresent());
        assertEquals("x", plan.get().transaction().edits().get(0).inserted());
        assertEquals(List.of(2), plan.get().selection().carets());
    }

    /** L'apostrophe ne ferme pas en java quand la règle du langage l'exclut. */
    @Test
    void reglesParLangageJsonNaccepteQueLesGuillemets() {
        TypingRules json = TypingRules.langage("json");
        assertFalsePour(json, "'");
        assertTruePour(json, "\"");
    }

    private void assertFalsePour(TypingRules regles, String frappe) {
        Document doc = Document.of("x");
        Optional<EditPlan> plan = SmartTyping.insert(doc,
                MultiSelection.caretsAt(1), frappe, regles);
        assertTrue(plan.isPresent());
        // pas de paire : insertion simple
        assertEquals(1, plan.get().transaction().edits().get(0).inserted().length());
    }

    private void assertTruePour(TypingRules regles, String frappe) {
        Document doc = Document.of("x");
        Optional<EditPlan> plan = SmartTyping.insert(doc,
                MultiSelection.caretsAt(1), frappe, regles);
        assertTrue(plan.isPresent());
        assertEquals(2, plan.get().transaction().edits().get(0).inserted().length());
    }
}
