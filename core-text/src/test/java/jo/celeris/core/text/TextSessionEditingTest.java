package jo.celeris.core.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests des gestes d'édition pilotés par la segmentation : la
 * suppression arrière retire le cluster de graphèmes entier, tous
 * curseurs confondus (ADR-003 — jamais une demi-paire de substitut, ni
 * un emoji amputé de son sélecteur).
 *
 * <p>Le geste coalescence avec la frappe dans la fenêtre de
 * l'historique : frappe, retour arrière et frappe suivante forment un
 * seul geste annulable, avec restauration des sélections (A-13).</p>
 */
class TextSessionEditingTest {

    private long maintenant;

    private TextSession sessionSur(String texte) {
        maintenant = 1_000L;
        return TextSession.of(texte, () -> maintenant);
    }

    @Test
    void deleteBackwardRemovesWholeGraphemes() {
        TextSession session = sessionSur("a👨‍👩b");
        session.setSelection(MultiSelection.caretsAt(6));
        assertTrue(session.deleteBackward());
        assertEquals("ab", session.text());
        assertEquals(MultiSelection.caretsAt(1), session.selection());
    }

    @Test
    void deleteBackwardRemovesAccentedGraphemeAsOne() {
        TextSession session = sessionSur("café au lait");
        session.setSelection(MultiSelection.caretsAt(4));
        assertTrue(session.deleteBackward());
        assertEquals("caf au lait", session.text());
    }

    @Test
    void deleteBackwardRemovesFlagAsOne() {
        TextSession session = sessionSur("drapeau : 🇫🇷.");
        session.setSelection(MultiSelection.caretsAt(14));
        assertTrue(session.deleteBackward());
        assertEquals("drapeau : .", session.text());
    }

    @Test
    void deleteBackwardAcrossAllCarets() {
        TextSession session = sessionSur("ab\ncd\nef");
        session.setSelection(MultiSelection.caretsAt(4, 7));
        assertTrue(session.deleteBackward());
        assertEquals("ab\nd\nf", session.text());
        assertEquals(MultiSelection.caretsAt(3, 5), session.selection());
    }

    @Test
    void deleteBackwardMergesOverlappingSpans() {
        // Deux curseurs adjacents : les plages de suppression se
        // chevauchent et fusionnent — le document n'est jamais muté à
        // moitié.
        TextSession session = sessionSur("abcd");
        session.setSelection(MultiSelection.caretsAt(2, 3));
        assertTrue(session.deleteBackward());
        assertEquals("ad", session.text());
    }

    @Test
    void deleteBackwardReplacesNonEmptySelections() {
        TextSession session = sessionSur("abcdef");
        session.setSelection(MultiSelection.of(new SelRange(2, 5)));
        assertTrue(session.deleteBackward());
        assertEquals("abf", session.text());
        assertEquals(MultiSelection.caretsAt(2), session.selection());
    }

    @Test
    void deleteBackwardAtDocumentStartIsANoop() {
        TextSession session = sessionSur("rien à supprimer");
        session.setSelection(MultiSelection.caretsAt(0));
        assertFalse(session.deleteBackward());
        assertEquals("rien à supprimer", session.text());
        assertFalse(session.canUndo());
    }

    @Test
    void deleteBackwardUndoRestoresSelection() {
        TextSession session = sessionSur("a👨‍👩b");
        session.setSelection(MultiSelection.caretsAt(6));
        assertTrue(session.deleteBackward());
        assertTrue(session.undo());
        assertEquals("a👨‍👩b", session.text());
        assertEquals(MultiSelection.caretsAt(6), session.selection());
        assertTrue(session.canRedo());
    }

    @Test
    void deleteBackwardCoalescesWithTyping() {
        TextSession session = sessionSur("");
        assertTrue(session.type("a"));
        maintenant += 10;
        assertTrue(session.deleteBackward());
        maintenant += 10;
        assertTrue(session.type("b"));
        assertEquals("b", session.text());
        // Un seul geste : la frappe et les suppressions fusionnent.
        assertEquals(1, session.undoDepth());
        assertTrue(session.undo());
        assertEquals("", session.text());
    }

    @Test
    void deleteBackwardRefusedInReadOnly() {
        TextSession session = sessionSur("abc");
        session.setSelection(MultiSelection.caretsAt(3));
        session.setReadOnly(true);
        assertFalse(session.deleteBackward());
        assertEquals("abc", session.text());
        assertEquals(0, session.undoDepth());
    }

    @Test
    void deleteBackwardGrowsWindowOnClusterLongerThanIt() {
        // Un cluster de quarante marques combinantes dépasse la
        // fenêtre de recherche initiale : la frontière certaine
        // exige d'élargir la fenêtre, jamais de couper le cluster.
        StringBuilder builder = new StringBuilder("a");
        for (int i = 0; i < 40; i++) {
            builder.append('\u0301');
        }
        TextSession session = sessionSur(builder.toString());
        session.setSelection(MultiSelection.caretsAt(builder.length()));
        assertTrue(session.deleteBackward());
        assertEquals("", session.text());
        assertTrue(session.undo());
        assertEquals(builder.toString(), session.text());
    }
}
