package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.SelRange;

import org.junit.jupiter.api.Test;

/**
 * Tests des fenêtres et suppressions d'entourage IME : la fenêtre
 * {@code getSurroundingText} honore les longueurs demandées, se borne
 * au document et porte sa position de document ; les suppressions
 * d'entourage ne coupent JAMAIS un cluster de graphèmes — une demande
 * qui tomberait au milieu d'un emoji absorbe le cluster entier (piège
 * Unicode du registre : jamais une demi-paire de substitution).
 */
class SurroundingTextsTest {

    /** La fenêtre honore les longueurs, la sélection est relative à la fenêtre. */
    @Test
    void window_honorsLengthsWithRelativeSelection() {
        Document document = Document.of("abcdefghij");

        SurroundingTexts.Window fenetre =
                SurroundingTexts.window(document, new SelRange(3, 6), 2, 1);

        assertEquals("bcdefg", fenetre.text());
        assertEquals(2, fenetre.selectionStart());
        assertEquals(5, fenetre.selectionEnd());
        assertEquals(1, fenetre.docOffset());
    }

    /** La fenêtre se borne au document : ni avant le début, ni après la fin. */
    @Test
    void window_clampsAtDocumentEdges() {
        Document document = Document.of("abcdefghij");

        SurroundingTexts.Window debut =
                SurroundingTexts.window(document, SelRange.caretAt(1), 10, 2);
        assertEquals("abcde", debut.text());
        assertEquals(0, debut.docOffset());
        assertEquals(1, debut.selectionStart());

        SurroundingTexts.Window fin =
                SurroundingTexts.window(document, SelRange.caretAt(9), 2, 10);
        assertEquals("hij", fin.text());
        assertEquals(10, fin.docOffset() + fin.text().length());
    }

    /** Des longueurs négatives se traitent comme nulles. */
    @Test
    void window_negativeLengthsAreZero() {
        Document document = Document.of("abcdef");

        SurroundingTexts.Window fenetre =
                SurroundingTexts.window(document, SelRange.caretAt(3), -4, -2);

        assertEquals("", fenetre.text());
        assertEquals(0, fenetre.selectionStart());
        assertEquals(0, fenetre.selectionEnd());
        assertEquals(3, fenetre.docOffset());
    }

    /** Suppression d'entourage en unités : autour de la sélection, bornée aux bords. */
    @Test
    void delete_unitsAroundTheSelection() {
        Document document = Document.of("abcdef");

        SurroundingTexts.Deletion plan =
                SurroundingTexts.deleteAround(document, SelRange.caretAt(3), 2, 1, false);

        assertEquals(1, plan.start());
        assertEquals(4, plan.end());
    }

    /** La suppression ne franchit jamais les bords du document. */
    @Test
    void delete_clampsAtDocumentEdges() {
        Document document = Document.of("abcdef");

        SurroundingTexts.Deletion plan =
                SurroundingTexts.deleteAround(document, SelRange.caretAt(1), 5, 9, false);

        assertEquals(0, plan.start());
        assertEquals(6, plan.end());
    }

    /**
     * Une suppression d'une unité qui tomberait sur la seconde moitié
     * d'une paire de substitution absorbe le cluster entier.
     */
    @Test
    void delete_neverHalvesASurrogatePair() {
        Document document = Document.of("ab\ud83d\ude00");

        // L'emoji occupe 2..4 ; le curseur en 4, une unité avant -> 3,
        // au milieu de la paire : la borne recule au début du cluster.
        SurroundingTexts.Deletion plan =
                SurroundingTexts.deleteAround(document, SelRange.caretAt(4), 1, 0, false);

        assertEquals(2, plan.start());
        assertEquals(4, plan.end());
    }

    /** En points de code, une paire de substitution compte pour un. */
    @Test
    void delete_codePointsCountPairsAsOne() {
        Document document = Document.of("ab\ud835\uddd3c");

        // Le caractère mathématique occupe 2..4 ; une unité de code AVANT
        // le curseur posé en 4 le retire en entier.
        SurroundingTexts.Deletion plan =
                SurroundingTexts.deleteAround(document, SelRange.caretAt(4), 1, 0, true);

        assertEquals(2, plan.start());
        assertEquals(4, plan.end());
    }

    /** Une famille liée par sélecteur forme un cluster : la suppression l'absorbe. */
    @Test
    void delete_absorbsZwjFamily() {
        String famille = "\ud83d\udc68\u200d\ud83d\udc69";
        Document document = Document.of("x" + famille + "y");

        // La famille occupe 1..6 ; deux unités avant le curseur en 6
        // tombent au milieu : le cluster entier part.
        SurroundingTexts.Deletion plan =
                SurroundingTexts.deleteAround(document, SelRange.caretAt(6), 2, 0, false);

        assertEquals(1, plan.start());
        assertEquals(6, plan.end());
    }

    /** Le côté droit suit la même discipline : la borne avance au cluster entier. */
    @Test
    void delete_afterSideSnapsForward() {
        Document document = Document.of("a\ud83d\ude00b");

        // L'emoji occupe 1..3 ; deux unités après le curseur en 1
        // tombent au milieu : la borne avance à la fin du cluster.
        SurroundingTexts.Deletion plan =
                SurroundingTexts.deleteAround(document, SelRange.caretAt(1), 0, 2, false);

        assertEquals(1, plan.start());
        assertEquals(3, plan.end());
    }
}
