package jo.celeris.core.text;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Random;

import org.junit.jupiter.api.Test;

/**
 * Tests de la résolution bidirectionnelle selon UAX #9 (sous-ensemble
 * documenté dans la Javadoc de {@code Bidi} — sans paires de crochets
 * N0, contrôles explicites conservés comme bornes invisibles).
 */
class BidiTest {

    private static final long SEED = 20261104L;

    private static final String PALETTE = "ab אבג1+%\u202B\u202C\u2067\u2069\t.👍\u0627\u0660";

    @Test
    void ltrLineStaysAtLevelZero() {
        Bidi.BidiInfo info = Bidi.analyze("abc");
        assertEquals(0, info.paragraphLevel());
        assertArrayEquals(new int[] {0, 0, 0}, info.levels());
        assertArrayEquals(new int[] {0, 1, 2}, info.visualToLogical());
    }

    @Test
    void rtlLineFromFirstStrong() {
        Bidi.BidiInfo info = Bidi.analyze("אבג");
        assertEquals(1, info.paragraphLevel());
        assertArrayEquals(new int[] {1, 1, 1}, info.levels());
        assertArrayEquals(new int[] {2, 1, 0}, info.visualToLogical());
    }

    @Test
    void hebrewRunInsideLtrGetsLevelOne() {
        Bidi.BidiInfo info = Bidi.analyze("abcאבגdef");
        assertEquals(0, info.paragraphLevel());
        assertArrayEquals(new int[] {0, 0, 0, 1, 1, 1, 0, 0, 0}, info.levels());
        assertArrayEquals(new int[] {0, 1, 2, 5, 4, 3, 6, 7, 8}, info.visualToLogical());
    }

    @Test
    void neutralsTakeSurroundingDirection() {
        Bidi.BidiInfo info = Bidi.analyze("abc אבג def");
        assertArrayEquals(new int[] {0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0}, info.levels());
        assertArrayEquals(new int[] {0, 1, 2, 3, 6, 5, 4, 7, 8, 9, 10}, info.visualToLogical());
    }

    @Test
    void digitsStayLtrInsideRtlParagraph() {
        Bidi.BidiInfo info = Bidi.analyze("אבג 123");
        assertEquals(1, info.paragraphLevel());
        // L'espace prend R (niveau 1) ; les chiffres européens montent à 2.
        assertArrayEquals(new int[] {1, 1, 1, 1, 2, 2, 2}, info.levels());
        // Double renversement : les chiffres se lisent de gauche à
        // droite, en tête visuelle de la phrase hébraïque.
        assertArrayEquals(new int[] {4, 5, 6, 3, 2, 1, 0}, info.visualToLogical());
    }

    @Test
    void arabicLettersForceRtlParagraph() {
        Bidi.BidiInfo info = Bidi.analyze("\u0627\u0644\u0648");
        assertEquals(1, info.paragraphLevel());
        assertArrayEquals(new int[] {1, 1, 1}, info.levels());
    }

    @Test
    void arabicIndicDigitsReadLeftToRight() {
        Bidi.BidiInfo info = Bidi.analyze("\u0627\u0660\u0661");
        assertEquals(1, info.paragraphLevel());
        // Les chiffres arabes montent au niveau 2 : bloc gauche à
        // droite dans la phrase droite à gauche.
        assertArrayEquals(new int[] {1, 2, 2}, info.levels());
        assertArrayEquals(new int[] {1, 2, 0}, info.visualToLogical());
    }

    @Test
    void explicitRleReversesRtlContent() {
        Bidi.BidiInfo info = Bidi.analyze("abc \u202Bאבג\u202C ghi");
        assertEquals(0, info.paragraphLevel());
        assertEquals(1, info.levels()[5]);
        assertEquals(1, info.levels()[6]);
        assertEquals(1, info.levels()[7]);
        assertArrayEquals(new int[] {8, 7, 6, 5},
                Arrays.copyOfRange(info.visualToLogical(), 5, 9));
    }

