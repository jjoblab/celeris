package jo.celeris.core.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests des éditions et de la transaction (ADR-003) : immuabilité,
 * inversion, composition, validation.
 */
class TransactionTest {

    @Test
    void edits_areExposedInOrder_andUnmodifiable() {
        Transaction t = Transaction.of(
                new TextEdit(0, "", "a"),
                new TextEdit(1, "b", "c"));
        assertEquals(2, t.edits().size());
        assertEquals(1, t.edits().get(1).start());
        assertThrows(UnsupportedOperationException.class,
                () -> t.edits().add(new TextEdit(0, "", "z")));
    }

    @Test
    void ofList_copiesDefensively() {
        List<TextEdit> source = new ArrayList<>();
        source.add(new TextEdit(0, "", "a"));
        Transaction t = Transaction.of(source);
        source.clear();
        assertEquals(1, t.edits().size());
    }

    @Test
    void emptyTransaction_isEmpty() {
        assertTrue(Transaction.of().isEmpty());
        assertFalse(Transaction.of(new TextEdit(0, "", "a")).isEmpty());
    }

    @Test
    void inverse_swapsTexts_singleEdit() {
        TextEdit edition = new TextEdit(3, "abc", "XY");
        TextEdit inverse = edition.inverse();
        assertEquals(3, inverse.start());
        assertEquals("XY", inverse.removed());
        assertEquals("abc", inverse.inserted());
        // Double inverse = identité.
        assertEquals(edition, inverse.inverse());
    }

    @Test
    void inverse_reversesEditOrder() {
        Transaction t = Transaction.of(
                new TextEdit(0, "", "a"),
                new TextEdit(1, "b", ""));
        Transaction inverse = t.inverse();
        assertEquals(2, inverse.edits().size());
        assertEquals(1, inverse.edits().get(0).start());
        assertEquals("b", inverse.edits().get(0).inserted());
        assertEquals(0, inverse.edits().get(1).start());
        assertEquals("a", inverse.edits().get(1).removed());
    }

    @Test
    void compose_concatenatesInOrder() {
        Transaction a = Transaction.of(new TextEdit(0, "ab", "X"));
        Transaction b = Transaction.of(new TextEdit(3, "", "!"));
        Transaction composee = Transaction.compose(a, b);
        assertEquals(2, composee.edits().size());
        assertEquals("ab", composee.edits().get(0).removed());
        assertEquals("!", composee.edits().get(1).inserted());
    }

    @Test
    void editEnd_isStartPlusRemovedLength() {
        assertEquals(5, new TextEdit(2, "abc", "z").end());
        assertEquals(2, new TextEdit(2, "", "z").end());
    }

    @Test
    void editSpan_computesEndAndDelta() {
        EditSpan span = new EditSpan(2, 3, 5);
        assertEquals(5, span.end());
        assertEquals(2, span.delta());
        assertEquals(-3, new EditSpan(2, 5, 2).delta());
    }

    @Test
    void edit_rejectsNegativeStart() {
        assertThrows(IllegalArgumentException.class,
                () -> new TextEdit(-1, "", "x"));
    }

    @Test
    void edit_rejectsNoOpEdit() {
        assertThrows(IllegalArgumentException.class,
                () -> new TextEdit(3, "", ""));
    }
}
