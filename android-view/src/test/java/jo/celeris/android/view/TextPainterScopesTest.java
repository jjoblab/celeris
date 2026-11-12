package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.core.syntax.scope.ScopeStacks;
import jo.celeris.core.syntax.scope.ScopeStyle;
import jo.celeris.core.syntax.scope.ScopeTheme;
import jo.celeris.core.syntax.scope.ScopeThemes;
import jo.celeris.core.syntax.TokenKind;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Le tracé par portées (M7.6, H-07) : la plage qui porte sa PILE se
 * peint à l'encre que le thème y résout — style en cache, aucune
 * allocation par image — et la plage sans pile garde la palette par
 * sorte. Les attributs (graisse, italique, souligné) se posent à
 * chaque tracé et ne fuient jamais vers la plage suivante (PIEGE-01).
 */
class TextPainterScopesTest {

    private static final EditorTheme THEME = EditorThemes.CLAIR;

    /** Pinceau mutable qui capture aussi les attributs posés. */
    private static final class PinceauMutable implements TextBrush {
        private int couleur;
        private boolean gras;
        private boolean italique;
        private boolean souligne;
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

        @Override
        public void setStyle(boolean bold, boolean italic, boolean underline) {
            this.gras = bold;
            this.italique = italic;
            this.souligne = underline;
        }
    }

    /** Un tracé : tranche, couleur et attributs à l'instant de l'appel. */
    private record Trace(int debut, int fin, int couleur, boolean gras,
            boolean italique, boolean souligne) {
    }

    /** Surface enregistreuse des tracés. */
    private static final class SurfaceEnregistreuse implements DrawSurface {
        private final List<Trace> traces = new ArrayList<>();

        @Override
        public void drawText(String text, float x, float y, TextBrush brush) {
            drawText(text, 0, text.length(), x, y, brush);
        }

        @Override
        public void drawText(CharSequence text, int start, int end, float x,
                float y, TextBrush brush) {
            PinceauMutable pinceau = (PinceauMutable) brush;
            traces.add(new Trace(start, end, pinceau.couleur, pinceau.gras,
                    pinceau.italique, pinceau.souligne));
        }

        @Override
        public void drawRect(float left, float top, float right, float bottom,
                TextBrush brush) {
            // la couche de texte ne trace aucun rectangle
        }

        List<Trace> traces() {
            return traces;
        }
    }

    /** Règle fixe : largeur 10 px, hauteur 20 px. */
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

    /** Source à mise en page imposée. */
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

