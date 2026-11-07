package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;

import org.junit.jupiter.api.Test;

/**
 * Tests du chrome de l'éditeur : le curseur d'écriture — une barre par
 * plage vide, clignotante — et les poignées de la plage unique non
 * vide, tracés après le pipeline au pinceau possédé par la vue.
 */
class ChromePainterTest {

    /** Règle de police régulière : 8 pixels par caractère, 16 par rangée. */
    private static final class RegleDroite implements FontRuler {
        @Override
        public float charWidth(FontSpec spec) {
            return spec.size() / 2f;
        }

        @Override
        public float lineHeight(FontSpec spec) {
            return spec.size();
        }

        @Override
        public float ascent(FontSpec spec) {
            return spec.size() * 3f / 4f;
        }

        @Override
        public float descent(FontSpec spec) {
            return spec.size() / 4f;
        }
    }

    /** Pinceau mutable minimal. */
    private static final class PinceauSimple implements TextBrush {
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

    /** Un rectangle tracé : ses bornes et sa couleur. */
    private record RectTrace(float left, float top, float right, float bottom,
            int couleur) {
    }

    /** Surface enregistreuse de rectangles. */
    private static final class SurfaceRectangles implements DrawSurface {
        private final List<RectTrace> rectangles = new ArrayList<>();

        @Override
        public void drawText(String text, float x, float y, TextBrush brush) {
            // le chrome du curseur ne trace aucun texte ici
        }

        @Override
        public void drawText(CharSequence text, int start, int end, float x,
                float y, TextBrush brush) {
            // le chrome du curseur ne trace aucun texte ici
        }

        @Override
        public void drawRect(float left, float top, float right, float bottom,
                TextBrush brush) {
            rectangles.add(new RectTrace(left, top, right, bottom,
                    brush.color()));
        }

        List<RectTrace> rectangles() {
            return rectangles;
        }
    }

    private static final EditorTheme THEME = EditorThemes.CLAIR;
    private static final float GOUTTIERE = 40f;

    private static EditorMetrics metrics() {
        return EditorMetrics.of(new FontSpec("mono", 16f), new RegleDroite());
    }

    /**
     * Le curseur d'écriture se trace : une barre verticale pleine
     * hauteur de rangée, au bord gauche du caractère visé, à l'encre du
     * curseur du thème.
     */
    @Test
    void curseur_barreAUffsetVise() {
        Document document = Document.of("abcdef\nghijkl");
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 0);
        ChromePainter chrome = new ChromePainter();
        SurfaceRectangles surface = new SurfaceRectangles();
        EditorMetrics metrics = metrics();

        chrome.tracer(surface, new PinceauSimple(), THEME, metrics,
                new CursorPill(), document, projection,
                List.of(SelRange.caretAt(3)), GOUTTIERE, 0f, 0f, true);

        assertEquals(1, surface.rectangles().size());
        RectTrace barre = surface.rectangles().get(0);
        // Le caractère visé : colonne 3 de la rangée 0, gouttière comprise.
        assertEquals(GOUTTIERE + 3 * metrics.charWidth(), barre.left()
                + Math.max(2f, metrics.charWidth() / 12f), 0.01f);
        assertEquals(0f, barre.top(), 0.01f);
        assertEquals(metrics.lineHeight(), barre.bottom(), 0.01f);
        assertEquals(THEME.cursorColor(), barre.couleur());
    }

    /**
     * Chaque curseur d'une sélection multiple trace sa barre (A-01) —
     * deux plages vides, deux barres.
     */
    @Test
    void curseur_chaqueCurseurMultipleTrace() {
        Document document = Document.of("abcdef\nghijkl");
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 0);
        ChromePainter chrome = new ChromePainter();
        SurfaceRectangles surface = new SurfaceRectangles();

        chrome.tracer(surface, new PinceauSimple(), THEME, metrics(),
                new CursorPill(), document, projection,
                List.of(SelRange.caretAt(2), SelRange.caretAt(9)),
                GOUTTIERE, 0f, 0f, true);

        assertEquals(2, surface.rectangles().size());
    }

    /** Le clignotement éteint : le curseur ne se trace pas. */
    @Test
    void curseur_etatEteintNeTracePas() {
        Document document = Document.of("abcdef");
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 0);
        ChromePainter chrome = new ChromePainter();
        SurfaceRectangles surface = new SurfaceRectangles();

        chrome.tracer(surface, new PinceauSimple(), THEME, metrics(),
                new CursorPill(), document, projection,
                List.of(SelRange.caretAt(3)), GOUTTIERE, 0f, 0f, false);

        assertTrue(surface.rectangles().isEmpty());
    }

    /** Une plage non vide ne porte pas de curseur, mais ses deux poignées. */
    @Test
    void poignees_plageNonVideSansCurseur() {
        Document document = Document.of("abcdef");
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 0);
        ChromePainter chrome = new ChromePainter();
        SurfaceRectangles surface = new SurfaceRectangles();

        chrome.tracer(surface, new PinceauSimple(), THEME, metrics(),
                new CursorPill(), document, projection,
                List.of(new SelRange(1, 4)), GOUTTIERE, 0f, 0f, true);

        assertEquals(2, surface.rectangles().size());
    }
}
