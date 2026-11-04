package jo.celeris.core.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests de la multi-sélection de première classe (ADR-003) : plages
 * ancre-curseur, normalisation (tri, fusion, déduplication) et projection
 * à travers une édition appliquée par la voie de transaction unique.
 *
 * <p>La projection verrouille l'exigence de parité A-01 côté modèle : les
 * plages après une édition se décalent, les plages couvertes se rabattent,
 * celles posées au point d'insertion suivent le texte inséré — la gravité
 * droite d'ADR-003, celle des sélections.</p>
 */
class MultiSelectionTest {

    @Test
    void normalizationMergesOverlapsAndSorts() {
        MultiSelection selection = MultiSelection.of(
                SelRange.caretAt(20),
                new SelRange(0, 5),
                new SelRange(3, 8));
        assertEquals(2, selection.caretCount());
        assertEquals(new SelRange(0, 8), selection.ranges().get(0));
        assertEquals(SelRange.caretAt(20), selection.ranges().get(1));
    }

    @Test
    void duplicateCaretsAreDeduplicated() {
        MultiSelection selection = MultiSelection.caretsAt(5, 5, 5);
        assertEquals(1, selection.caretCount());
        assertEquals(List.of(5), selection.carets());
    }

    @Test
    void mergedDirectionComesFromTheRangeCarryingTheCommonEnd() {
        MultiSelection selection = MultiSelection.of(
                new SelRange(0, 5),
                new SelRange(8, 3));
        SelRange merged = selection.ranges().get(0);
        assertTrue(merged.isBackward());
        assertEquals(0, merged.start());
        assertEquals(8, merged.end());
    }

    /**
     * Remplacement de [10, 20) par « abc » (delta -7) : le curseur posé
     * au début de la plage et la plage couverte [12, 15) se rabattent sur
     * la fin du remplacement puis fusionnent ; les plages suivantes se
     * décalent ; la liste reste triée.
     */
    @Test
    void projectShiftsFollowingRanges_andCollapsesCoveredOnes() {
        EditSpan span = new EditSpan(10, 10, 3);
        MultiSelection selection = MultiSelection.of(
                SelRange.caretAt(25),
                new SelRange(26, 28),
                new SelRange(12, 15),
                new SelRange(5, 8),
                SelRange.caretAt(10));
        MultiSelection projected = selection.project(span);
        assertEquals(4, projected.caretCount());
        assertEquals(new SelRange(5, 8), projected.ranges().get(0));
        assertEquals(SelRange.caretAt(13), projected.ranges().get(1));
        assertEquals(SelRange.caretAt(18), projected.ranges().get(2));
        assertEquals(new SelRange(19, 21), projected.ranges().get(3));
    }

    @Test
    void projectPreservesBackwardDirection() {
        EditSpan span = new EditSpan(10, 10, 3);
        MultiSelection selection = MultiSelection.of(new SelRange(25, 12));
        MultiSelection projected = selection.project(span);
        SelRange range = projected.ranges().get(0);
        assertTrue(range.isBackward());
        assertEquals(13, range.caret());
        assertEquals(18, range.anchor());
    }

    @Test
    void projectedCaretsMergedOntoTheSamePointFuse() {
        EditSpan span = new EditSpan(5, 7, 0);
        MultiSelection selection = MultiSelection.caretsAt(5, 12);
        MultiSelection projected = selection.project(span);
        assertEquals(1, projected.caretCount());
        assertEquals(SelRange.caretAt(5), projected.ranges().get(0));
    }

    @Test
    void emptySelectionHasNoCaret() {
        assertTrue(MultiSelection.empty().isEmpty());
        assertEquals(0, MultiSelection.empty().caretCount());
        assertEquals(List.of(), MultiSelection.empty().carets());
        assertEquals(MultiSelection.empty(), MultiSelection.of());
        assertEquals(MultiSelection.empty(), MultiSelection.of(List.of()));
    }

    @Test
    void negativeOffsetsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SelRange(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new SelRange(0, -1));
        assertThrows(IllegalArgumentException.class, () -> MultiSelection.caretsAt(-3));
    }

    @Test
    void equalityFollowsTheNormalizedRanges() {
        assertEquals(MultiSelection.caretsAt(2, 7),
                MultiSelection.of(SelRange.caretAt(7), SelRange.caretAt(2)));
        assertEquals(MultiSelection.caretsAt(2, 7).hashCode(),
                MultiSelection.of(SelRange.caretAt(7), SelRange.caretAt(2)).hashCode());
    }
}
