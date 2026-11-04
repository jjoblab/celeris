package jo.celeris.core.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests de la sélection portée par la session : la frappe multi-curseurs
 * (A-01), la restauration des sélections à l'annulation et au
 * rétablissement (A-13) et la coalescence des frappes consécutives.
 *
 * <p>A-13 exige que l'annulation restitue le texte ET la sélection exacte
 * d'avant — pluralité, direction et positions comprises ; le rétablissement
 * restitue la sélection d'après. La sélection se projette à travers TOUTE
 * édition de la voie unique, y compris les éditions distantes passées par
 * {@code apply} (ADR-003).</p>
 */
class TextSessionSelectionTest {

    /** Horloge scriptée : la fenêtre de coalescence se pilote sans sommeil. */
    private long maintenant;

    private TextSession session(String texte) {
        maintenant = 1_000L;
        return TextSession.of(texte, () -> maintenant);
    }

    private void avancer(long millis) {
        maintenant += millis;
    }

    @Test
    void undoRestoresTheExactSelectionBeforeTheEdit() {
        TextSession session = session("hello world");
        session.setSelection(MultiSelection.caretsAt(5));
        assertTrue(session.type(" X"));
        assertEquals("hello X world", session.text());
        assertEquals(MultiSelection.caretsAt(7), session.selection());
        assertTrue(session.undo());
        assertEquals("hello world", session.text());
        assertEquals(MultiSelection.caretsAt(5), session.selection());
        assertFalse(session.canUndo());
    }

    /**
     * A-01 côté modèle : la frappe simultanée sur trois curseurs est une
     * transaction appliquée aux trois plages ; l'annulation d'un seul geste
     * restitue le texte et les trois sélections d'avant.
     */
    @Test
    void multiCursorTyping_undoesAsOneGesture_andRestoresAllCarets() {
        TextSession session = session("aaaa\nbbbb\ncccc");
        session.setSelection(MultiSelection.caretsAt(0, 5, 10));
        assertTrue(session.type("-"));
        assertEquals("-aaaa\n-bbbb\n-cccc", session.text());
        assertEquals(MultiSelection.caretsAt(1, 7, 13), session.selection());
        assertTrue(session.undo());
        assertEquals("aaaa\nbbbb\ncccc", session.text());
        assertEquals(MultiSelection.caretsAt(0, 5, 10), session.selection());
        assertTrue(session.redo());
        assertEquals("-aaaa\n-bbbb\n-cccc", session.text());
        assertEquals(MultiSelection.caretsAt(1, 7, 13), session.selection());
    }

    @Test
    void redoRestoresTheSelectionAfterTheEdit() {
        TextSession session = session("abc");
        session.setSelection(MultiSelection.of(new SelRange(3, 1)));
        assertTrue(session.type("XY"));
        assertEquals("aXY", session.text());
        assertTrue(session.undo());
        assertTrue(session.redo());
        assertEquals("aXY", session.text());
        assertEquals(MultiSelection.caretsAt(3), session.selection());
    }

    @Test
    void undoRestoresBackwardSelectionsWithTheirDirection() {
        TextSession session = session("abcdef");
        SelRange inversee = new SelRange(6, 2);
        session.setSelection(MultiSelection.of(inversee));
        assertTrue(session.type("Z"));
        assertEquals("abZ", session.text());
        assertTrue(session.undo());
        assertEquals("abcdef", session.text());
        assertEquals(MultiSelection.of(inversee), session.selection());
        assertTrue(session.selection().ranges().get(0).isBackward());
    }

    @Test
    void typingCoalescesWithinTheWindow_andSplitsBeyond() {
        TextSession session = session("");
        assertTrue(session.type("a"));
        avancer(100);
        assertTrue(session.type("b"));
        avancer(100);
        assertTrue(session.type("c"));
        assertEquals("abc", session.text());
        // Une seule étape : un geste d'annulation retire tout.
        assertEquals(1, session.undoDepth());
        assertTrue(session.undo());
        assertEquals("", session.text());

        assertTrue(session.redo());
        avancer(2_000);
        assertTrue(session.type("d"));
        avancer(2_000);
        assertTrue(session.type("e"));
        assertEquals("abcde", session.text());
        assertTrue(session.undo());
        assertEquals("abcd", session.text());
        assertTrue(session.undo());
        assertEquals("abc", session.text());
    }

