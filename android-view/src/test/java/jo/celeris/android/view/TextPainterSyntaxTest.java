package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.core.syntax.TokenKind;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * Tests de la couche de texte colorée (B-01) : chaque plage de la mise
 * en page se trace avec l'encre de sa sorte — le même arbre que les
 * plis et les diagnostics (ADR-004), le même chemin de dessin sans
 * dérivé de thème (ADR-008).
 *
 * <p>PIEGE-01 : même sous la pire fabrique — un pinceau mutable unique
 * pour toute la couche — chaque tracé porte la couleur de SA plage,
 * posée à l'instant précis du dessin. PIEGE-15 : une rangée sans plage
 * reste sur le chemin rapide d'un seul tracé.</p>
 */
class TextPainterSyntaxTest {

    private static final EditorTheme THEME = EditorThemes.CLAIR;

    /**
     * Pinceau mutable minimal : la couleur courante et la police
     * courante — la police posée se capture comme la couleur (PIEGE-21).
     */
    private static final class PinceauMutable implements TextBrush {
        private int couleur;
        private @Nullable FontSpec police;

        @Override
        public void setColor(int argb) {
            couleur = argb;
        }

        @Override
        public int color() {
            return couleur;
        }

        @Override
        public void setFont(FontSpec police) {
            this.police = police;
        }
    }

    /**
     * Un tracé de texte : sa tranche, la couleur posée à l'instant de
     * l'appel, et la police posée au même instant.
     */
    private record Trace(CharSequence texte, int debut, int fin, float x, int couleur,
            @Nullable FontSpec police) {
    }

    /** Surface enregistreuse : capture chaque tracé avec la couleur au moment précis. */
    private static final class SurfaceEnregistreuse implements DrawSurface {
        private final List<Trace> traces = new ArrayList<>();

        @Override
        public void drawText(String text, float x, float y, TextBrush brush) {
            traces.add(new Trace(text, 0, text.length(), x, brush.color(),
                    policeDe(brush)));
        }

        @Override
        public void drawText(CharSequence text, int start, int end, float x, float y,
                TextBrush brush) {
            traces.add(new Trace(text, start, end, x, brush.color(), policeDe(brush)));
        }

        @Override
        public void drawRect(float left, float top, float right, float bottom,
                TextBrush brush) {
            // la couche de texte ne trace aucun rectangle
        }

        List<Trace> traces() {
            return traces;
        }

        private static @Nullable FontSpec policeDe(TextBrush brush) {
            if (brush instanceof PinceauMutable pinceau) {
                return pinceau.police;
            }
            return null;
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

    /** Source à mise en page imposée : une rangée, la mise en page donnée. */
    private static final class SourceImposee implements RowSource {
        private final RowLayout mise;

        SourceImposee(RowLayout mise) {
            this.mise = mise;
        }

        @Override
        public RowLayout layoutOf(RowKey key) {
            return mise;
        }
    }

    private static FrameState image(Document document, VisualProjection projection,
            int rangeesVisibles) {
        FrameState etat = new FrameState();
        etat.beginFrame(document, MultiSelection.empty(), projection, 1L,
                EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe()), THEME,
                0, rangeesVisibles, 40f, 0f, 0f);
        return etat;
    }

    /** Chaque plage se trace avec l'encre de sa sorte, de gauche à droite, sans trou. */
    @Test
    void chaquePlageSeTraceAvecSonEncre() {
        String texte = "int x = 42;";
        List<ColoredSpan> plages = List.of(
                new ColoredSpan(TokenKind.KEYWORD, 0, 3),
                new ColoredSpan(TokenKind.IDENTIFIER, 3, 5),
                new ColoredSpan(TokenKind.OPERATOR, 5, 7),
                new ColoredSpan(TokenKind.NUMBER, 7, 9),
                new ColoredSpan(TokenKind.PUNCTUATION, 9, 10));
        RowLayout mise = new RowLayout(0, 0, texte.length(), texte,
                texte.length() * 10f, List.of(), plages);
        SurfaceEnregistreuse surface = new SurfaceEnregistreuse();
        ScrollFrame image = new ScrollFrame();
        image.addLayer(new TextPainter(() -> new PinceauMutable(),
                new RowLayoutCache(), new SourceImposee(mise)));
        image.drawFrame(surface, image(Document.of(texte),
                new VisualProjection(1, new FoldModel()), 1));

        assertEquals(plages.size(), surface.traces().size(),
                "une plage, un tracé — jamais de regroupement silencieux");
        float x = 40f;
        for (int i = 0; i < plages.size(); i++) {
            Trace trace = surface.traces().get(i);
            ColoredSpan plage = plages.get(i);
            assertEquals(plage.start(), trace.debut());
            assertEquals(plage.end(), trace.fin());
            assertEquals(x, trace.x(), 1e-6f, "le tracé avance de la largeur de sa plage");
            assertEquals(THEME.syntaxe().couleur(plage.kind()), trace.couleur(),
                    "la plage " + plage.kind() + " porte son encre");
            x += (plage.end() - plage.start()) * 10f;
        }
    }