    @Test
    void latinInsideRleKeepsLtrDisplay() {
        // Le contenu latin d'un enchâssement droite à gauche monte au
        // niveau pair 2 (I2) : aucun renversement net à l'affichage.
        Bidi.BidiInfo info = Bidi.analyze("abc \u202Bdef\u202C ghi");
        assertEquals(0, info.paragraphLevel());
        assertEquals(2, info.levels()[5]);
        assertEquals(2, info.levels()[6]);
        assertEquals(2, info.levels()[7]);
        assertArrayEquals(new int[] {8, 5, 6, 7},
                Arrays.copyOfRange(info.visualToLogical(), 5, 9));
    }

    @Test
    void isolateRliScopesRtlContent() {
        Bidi.BidiInfo info = Bidi.analyze("a\u2067\u05D0\u05D1\u2069d");
        assertEquals(0, info.paragraphLevel());
        assertEquals(1, info.levels()[2]);
        assertEquals(1, info.levels()[3]);
        assertArrayEquals(new int[] {0, 1, 3, 2, 4, 5}, info.visualToLogical());
    }

    @Test
    void fsiResolvesByFirstStrongInside() {
        String line = "\u2068abc\u2069 \u2068\u05D0\u05D1\u2069";
        Bidi.BidiInfo info = Bidi.analyze(line);
        // Premier isolat : contenu latin, niveau pair ; second :
        // contenu hébreu, niveau impair renversé.
        assertEquals(2, info.levels()[1]);
        assertEquals(2, info.levels()[3]);
        assertEquals(1, info.levels()[7]);
        assertEquals(1, info.levels()[8]);
        assertArrayEquals(new int[] {1, 2, 3, 4},
                Arrays.copyOfRange(info.visualToLogical(), 1, 5));
        assertArrayEquals(new int[] {8, 7, 9},
                Arrays.copyOfRange(info.visualToLogical(), 7, 10));
    }

    @Test
    void trailingWhitespaceFollowsParagraphLevel() {
        // L'enchâssement gauche à droite reste ouvert : sans L1, les
        // blancs finaux garderaient son niveau 2.
        Bidi.BidiInfo info = Bidi.analyze("\u05D0 \u202Aabc   ");
        assertEquals(1, info.paragraphLevel());
        assertEquals(2, info.levels()[3]);
        assertEquals(2, info.levels()[5]);
        assertEquals(1, info.levels()[6]);
        assertEquals(1, info.levels()[7]);
        assertEquals(1, info.levels()[8]);
    }

    @Test
    void emptyLineYieldsTrivialResult() {
        Bidi.BidiInfo info = Bidi.analyze("");
        assertEquals(0, info.paragraphLevel());
        assertArrayEquals(new int[0], info.levels());
        assertArrayEquals(new int[0], info.visualToLogical());
    }

    @Test
    void euroSignAfterDigitsJoinsEuropeanNumber() {
        // W5 : le symbole collé au chiffre européen devient chiffre —
        // il monte au niveau 2 au lieu de rester neutre au niveau 1.
        Bidi.BidiInfo info = Bidi.analyze("\u05D0" + "50%");
        assertArrayEquals(new int[] {1, 2, 2, 2}, info.levels());
        assertArrayEquals(new int[] {1, 2, 3, 0}, info.visualToLogical());
    }

    @Test
    void percentBeforeDigitsJoinsEuropeanNumber() {
        // W5 dans l'autre sens : le symbole précède le chiffre.
        Bidi.BidiInfo info = Bidi.analyze("\u05D0 %50");
        assertArrayEquals(new int[] {1, 1, 2, 2, 2}, info.levels());
        assertArrayEquals(new int[] {2, 3, 4, 1, 0}, info.visualToLogical());
    }

    @Test
    void decimalSeparatorBetweenEuropeanDigitsSticks() {
        // W4 : le séparateur entre deux chiffres européens devient
        // chiffre — il suit le bloc gauche à droite.
        Bidi.BidiInfo info = Bidi.analyze("\u05D01.5");
        assertArrayEquals(new int[] {1, 2, 2, 2}, info.levels());
        assertArrayEquals(new int[] {1, 2, 3, 0}, info.visualToLogical());
    }

