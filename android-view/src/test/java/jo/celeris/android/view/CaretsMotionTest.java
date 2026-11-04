package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;

import org.junit.jupiter.api.Test;

/**
 * Tests du moteur de déplacement des curseurs (D-17, clavier matériel)
 * : les mouvements passent par les clusters de graphèmes et les
 * frontières de mots, montent et descendent par RANGÉES (A-18 — sous
 * coupures, jamais par lignes), et les variantes sélection étendent la
 * plage l'ancre figée.
 */
class CaretsMotionTest {

    /** D-17 — le mouvement horizontal saute des clusters entiers. */
    @Test
    void motion_movesByGraphemeClusters() {
        Document document = Document.of("a👨‍👩b");
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 0);

        // Le curseur à 1 avance au-delà de la famille emoji entière.
        MultiSelection apres = CaretsMotion.move(document, projection,
                MultiSelection.caretsAt(1), EditorAction.MOVE_RIGHT, 10);
        assertEquals(MultiSelection.caretsAt(6), apres);

        // Et recule d'un cluster entier depuis 6.
        MultiSelection avant = CaretsMotion.move(document, projection,
                MultiSelection.caretsAt(6), EditorAction.MOVE_LEFT, 10);
        assertEquals(MultiSelection.caretsAt(1), avant);

        // Aux bornes du document, le mouvement se borne.
        assertEquals(MultiSelection.caretsAt(0),
                CaretsMotion.move(document, projection,
                        MultiSelection.caretsAt(0), EditorAction.MOVE_LEFT, 10));
        assertEquals(MultiSelection.caretsAt(7),
                CaretsMotion.move(document, projection,
                        MultiSelection.caretsAt(7), EditorAction.MOVE_RIGHT, 10));
    }

    /** D-17 — le mouvement par mots saute des mots entiers, dans les deux sens. */
    @Test
    void motion_movesByWholeWords() {
        Document document = Document.of("un deux trois");
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 0);

        assertEquals(MultiSelection.caretsAt(3),
                CaretsMotion.move(document, projection,
                        MultiSelection.caretsAt(0), EditorAction.MOVE_WORD_RIGHT, 10));
        assertEquals(MultiSelection.caretsAt(0),
                CaretsMotion.move(document, projection,
                        MultiSelection.caretsAt(3), EditorAction.MOVE_WORD_LEFT, 10));
        assertEquals(MultiSelection.caretsAt(8),
                CaretsMotion.move(document, projection,
                        MultiSelection.caretsAt(3), EditorAction.MOVE_WORD_RIGHT, 10));
    }

    /** D-17/A-18 — monter et descendre se compte en RANGÉES, pas en lignes. */
    @Test
    void motion_movesByVisualRows() {
        // Ligne unique de 10 caractères, budget 4 : rangées [0,4), [4,8),
        // [8,10) — le curseur au milieu de la rangée 2 monte à la rangée 1.
        Document document = Document.of("aaaaaaaaaa");
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 4);

        MultiSelection montee = CaretsMotion.move(document, projection,
                MultiSelection.caretsAt(6), EditorAction.MOVE_UP, 10);
        assertEquals(MultiSelection.caretsAt(2), montee,
                "monter d'une rangée garde la colonne visuelle");

        MultiSelection descente = CaretsMotion.move(document, projection,
                MultiSelection.caretsAt(2), EditorAction.MOVE_DOWN, 10);
        assertEquals(MultiSelection.caretsAt(6), descente);

        // En haut de la première rangée, monter se borne.
        assertEquals(MultiSelection.caretsAt(0),
                CaretsMotion.move(document, projection,
                        MultiSelection.caretsAt(0), EditorAction.MOVE_UP, 10));
        // En bas de la dernière, descendre se borne (fin de tranche).
        MultiSelection borne = CaretsMotion.move(document, projection,
                MultiSelection.caretsAt(8), EditorAction.MOVE_DOWN, 10);
        assertEquals(10, borne.carets().get(0).intValue());
    }

    /** D-17 — début et fin de rangée suivent les tranches, pas la ligne entière. */
    @Test
    void motion_lineStartEndFollowSpans() {
        Document document = Document.of("aaaaaaaaaa");
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 4);

        assertEquals(MultiSelection.caretsAt(4),
                CaretsMotion.move(document, projection,
                        MultiSelection.caretsAt(6), EditorAction.LINE_START, 10));
        assertEquals(MultiSelection.caretsAt(8),
                CaretsMotion.move(document, projection,
                        MultiSelection.caretsAt(6), EditorAction.LINE_END, 10));
    }

    /** D-17 — la sélection étend la plage, l'ancre figée. */
    @Test
    void motion_selectExtendsWithFrozenAnchor() {
        Document document = Document.of("abcdef");
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 0);

        MultiSelection apres = CaretsMotion.move(document, projection,
                MultiSelection.caretsAt(2), EditorAction.SELECT_RIGHT, 10);
        assertEquals(MultiSelection.of(new SelRange(2, 3)), apres);

        MultiSelection encore = CaretsMotion.move(document, projection, apres,
                EditorAction.SELECT_RIGHT, 10);
        assertEquals(MultiSelection.of(new SelRange(2, 4)), encore,
                "l'ancre tient, le curseur avance");
    }

    /** D-17 — tout sélectionner couvre le document entier. */
    @Test
    void motion_selectAllCoversDocument() {
        Document document = Document.of("abcdef");
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 0);

        assertEquals(MultiSelection.of(new SelRange(0, 6)),
                CaretsMotion.move(document, projection,
                        MultiSelection.caretsAt(2), EditorAction.SELECT_ALL, 10));
    }

    /** D-17 — page haut et bas sautent de la hauteur visible en rangées. */
    @Test
    void motion_pagesByVisibleRows() {
        Document document = Document.of("a\nb\nc\nd\ne\nf\ng\nh");
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 0);

        MultiSelection page = CaretsMotion.move(document, projection,
                MultiSelection.caretsAt(1), EditorAction.PAGE_DOWN, 4);
        assertEquals(MultiSelection.caretsAt(9), page,
                "quatre rangées plus bas : ligne 4 (offset 8), colonne conservée");

        MultiSelection retour = CaretsMotion.move(document, projection, page,
                EditorAction.PAGE_UP, 4);
        assertEquals(MultiSelection.caretsAt(1), retour);
    }
}
