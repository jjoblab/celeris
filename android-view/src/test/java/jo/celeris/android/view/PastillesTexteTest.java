package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;

/**
 * La couche des pastilles de couleur du texte (le constat du
 * propriétaire : « lorsque je tape une couleur par exemple #fff,
 * #ffffff encadré le avec la couleur comme dans l'ancien projet ») :
 * chaque littéral visible se rend encadré — fond en voile de SA
 * couleur, trait plein d'un pixel — tous les littéraux d'une ligne, le
 * cadre rogné à la tranche de sa rangée (PIEGE-04).
 */
class PastillesTexteTest {

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

    /** Un tracé : rectangle plein ou trait. */
    private record Trace(String sorte, float x1, float y1, float x2, float y2,
            int couleur) {
    }

    /** Surface enregistreuse de rectangles et de traits. */
    private static final class SurfaceTraces implements DrawSurface {
        private final List<Trace> traces = new ArrayList<>();

        @Override
        public void drawText(String text, float x, float y, TextBrush brush) {
            // la couche ne trace aucun texte
        }

        @Override
        public void drawRect(float left, float top, float right, float bottom,
                TextBrush brush) {
            traces.add(new Trace("rect", left, top, right, bottom,
                    brush.color()));
        }

        @Override
        public void drawLine(float x1, float y1, float x2, float y2,
                TextBrush brush) {
            traces.add(new Trace("trait", x1, y1, x2, y2, brush.color()));
        }

        List<Trace> traces() {
            return traces;
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
     * Le littéral se rend encadré : fond en voile de SA couleur (le
     * quart de son alpha), trait plein d'un pixel — quatre côtés, le
     * cadre respire de deux dp horizontaux et d'un dp vertical autour
     * du littéral entier, dièse compris.
     */
    @Test
    void litteralEncadreDeSaCouleur() {
        Document document = Document.of("fond #FF5722;");
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 0);
        EditorMetrics metrics =
                EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        FrameState image = new FrameState();
        image.beginFrame(document, MultiSelection.of(new SelRange(0, 0)),
                projection, 1L, metrics, THEME, 0, 1, 20f, 0f, 0f);
        PastillesTexte couche = new PastillesTexte(() -> new PinceauMutable(),
                1f);
        SurfaceTraces surface = new SurfaceTraces();
        couche.paint(surface, image);
        // un fond + quatre côtés
        assertEquals(5, surface.traces().size());
        Trace fond = surface.traces().get(0);
        assertEquals("rect", fond.sorte());
        // le dièse vit à la colonne 5 : gauche = gouttière + 5 largeurs
        // de caractère, respirée de deux dp ; droite = 7 caractères plus loin
        assertEquals(20f + 5 * 10f - 2f, fond.x1(), 1e-6f);
        assertEquals(20f + 12 * 10f + 2f, fond.x2(), 1e-6f);
        assertEquals(0f + 1f, fond.y1(), 1e-6f);
        assertEquals(20f - 1f, fond.y2(), 1e-6f);
        assertEquals(0x3FFF5722, fond.couleur());
        for (int i = 1; i < 5; i++) {
            assertEquals("trait", surface.traces().get(i).sorte());
            assertEquals(0xFFFF5722, surface.traces().get(i).couleur());
        }
    }

    /** Tous les littéraux d'une ligne se rendent — le premier ne suffit plus. */
    @Test
    void tousLesLitterauxSEncadrent() {
        Document document = Document.of("a #fff b #0000CD c");
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 0);
        EditorMetrics metrics =
                EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        FrameState image = new FrameState();
        image.beginFrame(document, MultiSelection.of(new SelRange(0, 0)),
                projection, 1L, metrics, THEME, 0, 1, 20f, 0f, 0f);
        PastillesTexte couche = new PastillesTexte(() -> new PinceauMutable(),
                1f);
        SurfaceTraces surface = new SurfaceTraces();
        couche.paint(surface, image);
        // deux littéraux : deux fonds, huit côtés
        assertEquals(10, surface.traces().size());
        assertEquals(0x3FFFFFFF, surface.traces().get(0).couleur());
        assertEquals(0x3F0000CD, surface.traces().get(5).couleur());
    }

    /** La ligne sans littéral ne trace rien — pas de cadre sans couleur. */
    @Test
    void sansLitteralRienNeSeTrace() {
        Document document = Document.of("sans couleur");
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 0);
        EditorMetrics metrics =
                EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        FrameState image = new FrameState();
        image.beginFrame(document, MultiSelection.of(new SelRange(0, 0)),
                projection, 1L, metrics, THEME, 0, 1, 20f, 0f, 0f);
        PastillesTexte couche = new PastillesTexte(() -> new PinceauMutable(),
                1f);
        SurfaceTraces surface = new SurfaceTraces();
        couche.paint(surface, image);
        assertEquals(0, surface.traces().size());
    }

    /**
     * Le littéral coupé par le retour à la ligne encadre SA part visible
     * de chaque rangée (PIEGE-04) : le cadre ne déborde jamais de sa
     * tranche.
     */
    @Test
    void litteralCoupeSEncadreParTranche() {
        Document document = Document.of("#FF5722 suite");
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 4);
        EditorMetrics metrics =
                EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        FrameState image = new FrameState();
        image.beginFrame(document, MultiSelection.of(new SelRange(0, 0)),
                projection, 1L, metrics, THEME, 0, 2, 20f, 0f, 0f);
        PastillesTexte couche = new PastillesTexte(() -> new PinceauMutable(),
                1f);
        SurfaceTraces surface = new SurfaceTraces();
        couche.paint(surface, image);
        // rangée 0 : les colonnes 0 à 3 du littéral ; rangée 1 : les
        // colonnes 4 à 6 — chacune son fond, chacune ses quatre côtés
        assertEquals(10, surface.traces().size());
        Trace premiereTranche = surface.traces().get(0);
        assertEquals(20f - 2f, premiereTranche.x1(), 1e-6f);
        assertEquals(20f + 4 * 10f + 2f, premiereTranche.x2(), 1e-6f);
        Trace secondeTranche = surface.traces().get(5);
        assertEquals(20f - 2f, secondeTranche.x1(), 1e-6f);
        assertEquals(20f + 3 * 10f + 2f, secondeTranche.x2(), 1e-6f);
        assertEquals(20f + 1f, secondeTranche.y1(), 1e-6f);
    }
}
