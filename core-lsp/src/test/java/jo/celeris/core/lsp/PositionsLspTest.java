package jo.celeris.core.lsp;

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
        assertEquals(new PositionLsp(0, 0), PositionsLsp.position(doc, 0));
        assertEquals(new PositionLsp(0, 1), PositionsLsp.position(doc, 1));
        // l'offset du saut de ligne appartient à la ligne qu'il termine : colonne = longueur
        assertEquals(new PositionLsp(0, 2), PositionsLsp.position(doc, 2));
        assertEquals(new PositionLsp(1, 0), PositionsLsp.position(doc, 3));
        assertEquals(new PositionLsp(1, 2), PositionsLsp.position(doc, 5));
        assertEquals(new PositionLsp(2, 0), PositionsLsp.position(doc, 6));
        // retour
        assertEquals(0, PositionsLsp.offset(doc, new PositionLsp(0, 0)));
        assertEquals(2, PositionsLsp.offset(doc, new PositionLsp(0, 2)));
        assertEquals(3, PositionsLsp.offset(doc, new PositionLsp(1, 0)));
        assertEquals(5, PositionsLsp.offset(doc, new PositionLsp(1, 2)));
        assertEquals(6, PositionsLsp.offset(doc, new PositionLsp(2, 0)));
    }

    /** Les paires de substitution : la colonne compte deux unités par point de code astral. */
    @Test
    void lesPairesDeSubstitution() {
        // « 😀 » est une paire de deux unités UTF-16 ; « x » en suit une
        Document doc = Document.of("😀x\ny");
        assertEquals(new PositionLsp(0, 2), PositionsLsp.position(doc, 2));
        assertEquals(new PositionLsp(0, 3), PositionsLsp.position(doc, 3));
        assertEquals(2, PositionsLsp.offset(doc, new PositionLsp(0, 2)));
        assertEquals(4, PositionsLsp.offset(doc, new PositionLsp(1, 0)));
    }

    /** Le clampage : ligne au-delà, colonne au-delà, négatifs — jamais d'exception. */
    @Test
    void leClampage() {
        Document doc = Document.of("ab\ncd\n");
        assertEquals(6, PositionsLsp.offset(doc, new PositionLsp(99, 0)));
        assertEquals(2, PositionsLsp.offset(doc, new PositionLsp(0, 99)));
        assertEquals(0, PositionsLsp.offset(doc, new PositionLsp(-1, -1)));
        // une colonne au-delà du contenu de la ligne retombe sur sa fin
        assertEquals(5, PositionsLsp.offset(doc, new PositionLsp(1, 42)));
    }

    /** Le clampage sur ligne vide : la colonne ne peut pas dépasser zéro. */
    @Test
    void leClampageSurLigneVide() {
        Document doc = Document.of("a\n\nc");
        // ligne 1 vide : toute colonne retombe à son début
        assertEquals(2, PositionsLsp.offset(doc, new PositionLsp(1, 7)));
        assertEquals(new PositionLsp(1, 0), PositionsLsp.position(doc, 2));
    }

    /** Le document vide : une ligne, une colonne, l'offset zéro. */
    @Test
    void leDocumentVide() {
        Document doc = Document.of("");
        assertEquals(new PositionLsp(0, 0), PositionsLsp.position(doc, 0));
        assertEquals(0, PositionsLsp.offset(doc, new PositionLsp(0, 5)));
        assertEquals(0, PositionsLsp.offset(doc, new PositionLsp(3, 3)));
    }

    /** Les plages : la paire (début, fin) en offsets et retour. */
    @Test
    void lesPlages() {
        Document doc = Document.of("ab\ncd\nef");
        PlageLsp plage = PositionsLsp.plage(doc, 3, 5);
        assertEquals(new PositionLsp(1, 0), plage.debut());
        assertEquals(new PositionLsp(1, 2), plage.fin());
        int[] bornes = PositionsLsp.bornes(doc, plage);
        assertEquals(3, bornes[0]);
        assertEquals(5, bornes[1]);
        // une plage de serveur généreux : les bornes sont clampées aux lignes réelles
        int[] bornees = PositionsLsp.bornes(doc, new PlageLsp(
                new PositionLsp(0, 99), new PositionLsp(99, 99)));
        assertEquals(2, bornees[0]);
        assertEquals(8, bornees[1]);
    }
}
