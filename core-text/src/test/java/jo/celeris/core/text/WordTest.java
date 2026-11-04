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

    @Test
    void nextBoundaryAtEndReturnsLength() {
        assertEquals(13, Word.nextBoundary("Hello, world!", 13));
    }

    @Test
    void extendNumletStartsAWord() {
        // WB13b : le tiret bas ouvre un mot autant qu'il le prolonge.
        assertEquals(List.of(0, 4), Word.boundaries("_abc"));
        assertEquals(List.of(0, 3), Word.boundaries("_42"));
    }

    @Test
    void combiningMarkAtStartIsItsOwnWord() {
        // WB4 : une attache sans base ne rattache rien — la marque et
        // la lettre forment deux mots.
        assertEquals(List.of(0, 1, 2), Word.boundaries("\u0301a"));
    }

    @Test
    void markIsTransparentBetweenLetters() {
        // WB4 : la marque s'attache à sa base, la lettre suivante
        // joint à travers elle.
        assertEquals(List.of(0, 3), Word.boundaries("a\u0301b"));
    }

    @Test
    void doubleMarkWithoutBaseCutsBeforeLetter() {
        // Les attaches sans base remontent jusqu'au début : aucune
        // lettre effective ne précède, la lettre suivante coupe.
        assertEquals(List.of(0, 2, 3), Word.boundaries("\u0301\u0301b"));
    }

    @Test
    void emojiWithMarkDoesNotJoinFollowingLetter() {
        // Le précédent effectif au-delà de l'emoji et de sa marque
        // n'est pas une lettre : la lettre suivante coupe.
        assertEquals(List.of(0, 1, 4, 5), Word.boundaries("a😀\u0301b"));
    }

    @Test
    void markBetweenLetterAndSeparatorKeepsWord() {
        // WB6 puis WB7 à travers les attaches : la marque collée au
        // séparateur médian n'empêche pas le mot (bug WB4 :
        // le regard devait traverser les attaches des deux côtés).
        assertEquals(List.of(0, 4), Word.boundaries("a.\u0301b"));
    }

    @Test
    void markAfterSeparatorKeepsNumber() {
        // WB12 à travers les attaches, en miroir de WB7.
        assertEquals(List.of(0, 4), Word.boundaries("1,\u03012"));
    }

    @Test
    void midSeparatorAtEndCuts() {
        // Rien derrière le séparateur médian : le mot s'arrête avant.
        assertEquals(List.of(0, 2, 3), Word.boundaries("ab."));
    }

    @Test
    void markBeforeDecimalSeparatorKeepsNumber() {
        // WB11 à travers les attaches : la marque entre le chiffre et
        // la virgule n'empêche pas le groupe.
        assertEquals(List.of(0, 4), Word.boundaries("1\u0301,2"));
        // Sans base numérique avant, le séparateur ne recolle rien.
        assertEquals(List.of(0, 1, 2, 3), Word.boundaries("\u0301,2"));
    }
}
