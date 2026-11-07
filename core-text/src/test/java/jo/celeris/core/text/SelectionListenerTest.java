package jo.celeris.core.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests de l'auditeur de sélection (M7.5, §7 — la barre de sélection
 * des popups) : la notification part de TOUTE source de changement —
 * pose directe, projection par édition, annulation et refaire — et
 * seulement quand la sélection change vraiment.
 */
class SelectionListenerTest {

    private static final String DOC = "int a = 1;\nint b = 2;\n";

    /** Journal des sélections notifiées. */
    private static final class Journal implements SelectionListener {
        private final List<MultiSelection> notifiees = new ArrayList<>();

        @Override
        public void onSelectionChanged(MultiSelection selection) {
            notifiees.add(selection);
        }
    }

    /** La pose directe notifie, la pose identique se tait. */
    @Test
    void setSelectionNotifiesOnlyOnChange() {
        TextSession session = TextSession.of(DOC, () -> 0L);
        Journal journal = new Journal();
        session.addSelectionListener(journal);
        session.setSelection(MultiSelection.of(new SelRange(2, 8)));
        session.setSelection(MultiSelection.of(new SelRange(2, 8)));
        assertEquals(1, journal.notifiees.size(),
                "la pose identique ne notifie pas");
        assertEquals(new SelRange(2, 8),
                journal.notifiees.get(0).ranges().get(0));
    }

    /** La frappe projette la sélection : le curseur déplacé notifie. */
    @Test
    void typingMovesTheCaretAndNotifies() {
        TextSession session = TextSession.of(DOC, () -> 0L);
        session.setSelection(MultiSelection.of(SelRange.caretAt(0)));
        Journal journal = new Journal();
        session.addSelectionListener(journal);
        session.type("x");
        assertEquals(1, journal.notifiees.size(),
                "le curseur projeté par la frappe notifie");
        assertEquals(1, journal.notifiees.get(0).ranges().get(0).caret(),
                "le curseur notifié vit après l'insertion");
    }

    /** La frappe SUR une sélection non vide la vide : la barre s'y ferme. */
    @Test
    void typingOverSelectionNotifiesTheEmptiedSelection() {
        TextSession session = TextSession.of(DOC, () -> 0L);
        session.setSelection(MultiSelection.of(new SelRange(2, 8)));
        Journal journal = new Journal();
        session.addSelectionListener(journal);
        session.type("z");
        assertTrue(journal.notifiees.get(0).ranges().get(0).isEmpty(),
                "la sélection remplacée par la frappe est vide à la notification");
    }

    /** L'annulation restitue la sélection d'avant : elle notifie aussi. */
    @Test
    void undoRestoresSelectionAndNotifies() {
        TextSession session = TextSession.of(DOC, () -> 0L);
        session.setSelection(MultiSelection.of(new SelRange(2, 8)));
        session.type("z");
        Journal journal = new Journal();
        session.addSelectionListener(journal);
        session.undo();
        assertEquals(1, journal.notifiees.size(),
                "la restitution d'annulation notifie");
        assertEquals(new SelRange(2, 8),
                journal.notifiees.get(0).ranges().get(0),
                "la sélection notifiée est celle d'avant le geste");
    }

    /** Le désabonnement coupe la notification — le détachement tient. */
    @Test
    void removeSelectionListenerStopsNotification() {
        TextSession session = TextSession.of(DOC, () -> 0L);
        Journal journal = new Journal();
        session.addSelectionListener(journal);
        session.setSelection(MultiSelection.of(SelRange.caretAt(3)));
        session.removeSelectionListener(journal);
        session.setSelection(MultiSelection.of(SelRange.caretAt(4)));
        assertEquals(1, journal.notifiees.size(),
                "l'auditeur détaché ne reçoit plus rien");
    }

    /** La transaction à sélection explicite notifie, même vide d'édition. */
    @Test
    void emptyTransactionWithExplicitSelectionNotifies() {
        TextSession session = TextSession.of(DOC, () -> 0L);
        Journal journal = new Journal();
        session.addSelectionListener(journal);
        session.apply(Transaction.of(), MultiSelection.of(SelRange.caretAt(5)));
        assertEquals(1, journal.notifiees.size(),
                "le saut par transaction vide notifie le déplacement");
    }
}
