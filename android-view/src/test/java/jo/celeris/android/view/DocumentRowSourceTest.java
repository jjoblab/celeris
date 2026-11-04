package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jo.celeris.core.text.Document;

import org.junit.jupiter.api.Test;

/**
 * Tests de la source de rangées sur document : la rangée porte la ligne
 * que la projection lui donne (PIEGE-04), au texte près du saut.
 */
class DocumentRowSourceTest {

    /** Règle fixe : caractère de 10 px. */
    private static final class RegleFixe implements FontRuler {
        @Override
        public float charWidth(FontSpec spec) {
            return 10f;
        }

        @Override
        public float lineHeight(FontSpec spec) {
            return 20f;
        }

        @Override
        public float ascent(FontSpec spec) {
            return 16f;
        }

        @Override
        public float descent(FontSpec spec) {
            return 4f;
        }
    }

    /** Sans pli, la rangée porte la ligne entière, sans le saut de ligne. */
    @Test
    void rangeePorteLaLigneEntiere() {
        Document document = Document.of("alpha\nbeta");
        EditorMetrics metrics = EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        DocumentRowSource source = new DocumentRowSource(document,
                new VisualProjection(2, new FoldModel()), metrics);

        RowLayout premiere = source.layoutOf(new RowKey(1L, 0, 1, new FontSpec("mono", 14f)));
        assertEquals(0, premiere.line());
        assertEquals("alpha", premiere.text());
        assertEquals(0, premiere.start());
        assertEquals(5, premiere.end());
        assertEquals(50f, premiere.width(), 1e-6f);

        RowLayout seconde = source.layoutOf(new RowKey(1L, 1, 1, new FontSpec("mono", 14f)));
        assertEquals(1, seconde.line());
        assertEquals("beta", seconde.text());
        assertEquals(40f, seconde.width(), 1e-6f);
    }

    /** La rangée d'un pli porte la ligne d'en-tête (conversion par la projection). */
    @Test
    void rangeeDePliPorteLEnTete() {
        Document document = Document.of("a\nbb\nccc\ndddd");
        EditorMetrics metrics = EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        FoldModel plis = new FoldModel();
        plis.fold(1, 2); // l'en-tête 1 reste, la ligne 2 est cachée
        VisualProjection projection = new VisualProjection(4, plis);
        DocumentRowSource source = new DocumentRowSource(document, projection, metrics);

        RowLayout pliee = source.layoutOf(new RowKey(1L, 1, 1, new FontSpec("mono", 14f)));
        assertEquals(1, pliee.line(), "la rangée du pli porte l'en-tête, pas la ligne cachée");
        assertEquals("bb", pliee.text());

        RowLayout apres = source.layoutOf(new RowKey(1L, 2, 1, new FontSpec("mono", 14f)));
        assertEquals(3, apres.line(), "la rangée après le pli porte la ligne suivante");
        assertEquals("dddd", apres.text());
    }

    /** Une ligne vide donne une rangée vide de largeur nulle. */
    @Test
    void ligneVideDonneRangeeVide() {
        Document document = Document.of("a\n\nc");
        EditorMetrics metrics = EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        DocumentRowSource source = new DocumentRowSource(document,
                new VisualProjection(3, new FoldModel()), metrics);

        RowLayout vide = source.layoutOf(new RowKey(1L, 1, 1, new FontSpec("mono", 14f)));
        assertEquals("", vide.text());
        assertEquals(0f, vide.width(), 1e-6f);
        assertEquals(1, vide.line());
    }
}
