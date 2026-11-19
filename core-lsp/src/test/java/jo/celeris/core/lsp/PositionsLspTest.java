package jo.celeris.core.lsp;

import jo.celeris.core.lsp.types.RangeLsp;
import jo.celeris.core.lsp.types.LspPosition;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;

/**
 * La transposition des positions LSP (ADR-005, point 5) : les colonnes
 * du protocole comptent les UNITÉS UTF-16 — le repère naturel d'une
 * chaîne Java — et traversent une frontière explicite avec le document.
 * Les bornes sont clampées : ligne au-delà vers la dernière, colonne au-delà
 * vers la fin du contenu, négatifs vers zéro — jamais d'exception
 * sur une position de serveur généreux.
 */
class PositionsLspTest {

    /** L'aller-retour offset → position → offset sur un document simple. */
    @Test
    void lallerRetourSimple() {
        Document doc = Document.of("ab\ncd\n");
        assertEquals(new LspPosition(0, 0), LspPositions.position(doc, 0));
        assertEquals(new LspPosition(0, 1), LspPositions.position(doc, 1));
        // l'offset du saut de ligne appartient à la ligne qu'il termine : colonne = longueur
        assertEquals(new LspPosition(0, 2), LspPositions.position(doc, 2));
        assertEquals(new LspPosition(1, 0), LspPositions.position(doc, 3));
        assertEquals(new LspPosition(1, 2), LspPositions.position(doc, 5));
        assertEquals(new LspPosition(2, 0), LspPositions.position(doc, 6));
        // retour
        assertEquals(0, LspPositions.offset(doc, new LspPosition(0, 0)));
        assertEquals(2, LspPositions.offset(doc, new LspPosition(0, 2)));
        assertEquals(3, LspPositions.offset(doc, new LspPosition(1, 0)));
        assertEquals(5, LspPositions.offset(doc, new LspPosition(1, 2)));
        assertEquals(6, LspPositions.offset(doc, new LspPosition(2, 0)));
    }

    /** Les paires de substitution : la colonne compte deux unités par point de code astral. */
    @Test
    void lesPairesDeSubstitution() {
        // « 😀 » est une paire de deux unités UTF-16 ; « x » en suit une
        Document doc = Document.of("😀x\ny");
        assertEquals(new LspPosition(0, 2), LspPositions.position(doc, 2));
        assertEquals(new LspPosition(0, 3), LspPositions.position(doc, 3));
        assertEquals(2, LspPositions.offset(doc, new LspPosition(0, 2)));
        assertEquals(4, LspPositions.offset(doc, new LspPosition(1, 0)));
    }

    /** Le clampage : ligne au-delà, colonne au-delà, négatifs — jamais d'exception. */
    @Test
    void leClampage() {
        Document doc = Document.of("ab\ncd\n");
        assertEquals(6, LspPositions.offset(doc, new LspPosition(99, 0)));
        assertEquals(2, LspPositions.offset(doc, new LspPosition(0, 99)));
        assertEquals(0, LspPositions.offset(doc, new LspPosition(-1, -1)));
        // une colonne au-delà du contenu de la ligne retombe sur sa fin
        assertEquals(5, LspPositions.offset(doc, new LspPosition(1, 42)));
    }

    /** Le clampage sur ligne vide : la colonne ne peut pas dépasser zéro. */
    @Test
    void leClampageSurLigneVide() {
        Document doc = Document.of("a\n\nc");
        // ligne 1 vide : toute colonne retombe à son début
        assertEquals(2, LspPositions.offset(doc, new LspPosition(1, 7)));
        assertEquals(new LspPosition(1, 0), LspPositions.position(doc, 2));
    }

    /** Le document vide : une ligne, une colonne, l'offset zéro. */
    @Test
    void leDocumentVide() {
        Document doc = Document.of("");
        assertEquals(new LspPosition(0, 0), LspPositions.position(doc, 0));
        assertEquals(0, LspPositions.offset(doc, new LspPosition(0, 5)));
        assertEquals(0, LspPositions.offset(doc, new LspPosition(3, 3)));
    }

    /** Les plages : la paire (début, fin) en offsets et retour. */
    @Test
    void lesPlages() {
        Document doc = Document.of("ab\ncd\nef");
        RangeLsp range = LspPositions.range(doc, 3, 5);
        assertEquals(new LspPosition(1, 0), range.start());
        assertEquals(new LspPosition(1, 2), range.end());
        int[] bounds = LspPositions.bounds(doc, range);
        assertEquals(3, bounds[0]);
        assertEquals(5, bounds[1]);
        // une plage de serveur généreux : les bornes sont clampées aux lignes réelles
        int[] bornees = LspPositions.bounds(doc, new RangeLsp(
                new LspPosition(0, 99), new LspPosition(99, 99)));
        assertEquals(2, bornees[0]);
        assertEquals(8, bornees[1]);
    }
}