    /**
     * La frappe multi-curseurs consécutive coalesce elle aussi : deux
     * frappes sur deux curseurs forment UN geste, annulé d'un coup.
     */
    @Test
    void multiCursorTyping_coalescesAcrossKeystrokes() {
        TextSession session = session("ab");
        session.setSelection(MultiSelection.caretsAt(1, 2));
        assertTrue(session.type("X"));
        avancer(50);
        assertTrue(session.type("Y"));
        assertEquals("aXYbXY", session.text());
        assertEquals(1, session.undoDepth(), "deux frappes multi-curseurs = un geste");
        assertTrue(session.undo());
        assertEquals("ab", session.text());
        assertEquals(MultiSelection.caretsAt(1, 2), session.selection());
    }

    @Test
    void multiCursorTyping_neverCoalescesWithASingleCursorBurst() {
        TextSession session = session("ab");
        session.setSelection(MultiSelection.caretsAt(1, 2));
        assertTrue(session.type("X"));
        // Le nombre de curseurs change : le geste est distinct.
        session.setSelection(MultiSelection.caretsAt(3));
        avancer(50);
        assertTrue(session.type("Y"));
        assertEquals(2, session.undoDepth());
    }

    @Test
    void newEditClearsTheRedoStack() {
        TextSession session = session("");
        assertTrue(session.type("a"));
        assertTrue(session.undo());
        assertTrue(session.canRedo());
        assertTrue(session.type("b"));
        assertFalse(session.canRedo());
        assertEquals("b", session.text());
    }

    /**
     * Une édition distante (agent, hôte) passée par la voie unique projette
     * la sélection comme toute édition locale.
     */
    @Test
    void externalEditsProjectTheSelectionThroughApply() {
        TextSession session = session("0123456789abcdefghij0123456789");
        session.setSelection(MultiSelection.caretsAt(25));
        assertTrue(session.apply(Transaction.of(new TextEdit(10, "abcdefghij", "abc"))));
        assertEquals("0123456789abc" + "0123456789", session.text());
        assertEquals(MultiSelection.caretsAt(18), session.selection());
    }

    @Test
    void replaceRangeProjectsTheSelectionToo() {
        TextSession session = session("hello world");
        session.setSelection(MultiSelection.of(new SelRange(5, 11)));
        assertTrue(session.replaceRange(0, 5, "goodbye"));
        assertEquals("goodbye world", session.text());
        // L'ancre et le curseur suivent le décalage (+2) : « world » reste
        // couvert dans les coordonnées d'après.
        assertEquals(new SelRange(7, 13), session.selection().ranges().get(0));
    }

    @Test
    void replaceAllProjectsTheSelection_andUndoRestoresIt() {
        TextSession session = session("deux x deux");
        session.setSelection(MultiSelection.caretsAt(11));
        assertEquals(2, session.replaceAll("deux", "2", new FindOptions()));
        assertEquals("2 x 2", session.text());
        // Le curseur suit le delta cumulé des deux remplacements (-6).
        assertEquals(MultiSelection.caretsAt(5), session.selection());
        assertTrue(session.undo());
        assertEquals("deux x deux", session.text());
        assertEquals(MultiSelection.caretsAt(11), session.selection());
    }

    @Test
    void setSelectionRejectsRangesBeyondTheDocument() {
        TextSession session = session("abc");
        assertThrows(IllegalArgumentException.class,
                () -> session.setSelection(MultiSelection.caretsAt(4)));
        session.setSelection(MultiSelection.caretsAt(3));
        assertEquals(MultiSelection.caretsAt(3), session.selection());
    }

    @Test
    void typeOnEmptySelectionChangesNothing() {
        TextSession session = session("abc");
        session.setSelection(MultiSelection.empty());
        assertFalse(session.type("x"));
        assertEquals("abc", session.text());
        assertEquals(0, session.undoDepth());
    }

    @Test
    void readOnlyRefusesTyping_withoutConsumingAnything() {
        TextSession session = session("abc");
        session.setSelection(MultiSelection.caretsAt(3));
        session.setReadOnly(true);
        assertFalse(session.type("d"));
        assertEquals("abc", session.text());
        assertEquals(0, session.undoDepth());
    }

    @Test
    void freshSessionCarriesACaretAtOrigin() {
        TextSession session = session("texte");
        assertEquals(MultiSelection.caretsAt(0), session.selection());
        assertTrue(session.type("il "));
        assertEquals("il texte", session.text());
        assertEquals(MultiSelection.caretsAt(3), session.selection());
    }
}
