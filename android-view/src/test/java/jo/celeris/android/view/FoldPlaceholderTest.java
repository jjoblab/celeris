package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;

/**
 * La couche du placeholder de pli : la bande teinte la chip du « {…} »
 * — à la colonne de l'ouvrante, large du placeholder, pleine hauteur de
 * la rangée, muette sans pièce et sur les tranches de continuation.
 */
class FoldPlaceholderTest {

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
    private static final class RectTrace {
        final float gauche;
        final float haut;
        final float droite;
        final float bas;
        final int couleur;

        RectTrace(float gauche, float haut, float droite, float bas, int couleur) {
            this.gauche = gauche;
            this.haut = haut;
            this.droite = droite;
            this.bas = bas;
            this.couleur = couleur;
        }
    }

    /** Surface enregistreuse de rectangles. */
    private static final class SurfaceRectangles implements DrawSurface {
        private final List<RectTrace> rectangles = new ArrayList<>();

        @Override
        public void drawText(String text, float x, float y, TextBrush brush) {
            // la bande ne trace aucun texte
        }

        @Override
        public void drawRect(float left, float top, float right, float bottom,
                TextBrush brush) {
            rectangles.add(new RectTrace(left, top, right, bottom,
                    ((PinceauMutable) brush).color()));
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
     * La bande de la chip : à la colonne de l'ouvrante, large du
     * placeholder, pleine hauteur de rangée, teinte de sélection au
     * voile — l'encre du texte reste seule à parler.
     */
    @Test
    void bande_teinteLaChipDuPlaceholder() {
        Document document = Document.of("class A {\n}\n");
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 0);
        EditorMetrics metrics =
                EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        FrameState image = new FrameState();
        image.beginFrame(document, MultiSelection.of(new SelRange(0, 0)),
                projection, 1L, metrics, THEME, 0, 2, 20f, 0f, 0f);
        FoldPlaceholder couche = new FoldPlaceholder(() -> new PinceauMutable(),
                ligne -> ligne == 0
                        ? new FoldedRowSource.PieceDePli(8, 1, "{…}")
                        : null);
        SurfaceRectangles surface = new SurfaceRectangles();
        couche.paint(surface, image);
        assertEquals(1, surface.rectangles().size());
        RectTrace bande = surface.rectangles().get(0);
        assertEquals(20f + 8 * 10f, bande.gauche, 1e-6f);
        assertEquals(20f + 11 * 10f, bande.droite, 1e-6f);
        assertEquals(0f, bande.haut, 1e-6f);
        assertEquals(20f, bande.bas, 1e-6f);
        assertEquals((THEME.selectionColor() & 0x00FFFFFF) | 0x66000000,
                bande.couleur);
    }

    /** Sans pièce, la couche reste muette — aucune bande ne se trace. */
    @Test
    void sansPiece_coucheMuette() {
        Document document = Document.of("class A {\n}\n");
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 0);
        EditorMetrics metrics =
                EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        FrameState image = new FrameState();
        image.beginFrame(document, MultiSelection.of(new SelRange(0, 0)),
                projection, 1L, metrics, THEME, 0, 2, 20f, 0f, 0f);
        FoldPlaceholder couche = new FoldPlaceholder(() -> new PinceauMutable(),
                ligne -> null);
        SurfaceRectangles surface = new SurfaceRectangles();
        couche.paint(surface, image);
        assertEquals(0, surface.rectangles().size());
    }
}