    @Test
    void arabicDecimalSeparatorBetweenArabicDigitsSticks() {
        // W4 côté arabe : le point entre deux chiffres arabes les suit.
        Bidi.BidiInfo info = Bidi.analyze("\u05D0\u0660.\u0662");
        assertArrayEquals(new int[] {1, 2, 2, 2}, info.levels());
        assertArrayEquals(new int[] {1, 2, 3, 0}, info.visualToLogical());
    }

    @Test
    void decimalSeparatorBetweenDifferentNumberKindsStaysNeutral() {
        // W4 refuse : chiffre européen et chiffre arabe diffèrent, le
        // séparateur redevient neutre (W6) puis prend la direction
        // environnante (N1 : les deux nombres agissent comme du RTL).
        Bidi.BidiInfo info = Bidi.analyze("\u05D01.\u0660");
        assertArrayEquals(new int[] {1, 2, 1, 2}, info.levels());
        assertArrayEquals(new int[] {3, 2, 1, 0}, info.visualToLogical());
    }

    @Test
    void europeanNumberAfterArabicLetterReadsAsArabicNumber() {
        // W2 : le chiffre européen après une lettre arabe devient
        // chiffre arabe — le symbole pourcent (W5, réservé aux chiffres
        // européens) ne peut plus s'y coller : il reste neutre au
        // niveau 1.
        Bidi.BidiInfo info = Bidi.analyze("\u0627" + "1%");
        assertArrayEquals(new int[] {1, 2, 1}, info.levels());
        assertArrayEquals(new int[] {2, 1, 0}, info.visualToLogical());
    }

    @Test
    void europeanNumberAfterLatinTakesLatinLevel() {
        // W7 : le chiffre européen dans un contexte fort gauche à
        // droite devient L — il reste au niveau 0 au lieu de monter à 2.
        Bidi.BidiInfo info = Bidi.analyze("a1");
        assertArrayEquals(new int[] {0, 0}, info.levels());
        assertArrayEquals(new int[] {0, 1}, info.visualToLogical());
    }

    @Test
    void neutralBetweenTwoRtlRunsTakesRtlInsideLtr() {
        // N1 : le point entre deux lettres droite à gauche agit comme
        // du RTL — il monte au niveau 1 dans le paragraphe gauche à
        // droite.
        Bidi.BidiInfo info = Bidi.analyze("a\u05D0.\u05D1a");
        assertArrayEquals(new int[] {0, 1, 1, 1, 0}, info.levels());
        assertArrayEquals(new int[] {0, 3, 2, 1, 4}, info.visualToLogical());
    }

    @Test
    void neutralBetweenTwoLatinTakesLatinInsideRtl() {
        // N1 en miroir : le point entre deux lettres latines au sein
        // d'un paragraphe droite à gauche prend la direction latine et
        // monte au niveau pair 2 avec elles.
        Bidi.BidiInfo info = Bidi.analyze("\u05D0a.a\u05D1");
        assertArrayEquals(new int[] {1, 2, 2, 2, 1}, info.levels());
        assertArrayEquals(new int[] {4, 1, 2, 3, 0}, info.visualToLogical());
    }

    /**
     * L'ordre visuel est toujours une permutation : fuzz à graine fixe
     * sur une palette mêlant latin, hébreu, chiffres, contrôles
     * explicites et emoji.
     */
    @Test
    void fuzzVisualOrderIsAlwaysAPermutation() {
        Random random = new Random(SEED);
        for (int round = 0; round < 500; round++) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 14; i++) {
                builder.append(PALETTE.charAt(random.nextInt(PALETTE.length())));
            }
            Bidi.BidiInfo info = Bidi.analyze(builder.toString());
            int length = builder.length();
            boolean[] seen = new boolean[length];
            for (int index : info.visualToLogical()) {
                assertTrue(index >= 0 && index < length, "index hors bornes au tour " + round);
                assertTrue(!seen[index], "permutation dupliquée au tour " + round);
                seen[index] = true;
            }
            for (int i = 0; i < length; i++) {
                assertTrue(seen[i], "permutation incomplète au tour " + round);
                assertEquals(i, info.logicalToVisual()[info.visualToLogical()[i]],
                        "inverse incohérente au tour " + round);
                assertTrue(info.levels()[i] >= 0, "niveau négatif au tour " + round);
            }
        }
    }
}
