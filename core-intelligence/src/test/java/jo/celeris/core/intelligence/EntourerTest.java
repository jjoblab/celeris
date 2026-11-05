package jo.celeris.core.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;

/**
 * Entourer la sélection (A-08, M5) : accolades, guillemets ou bloc
 * multiligne — le plan précise les positions du curseur après le geste
 * (après le fermant pour la paire et la citation, sur le contenu pour le
 * bloc). La multi-sélection entoure chaque plage.
 */
class EntourerTest {

    /** Entourer par une paire : le curseur se pose après le fermant. */
    @Test
    void paireCurseurApresLeFermant() {
        Document doc = Document.of("return x;");
        EditPlan plan = SmartTyping.surround(doc,
                MultiSelection.of(new SelRange(7, 8)), "(", ")", false,
                IndentStyle.spaces(4));
        assertEquals("(x)", plan.transaction().edits().get(0).inserted());
        assertEquals("x", plan.transaction().edits().get(0).removed());
        List<SelRange> apres = plan.selection().ranges();
        assertEquals(1, apres.size());
        // après le fermant : début de plage + longueur insérée
        assertEquals(10, apres.get(0).caret());
        assertTrue(apres.get(0).isEmpty());
    }

    /** Entourer par des guillemets : curseur après le fermant. */
    @Test
    void guillemetsCurseurApresLeFermant() {
        Document doc = Document.of("String s = x;");
        EditPlan plan = SmartTyping.surround(doc,
                MultiSelection.of(new SelRange(11, 12)), "\"", "\"", false,
                IndentStyle.spaces(4));
        assertEquals("\"x\"", plan.transaction().edits().get(0).inserted());
        assertEquals(14, plan.selection().ranges().get(0).caret());
    }

    /** Entourer par un bloc : le contenu gagne un cran entre les accolades. */
    @Test
    void blocContenuIndente() {
        Document doc = Document.of("int x;");
        EditPlan plan = SmartTyping.surround(doc,
                MultiSelection.of(new SelRange(0, 6)), "{", "}", true,
                IndentStyle.spaces(4));
        String insere = plan.transaction().edits().get(0).inserted();
        assertEquals("{\n    int x;\n}", insere);
        // le curseur se pose après le fermant
        assertEquals(14, plan.selection().ranges().get(0).caret());
    }

    /** Multi-sélection : chaque plage est entourée, deltas ajustés. */
    @Test
    void multiSelectionChaquePlageEntouree() {
        Document doc = Document.of("a b");
        // « a » (0..1) et « b » (2..3)
        EditPlan plan = SmartTyping.surround(doc,
                MultiSelection.of(new SelRange(0, 1), new SelRange(2, 3)),
                "[", "]", false, IndentStyle.spaces(4));
        assertEquals(2, plan.transaction().edits().size());
        assertEquals("[a]", plan.transaction().edits().get(0).inserted());
        assertEquals("[b]", plan.transaction().edits().get(1).inserted());
        // seconde plage : décalée de deux caractères (trois insérés, un retiré)
        assertEquals(4, plan.transaction().edits().get(1).start());
        List<Integer> carets = plan.selection().carets();
        assertEquals(List.of(3, 7), carets);
    }

    /** Le plan s'applique et s'annule par la voie unique. */
    @Test
    void planAppliqueEtAnnule() {
        Document doc = Document.of("return x;");
        EditPlan plan = SmartTyping.surround(doc,
                MultiSelection.of(new SelRange(7, 8)), "{", "}", false,
                IndentStyle.spaces(4));
        jo.celeris.core.text.TextSession session =
                jo.celeris.core.text.TextSession.of("return x;", () -> 0L);
        session.setSelection(MultiSelection.of(new SelRange(7, 8)));
        assertTrue(session.apply(plan.transaction(), plan.selection()));
        assertEquals("return {x};", session.text());
        assertTrue(session.undo());
        assertEquals("return x;", session.text());
    }
}
