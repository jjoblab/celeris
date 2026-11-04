package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import jo.celeris.core.text.Document;

import org.junit.jupiter.api.Test;

/**
 * Tests de l'arbre d'accessibilité virtuel (G-05) : l'éditeur s'expose
 * comme un nœud éditable dont les enfants virtuels sont les RANGÉES
 * VISIBLES — texte de la tranche et bornes à l'écran, jamais le
 * document entier (G-01 : les très gros fichiers se servent par
 * fenêtres).
 */
class VirtualRowsTest {

    /** Règle fixe : largeur 10 px, hauteur 20 px, ascendance 16, descendance 4. */
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

    private EditorMetrics metrics() {
        return EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
    }

    /** Chaque rangée visible porte le texte de SA tranche (A-18). */
    @Test
    void rows_carryTheirSpanText() {
        Document document = Document.of("abcdefgh");
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 4);

        List<VirtualRows.Row> rangees = VirtualRows.visible(document, projection,
                metrics(), 0, 2, 20f, 0f, 0f);

        assertEquals(2, rangees.size());
        assertEquals("abcd", rangees.get(0).text());
        assertEquals("efgh", rangees.get(1).text());
    }

    /** Les bornes de chaque rangée : du bord de texte à la largeur tracée. */
    @Test
    void rows_carryTheirScreenBounds() {
        Document document = Document.of("abcdefgh");
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 4);

        List<VirtualRows.Row> rangees = VirtualRows.visible(document, projection,
                metrics(), 0, 2, 20f, 0f, 0f);

        VirtualRows.Row premiere = rangees.get(0);
        assertEquals(0, premiere.row());
        assertEquals(20f, premiere.left(), 1e-6f);
        assertEquals(0f, premiere.top(), 1e-6f);
        assertEquals(20f + 4 * 10f, premiere.right(), 1e-6f);
        assertEquals(20f, premiere.bottom(), 1e-6f);
        VirtualRows.Row seconde = rangees.get(1);
        assertEquals(1, seconde.row());
        assertEquals(20f, seconde.top(), 1e-6f);
        assertEquals(40f, seconde.bottom(), 1e-6f);
    }

    /** La tranche ne déborde jamais de sa ligne (PIEGE-04). */
    @Test
    void rows_clampTheSpanToTheLine() {
        Document document = Document.of("ab\ncd");
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 10);

        List<VirtualRows.Row> rangees = VirtualRows.visible(document, projection,
                metrics(), 0, 2, 20f, 0f, 0f);

        assertEquals("ab", rangees.get(0).text());
        assertEquals("cd", rangees.get(1).text());
    }

    /** La fenêtre ne sert que les rangées demandées, défilement déduit. */
    @Test
    void rows_serveTheWindowedRowsScrolled() {
        Document document = Document.of("ab\ncd\nef");
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 0);

        List<VirtualRows.Row> rangees = VirtualRows.visible(document, projection,
                metrics(), 1, 1, 20f, 0f, 20f);

        assertEquals(1, rangees.size());
        assertEquals("cd", rangees.get(0).text());
        assertEquals(0f, rangees.get(0).top(), 1e-6f);
        assertEquals(20f, rangees.get(0).bottom(), 1e-6f);
    }
}
