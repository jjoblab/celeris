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
        GutterNumbers numeros = new GutterNumbers();
        assertEquals("1", numeros.label(1));
        assertEquals("7", numeros.label(7));
        assertEquals("42", numeros.label(42));
        assertEquals("1000", numeros.label(1000));
        assertEquals("100000", numeros.label(100000));
    }

    /** La même instance revient à chaque image : la mémoïsation est par identité. */
    @Test
    void memoisationParInstance() {
        GutterNumbers numeros = new GutterNumbers();
        String premiere = numeros.label(12);
        assertSame(premiere, numeros.label(12));
        assertSame(premiere, numeros.label(12));
    }

    /** Les numéros croissent dans le désordre : la mémoïsation comble les trous. */
    @Test
    void croissanceDansLeDesordre() {
        GutterNumbers numeros = new GutterNumbers();
        assertEquals("50", numeros.label(50));
        assertEquals("3", numeros.label(3));
        assertSame(numeros.label(50), numeros.label(50));
    }

    /** Le nombre de chiffres est servi par l'étiquette mémoïsée. */
    @Test
    void chiffresServisParEtiquette() {
        GutterNumbers numeros = new GutterNumbers();
        assertEquals(1, numeros.digits(9));
        assertEquals(4, numeros.digits(4096));
    }

    /** Un numéro invalide est une erreur de programmation, pas un silence. */
    @Test
    void numeroInvalideRejete() {
        GutterNumbers numeros = new GutterNumbers();
        assertThrows(IllegalArgumentException.class, () -> numeros.label(0));
        assertThrows(IllegalArgumentException.class, () -> numeros.label(-3));
    }
}