    /** Une rangée sans plage reste sur le chemin rapide : un seul tracé, encre de base (PIEGE-15). */
    @Test
    void rangeeSansPlageResteSurLeCheminRapide() {
        String texte = "sans coloration";
        RowLayout mise = new RowLayout(0, 0, texte.length(), texte,
                texte.length() * 10f);
        SurfaceEnregistreuse surface = new SurfaceEnregistreuse();
        ScrollFrame image = new ScrollFrame();
        image.addLayer(new TextPainter(() -> new PinceauMutable(),
                new RowLayoutCache(), new SourceImposee(mise)));
        image.drawFrame(surface, image(Document.of(texte),
                new VisualProjection(1, new FoldModel()), 1));

        assertEquals(1, surface.traces().size());
        assertEquals(THEME.baseTextColor(), surface.traces().get(0).couleur());
    }

    /**
     * Sous segments bidirectionnels (ADR-008), la couleur suit le
     * découpage : chaque visuel se peint par l'intersection de sa plage
     * logique et du segment, de gauche à droite dans l'ordre visuel.
     */
    @Test
    void laCouleurSuitLesSegmentsBidi() {
        // Ordre visuel inversé : le segment [6, 11) se trace avant [0, 6).
        String texte = "abcde «Hebrew»";
        List<VisualRuns.Run> segments = List.of(
                new VisualRuns.Run(6, 11, true),
                new VisualRuns.Run(0, 6, false));
        List<ColoredSpan> plages = List.of(
                new ColoredSpan(TokenKind.STRING, 0, 6),
                new ColoredSpan(TokenKind.COMMENT, 6, 11));
        RowLayout mise = new RowLayout(0, 0, 11, texte.substring(0, 11),
                11 * 10f, segments, plages);
        SurfaceEnregistreuse surface = new SurfaceEnregistreuse();
        ScrollFrame image = new ScrollFrame();
        image.addLayer(new TextPainter(() -> new PinceauMutable(),
                new RowLayoutCache(), new SourceImposee(mise)));
        image.drawFrame(surface, image(Document.of(texte),
                new VisualProjection(1, new FoldModel()), 1));

        // Deux tracés : le segment visuel de gauche porte la plage
        // COMMENT [6, 11), celui de droite la plage STRING [0, 6).
        assertEquals(2, surface.traces().size());
        Trace premierVisuel = surface.traces().get(0);
        assertEquals(6, premierVisuel.debut());
        assertEquals(11, premierVisuel.fin());
        assertEquals(40f, premierVisuel.x(), 1e-6f);
        assertEquals(THEME.syntaxe().couleur(TokenKind.COMMENT), premierVisuel.couleur());
        Trace secondVisuel = surface.traces().get(1);
        assertEquals(0, secondVisuel.debut());
        assertEquals(6, secondVisuel.fin());
        assertEquals(40f + 5 * 10f, secondVisuel.x(), 1e-6f,
                "le second segment se trace après le premier, largeur rendue en tête");
        assertEquals(THEME.syntaxe().couleur(TokenKind.STRING), secondVisuel.couleur());
    }

