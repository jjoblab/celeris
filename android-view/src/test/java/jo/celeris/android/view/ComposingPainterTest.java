package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.core.text.Document;

import org.junit.jupiter.api.Test;

/**
 * Tests de la couche de composition IME : le soulignement suit les
 * tranches de rangée (PIEGE-04 — le soulignement ne déborde jamais de
 * sa rangée), rien ne se trace sans composition active ni sur une
 * plage vide.
 */
class ComposingPainterTest {

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
            // le soulignement ne trace aucun texte
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

    /** Sans composition active, la couche ne trace rien. */
    @Test
    void composing_inactivePaintsNothing() {
        Document document = Document.of("abcdefgh");
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 0);
        EditorMetrics metrics =
                EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        FrameState image = new FrameState();
        image.beginFrame(document,
                jo.celeris.core.text.MultiSelection.of(
                        jo.celeris.core.text.SelRange.caretAt(0)),
                projection, 1L, metrics, THEME, 0, 2, 20f, 0f, 0f);

        CompositionState etat = new CompositionState();
        SurfaceRectangles surface = new SurfaceRectangles();
        new ComposingPainter(() -> new PinceauMutable(), etat).paint(surface, image);

        assertTrue(surface.rectangles().isEmpty());
    }

    /**
     * PIEGE-04 — le soulignement suit les tranches : une plage composée
     * à cheval sur des rangées coupées trace une bande par rangée,
     * bornée à la tranche, posée dans la bande de descente.
     */
    @Test
    void composing_underlineFollowsRowSpans() {
        Document document = Document.of("abcdefgh");
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 4);
        EditorMetrics metrics =
                EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        FrameState image = new FrameState();
        image.beginFrame(document,
                jo.celeris.core.text.MultiSelection.of(
                        jo.celeris.core.text.SelRange.caretAt(0)),
                projection, 1L, metrics, THEME, 0, 2, 20f, 0f, 0f);

        CompositionState etat = new CompositionState();
        etat.adopt(etat.planRegion(document, 2, 7));
        SurfaceRectangles surface = new SurfaceRectangles();
        new ComposingPainter(() -> new PinceauMutable(), etat).paint(surface, image);

        assertEquals(2, surface.rectangles().size());
        // Rangée 0, tranche [0,4) : soulignement des colonnes 2 à 4,
        // dans la bande de descente [16, 20).
        RectTrace premiere = surface.rectangles().get(0);
        assertEquals(20f + 2 * 10f, premiere.gauche(), 1e-6f);
        assertEquals(20f + 4 * 10f, premiere.droite(), 1e-6f);
        assertEquals(16f, premiere.haut(), 1e-6f);
        assertEquals(20f, premiere.bas(), 1e-6f);
        // Rangée 1, tranche [4,8) : colonnes 0 à 3, descente [36, 40).
        RectTrace seconde = surface.rectangles().get(1);
        assertEquals(20f, seconde.gauche(), 1e-6f);
        assertEquals(20f + 3 * 10f, seconde.droite(), 1e-6f);
        assertEquals(36f, seconde.haut(), 1e-6f);
        assertEquals(40f, seconde.bas(), 1e-6f);
    }

    /** Une plage composée vide ne trace rien. */
    @Test
    void composing_emptySpanPaintsNothing() {
        Document document = Document.of("abcdefgh");
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 0);
        EditorMetrics metrics =
                EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        FrameState image = new FrameState();
        image.beginFrame(document,
                jo.celeris.core.text.MultiSelection.of(
                        jo.celeris.core.text.SelRange.caretAt(2)),
                projection, 1L, metrics, THEME, 0, 2, 20f, 0f, 0f);

        CompositionState etat = new CompositionState();
        etat.adopt(etat.planRegion(document, 2, 2));
        SurfaceRectangles surface = new SurfaceRectangles();
        new ComposingPainter(() -> new PinceauMutable(), etat).paint(surface, image);

        assertTrue(surface.rectangles().isEmpty());
    }
}
