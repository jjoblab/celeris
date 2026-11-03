package jo.celeris.core.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Tests de l'instantané immuable de document (ADR-003) : immuabilité par
 * édition, révisions, bornes strictes.
 */
class DocumentTest {

    @Test
    void of_exposesTextAndLength() {
        Document doc = Document.of("int a = 1;\nint b = 2;\n");
        assertEquals("int a = 1;\nint b = 2;\n", doc.text());
        assertEquals(22, doc.length());
        assertEquals(0, doc.revision());
    }

    @Test
    void of_empty_isZeroLength() {
        Document doc = Document.of("");
        assertEquals(0, doc.length());
        assertEquals("", doc.text());
    }

    @Test
    void replace_returnsNewSnapshot_originalUntouched() {
        Document doc = Document.of("abcdef");
        Document remplace = doc.replace(1, 3, "XY");
        assertEquals("aXYdef", remplace.text());
        assertEquals(6, remplace.length());
        // L'instantané d'origine reste lisible tel quel : les lecteurs sur
        // instantané (ADR-007) ne voient jamais un état muté sous eux.
        assertEquals("abcdef", doc.text());
        assertEquals(6, doc.length());
    }

    @Test
    void revision_incrementsPerEffectiveEdit() {
        Document doc = Document.of("a");
        assertEquals(0, doc.revision());
        Document avecB = doc.replace(0, 0, "b");
        assertEquals(1, avecB.revision());
        assertEquals(2, avecB.replace(0, 1, "").revision());
    }

    @Test
    void replace_noOp_returnsSameSnapshot() {
        Document doc = Document.of("abc");
        assertSame(doc, doc.replace(1, 1, ""));
        assertEquals(0, doc.revision());
    }

    @Test
    void replace_outOfBounds_throws() {
        Document doc = Document.of("abc");
        assertThrows(IndexOutOfBoundsException.class, () -> doc.replace(-1, 1, "x"));
        assertThrows(IndexOutOfBoundsException.class, () -> doc.replace(1, 9, "x"));
        assertThrows(IndexOutOfBoundsException.class, () -> doc.replace(2, 1, "x"));
    }

    @Test
    void subText_extractsRange() {
        Document doc = Document.of("abcdef");
        assertEquals("cd", doc.subText(2, 4));
        assertEquals("", doc.subText(3, 3));
        assertEquals("abcdef", doc.subText(0, 6));
    }

    @Test
    void subText_outOfBounds_throws() {
        Document doc = Document.of("abc");
        assertThrows(IndexOutOfBoundsException.class, () -> doc.subText(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> doc.subText(0, 9));
        assertThrows(IndexOutOfBoundsException.class, () -> doc.subText(2, 1));
    }
}
