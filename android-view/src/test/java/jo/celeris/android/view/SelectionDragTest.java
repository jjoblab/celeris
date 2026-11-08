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

    /**
     * L'armement sur mot (M7.5, appui long) : le mot de départ reste
     * ENGLOBÉ — le doigt qui revient dessus ne le ronge pas, la
     * sélection ne rétrécit jamais sous le mot.
     */
    @Test
    void dragArmedOnWordKeepsTheWholeWord() {
        SelectionDrag drag = new SelectionDrag();
        // mot 10..14, ancre au début : le doigt traîne DANS le mot
        drag.down(10, 10, 14);
        assertEquals(new SelRange(10, 14), drag.dragTo(12),
                "le doigt dans le mot garde le mot entier");
        // le doigt dépasse la fin : la sélection s'étend, jamais rongée
        assertEquals(new SelRange(10, 20), drag.dragTo(20));
        // le doigt revient dans le mot : le mot reste entier
        assertEquals(new SelRange(10, 14), drag.dragTo(11));
        drag.up();
    }

    /** L'ancre à la FIN du mot : glisser à gauche étend vers le début. */
    @Test
    void dragAnchoredAtWordEndExtendsLeft() {
        SelectionDrag drag = new SelectionDrag();
        // mot 10..14, ancre à la fin : le doigt part à gauche
        drag.down(14, 10, 14);
        SelRange versGauche = drag.dragTo(4);
        assertEquals(4, versGauche.start());
        assertEquals(14, versGauche.end());
        assertEquals(14, versGauche.anchor(), "l'ancre reste figée (PIEGE-03)");
        // le doigt revient dans le mot : le mot reste entier
        SelRange dansLeMot = drag.dragTo(12);
        assertEquals(10, dansLeMot.start());
        assertEquals(14, dansLeMot.end());
        // le doigt dépasse à droite : le mot reste entier, ancre figée
        SelRange tropLoin = drag.dragTo(30);
        assertEquals(10, tropLoin.start());
        assertEquals(14, tropLoin.end());
        assertEquals(14, tropLoin.anchor(), "l'ancre reste figée (PIEGE-03)");
        drag.up();
    }

    /** L'armement sur mot invalide se refuse : jamais de fin avant le début. */
    @Test
    void armementSurMotInvalideRejete() {
        SelectionDrag drag = new SelectionDrag();
        assertThrows(IllegalArgumentException.class,
                () -> drag.down(10, 14, 10));
    }
}
