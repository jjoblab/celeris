package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jo.celeris.core.text.Document;

import org.junit.jupiter.api.Test;

/**
 * Tests du dévoilement du curseur : après une édition IME, le
 * défilement se réajuste AU MINIMUM pour rendre le curseur visible —
 * rien ne bouge s'il l'est déjà, jamais le défilement ne devient
 * négatif.
 */
class CaretRevealTest {

    private static final float MARGE = 8f;

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

    /** Un curseur déjà visible ne fait bouger aucun défilement. */
    @Test
    void reveal_visibleCaretKeepsBothScrolls() {
        Document document = Document.of("abcdef");
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 0);

        CaretReveal.Revealed devoile = CaretReveal.reveal(2, document, projection,
                metrics(), 20f, 200, 100, 0f, 0f, MARGE);

        assertEquals(0f, devoile.scrollX(), 1e-6f);
        assertEquals(0f, devoile.scrollY(), 1e-6f);
    }

    /** Un curseur à droite de la fenêtre fait défiler vers la droite, au minimum. */
    @Test
    void reveal_caretRightOfViewportScrollsRight() {
        Document document = Document.of("abcdefghijklmnop");
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 0);
        // Curseur en 12 : x = 20 + 120 = 140 ; fenêtre 100, marge 8 :
        // il manque 140 - (100 - 8) = 48 px.
        CaretReveal.Revealed devoile = CaretReveal.reveal(12, document, projection,
                metrics(), 20f, 100, 100, 0f, 0f, MARGE);

        assertEquals(48f, devoile.scrollX(), 1e-6f);
    }

    /** Un curseur à gauche de la zone de texte fait défiler vers la gauche. */
    @Test
    void reveal_caretLeftOfTextAreaScrollsLeft() {
        Document document = Document.of("abcdefghijklmnop");
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 0);
        // Curseur en 2 : x écran = 20 + 20 - 60 = -20 ; bord gauche du
        // texte 20 + 8 = 28 : il manque 28 - (-20) = 48 px, le
        // défilement passe de 60 à 12.
        CaretReveal.Revealed devoile = CaretReveal.reveal(2, document, projection,
                metrics(), 20f, 100, 100, 60f, 0f, MARGE);

        assertEquals(12f, devoile.scrollX(), 1e-6f);
    }

    /** Un curseur au-dessus de la fenêtre fait défiler vers le haut. */
    @Test
    void reveal_caretAboveViewportScrollsUp() {
        Document document = Document.of("a\nb\nc\nd\ne\nf");
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 0);
        // Curseur ligne 1 (rangée 1, haut 20) ; défilement 60 : haut écran
        // -40, marge 8 : il manque 48 px vers le haut.
        CaretReveal.Revealed devoile = CaretReveal.reveal(2, document, projection,
                metrics(), 20f, 200, 100, 0f, 60f, MARGE);

        assertEquals(12f, devoile.scrollY(), 1e-6f);
    }

    /** Un curseur sous la fenêtre fait défiler vers le bas, ligne entière comprise. */
    @Test
    void reveal_caretBelowViewportScrollsDown() {
        Document document = Document.of("a\nb\nc\nd\ne\nf");
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 0);
        // Curseur ligne 3 (haut 60, bas 80) ; fenêtre 100, marge 8 :
        // il manque 80 - 92 = -12 -> 0 ? Non : bas 80 > 100 - 8 = 92 ?
        // bas 80 < 92 : visible. Prenons la ligne 4 : haut 80, bas 100 :
        // il manque 100 - 92 = 8.
        CaretReveal.Revealed devoile = CaretReveal.reveal(8, document, projection,
                metrics(), 20f, 200, 100, 0f, 0f, MARGE);

        assertEquals(8f, devoile.scrollY(), 1e-6f);
    }

    /** Le dévoilement ne rend jamais un défilement négatif. */
    @Test
    void reveal_neverScrollsNegative() {
        Document document = Document.of("abcdef");
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 0);

        CaretReveal.Revealed devoile = CaretReveal.reveal(0, document, projection,
                metrics(), 20f, 200, 100, 0f, 0f, MARGE);

        assertEquals(0f, devoile.scrollX(), 1e-6f);
        assertEquals(0f, devoile.scrollY(), 1e-6f);
    }
}
