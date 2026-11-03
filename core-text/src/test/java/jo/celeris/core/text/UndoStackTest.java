package jo.celeris.core.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests des piles d'annulation et de la coalescence des frappes — portage
 * étendu des tests du dépôt de départ, horloge injectée (ADR-007).
 */
class UndoStackTest {

    private static TextEdit insertion(int start, String texte) {
        return new TextEdit(start, "", texte);
    }

    @Test
    void pushAndUndo() {
        UndoStack pile = new UndoStack(100, () -> 0L);
        pile.pushStep(UndoStep.of(insertion(0, "hello")));
        assertTrue(pile.canUndo());
        UndoStep undone = pile.undo();
        assertNotNull(undone);
        assertEquals(1, undone.edits().size());
        assertEquals("hello", undone.edits().get(0).inserted());
        assertFalse(pile.canUndo());
    }

    @Test
    void undoThenRedo() {
        UndoStack pile = new UndoStack(100, () -> 0L);
        pile.pushStep(UndoStep.of(insertion(0, "hello")));
        assertNotNull(pile.undo());
        assertTrue(pile.canRedo());
        assertEquals(1, pile.redoDepth());

        assertNotNull(pile.redo());
        assertTrue(pile.canUndo());
        assertEquals(0, pile.redoDepth());
    }

    @Test
    void undoEmpty_returnsNull() {
        assertNull(new UndoStack(100, () -> 0L).undo());
    }

    @Test
    void redoEmpty_returnsNull() {
        assertNull(new UndoStack(100, () -> 0L).redo());
    }

    @Test
    void newEditClearsRedoStack() {
        UndoStack pile = new UndoStack(100, () -> 0L);
        pile.pushStep(UndoStep.of(insertion(0, "a")));
        pile.undo();
        assertTrue(pile.canRedo());

        // Une nouvelle édition vide le refaire.
        pile.pushStep(UndoStep.of(insertion(0, "b")));
        assertFalse(pile.canRedo());
    }

    @Test
    void redo_doesNotClearRemainingRedoStack() {
        // Le redo repose l'étape par ajout direct : les étapes de refaire
        // RESTANTES ne doivent pas être vidées (PIEGE-07 : un undo
        // immédiat après redo revient, il ne saute pas d'étape).
        UndoStack pile = new UndoStack(100, () -> 0L);
        pile.pushStep(UndoStep.of(insertion(0, "a")));
        pile.pushStep(UndoStep.of(insertion(1, "b")));
        pile.undo();
        pile.undo();
        assertEquals(2, pile.redoDepth());
        assertNotNull(pile.redo());
        assertEquals(1, pile.redoDepth(), "le redo ne vide pas la pile de refaire restante");
        assertNotNull(pile.redo());
        assertEquals(0, pile.redoDepth());
    }

    @Test
    void maxDepthTrimsOldest() {
        UndoStack pile = new UndoStack(3, () -> 0L);
        for (int i = 0; i < 5; i++) {
            pile.pushStep(UndoStep.of(insertion(i, String.valueOf(i))));
        }
        assertEquals(3, pile.undoDepth());
        // La plus ancienne est tombée : le sommet est l'étape 4.
        UndoStep sommet = pile.undo();
        assertNotNull(sommet);
        assertEquals("4", sommet.edits().get(0).inserted());
    }

    @Test
    void maxDepthInvalid_fails() {
        assertThrows(IllegalArgumentException.class,
                () -> new UndoStack(0, () -> 0L));
    }

    @Test
    void clearResetsAll() {
        UndoStack pile = new UndoStack(100, () -> 0L);
        pile.pushStep(UndoStep.of(insertion(0, "a")));
        pile.undo();
        pile.clear();
        assertFalse(pile.canUndo());
        assertFalse(pile.canRedo());
    }

    @Test
    void depthCounts() {
        UndoStack pile = new UndoStack(100, () -> 0L);
        assertEquals(0, pile.undoDepth());
        assertEquals(0, pile.redoDepth());

        pile.pushStep(UndoStep.of(insertion(0, "a")));
        assertEquals(1, pile.undoDepth());

        pile.undo();
        assertEquals(0, pile.undoDepth());
        assertEquals(1, pile.redoDepth());
    }

    @Test
    void armedChain_coalescesAdjacentSingleChars() {
        UndoStack pile = new UndoStack(100, () -> 0L);
        pile.pushStep(UndoStep.of(insertion(0, "h")));
        pile.armNextCoalesce(insertion(0, "h"));
        assertTrue(pile.tryCoalesce(insertion(1, "e")));
        assertEquals(1, pile.undoDepth());
        // La fusion étend l'étape au sommet : deux éditions dedans.
        UndoStep fusionnee = pile.undo();
        assertNotNull(fusionnee);
        assertEquals(2, fusionnee.edits().size());
    }

