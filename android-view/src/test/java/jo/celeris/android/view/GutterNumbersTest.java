package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Tests des étiquettes de numéros mémoïsées (PIEGE-15 : jamais une
 * chaîne allouée par image dans la boucle de dessin).
 */
class GutterNumbersTest {

    /** Les étiquettes sont exactes, des unités aux centaines de mille. */
    @Test
    void etiquettesExactes() {
        GutterNumbers numbers = new GutterNumbers();
        assertEquals("1", numbers.label(1));
        assertEquals("7", numbers.label(7));
        assertEquals("42", numbers.label(42));
        assertEquals("1000", numbers.label(1000));
        assertEquals("100000", numbers.label(100000));
    }

    /** La même instance revient à chaque image : la mémoïsation est par identité. */
    @Test
    void memoisationParInstance() {
        GutterNumbers numbers = new GutterNumbers();
        String premiere = numbers.label(12);
        assertSame(premiere, numbers.label(12));
        assertSame(premiere, numbers.label(12));
    }

    /** Les numéros croissent dans le désordre : la mémoïsation comble les trous. */
    @Test
    void croissanceDansLeDesordre() {
        GutterNumbers numbers = new GutterNumbers();
        assertEquals("50", numbers.label(50));
        assertEquals("3", numbers.label(3));
        assertSame(numbers.label(50), numbers.label(50));
    }

    /** Le nombre de chiffres est servi par l'étiquette mémoïsée. */
    @Test
    void chiffresServisParEtiquette() {
        GutterNumbers numbers = new GutterNumbers();
        assertEquals(1, numbers.digits(9));
        assertEquals(4, numbers.digits(4096));
    }

    /** Un numéro invalide est une erreur de programmation, pas un silence. */
    @Test
    void numeroInvalideRejete() {
        GutterNumbers numbers = new GutterNumbers();
        assertThrows(IllegalArgumentException.class, () -> numbers.label(0));
        assertThrows(IllegalArgumentException.class, () -> numbers.label(-3));
    }
}
