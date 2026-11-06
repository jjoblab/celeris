package jo.celeris.core.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.TextSession;

/**
 * Déplacer, dupliquer, joindre et supprimer des lignes (A-10, M5) : le
 * bloc de lignes touchées par la sélection est l'unité du geste — la
 * multi-sélection opère chaque bloc, deltas ajustés, un seul plan par la
 * voie unique.
 */
class ActionsLignesTest {

    /** La ligne montée échange sa place avec celle du dessus. */
    @Test
    void monterEchangeAvecLeDessus() {
        Document doc = Document.of("a\nb\nc");
        EditPlan plan = LineActions.moveLines(doc,
                MultiSelection.caretsAt(2), -1);
        assertEquals(1, plan.transaction().edits().size());
        assertEquals("a\nb", plan.transaction().edits().get(0).removed());
        assertEquals("b\na", plan.transaction().edits().get(0).inserted());
        assertEquals(List.of(0), plan.selection().carets());
    }

    /** La ligne descendue échange sa place avec celle du dessous. */
    @Test
    void descendreEchangeAvecLeDessous() {
        Document doc = Document.of("a\nb\nc");
        EditPlan plan = LineActions.moveLines(doc,
                MultiSelection.caretsAt(2), 1);
        assertEquals("b\nc", plan.transaction().edits().get(0).removed());
        assertEquals("c\nb", plan.transaction().edits().get(0).inserted());
        assertEquals(List.of(4), plan.selection().carets());
    }

    /** La première ligne ne monte pas : le plan est vide, rien ne bouge. */
    @Test
    void laPremiereLigneNeMontePas() {
        Document doc = Document.of("a\nb");
        EditPlan plan = LineActions.moveLines(doc,
                MultiSelection.caretsAt(0), -1);
        assertTrue(plan.transaction().isEmpty());
        assertEquals(List.of(0), plan.selection().carets());
    }

    /** La dernière ligne ne descend pas. */
    @Test
    void laDerniereLigneNeDescendPas() {
        Document doc = Document.of("a\nb");
        EditPlan plan = LineActions.moveLines(doc,
                MultiSelection.caretsAt(2), 1);
        assertTrue(plan.transaction().isEmpty());
    }

    /** Un bloc de plusieurs lignes monte d'un coup. */
    @Test
    void unBlocMonteDUnCoup() {
        Document doc = Document.of("a\nb\nc\nd");
        // sélection couvrant les lignes 1 et 2 (« b\nc »)
        EditPlan plan = LineActions.moveLines(doc,
                MultiSelection.of(new jo.celeris.core.text.SelRange(2, 6)), -1);
        assertEquals("a\nb\nc", plan.transaction().edits().get(0).removed());
        assertEquals("b\nc\na", plan.transaction().edits().get(0).inserted());
    }

    /** Dupliquer : la copie suit le bloc, les curseurs y descendent. */
    @Test
    void dupliquerPlaceLesCurseursDansLaCopie() {
        Document doc = Document.of("a\nb");
        // curseur en fin de ligne 0 (colonne 1)
        EditPlan plan = LineActions.duplicateLines(doc,
                MultiSelection.caretsAt(1));
        assertEquals(1, plan.transaction().edits().size());
        assertEquals("a\n", plan.transaction().edits().get(0).inserted());
        assertEquals(2, plan.transaction().edits().get(0).start());
        assertEquals(List.of(3), plan.selection().carets());
    }

    /** Joindre une ligne seule la colle à la suivante, indentation retirée. */
    @Test
    void joindreColleALaSuivante() {
        Document doc = Document.of("a\n    b\nc");
        EditPlan plan = LineActions.joinLines(doc,
                MultiSelection.caretsAt(1));
        assertEquals("\n    ", plan.transaction().edits().get(0).removed());
        assertTrue(plan.transaction().edits().get(0).inserted().isEmpty());
        assertEquals(List.of(1), plan.selection().carets());
    }

    /** Joindre un bloc de lignes ne touche pas la ligne qui suit. */
    @Test
    void joindreUnBlocResteDansLeBloc() {
        Document doc = Document.of("a\nb\nc");
        EditPlan plan = LineActions.joinLines(doc,
                MultiSelection.of(new jo.celeris.core.text.SelRange(0, 3)));
        assertEquals("a\nb", plan.transaction().edits().get(0).removed());
        assertEquals("ab", plan.transaction().edits().get(0).inserted());
    }

    /** Supprimer : le bloc disparaît avec son saut, le curseur reste en place. */
    @Test
    void supprimerEffaceLeBlocAvecSonSaut() {
        Document doc = Document.of("a\nb\nc");
        EditPlan plan = LineActions.deleteLines(doc,
                MultiSelection.caretsAt(2));
        assertEquals("b\n", plan.transaction().edits().get(0).removed());
        assertEquals(List.of(2), plan.selection().carets());
    }

    /** Multi-sélection (A-10) : chaque bloc est traité, deltas ajustés. */
    @Test
    void multiSelectionChaqueBlocSupprime() {
        Document doc = Document.of("a\nb\nc\nd");
        EditPlan plan = LineActions.deleteLines(doc,
                MultiSelection.caretsAt(0, 4));
        assertEquals(2, plan.transaction().edits().size());
        assertEquals("a\n", plan.transaction().edits().get(0).removed());
        assertEquals("c\n", plan.transaction().edits().get(1).removed());
        assertEquals(2, plan.transaction().edits().get(1).start());
        assertEquals(List.of(0, 2), plan.selection().carets());
    }

    /** Le plan s'applique par la voie unique et s'annule. */
    @Test
    void planAppliqueEtAnnule() {
        TextSession session = TextSession.of("a\nb\nc", () -> 0L);
        session.setSelection(MultiSelection.caretsAt(2));
        EditPlan plan = LineActions.moveLines(session.document(),
                session.selection(), -1);
        assertTrue(session.apply(plan.transaction(), plan.selection()));
        assertEquals("b\na\nc", session.text());
        assertTrue(session.undo());
        assertEquals("a\nb\nc", session.text());
    }
}
