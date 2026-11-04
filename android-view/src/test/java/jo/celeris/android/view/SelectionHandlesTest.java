package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jo.celeris.core.text.Document;

import org.junit.jupiter.api.Test;

/**
 * Tests de la géométrie des poignées de sélection : les positions
 * d'extrémités suivent les tranches de rangée (A-18) et la visée
 * désigne la poignée touchée — START, END ou aucune.
 */
class SelectionHandlesTest {

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

    /**
     * A-18 — l'extrémité d'une plage coupée se positionne sur SA rangée
     * : la colonne dans la tranche, l'ordonnée de la rangée de
     * continuation, jamais la ligne entière.
     */
    @Test
    void handlePosition_followsRowSpans() {
        Document document = Document.of("aaaaaaaaaa\nbb");
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 4);
        EditorMetrics metrics =
                EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());

        // L'offset 6 est sur la deuxième tranche de la ligne 0 (rangée 1,
        // colonnes 2 dans la tranche [4,8)).
        float x = SelectionHandles.xOfOffset(document, projection, metrics,
                20f, 0f, 6);
        float y = SelectionHandles.yOfOffset(document, projection, metrics, 0f, 6);
        assertEquals(20f + 2 * 10f, x, 1e-6f);
        assertEquals(20f, y, 1e-6f);

        // Avec défilement horizontal, la colonne suit.
        float xDefile = SelectionHandles.xOfOffset(document, projection, metrics,
                20f, 35f, 6);
        assertEquals(20f + 2 * 10f - 35f, xDefile, 1e-6f);
    }

    /** La visée désigne la poignée touchée : START, END, ou aucune. */
    @Test
    void hitTest_picksNearestHandle() {
        Document document = Document.of("abcdefghij");
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 0);
        EditorMetrics metrics =
                EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        // Plage [1,9) : START à la colonne 1 (x=30), END à la colonne 9
        // (x=110) — le milieu (x=70) est hors de portée des deux.
        float start = SelectionHandles.xOfOffset(document, projection, metrics,
                20f, 0f, 1);
        float end = SelectionHandles.xOfOffset(document, projection, metrics,
                20f, 0f, 9);

        assertEquals(SelectionHandles.HandleId.START,
                SelectionHandles.hitTest(start + 2f, 10f, 20f, 0f, 0f,
                        document, projection, metrics, 1, 9));
        assertEquals(SelectionHandles.HandleId.END,
                SelectionHandles.hitTest(end - 2f, 10f, 20f, 0f, 0f,
                        document, projection, metrics, 1, 9));
        assertEquals(SelectionHandles.HandleId.NONE,
                SelectionHandles.hitTest(70f, 10f, 20f, 0f, 0f,
                        document, projection, metrics, 1, 9),
                "entre les poignées : aucune");
    }

    /** Le glissement d'une poignée ne bouge QUE son extrémité. */
    @Test
    void drag_movesOnlyItsEndpoint() {
        jo.celeris.core.text.SelRange plage = new jo.celeris.core.text.SelRange(5, 2);
        assertEquals(new jo.celeris.core.text.SelRange(5, 4),
                SelectionHandles.dragged(plage, SelectionHandles.HandleId.START, 4),
                "la poignée START déplace le début, l'extrémité opposée tient");
        assertEquals(new jo.celeris.core.text.SelRange(2, 7),
                SelectionHandles.dragged(plage, SelectionHandles.HandleId.END, 7),
                "la poignée END déplace la fin, l'extrémité opposée tient");
    }
}
