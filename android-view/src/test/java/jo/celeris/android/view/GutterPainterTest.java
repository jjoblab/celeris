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
 * Tests de la couche de gouttière : numéros absolus par rangée, alignés
 * à droite, insensibles au défilement horizontal (D-01), servis par la
 * projection (PIEGE-04) et mémoïsés (PIEGE-15).
 */
class GutterPainterTest {

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

    /** Un texte tracé : son contenu, son abscisse et sa couleur. */
    private record TexteTrace(String texte, float x, int couleur) {
    }

    /** Surface enregistreuse de textes. */
    private static final class SurfaceTextes implements DrawSurface {
        private final List<TexteTrace> textes = new ArrayList<>();

        @Override
        public void drawText(String text, float x, float y, TextBrush brush) {
            textes.add(new TexteTrace(text, x, brush.color()));
        }

        @Override
        public void drawRect(float left, float top, float right, float bottom,
                TextBrush brush) {
            // la gouttière ne trace aucun rectangle
        }

        List<TexteTrace> textes() {
            return textes;
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
     * D-01 — les numéros absolus, un par rangée visible, alignés à
     * droite dans la gouttière.
     */
    @Test
    void numerosAbsolusAlignesADroite() {
        SurfaceTextes surface = new SurfaceTextes();
        ScrollFrame image = new ScrollFrame();
        GutterPainter gouttiere = new GutterPainter(() -> new PinceauMutable(), 4f);
        image.addLayer(gouttiere);
        Document document = Document.of("un\ndeux\ntrois");
        EditorMetrics metrics = EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        // gouttière de 18 px pour « 3 » (un chiffre) : 1 × 10 + 2 × 4.
        float largeur = GutterGeometry.width(3, 10f, 4f);
        FrameState etat = new FrameState();
        etat.beginFrame(document, MultiSelection.empty(),
                new VisualProjection(3, new FoldModel()), 1L, metrics, THEME,
                0, 3, largeur, 0f, 0f);
        image.drawFrame(surface, etat);

        assertEquals(3, surface.textes().size());
        assertEquals("1", surface.textes().get(0).texte());
        assertEquals("2", surface.textes().get(1).texte());
        assertEquals("3", surface.textes().get(2).texte());
        // alignés à droite : « 1 » et « 3 » finissent à 18 - 4, abscisse 4.
        assertEquals(4f, surface.textes().get(0).x(), 1e-6f);
        assertEquals(4f, surface.textes().get(2).x(), 1e-6f);
        assertEquals(THEME.gutterTextColor(), surface.textes().get(0).couleur());
    }

    /**
     * La gouttière ne défile jamais horizontalement : le défilement
     * horizontal du contenu ne déplace pas les numéros.
     */
    @Test
    void gouttiereInsensibleAuDefilementHorizontal() {
        SurfaceTextes surface = new SurfaceTextes();
        ScrollFrame image = new ScrollFrame();
        image.addLayer(new GutterPainter(() -> new PinceauMutable(), 4f));
        Document document = Document.of("un\ndeux\ntrois");
        EditorMetrics metrics = EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        float largeur = GutterGeometry.width(3, 10f, 4f);
        FrameState etat = new FrameState();
        etat.beginFrame(document, MultiSelection.empty(),
                new VisualProjection(3, new FoldModel()), 1L, metrics, THEME,
                0, 3, largeur, 120f, 0f);
        image.drawFrame(surface, etat);

        assertEquals(4f, surface.textes().get(0).x(), 1e-6f,
                "l'abscisse des numéros ne dépend pas du défilement horizontal");
    }

    /**
     * La rangée d'un pli porte le numéro de son en-tête (PIEGE-04) : la
     * conversion passe par la projection.
     */
    @Test
    void rangeeDePliPorteLeNumeroDeSonEnTete() {
        SurfaceTextes surface = new SurfaceTextes();
        ScrollFrame image = new ScrollFrame();
        image.addLayer(new GutterPainter(() -> new PinceauMutable(), 4f));
        Document document = Document.of("a\nb\nc\nd\ne");
        EditorMetrics metrics = EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        FoldModel plis = new FoldModel();
        plis.fold(1, 3); // l'en-tête 1 reste, les lignes 2 à 4 sont cachées
        VisualProjection projection = new VisualProjection(5, plis);
        FrameState etat = new FrameState();
        etat.beginFrame(document, MultiSelection.empty(), projection, 1L, metrics, THEME,
                0, projection.rowCount(), 46f, 0f, 0f);
        image.drawFrame(surface, etat);

        // 5 lignes, pli (1, 3) : les lignes 2 et 3 sont cachées —
        // rangées : 0 → ligne 0, 1 → lignes 1 à 3 (pli), 2 → ligne 4.
        assertEquals(3, surface.textes().size());
        assertEquals("1", surface.textes().get(0).texte());
        assertEquals("2", surface.textes().get(1).texte(), "la rangée du pli porte l'en-tête");
        assertEquals("5", surface.textes().get(2).texte());
    }

    /** Les étiquettes sont mémoïsées : deux images, mêmes instances (PIEGE-15). */
    @Test
    void etiquettesMemoiseesEntreImages() {
        SurfaceTextes surface = new SurfaceTextes();
        ScrollFrame image = new ScrollFrame();
        image.addLayer(new GutterPainter(() -> new PinceauMutable(), 4f));
        Document document = Document.of("a\nb");
        EditorMetrics metrics = EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        FrameState etat = new FrameState();
        etat.beginFrame(document, MultiSelection.empty(),
                new VisualProjection(2, new FoldModel()), 1L, metrics, THEME,
                0, 2, 28f, 0f, 0f);
        image.drawFrame(surface, etat);
        image.drawFrame(surface, etat);

        assertTrue(surface.textes().get(0).texte() == surface.textes().get(2).texte(),
                "la même instance d'étiquette sert les deux images");
        assertTrue(surface.textes().get(1).texte() == surface.textes().get(3).texte());
    }
}
