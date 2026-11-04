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

    @Test
    void regionalIndicatorRangeBoundaries() {
        // Première et dernière paires de la plage U+1F1E6-U+1F1FF :
        // chaque extrême reste un indicateur régional.
        assertEquals(List.of(0, 4), Grapheme.boundaries("\uD83C\uDDE6\uD83C\uDDE6"));
        assertEquals(List.of(0, 4), Grapheme.boundaries("\uD83C\uDDFF\uD83C\uDDFF"));
    }

    @Test
    void emojiModifierRangeBoundaries() {
        // Première et dernière attaches de la plage U+1F3FB-U+1F3FF.
        assertEquals(List.of(0, 4), Grapheme.boundaries("👍\uD83C\uDFFB"));
        assertEquals(List.of(0, 4), Grapheme.boundaries("👍\uD83C\uDFFF"));
        // U+1F3FA est un pictogramme, pas une attache : deux clusters.
        assertEquals(List.of(0, 2, 4), Grapheme.boundaries("👍\uD83C\uDFFA"));
    }

    @Test
    void hangulSyllableBoundaries() {
        // Première syllabe (LV) et dernière (LVT) de U+AC00-U+D7A3 :
        // toutes deux suivies d'une consonne finale, elles joignent (GB7).
        assertEquals(List.of(0, 2), Grapheme.boundaries("\uAC00\u11A8"));
        assertEquals(List.of(0, 2), Grapheme.boundaries("\uD7A3\u11A8"));
    }

    @Test
    void hangulJamoBoundaries() {
        // Bornes des plages de jamo : dernière consonne de départ et
        // première voyelle ; dernière voyelle et première finale.
        assertEquals(List.of(0, 2), Grapheme.boundaries("\u115F\u1160"));
        assertEquals(List.of(0, 2), Grapheme.boundaries("\u11A7\u11A8"));
        // GB6 : deux consonnes de départ se suivent dans le cluster.
        assertEquals(List.of(0, 2), Grapheme.boundaries("\u1100\u1100"));
        // GB7 : deux voyelles de suite.
        assertEquals(List.of(0, 2), Grapheme.boundaries("\u1160\u1160"));
    }

    @Test
    void zwnjJoinsAsExtend() {
        // U+200C est une attache au sens des clusters : la marque qui
        // suit rejoint le cluster ouvert.
        assertEquals(List.of(0, 3), Grapheme.boundaries("a\u200C\u0301"));
    }

    @Test
    void zwjAloneDoesNotGlueEmoji() {
        // GB9 : le sélecteur suit toujours le caractère précédent ; mais
        // sans pictogramme avant lui, GB11 ne colle pas l'emoji suivant.
        assertEquals(List.of(0, 2, 4), Grapheme.boundaries("a\u200D😀"));
        assertEquals(List.of(0, 1, 3), Grapheme.boundaries("\u200D😀"));
    }

    @Test
    void twoConsecutiveEmojiStayApart() {
        // Deux pictogrammes juxtaposés forment deux clusters : GB11
        // exige le sélecteur de largeur nulle.
        assertEquals(List.of(0, 2, 4), Grapheme.boundaries("😀😀"));
    }

    @Test
    void emojiWithSkinToneZwjSequence() {
        // Homme, teinte, sélecteur, symbole médical, sélecteur de
        // variante : un seul cluster (GB9, GB11).
        assertEquals(List.of(0, 7),
                Grapheme.boundaries("👨\uD83C\uDFFD\u200D\u2695\uFE0F"));
    }

    @Test
    void regionalIndicatorOddRunAfterLetter() {
        // Une lettre avant les indicateurs borne le décompte : la
        // paire forme un cluster, l'indicateur impair reste seul.
        assertEquals(List.of(0, 1, 5, 7),
                Grapheme.boundaries("a\uD83C\uDDE6\uD83C\uDDE6\uD83C\uDDE6"));
    }

    @Test
    void controlBreaksCombiningChain() {
        // Un contrôle isole : la marque ne traverse pas GB4/GB5 pour
        // rejoindre la base, et le contrôle ne s'attache jamais.
        assertEquals(List.of(0, 1, 2, 3), Grapheme.boundaries("e\u0001\u0301"));
        assertEquals(List.of(0, 2, 3), Grapheme.boundaries("e\u0301\u0001"));
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
