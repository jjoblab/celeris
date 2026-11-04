package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jo.celeris.core.text.SelRange;

import org.junit.jupiter.api.Test;

/**
 * Tests du glissement de sélection tactile.
 *
 * <p>PIEGE-03 : dans le dépôt de départ, l'ancre d'un glissement relue à
 * chaque MOVE sautait quand le doigt franchissait l'autre borne —
 * l'ancre doit être figée au DOWN et ne jamais bouger pendant toute la
 * gestuelle.</p>
 */
class SelectionDragTest {

    /**
     * PIEGE-03 — l'ancre est figée au DOWN et traverse les allers-retours
     * sans jamais sauter.
     */
    @Test
    void selectionDrag_anchorFrozenAtDown() {
        SelectionDrag drag = new SelectionDrag();

        drag.down(10);
        assertTrue(drag.isActive());

        // Le doigt descend : la sélection s'étend depuis l'ancre figée.
        assertEquals(new SelRange(10, 30), drag.dragTo(30));

        // Le doigt REMONTE au-delà de l'ancre : la sélection s'inverse
        // proprement, l'ancre ne saute pas à la borne opposée.
        assertEquals(new SelRange(10, 5), drag.dragTo(5));
        assertEquals(10, drag.dragTo(5).anchor(), "l'ancre reste figée au DOWN (PIEGE-03)");

        // Nouvel aller-retour : la sélection revient exactement, sans dérive.
        assertEquals(new SelRange(10, 30), drag.dragTo(30));
        assertEquals(new SelRange(10, 5), drag.dragTo(5));

        drag.up();
        assertFalse(drag.isActive());
    }

    /** Une nouvelle gestuelle fige une nouvelle ancre ; l'ancienne ne survit pas. */
    @Test
    void nouvelleGestuelleNouvelleAncre() {
        SelectionDrag drag = new SelectionDrag();
        drag.down(10);
        drag.dragTo(30);
        drag.up();

        drag.down(40);
        assertEquals(new SelRange(40, 45), drag.dragTo(45));
        assertEquals(40, drag.dragTo(45).anchor());

        // L'ancre de la gestuelle précédente (10) est bien oubliée.
        drag.dragTo(12);
        assertEquals(new SelRange(40, 12), drag.dragTo(12));
    }

    /** Sans DOWN, un MOVE est rejeté : l'ancre ne peut pas être implicite. */
    @Test
    void deplacementSansAppuiRejete() {
        SelectionDrag drag = new SelectionDrag();
        assertThrows(IllegalStateException.class, () -> drag.dragTo(5));
    }
}
