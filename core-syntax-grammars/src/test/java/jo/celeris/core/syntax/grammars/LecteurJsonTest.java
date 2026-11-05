package jo.celeris.core.syntax.grammars;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Le lecteur JSON strict de la voie de grammaires : objets, tableaux,
 * chaînes échappées (unicode compris), nombres, booléens et null — et
 * le refus net de tout texte mal formé, avec la position en cause.
 */
class LecteurJsonTest {

    /** Les objets imbriqués se lisent, l'ordre des champs se garde. */
    @Test
    void objetsImbriques() {
        Object lu = LecteurJson.lire(
                "{\"a\": 1, \"b\": [true, false, null], \"c\": {\"d\": \"e\"}}");
        Map<?, ?> objet = assertInstanceOf(Map.class, lu);
        assertEquals(3, objet.size());
        List<?> b = assertInstanceOf(List.class, objet.get("b"));
        assertEquals(true, b.get(0));
        assertEquals(false, b.get(1));
        assertNotNull(b.get(2), "null se lit comme la sentinelle");
        Map<?, ?> c = assertInstanceOf(Map.class, objet.get("c"));
        assertEquals("e", c.get("d"));
        assertEquals("[a, b, c]", objet.keySet().toString(),
                "l'ordre des champs se garde (LinkedHashMap)");
    }

    /** Les chaînes échappées se décodent, unicode compris. */
    @Test
    void chainesEchappees() {
        Object lu = LecteurJson.lire(
                "{\"e\": \"a\\\"b\\\\c\\/d\\be\\ff\\ng\\rh\\ti\", \"u\": \"\\u0041\\u00e9\"}");
        Map<?, ?> objet = assertInstanceOf(Map.class, lu);
        assertEquals("a\"b\\c/d\be\ff\ng\rh\ti", objet.get("e"));
        assertEquals("A\u00e9", objet.get("u"));
    }

    /** Les nombres se lisent, signés, flottants et exposants compris. */
    @Test
    void nombres() {
        Object lu = LecteurJson.lire("[0, 1, -2, 3.5, -4.25e10, 5E-3]");
        List<?> tableau = assertInstanceOf(List.class, lu);
        assertEquals(0.0, tableau.get(0));
        assertEquals(1.0, tableau.get(1));
        assertEquals(-2.0, tableau.get(2));
        assertEquals(3.5, tableau.get(3));
        assertEquals(-4.25e10, tableau.get(4));
        assertEquals(5e-3, tableau.get(5));
    }

    /** Le tableau vide et l'objet vide se lisent. */
    @Test
    void vides() {
        assertEquals(0, assertInstanceOf(List.class, LecteurJson.lire("[]")).size());
        assertEquals(0, assertInstanceOf(Map.class, LecteurJson.lire("{}")).size());
    }

    /** Les blancs autour des valeurs se sautent. */
    @Test
    void blancs() {
        Object lu = LecteurJson.lire("  [ 1 , 2 ]  ");
        assertEquals(2, assertInstanceOf(List.class, lu).size());
    }

    /** Les textes mal formés sont refusés avec la position. */
    @Test
    void refus() {
        assertThrows(IllegalArgumentException.class, () -> LecteurJson.lire(""));
        assertThrows(IllegalArgumentException.class, () -> LecteurJson.lire("[1, 2"));
        assertThrows(IllegalArgumentException.class, () -> LecteurJson.lire("{\"a\" 1}"));
        assertThrows(IllegalArgumentException.class, () -> LecteurJson.lire("{a: 1}"));
        assertThrows(IllegalArgumentException.class, () -> LecteurJson.lire("[1 2]"));
        assertThrows(IllegalArgumentException.class, () -> LecteurJson.lire("\"sans fin"));
        assertThrows(IllegalArgumentException.class, () -> LecteurJson.lire("\"\\q\""));
        assertThrows(IllegalArgumentException.class, () -> LecteurJson.lire("\"\\u00\""));
        assertThrows(IllegalArgumentException.class, () -> LecteurJson.lire("trot"));
        assertThrows(IllegalArgumentException.class, () -> LecteurJson.lire("-"));
        assertThrows(IllegalArgumentException.class, () -> LecteurJson.lire("[}"));
    }
}
