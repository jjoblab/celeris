package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jo.celeris.core.text.Document;

import org.junit.jupiter.api.Test;

/**
 * Tests des mouvements à granularité d'accessibilité (G-05) : la
 * granularité CARACTÈRE suit les clusters de graphèmes (jamais une
 * demi-paire), la granularité MOT les mots entiers, la granularité
 * LIGNE les RANGÉES visuelles sous coupures (A-18 — la lecture suit
 * l'écran, pas la ligne logique) et la PAGE la hauteur visible.
 */
class MovementGranularityTest {

    /** La granularité CARACTÈRE saute des clusters entiers, jamais une demi-paire. */
    @Test
    void character_movesByWholeClusters() {
        Document document = Document.of("a\ud83d\ude00b");
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 0);

        assertEquals(3, MovementGranularity.next(document, projection, 1,
                MovementGranularity.CHARACTER, 10));
        assertEquals(1, MovementGranularity.previous(document, projection, 3,
                MovementGranularity.CHARACTER, 10));
    }

    /** Les bornes du document bornent les mouvements. */
    @Test
    void character_staysWithinTheDocument() {
        Document document = Document.of("abc");
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 0);

        assertEquals(3, MovementGranularity.next(document, projection, 3,
                MovementGranularity.CHARACTER, 10));
        assertEquals(0, MovementGranularity.previous(document, projection, 0,
                MovementGranularity.CHARACTER, 10));
    }

    /** La granularité MOT saute des mots entiers. */
    @Test
    void word_movesByWholeWords() {
        Document document = Document.of("un deux");
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 0);

        assertEquals(2, MovementGranularity.next(document, projection, 0,
                MovementGranularity.WORD, 10));
        assertEquals(0, MovementGranularity.previous(document, projection, 2,
                MovementGranularity.WORD, 10));
    }

    /**
     * La granularité LIGNE suit les RANGÉES visuelles (A-18) : sous
     * coupures, descendre d'une ligne garde la colonne DANS la rangée
     * suivante — la lecture suit l'écran.
     */
    @Test
    void line_followsVisualRowsUnderWrap() {
        Document document = Document.of("abcdefgh");
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 4);

        assertEquals(6, MovementGranularity.next(document, projection, 2,
                MovementGranularity.LINE, 10));
        assertEquals(2, MovementGranularity.previous(document, projection, 6,
                MovementGranularity.LINE, 10));
    }

    /** La dernière rangée tient le curseur : descendre n'a plus d'effet. */
    @Test
    void line_clampsAtTheLastRow() {
        Document document = Document.of("abcdefgh");
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 4);

        assertEquals(6, MovementGranularity.next(document, projection, 6,
                MovementGranularity.LINE, 10));
        assertEquals(2, MovementGranularity.previous(document, projection, 2,
                MovementGranularity.LINE, 10));
    }

    /** La granularité PAGE saute de la hauteur visible en rangées. */
    @Test
    void page_movesByVisibleRows() {
        Document document = Document.of("a\nb\nc\nd\ne\nf");
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 0);

        // Départs de ligne : 0, 2, 4, 6, 8, 10 — une page de 3 rangées.
        assertEquals(6, MovementGranularity.next(document, projection, 0,
                MovementGranularity.PAGE, 3));
        assertEquals(0, MovementGranularity.previous(document, projection, 6,
                MovementGranularity.PAGE, 3));
    }
}
