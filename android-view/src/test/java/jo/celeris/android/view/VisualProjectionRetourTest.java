package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jo.celeris.core.text.Document;

import org.junit.jupiter.api.Test;

/**
 * Tests de la projection lignes ↔ rangées sous retour à la ligne souple
 * par offsets (A-18, variante code).
 *
 * <p>La coupure se compte en colonnes de caractère dans la ligne du
 * document : une ligne longue occupe plusieurs rangées, chaque rangée
 * porte sa tranche d'offsets, et une ligne vide garde sa rangée de
 * respiration. PIEGE-08 : le budget de colonnes vaut au moins un
 * caractère — un contenu plus large que la fenêtre est coupé par
 * caractère, jamais laissé déborder. PIEGE-04 : les conversions rangée ↔
 * ligne passent par la projection même sous plis ET sous retour à la
 * ligne — l'identité naïve « rangée = ligne » est fausse dès l'un ou
 * l'autre.</p>
 */
class VisualProjectionRetourTest {

    /** Document de travail : lignes de 10, 2, 0 et 2 caractères. */
    private static Document documentTravail() {
        return Document.of("aaaaaaaaaa\nbb\n\ncc");
    }

    /**
     * A-18 — une ligne plus large que le budget de colonnes occupe
     * plusieurs rangées, chacune porte sa tranche d'offsets.
     */
    @Test
    void softWrap_rowsCarryOffsetSpans() {
        VisualProjection projection =
                new VisualProjection(documentTravail(), new FoldModel(), 4);

        // Ligne 0 (10 caractères) : 3 rangées ; lignes 1 à 3 : une chacune.
        assertEquals(6, projection.rowCount());

        // Chaque rangée connaît sa ligne et sa tranche dans la ligne.
        for (int rangee = 0; rangee < 3; rangee++) {
            assertEquals(0, projection.firstLineOfRow(rangee));
        }
        assertEquals(1, projection.firstLineOfRow(3));
        assertEquals(2, projection.firstLineOfRow(4));
        assertEquals(3, projection.firstLineOfRow(5));

        // Les tranches de la ligne 0 : [0,4), [4,8), [8,10).
        assertEquals(0, projection.spanStartAtRow(0));
        assertEquals(4, projection.spanEndAtRow(0));
        assertEquals(4, projection.spanStartAtRow(1));
        assertEquals(8, projection.spanEndAtRow(1));
        assertEquals(8, projection.spanStartAtRow(2));
        assertEquals(10, projection.spanEndAtRow(2));

        // La ligne vide garde sa rangée, de tranche vide.
        assertEquals(0, projection.spanStartAtRow(4));
        assertEquals(0, projection.spanEndAtRow(4));

        // La conversion inverse : la première rangée de chaque ligne.
        assertEquals(0, projection.rowOfLine(0));
        assertEquals(3, projection.rowOfLine(1));
        assertEquals(4, projection.rowOfLine(2));
        assertEquals(5, projection.rowOfLine(3));
    }

    /**
     * PIEGE-08 — le budget de colonnes vaut au moins un caractère : un
     * mot plus large que la fenêtre est coupé par caractère ; budget
     * nul ou négatif coupe le retour à la ligne (chaque ligne une
     * rangée, tranche entière).
     */
    @Test
    void softWrap_budgetOfOneColumnBreaksByCharacter() {
        Document document = Document.of("abcdef");
        VisualProjection colonneParColonne = new VisualProjection(document,
                new FoldModel(), 1);
        assertEquals(6, colonneParColonne.rowCount());
        for (int rangee = 0; rangee < 6; rangee++) {
            assertEquals(rangee, colonneParColonne.spanStartAtRow(rangee));
            assertEquals(rangee + 1, colonneParColonne.spanEndAtRow(rangee));
        }

        VisualProjection sansRetour = new VisualProjection(document,
                new FoldModel(), 0);
        assertEquals(1, sansRetour.rowCount());
        assertEquals(0, sansRetour.spanStartAtRow(0));
        assertEquals(6, sansRetour.spanEndAtRow(0));
    }

    /**
     * PIEGE-04 — les conversions passent par la projection sous plis ET
     * sous retour à la ligne : l'en-tête plié garde ses rangées coupées,
     * les lignes cachées disparaissent.
     */
    @Test
    void softWrap_rowsConvertThroughFoldModel() {
        // Lignes de 6, 2 et 3 caractères ; budget 4 : la ligne 0 occupe
        // deux rangées.
        Document document = Document.of("aaaaaa\nbb\nccc");
        FoldModel plis = new FoldModel();
        plis.fold(0, 1); // l'en-tête 0 reste, la ligne 1 est cachée
        VisualProjection projection = new VisualProjection(document, plis, 4);

        // Rangées visibles : 2 pour la ligne 0, 1 pour la ligne 2.
        assertEquals(3, projection.rowCount());

        // La ligne cachée se projette sur les rangées de son en-tête.
        assertEquals(0, projection.rowOfLine(1));
        assertEquals(2, projection.rowOfLine(2));

        // La rangée d'en-tête couvre ses lignes cachées, la rangée du
        // milieu est la seconde tranche de l'en-tête.
        assertEquals(0, projection.firstLineOfRow(0));
        assertEquals(1, projection.lastLineOfRow(0));
        assertEquals(0, projection.firstLineOfRow(1));
        assertEquals(4, projection.spanStartAtRow(1));
        assertEquals(6, projection.spanEndAtRow(1));
        assertEquals(2, projection.firstLineOfRow(2));
    }

    /** Un pli dont l'en-tête suit des lignes coupées décale d'autant. */
    @Test
    void softWrap_foldAfterWrappedLines() {
        Document document = Document.of("aaaaaa\nbb\nccc\ndddd");
        FoldModel plis = new FoldModel();
        plis.fold(1, 2); // l'en-tête 1 reste, la ligne 2 est cachée
        VisualProjection projection = new VisualProjection(document, plis, 4);

        // Ligne 0 : 2 rangées, ligne 1 : 1, ligne 2 cachée, ligne 3 : 1.
        assertEquals(4, projection.rowCount());
        assertEquals(0, projection.firstLineOfRow(0));
        assertEquals(0, projection.firstLineOfRow(1));
        assertEquals(1, projection.firstLineOfRow(2));
        assertEquals(2, projection.lastLineOfRow(2), "l'en-tête couvre sa ligne cachée");
        assertEquals(3, projection.firstLineOfRow(3));
        assertEquals(3, projection.rowOfLine(3));
    }

    /** La longueur de la plus large ligne, pour le bornage horizontal. */
    @Test
    void softWrap_exposesLongestLineLength() {
        VisualProjection projection =
                new VisualProjection(documentTravail(), new FoldModel(), 4);
        assertEquals(10, projection.longestLineLength());

        // Budget nul : la projection connaît quand même les longueurs.
        VisualProjection sansRetour =
                new VisualProjection(documentTravail(), new FoldModel(), 0);
        assertEquals(10, sansRetour.longestLineLength());

        // L'ancienne forme (nombre de lignes seul) ne les connaît pas.
        VisualProjection anonyme = new VisualProjection(4, new FoldModel());
        assertEquals(0, anonyme.longestLineLength());
    }
}
