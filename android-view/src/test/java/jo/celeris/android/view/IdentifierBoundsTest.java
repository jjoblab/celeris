package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;

/**
 * Les bornes de l'identifiant sous une position (sélection par appui
 * long, M7.5) : le mot entier se prend quand le doigt vit sur un de ses
 * caractères, le blanc entre deux mots et la ponctuation ne prennent
 * rien, les bornes restent dans le document.
 */
class IdentifierBoundsTest {

    /** Le point au milieu prend le mot entier, des deux côtés. */
    @Test
    void around_midWordTakesTheWholeWord() {
        Document doc = Document.of("alpha beta gamma");
        IdentifierBounds.Around mot = IdentifierBounds.around(doc, 2);
        assertEquals(0, mot.start());
        assertEquals(5, mot.end());
    }

    /** Le point sur le dernier caractère du mot prend le mot entier. */
    @Test
    void around_wordEndTakesTheWholeWord() {
        Document doc = Document.of("alpha beta");
        IdentifierBounds.Around mot = IdentifierBounds.around(doc, 4);
        assertEquals(0, mot.start());
        assertEquals(5, mot.end());
    }

    /** Le point sur le blanc entre deux mots ne sélectionne rien. */
    @Test
    void around_spaceBetweenWordsTakesNothing() {
        Document doc = Document.of("alpha beta");
        IdentifierBounds.Around mot = IdentifierBounds.around(doc, 5);
        assertEquals(5, mot.start());
        assertEquals(5, mot.end());
    }

    /** Le point sur un signe de ponctuation ne sélectionne rien. */
    @Test
    void around_punctuationTakesNothing() {
        Document doc = Document.of("calcul(x)");
        IdentifierBounds.Around mot = IdentifierBounds.around(doc, 6);
        assertEquals(6, mot.start());
        assertEquals(6, mot.end());
    }

    /** Le tiret bas prolonge le mot, les chiffres aussi. */
    @Test
    void around_underscoreAndDigitsExtendTheWord() {
        Document doc = Document.of("mot_de_passe42 = lu");
        IdentifierBounds.Around mot = IdentifierBounds.around(doc, 7);
        assertEquals(0, mot.start());
        assertEquals(14, mot.end());
    }

    /** La position se borne au document : jamais hors bornes. */
    @Test
    void around_positionClampedToDocument() {
        Document doc = Document.of("alpha");
        IdentifierBounds.Around tropLoin = IdentifierBounds.around(doc, 99);
        assertEquals(5, tropLoin.start());
        assertEquals(5, tropLoin.end());
        IdentifierBounds.Around negatif = IdentifierBounds.around(doc, -3);
        assertEquals(0, negatif.start());
        IdentifierBounds.Around dernier = IdentifierBounds.around(doc, 4);
        assertEquals(0, dernier.start());
        assertEquals(5, dernier.end());
    }
}
