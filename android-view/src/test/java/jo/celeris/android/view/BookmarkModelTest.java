package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.EditSpan;

/**
 * Les signets (B-15, M5) : ajout et retrait par ligne, saut au suivant
 * et au précédent en boucle, persistance à l'édition — les signets
 * suivent les lignes par la projection des offsets (ADR-003), celui
 * dont la ligne disparaît tombe.
 */
class BookmarkModelTest {

    /** L'ajout et le retrait par bascule, les lignes triées. */
    @Test
    void ajoutEtRetraitParBascule() {
        Document doc = Document.of("a\nb\nc\nd\ne\n");
        BookmarkModel signets = new BookmarkModel();
        signets.toggle(doc, 4);
        signets.toggle(doc, 1);
        assertTrue(signets.isBookmarked(doc, 1));
        assertTrue(signets.isBookmarked(doc, 4));
        assertFalse(signets.isBookmarked(doc, 2));
        assertEquals(List.of(1, 4), signets.lines(doc));
        signets.toggle(doc, 1);
        assertFalse(signets.isBookmarked(doc, 1));
        assertEquals(List.of(4), signets.lines(doc));
    }

    /** Le saut au signet suivant et précédent, en boucle (B-15). */
    @Test
    void sautAuSuivantEtPrecedentEnBoucle() {
        Document doc = Document.of("a\nb\nc\nd\ne\nf\ng\n");
        BookmarkModel signets = new BookmarkModel();
        signets.toggle(doc, 2);
        signets.toggle(doc, 4);
        signets.toggle(doc, 6);
        assertEquals(4, signets.nextAfter(doc, 3));
        assertEquals(6, signets.nextAfter(doc, 5));
        assertEquals(2, signets.nextAfter(doc, 6));
        assertEquals(4, signets.previousBefore(doc, 6));
        assertEquals(2, signets.previousBefore(doc, 4));
        assertEquals(6, signets.previousBefore(doc, 2));
        assertEquals(-1, new BookmarkModel().nextAfter(doc, 3));
    }

    /** Le signet suit sa ligne à l'insertion au-dessus (persistance). */
    @Test
    void signetSuitSaLigneAInsertion() {
        Document doc = Document.of("a\nb\nc\n");
        BookmarkModel signets = new BookmarkModel();
        signets.toggle(doc, 2);
        // une ligne insérée avant (offset 0, « x\n »)
        signets.project(new EditSpan(0, 0, 2));
        Document edite = Document.of("x\na\nb\nc\n");
        assertEquals(List.of(3), signets.lines(edite));
        assertTrue(signets.isBookmarked(edite, 3));
    }

    /** Le signet survit à l'édition au milieu de sa ligne. */
    @Test
    void signetSurvitALEditionDansSaLigne() {
        Document doc = Document.of("abc\n");
        BookmarkModel signets = new BookmarkModel();
        signets.toggle(doc, 0);
        // insertion au milieu de la ligne (offset 1)
        signets.project(new EditSpan(1, 0, 2));
        assertTrue(signets.isBookmarked(Document.of("axxbc\n"), 0));
    }

    /** Le signet dont la ligne disparaît tombe. */
    @Test
    void signetDontLaLigneDisparaitTombe() {
        Document doc = Document.of("a\nb\nc\n");
        BookmarkModel signets = new BookmarkModel();
        signets.toggle(doc, 1);
        signets.toggle(doc, 2);
        // suppression de « b\nc » (offsets 2..5) : la ligne 1 disparaît
        signets.project(new EditSpan(2, 3, 0));
        Document edite = Document.of("a\n\n");
        // la ligne 1 fusionne, le signet de la ligne 2 tombe au point de fusion
        List<Integer> restants = signets.lines(edite);
        assertTrue(restants.size() <= 1);
    }

    /** La ligne vide finale porte son signet. */
    @Test
    void ligneVideFinalePorteSonSignet() {
        Document doc = Document.of("a\n");
        BookmarkModel signets = new BookmarkModel();
        signets.toggle(doc, 1);
        assertTrue(signets.isBookmarked(doc, 1));
        assertEquals(List.of(1), signets.lines(doc));
    }
}
