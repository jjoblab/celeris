package jo.celeris.core.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * La surcharge {@code apply(Transaction, MultiSelection)} : le geste
 * intelligent (M5) choisit sa sélection d'après — la projection automatique
 * ne peut pas la connaître (le curseur d'une paire insérée vit entre les
 * deux caractères, pas après le fermant). L'étape d'annulation enregistre
 * la sélection explicite : le refaire la restitue telle quelle (A-13,
 * pluralité et positions comprises).
 */
class TextSessionApplySelectionTest {

    /** L'horloge figée : la coalescence des frappes reste hors du propos. */
    private TextSession session(String texte) {
        return TextSession.of(texte, () -> 0L);
    }

    /** Le curseur d'une paire insérée vit entre les deux caractères. */
    @Test
    void laSelectionExplicitePrimeSurLaProjection() {
        TextSession session = session("ab");
        session.setSelection(MultiSelection.caretsAt(1));
        Transaction paire = Transaction.of(new TextEdit(1, "", "()"));
        assertTrue(session.apply(paire, MultiSelection.caretsAt(2)));
        assertEquals("a()b", session.text());
        assertEquals(List.of(2), session.selection().carets());
    }

    /** Le refaire restitue la sélection explicite, pas la projection. */
    @Test
    void leRefaireRestitueLaSelectionExplicite() {
        TextSession session = session("ab");
        session.setSelection(MultiSelection.caretsAt(1));
        Transaction paire = Transaction.of(new TextEdit(1, "", "()"));
        assertTrue(session.apply(paire, MultiSelection.caretsAt(2)));
        assertTrue(session.undo());
        assertEquals("ab", session.text());
        assertEquals(List.of(1), session.selection().carets());
        assertTrue(session.redo());
        assertEquals("a()b", session.text());
        assertEquals(List.of(2), session.selection().carets());
    }

    /** Une transaction vide ne crée pas d'étape : la sélection bouge seule. */
    @Test
    void transactionVideDeplaceLaSelectionSansEtape() {
        TextSession session = session("f()");
        session.setSelection(MultiSelection.caretsAt(2));
        int profondeur = session.undoDepth();
        assertTrue(session.apply(Transaction.of(), MultiSelection.caretsAt(3)));
        assertEquals("f()", session.text());
        assertEquals(List.of(3), session.selection().carets());
        assertEquals(profondeur, session.undoDepth());
    }

    /** Lecture seule : la mutation est refusée, la sélection ne bouge pas. */
    @Test
    void lectureSeuleRefuse() {
        TextSession session = session("ab");
        session.setSelection(MultiSelection.caretsAt(1));
        session.setReadOnly(true);
        assertFalse(session.apply(
                Transaction.of(new TextEdit(1, "", "()")),
                MultiSelection.caretsAt(2)));
        assertEquals("ab", session.text());
        assertEquals(List.of(1), session.selection().carets());
    }

    /** Les bornes invalides lèvent comme la voie ordinaire. */
    @Test
    void bornesInvalidesLevent() {
        TextSession session = session("ab");
        session.setSelection(MultiSelection.caretsAt(0));
        Transaction horsBornes = Transaction.of(new TextEdit(5, "", "x"));
        org.junit.jupiter.api.Assertions.assertThrows(
                IndexOutOfBoundsException.class,
                () -> session.apply(horsBornes, MultiSelection.caretsAt(6)));
    }

    /** L'étape explicite s'annule avec restitution de la sélection d'avant. */
    @Test
    void annulationRestitueLaSelectionDAvant() {
        TextSession session = session("ab");
        session.setSelection(MultiSelection.of(new SelRange(0, 2)));
        Transaction majuscule = Transaction.of(new TextEdit(0, "ab", "AB"));
        assertTrue(session.apply(majuscule, MultiSelection.of(new SelRange(0, 2))));
        assertTrue(session.undo());
        assertEquals("ab", session.text());
        assertEquals(0, session.selection().ranges().get(0).start());
        assertEquals(2, session.selection().ranges().get(0).end());
    }
}