    private static FrameState image(Document document) {
        FrameState etat = new FrameState();
        etat.beginFrame(document, MultiSelection.empty(),
                new VisualProjection(1, new FoldModel()), 1L,
                EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe()),
                THEME, 0, 1, 40f, 0f, 0f);
        return etat;
    }

    @Test
    void laPlageAPeintSePeintALaCouleurDuTheme() {
        int pileMotCle = ScopeStacks.push(
                ScopeStacks.root("source.demo"), "keyword.other");
        String texte = "import os";
        List<ColoredSpan> plages = List.of(
                new ColoredSpan(TokenKind.KEYWORD, 0, 6, pileMotCle),
                new ColoredSpan(TokenKind.IDENTIFIER, 7, 9));
        RowLayout mise = new RowLayout(0, 0, texte.length(), texte,
                texte.length() * 10f, List.of(), plages);
        SurfaceEnregistreuse surface = new SurfaceEnregistreuse();
        ScrollFrame cadre = new ScrollFrame();
        cadre.addLayer(new TextPainter(PinceauMutable::new,
                new RowLayoutCache(), new SourceImposee(mise)));
        cadre.drawFrame(surface, image(Document.of(texte)));

        assertEquals(2, surface.traces().size());
        ScopeStyle styleMotCle = THEME.portees().styleFor(pileMotCle);
        assertNotNull(styleMotCle, "le mot-clé résout dans le thème livré");
        int encreAttendue = styleMotCle.foreground();
        assertEquals(encreAttendue, surface.traces().get(0).couleur(),
                "la plage à pile se peint à l'encre que le thème y résout");
        assertEquals(THEME.syntaxe().couleur(TokenKind.IDENTIFIER),
                surface.traces().get(1).couleur(),
                "la plage sans pile garde la palette par sorte");
    }

    @Test
    void lesAttributsSePosentSansFuirVersLaPlageSuivante() {
        ScopeTheme theme = ScopeTheme.of(List.of(
                new ScopeTheme.Rule("entity.name.function",
                        new jo.celeris.core.syntax.scope.ScopeStyle(0, true, false,
                                false))));
        EditorTheme aThemes = new EditorTheme(9, 0xFFFFFFFF, 0xFF202124,
                0xFFD3E3FD, 0xFF1A73E8, 0xFF5F6368, 0x14202124,
                THEME.syntaxe(), theme);
        int pileFonction = ScopeStacks.push(
                ScopeStacks.root("source.demo"), "entity.name.function");
        String texte = "def un";
        List<ColoredSpan> plages = List.of(
                new ColoredSpan(TokenKind.IDENTIFIER, 0, 3, pileFonction),
                new ColoredSpan(TokenKind.IDENTIFIER, 4, 6));
        RowLayout mise = new RowLayout(0, 0, texte.length(), texte,
                texte.length() * 10f, List.of(), plages);
        SurfaceEnregistreuse surface = new SurfaceEnregistreuse();
        ScrollFrame cadre = new ScrollFrame();
        cadre.addLayer(new TextPainter(PinceauMutable::new,
                new RowLayoutCache(), new SourceImposee(mise)));
        FrameState etat = new FrameState();
        etat.beginFrame(Document.of(texte), MultiSelection.empty(),
                new VisualProjection(1, new FoldModel()), 1L,
                EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe()),
                aThemes, 0, 1, 40f, 0f, 0f);
        cadre.drawFrame(surface, etat);

        assertEquals(2, surface.traces().size());
        assertTrue(surface.traces().get(0).gras(),
                "la règle bold du thème se pose sur sa plage");
        assertTrue(!surface.traces().get(1).gras(),
                "la graisse ne fuit jamais vers la plage suivante (PIEGE-01)");
    }

    @Test
    void leThemeVideRendLaPalettePartout() {
        int pile = ScopeStacks.push(ScopeStacks.root("source.demo"),
                "keyword.other");
        String texte = "if x";
        List<ColoredSpan> plages = List.of(
                new ColoredSpan(TokenKind.KEYWORD, 0, 2, pile));
        RowLayout mise = new RowLayout(0, 0, texte.length(), texte,
                texte.length() * 10f, List.of(), plages);
        EditorTheme themeVide = new EditorTheme(8, 0xFFFFFFFF, 0xFF202124,
                0xFFD3E3FD, 0xFF1A73E8, 0xFF5F6368, 0x14202124,
                THEME.syntaxe(), ScopeTheme.empty());
        SurfaceEnregistreuse surface = new SurfaceEnregistreuse();
        ScrollFrame cadre = new ScrollFrame();
        cadre.addLayer(new TextPainter(PinceauMutable::new,
                new RowLayoutCache(), new SourceImposee(mise)));
        FrameState etat = new FrameState();
        etat.beginFrame(Document.of(texte), MultiSelection.empty(),
                new VisualProjection(1, new FoldModel()), 1L,
                EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe()),
                themeVide, 0, 1, 40f, 0f, 0f);
        cadre.drawFrame(surface, etat);

        assertEquals(1, surface.traces().size());
        assertEquals(THEME.syntaxe().couleur(TokenKind.KEYWORD),
                surface.traces().get(0).couleur(),
                "sans règle qui matche, la palette par sorte fait foi");
    }

    @Test
    void lesThemesLivresPortentLeursPortees() {
        assertTrue(EditorThemes.CLAIR.portees() != ScopeTheme.empty());
        assertTrue(EditorThemes.SOMBRE.portees() != ScopeTheme.empty());
        assertTrue(EditorThemes.CONTRASTE_ELEVE.portees() != ScopeTheme.empty());
        assertEquals(ScopeThemes.light().rules().size(),
                EditorThemes.CLAIR.portees().rules().size());
    }
}
