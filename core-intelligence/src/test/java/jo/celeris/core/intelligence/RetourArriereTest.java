package jo.celeris.core.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;

/**
 * Le retour arrière intelligent (A-07, M5) : la paire vide disparaît
 * entière, l'indentation excédentaire se dé-crante d'un cran, tout le
 * reste retombe sur la suppression par clusters de graphèmes du cœur
 * (ADR-003, livré M2) — le moteur rend la main et la session s'en charge.
 */
class RetourArriereTest {

    private final TypingRules regles = TypingRules.langage("java");

    /** La paire vide disparaît entière : un seul geste, un seul pas d'annulation. */
    @Test
    void paireVideDisparaitEntiere() {
        Document doc = Document.of("f()");
        Optional<EditPlan> plan = SmartTyping.backspace(doc,
                MultiSelection.caretsAt(2), regles, IndentStyle.spaces(4));
        assertTrue(plan.isPresent());
        EditPlan geste = plan.get();
        assertEquals("()", geste.transaction().edits().get(0).removed());
        assertTrue(geste.transaction().edits().get(0).inserted().isEmpty());
        assertEquals(List.of(1), geste.selection().carets());
    }

    /** La paire de citations vide disparaît entière. */
    @Test
    void citationsVidesDisparaissent() {
        Document doc = Document.of("x = \"\";");
        Optional<EditPlan> plan = SmartTyping.backspace(doc,
                MultiSelection.caretsAt(5), regles, IndentStyle.spaces(4));
        assertTrue(plan.isPresent());
        assertEquals("\"\"", plan.get().transaction().edits().get(0).removed());
    }

    /** L'accolade vide disparaît entière. */
    @Test
    void accoladesVidesDisparaissent() {
        Document doc = Document.of("class A {}");
        Optional<EditPlan> plan = SmartTyping.backspace(doc,
                MultiSelection.caretsAt(9), regles, IndentStyle.spaces(4));
        assertTrue(plan.isPresent());
        assertEquals("{}", plan.get().transaction().edits().get(0).removed());
    }

    /** L'indentation excédentaire se retire d'un cran, pas caractère par caractère. */
    @Test
    void indentationExcedentaireUnCran() {
        Document doc = Document.of("      x");
        // caret à la colonne 4 : quatre espaces se retirent d'un coup
        Optional<EditPlan> plan = SmartTyping.backspace(doc,
                MultiSelection.caretsAt(4), regles, IndentStyle.spaces(4));
        assertTrue(plan.isPresent());
        assertEquals("    ", plan.get().transaction().edits().get(0).removed());
        assertEquals(List.of(0), plan.get().selection().carets());
    }

    /** Sous le cran, seul le reste se retire. */
    @Test
    void sousLeCranSeulLeResteSeRetire() {
        Document doc = Document.of("   x");
        // caret à la colonne 3 : trois espaces (un cran incomplet) se retirent
        Optional<EditPlan> plan = SmartTyping.backspace(doc,
                MultiSelection.caretsAt(3), regles, IndentStyle.spaces(4));
        assertTrue(plan.isPresent());
        assertEquals("   ", plan.get().transaction().edits().get(0).removed());
    }

    /** Une sélection non vide est supprimée, sans intelligence supplémentaire. */
    @Test
    void selectionNonVideSupprimee() {
        Document doc = Document.of("abcdef");
        Optional<EditPlan> plan = SmartTyping.backspace(doc,
                MultiSelection.of(new jo.celeris.core.text.SelRange(2, 5)),
                regles, IndentStyle.spaces(4));
        assertTrue(plan.isPresent());
        assertEquals("cde", plan.get().transaction().edits().get(0).removed());
    }

    /** Rien de spécial : la main retourne au cœur (clusters de graphèmes). */
    @Test
    void rienDeSpecialRendLaMain() {
        Document doc = Document.of("famille 👨‍👩‍👧");
        Optional<EditPlan> plan = SmartTyping.backspace(doc,
                MultiSelection.caretsAt(7), regles, IndentStyle.spaces(4));
        // le repli est documenté : deleteBackward du cœur retire le cluster entier
        assertTrue(plan.isEmpty());
    }

    /** Multi-curseurs : chaque paire vide disparaît, deltas ajustés. */
    @Test
    void multiCurseursChaquePaire() {
        Document doc = Document.of("f() g()");
        Optional<EditPlan> plan = SmartTyping.backspace(doc,
                MultiSelection.caretsAt(2, 6), regles, IndentStyle.spaces(4));
        assertTrue(plan.isPresent());
        EditPlan geste = plan.get();
        assertEquals(2, geste.transaction().edits().size());
        assertEquals(1, geste.transaction().edits().get(0).start());
        // seconde paire : décalée du retrait des deux caractères de la première
        assertEquals(3, geste.transaction().edits().get(1).start());
        assertEquals(List.of(1, 3), geste.selection().carets());
    }

    /** Le plan s'applique par la voie unique. */
    @Test
    void planAppliqueParLaVoieUnique() {
        Document doc = Document.of("f()");
        Optional<EditPlan> plan = SmartTyping.backspace(doc,
                MultiSelection.caretsAt(2), regles, IndentStyle.spaces(4));
        assertTrue(plan.isPresent());
        jo.celeris.core.text.TextSession session =
                jo.celeris.core.text.TextSession.of("f()", () -> 0L);
        session.setSelection(MultiSelection.caretsAt(2));
        assertTrue(session.apply(plan.get().transaction(), plan.get().selection()));
        assertEquals("f", session.text());
        assertTrue(session.undo());
        assertEquals("f()", session.text());
    }
}
