package jo.celeris.core.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

/**
 * Tests de la segmentation en clusters de graphèmes selon UAX #29
 * (ADR-003 : la norme vit dans le cœur, jamais dans la vue).
 */
class GraphemeTest {

    private static final long SEED = 20261104L;

    private static final String PALETTE =
            "a é\r\n\u0007🇫🇷🇩🇪👨‍👩❤️‍🔥👍🏽각가각का\u0600أbcd";

    @Test
    void asciiBreaksAfterEachCharacter() {
        assertEquals(List.of(0, 1, 2, 3), Grapheme.boundaries("abc"));
    }

    @Test
    void combiningMarkJoinsItsBase() {
        assertEquals(List.of(0, 2), Grapheme.boundaries("e\u0301"));
    }

    @Test
    void crLfFormsOneCluster() {
        assertEquals(List.of(0, 1, 3, 4), Grapheme.boundaries("a\r\nb"));
    }

    @Test
    void controlCharactersAreTheirOwnClusters() {
        assertEquals(List.of(0, 1, 2, 3), Grapheme.boundaries("a\u0007b"));
    }

    @Test
    void regionalIndicatorPairsFormFlags() {
        assertEquals(List.of(0, 4), Grapheme.boundaries("🇫🇷"));
        assertEquals(List.of(0, 4, 8), Grapheme.boundaries("🇫🇷🇩🇪"));
    }

    @Test
    void zwjSequencesFormOneCluster() {
        assertEquals(List.of(0, 5), Grapheme.boundaries("👨‍👩"));
        assertEquals(List.of(0, 5), Grapheme.boundaries("❤️‍🔥"));
    }

    @Test
    void emojiModifierJoinsItsBase() {
        assertEquals(List.of(0, 4), Grapheme.boundaries("👍🏽"));
    }

    @Test
    void hangulJamoComposeOneCluster() {
        assertEquals(List.of(0, 2), Grapheme.boundaries("가"));
        assertEquals(List.of(0, 3), Grapheme.boundaries("각"));
        assertEquals(List.of(0, 1), Grapheme.boundaries("각"));
    }

    @Test
    void spacingMarkJoinsItsBase() {
        assertEquals(List.of(0, 2), Grapheme.boundaries("का"));
    }

    @Test
    void prependCharacterJoinsTheNext() {
        assertEquals(List.of(0, 2), Grapheme.boundaries("\u0600أ"));
    }

    @Test
    void nextBoundaryAtEndReturnsLength() {
        assertEquals(3, Grapheme.nextBoundary("abc", 3));
        assertEquals(2, Grapheme.nextBoundary("e\u0301", 0));
        assertThrows(IndexOutOfBoundsException.class, () -> Grapheme.nextBoundary("abc", 4));
    }

    /**
     * Cohérence avant/arrière : la frontière précédente de toute frontière
     * est la frontière qui la précède, et réciproquement — sur des textes
     * aléatoires à graine fixe (exigence de reproductibilité de la
     * mutation).
     */
    @Test
    void previousBoundaryComplementsNextEverywhere() {
        Random random = new Random(SEED);
        for (int round = 0; round < 500; round++) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 12; i++) {
                builder.append(PALETTE.charAt(random.nextInt(PALETTE.length())));
            }
            String text = builder.toString();
            List<Integer> boundaries = Grapheme.boundaries(text);
            for (int i = 1; i < boundaries.size(); i++) {
                int boundary = boundaries.get(i);
                assertEquals(boundaries.get(i - 1).intValue(),
                        Grapheme.previousBoundary(text, boundary),
                        "frontière précédente incohérente dans « " + text + " »");
                assertEquals(boundary, Grapheme.nextBoundary(text, boundaries.get(i - 1)),
                        "frontière suivante incohérente dans « " + text + " »");
            }
            for (int probe = 0; probe <= text.length(); probe++) {
                assertTrue(boundaries.contains(Grapheme.previousBoundary(text, probe)),
                        "frontière précédente inconnue dans « " + text + " »");
            }
        }
    }
}