    /**
     * PIEGE-21, volet coloration : chaque pinceau par sorte porte la
     * police de l'image au moment de son tracé — un pinceau jamais
     * habillé trace à la police par défaut de la plateforme pendant
     * que les positions avancent à la police de l'éditeur : les mots
     * colorés s'éparpilleraient.
     */
    @Test
    void lesPlagesSeTracentALaPoliceDeLEditeur() {
        String texte = "int x = 42;";
        List<ColoredSpan> plages = List.of(
                new ColoredSpan(TokenKind.KEYWORD, 0, 3),
                new ColoredSpan(TokenKind.IDENTIFIER, 3, 5),
                new ColoredSpan(TokenKind.NUMBER, 7, 9));
        RowLayout mise = new RowLayout(0, 0, texte.length(), texte,
                texte.length() * 10f, List.of(), plages);
        SurfaceEnregistreuse surface = new SurfaceEnregistreuse();
        ScrollFrame image = new ScrollFrame();
        image.addLayer(new TextPainter(() -> new PinceauMutable(),
                new RowLayoutCache(), new SourceImposee(mise)));
        image.drawFrame(surface, image(Document.of(texte),
                new VisualProjection(1, new FoldModel()), 1));

        assertEquals(plages.size(), surface.traces().size(),
                "une plage, un tracé — chacun à la police de l'image");
        for (Trace trace : surface.traces()) {
            assertEquals(new FontSpec("mono", 14f), trace.police(),
                    "chaque plage se trace à la police de l'éditeur (PIEGE-21)");
        }
    }

    /**
     * PIEGE-01, volet hostile : la fabrique ne donne qu'UN pinceau
     * mutable — chaque tracé pose sa couleur juste avant de dessiner,
     * aucune couleur résiduelle ne se propage d'une plage à l'autre.
     */
    @Test
    void pinceauPartageChaqueTracePoseSaCouleur() {
        String texte = "int 42";
        List<ColoredSpan> plages = List.of(
                new ColoredSpan(TokenKind.KEYWORD, 0, 3),
                new ColoredSpan(TokenKind.NUMBER, 3, 6));
        RowLayout mise = new RowLayout(0, 0, texte.length(), texte,
                texte.length() * 10f, List.of(), plages);
        TextBrush unique = new PinceauMutable();
        SurfaceEnregistreuse surface = new SurfaceEnregistreuse();
        ScrollFrame image = new ScrollFrame();
        image.addLayer(new TextPainter(() -> unique, new RowLayoutCache(),
                new SourceImposee(mise)));
        image.drawFrame(surface, image(Document.of(texte),
                new VisualProjection(1, new FoldModel()), 1));

        assertEquals(2, surface.traces().size());
        assertEquals(THEME.syntaxe().couleur(TokenKind.KEYWORD),
                surface.traces().get(0).couleur());
        assertEquals(THEME.syntaxe().couleur(TokenKind.NUMBER),
                surface.traces().get(1).couleur(),
                "la seconde plage ne porte jamais la couleur résiduelle de la première");
    }

    /** Une rangée colorée ne peint que ses plages : le cache sert la même mise en page deux fois. */
    @Test
    void leCacheResertLaMemeMiseEnPageColoree() {
        String texte = "int x;";
        List<ColoredSpan> plages = List.of(
                new ColoredSpan(TokenKind.KEYWORD, 0, 3),
                new ColoredSpan(TokenKind.IDENTIFIER, 3, 6));
        RowLayout mise = new RowLayout(0, 0, texte.length(), texte,
                texte.length() * 10f, List.of(), plages);
        RowLayoutCache cache = new RowLayoutCache();
        SurfaceEnregistreuse surface = new SurfaceEnregistreuse();
        ScrollFrame image = new ScrollFrame();
        image.addLayer(new TextPainter(() -> new PinceauMutable(), cache,
                new SourceImposee(mise)));
        FrameState etat = image(Document.of(texte),
                new VisualProjection(1, new FoldModel()), 1);
        image.drawFrame(surface, etat);
        image.drawFrame(surface, etat);

        assertEquals(4, surface.traces().size(), "deux images, deux plages chacune");
        assertTrue(surface.traces().get(0).texte() == surface.traces().get(2).texte(),
                "la même instance de texte sert les deux images (PIEGE-15)");
    }
}
