package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;

import org.junit.jupiter.api.Test;

/**
 * Tests de la couche de sélection sous retour à la ligne par offsets :
 * le fond d'une plage suit les tranches de rangée (PIEGE-04), chaque
 * rangée ne peint que l'intersection de la plage avec SA tranche.
 */
class SelectionPainterTest {

    private static final EditorTheme THEME =
            new EditorTheme(1, 0xFFFFFFFF, 0xFF202124, 0xFF1A73E8,
                    0xFF000000, 0xFF5F6368);

    /** Pinceau mutable minimal : la couleur courante, rien d'autre. */
    private static final class PinceauMutable implements TextBrush {
        private int couleur;

        @Override
        public void setColor(int argb) {
            couleur = argb;
        }

        @Override
        public int color() {
            return couleur;
        }
    }

    /** Un rectangle tracé : ses bornes. */
    private record RectTrace(float gauche, float haut, float droite, float bas) {
    }

    /** Surface enregistreuse de rectangles. */
    private static final class SurfaceRectangles implements DrawSurface {
        private final List<RectTrace> rectangles = new ArrayList<>();

        @Override
        public void drawText(String text, float x, float y, TextBrush brush) {
            // la sélection ne trace aucun texte
        }

        @Override
        public void drawRect(float left, float top, float right, float bottom,
                TextBrush brush) {
            rectangles.add(new RectTrace(left, top, right, bottom));
        }

        List<RectTrace> rectangles() {
            return rectangles;
        }
    }

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
     * PIEGE-04 — une plage à cheval sur plusieurs rangées coupées peint
     * une bande par rangée, bornée à la tranche de chaque rangée : jamais
     * le fond ne déborde de sa rangée.
     */
    @Test
    void selection_clipsToRowSpan() {
        Document document = Document.of("abcdefgh");
        // Budget 4 : la ligne unique occupe deux rangées [0,4) et [4,8).
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 4);
        EditorMetrics metrics =
                EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());

        FrameState image = new FrameState();
        image.beginFrame(document,
                MultiSelection.of(new SelRange(2, 7)), projection, 1L, metrics,
                THEME, 0, 2, 20f, 0f, 0f);

        SurfaceRectangles surface = new SurfaceRectangles();
        new SelectionPainter(new PinceauMutable()).paint(surface, image);

        assertEquals(2, surface.rectangles().size());
        // Rangée 0, tranche [0,4) : la plage y entre aux colonnes 2 et 4.
        RectTrace premiere = surface.rectangles().get(0);
        assertEquals(20f + 2 * 10f, premiere.gauche(), 1e-6f);
        assertEquals(20f + 4 * 10f, premiere.droite(), 1e-6f);
        assertEquals(0f, premiere.haut(), 1e-6f);
        assertEquals(20f, premiere.bas(), 1e-6f);
        // Rangée 1, tranche [4,8) : la plage y couvre les colonnes 0 à 3.
        RectTrace seconde = surface.rectangles().get(1);
        assertEquals(20f, seconde.gauche(), 1e-6f);
        assertEquals(20f + 3 * 10f, seconde.droite(), 1e-6f);
        assertEquals(20f, seconde.haut(), 1e-6f);
        assertEquals(40f, seconde.bas(), 1e-6f);
    }

    /** Sans retour à la ligne, chaque ligne garde sa bande entière. */
    @Test
    void selection_withoutWrapSpansWholeLines() {
        Document document = Document.of("abcd\nefgh");
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 0);
        EditorMetrics metrics =
                EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());

        FrameState image = new FrameState();
        image.beginFrame(document,
                MultiSelection.of(new SelRange(1, 7)), projection, 1L, metrics,
                THEME, 0, 2, 20f, 0f, 0f);

        SurfaceRectangles surface = new SurfaceRectangles();
        new SelectionPainter(new PinceauMutable()).paint(surface, image);

        assertEquals(2, surface.rectangles().size());
        // Rangée 0 : colonnes 1 à 4 de la ligne 0.
        RectTrace premiere = surface.rectangles().get(0);
        assertEquals(20f + 10f, premiere.gauche(), 1e-6f);
        assertEquals(20f + 40f, premiere.droite(), 1e-6f);
        // Rangée 1 : colonnes 0 à 3 de la ligne 1.
        RectTrace seconde = surface.rectangles().get(1);
        assertEquals(20f, seconde.gauche(), 1e-6f);
        assertEquals(20f + 30f, seconde.droite(), 1e-6f);
    }
}