    @Test
    void coalescenceWithoutArming_isRefused() {
        // Sans l'amorçage, la condition d'adjacence est inatteignable —
        // chaque caractère tapé créait une étape séparée (correctif porté).
        UndoStack pile = new UndoStack(100, () -> 0L);
        pile.pushStep(UndoStep.of(insertion(0, "h")));
        assertFalse(pile.tryCoalesce(insertion(1, "e")));
        assertEquals(1, pile.undoDepth());
    }

    @Test
    void coalescenceJustBeforeTimeout_fuses() {
        long[] maintenant = {1_000L};
        UndoStack pile = new UndoStack(100, () -> maintenant[0]);
        pile.pushStep(UndoStep.of(insertion(0, "a")));
        pile.armNextCoalesce(insertion(0, "a"));
        maintenant[0] = 1_000L + UndoStack.COALESCE_TIMEOUT_MS - 1;
        assertTrue(pile.tryCoalesce(insertion(1, "b")));
    }

    @Test
    void coalescenceAtExactTimeout_cuts() {
        long[] maintenant = {1_000L};
        UndoStack pile = new UndoStack(100, () -> maintenant[0]);
        pile.pushStep(UndoStep.of(insertion(0, "a")));
        pile.armNextCoalesce(insertion(0, "a"));
        maintenant[0] = 1_000L + UndoStack.COALESCE_TIMEOUT_MS;
        assertFalse(pile.tryCoalesce(insertion(1, "b")));
    }

    @Test
    void coalescenceNonAdjacent_isRefused() {
        UndoStack pile = new UndoStack(100, () -> 0L);
        pile.pushStep(UndoStep.of(insertion(0, "a")));
        pile.armNextCoalesce(insertion(0, "a"));
        // La chaîne attend la position 1 : la frappe à 2 ne fusionne pas.
        assertFalse(pile.tryCoalesce(insertion(2, "b")));
    }

    @Test
    void coalescenceMultiChar_isRefused() {
        UndoStack pile = new UndoStack(100, () -> 0L);
        pile.pushStep(UndoStep.of(insertion(0, "a")));
        pile.armNextCoalesce(insertion(0, "a"));
        // Une frappe de deux caractères ne coalesce jamais.
        assertFalse(pile.tryCoalesce(insertion(1, "bc")));
    }

    @Test
    void coalescenceRemoval_isRefused() {
        UndoStack pile = new UndoStack(100, () -> 0L);
        pile.pushStep(UndoStep.of(insertion(0, "ab")));
        pile.armNextCoalesce(insertion(0, "ab"));
        // Un remplacement (retrait non vide) ne coalesce jamais.
        assertFalse(pile.tryCoalesce(new TextEdit(2, "b", "c")));
    }

    @Test
    void tryCoalesceOnEmptyStack_isRefused() {
        UndoStack pile = new UndoStack(100, () -> 0L);
        assertFalse(pile.tryCoalesce(insertion(0, "a")));
    }

    @Test
    void undoCutsTheChain() {
        UndoStack pile = new UndoStack(100, () -> 0L);
        pile.pushStep(UndoStep.of(insertion(0, "a")));
        pile.armNextCoalesce(insertion(0, "a"));
        pile.undo();
        pile.redo();
        // L'annulation coupe la chaîne : la frappe adjacente suivante ne
        // fusionne pas avec l'étape reposée au sommet.
        assertFalse(pile.tryCoalesce(insertion(1, "b")));
    }

    @Test
    void pushStepCutsTheChain() {
        UndoStack pile = new UndoStack(100, () -> 0L);
        pile.pushStep(UndoStep.of(insertion(0, "a")));
        pile.armNextCoalesce(insertion(0, "a"));
        // Un lot (empilement direct) coupe la chaîne.
        pile.pushStep(UndoStep.of(insertion(5, "z")));
        assertFalse(pile.tryCoalesce(insertion(1, "b")));
    }

    @Test
    void armNextCoalesce_multiCharDoesNotArm() {
        UndoStack pile = new UndoStack(100, () -> 0L);
        pile.pushStep(UndoStep.of(insertion(0, "ab")));
        pile.armNextCoalesce(insertion(0, "ab"));
        assertFalse(pile.tryCoalesce(insertion(2, "c")));
    }

    @Test
    void undoStep_copiesDefensively() {
        List<TextEdit> source = new ArrayList<>();
        source.add(insertion(0, "a"));
        source.add(insertion(1, "b"));
        UndoStep step = new UndoStep(source);
        source.clear();
        assertEquals(2, step.edits().size());
    }

    @Test
    void undoStep_inverse_reversesAndInverts() {
        UndoStep step = new UndoStep(List.of(
                new TextEdit(0, "", "a"),
                new TextEdit(1, "b", "c")));
        UndoStep inverse = step.inverse();
        assertEquals(2, inverse.edits().size());
        assertEquals(1, inverse.edits().get(0).start());
        assertEquals("c", inverse.edits().get(0).removed());
        assertEquals("b", inverse.edits().get(0).inserted());
        assertEquals("a", inverse.edits().get(1).removed());
    }
}
