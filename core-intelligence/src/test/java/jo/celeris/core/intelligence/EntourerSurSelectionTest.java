package jo.celeris.core.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextSession;

import org.junit.jupiter.api.Test;

/**
 * L'entourage de la sélection (catégorie D de l'inventaire — « Surround
 * With », le moteur naît) : la sélection non vide se ceint, le bloc
 * gagne un cran d'indentation par ligne portante (les vides restent
 * vides), la sélection se reporte sur le bloc ceint — une transaction
 * annulable d'un geste. Pur, sans appareil.
 */
class EntourerSurSelectionTest {

    /** La sélection à entourer : deux lignes porteuses et une vide. */
    private static final String DOCUMENT = "x\nune\n\ndeux\ny\n";

    @Test
    void lentourageCeintIndenteEtSelectionneLeBloc() {
        TextSession session = TextSession.of(DOCUMENT, () -> 0L);
        // la sélection couvre « une\n\ndeux » — lignes 2 à 4
        session.setSelection(MultiSelection.of(new SelRange(2, 11)));
        EditPlan plan = Entourer.entourer(session.document(),
                session.selection(), "if (condition) {", "}");
        assertNotNull(plan, "la sélection non vide a son plan");
        assertTrue(session.apply(plan.transaction(), plan.selection()),
                "la transaction s'applique");
        assertEquals("""
                x
                if (condition) {
                    une

                    deux
                }
                y
                """, session.document().text(),
                "le bloc se ceint, chaque ligne portante gagne son cran — "
                        + "la vide reste vide");
        SelRange ceinte = session.selection().ranges().get(0);
        assertEquals("    une\n\n    deux",
                session.document().subText(ceinte.start(), ceinte.end()),
                "la sélection se reporte sur le bloc ceint — cran compris");
        session.undo();
        assertEquals(DOCUMENT, session.document().text(),
                "un geste défait l'entourage entier");
    }

    @Test
    void laSelectionVideNeSEntourePas() {
        TextSession session = TextSession.of(DOCUMENT, () -> 0L);
        session.setSelection(MultiSelection.of(SelRange.caretAt(3)));
        assertNull(Entourer.entourer(session.document(), session.selection(),
                "try {", "}"), "rien à entourer ne se devine jamais");
        assertEquals(DOCUMENT, session.document().text(),
                "le document ne bouge pas");
    }

    @Test
    void laSelectionMultipleNeSEntourePas() {
        TextSession session = TextSession.of(DOCUMENT, () -> 0L);
        session.setSelection(MultiSelection.of(new SelRange(2, 5),
                new SelRange(7, 11)));
        assertNull(Entourer.entourer(session.document(), session.selection(),
                "try {", "}"), "deux plages ne se ceignent pas d'un seul geste");
        assertEquals(DOCUMENT, session.document().text(),
                "le document ne bouge pas");
    }
}
