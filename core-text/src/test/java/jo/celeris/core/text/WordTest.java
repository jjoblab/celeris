package jo.celeris.core.text;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests de la segmentation en mots selon UAX #29 (sous-ensemble WB3 à
 * WB999 documenté dans la Javadoc de {@code Word}).
 */
class WordTest {

    @Test
    void simpleEnglishWords() {
        assertEquals(List.of(0, 5, 6, 7, 12, 13), Word.boundaries("Hello, world!"));
    }

    @Test
    void underscoreExtendsTheWord() {
        assertEquals(List.of(0, 12), Word.boundaries("mot_de_passe"));
    }

    @Test
    void apostropheKeepsFrenchElision() {
        assertEquals(List.of(0, 9), Word.boundaries("l'éditeur"));
    }

    @Test
    void decimalSeparatorsStickToNumbers() {
        assertEquals(List.of(0, 4), Word.boundaries("3.14"));
        assertEquals(List.of(0, 5), Word.boundaries("1,000"));
    }

    @Test
    void colonBetweenLettersKeepsTheWord() {
        assertEquals(List.of(0, 7), Word.boundaries("foo:bar"));
    }

    @Test
    void lettersAndDigitsGroup() {
        assertEquals(List.of(0, 9), Word.boundaries("abc123def"));
    }

    @Test
    void eachHanCharacterIsItsOwnWord() {
        assertEquals(List.of(0, 1, 2), Word.boundaries("安心"));
    }

    @Test
    void katakanaRunsGroup() {
        assertEquals(List.of(0, 4), Word.boundaries("カタカナ"));
    }

    @Test
    void whitespaceRunsGroup() {
        assertEquals(List.of(0, 1, 3, 4), Word.boundaries("a  b"));
    }

    @Test
    void lineBreaksAlwaysCut() {
        assertEquals(List.of(0, 1, 3, 4), Word.boundaries("a\r\nb"));
    }

    @Test
    void zwjEmojiFamilyIsOneWord() {
        assertEquals(List.of(0, 5), Word.boundaries("👨‍👩"));
    }

    @Test
    void nextBoundaryFromMidWord() {
        assertEquals(5, Word.nextBoundary("Hello, world!", 2));
        assertEquals(6, Word.nextBoundary("Hello, world!", 5));
    }
}
