package jo.celeris.core.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests de la projection des plages ancrées — portage de la sémantique de
 * décalage du pipeline commun du dépôt de départ (PIEGE-07) : gravité
 * droite des débuts, gravité gauche des fins, chute des plages réduites à
 * rien.
 */
class AnchoredRangesTest {

    @Test
    void insertionBeforeRange_shiftsEverything() {
        // Insertion de 2 caractères à l'offset 3 : la plage [5, 10) suit.
        AnchoredRanges suivi = new AnchoredRanges();
        suivi.set(List.of(new AnchoredRange(5, 10)));
        suivi.project(new EditSpan(3, 0, 2));
        assertEquals(List.of(new AnchoredRange(7, 12)), suivi.ranges());
    }

    @Test
    void insertionAfterRange_changesNothing() {
        AnchoredRanges suivi = new AnchoredRanges();
        suivi.set(List.of(new AnchoredRange(1, 2)));
        suivi.project(new EditSpan(3, 0, 2));
        assertEquals(List.of(new AnchoredRange(1, 2)), suivi.ranges());
    }

    @Test
    void rangeStartingAtInsertionPoint_staysInPlace() {
        // Gravité droite du début : à la frontière de l'insertion, le début
        // reste ; la fin suit le delta.
        AnchoredRanges suivi = new AnchoredRanges();
        suivi.set(List.of(new AnchoredRange(3, 8)));
        suivi.project(new EditSpan(3, 0, 2));
        assertEquals(List.of(new AnchoredRange(3, 10)), suivi.ranges());
    }

    @Test
    void equalLengthReplacement_insideRange_keepsRange() {
        // Remplacement de même longueur à l'intérieur : la plage englobante
        // ne bouge pas.
        AnchoredRanges suivi = new AnchoredRanges();
        suivi.set(List.of(new AnchoredRange(1, 10)));
        suivi.project(new EditSpan(3, 4, 4));
        assertEquals(List.of(new AnchoredRange(1, 10)), suivi.ranges());
    }

    @Test
    void rangeInsideReplacement_drops() {
        // La plage [3, 5) est entièrement remplacée : réduite à rien, elle
        // tombe du suivi.
        AnchoredRanges suivi = new AnchoredRanges();
        suivi.set(List.of(new AnchoredRange(3, 5)));
        suivi.project(new EditSpan(2, 4, 2));
        assertEquals(List.of(), suivi.ranges());
    }

    @Test
    void rangeStartingAtReplacementEnd_snapsToNewContent() {
        // Gravité droite : le début à la frontière de fin suit l'édition ;
        // la fin suit le delta.
        AnchoredRanges suivi = new AnchoredRanges();
        suivi.set(List.of(new AnchoredRange(6, 9)));
        suivi.project(new EditSpan(2, 4, 2));
        assertEquals(List.of(new AnchoredRange(4, 7)), suivi.ranges());
    }

    @Test
    void rangeContainingReplacement_stretchesByDelta() {
        AnchoredRanges suivi = new AnchoredRanges();
        suivi.set(List.of(new AnchoredRange(0, 20)));
        suivi.project(new EditSpan(2, 4, 2));
        assertEquals(List.of(new AnchoredRange(0, 18)), suivi.ranges());
    }

    @Test
    void endInsideReplacement_fallsBackToReplacementStart() {
        // Gravité gauche de la fin : à l'intérieur de l'édition, elle
        // retombe au début de la plage remplacée.
        AnchoredRanges suivi = new AnchoredRanges();
        suivi.set(List.of(new AnchoredRange(0, 4)));
        suivi.project(new EditSpan(2, 4, 2));
        assertEquals(List.of(new AnchoredRange(0, 2)), suivi.ranges());
    }

    @Test
    void projections_accumulate() {
        // Deux éditions successives projettent cumulativement.
        AnchoredRanges suivi = new AnchoredRanges();
        suivi.set(List.of(new AnchoredRange(5, 6)));
        suivi.project(new EditSpan(0, 0, 3));
        suivi.project(new EditSpan(1, 0, 1));
        assertEquals(List.of(new AnchoredRange(9, 10)), suivi.ranges());
    }

    @Test
    void set_replacesEverything() {
        AnchoredRanges suivi = new AnchoredRanges();
        suivi.set(List.of(new AnchoredRange(5, 6)));
        suivi.set(List.of(new AnchoredRange(0, 1), new AnchoredRange(2, 3)));
        assertEquals(List.of(new AnchoredRange(0, 1), new AnchoredRange(2, 3)),
                suivi.ranges());
    }

    @Test
    void emptyRange_isRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new AnchoredRange(3, 2));
        assertThrows(IllegalArgumentException.class,
                () -> new AnchoredRange(-1, 2));
    }

    @Test
    void startGravity_atExactBoundaries() {
        // Début exactement au début de l'édition : il reste (gravité
        // droite portée — l'offset à la frontière ne suit pas l'insertion).
        assertEquals(3, AnchoredRanges.mapStart(3, new EditSpan(3, 0, 5)));
        // Début juste après l'insertion : décalé du delta.
        assertEquals(9, AnchoredRanges.mapStart(4, new EditSpan(3, 0, 5)));
        // Fin exactement à la fin de l'édition : décalée du delta (-3).
        assertEquals(4, AnchoredRanges.mapEnd(7, new EditSpan(3, 4, 1)));
        // Fin après l'édition : décalée du delta.
        assertEquals(5, AnchoredRanges.mapEnd(8, new EditSpan(3, 4, 1)));
        // Fin juste avant l'édition : intacte.
        assertEquals(2, AnchoredRanges.mapEnd(2, new EditSpan(3, 4, 1)));
    }
}
