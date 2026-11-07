package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.ai.SuggestionFantome;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;

/**
 * Tests de la couche du texte fantôme (F-01, ADR-012) : la suggestion
 * se dessine au chemin des indices en ligne — une cellule d'encre
 * pleine par caractère, sur la même ligne de base que le code
 * (PIEGE-11), à l'encre fanée du thème.
 */
class FantomePainterTest {

    private static final EditorTheme THEME =
            new EditorTheme(1, 0xFF101014, 0xFFE3E3E6, 0xFF1A73E8,
            0xFFE3E3E6, 0xFF9AA0A6);

    /** Un dessin croisé : le texte, la position et la couleur au moment de l'appel. */
    private record Dessin(String texte, float x, float y, int couleur) {
    }

    /** Surface enregistreuse : capture chaque tracé de texte. */
    private static final class SurfaceEnregistreuse implements DrawSurface {
        private final List<Dessin> dessins = new ArrayList<>();

        @Override
        public void drawText(String text, float x, float y, TextBrush brush) {
            dessins.add(new Dessin(text, x, y, brush.color()));
        }

        @Override
        public void drawRect(float left, float top, float right, float bottom,
                TextBrush brush) {
            // le fantôme ne trace pas de rectangle
        }

        List<Dessin> dessins() {
            return dessins;
        }
    }

    /** Pinceau minimal : la couleur posée, lue au moment du tracé. */
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

    /** Fabrique : chaque appel rend un pinceau neuf. */
    private static final class FabriqueSimple implements BrushFactory {
        @Override
        public TextBrush newBrush() {
            return new PinceauSimple();
        }
    }

    /** Règle fixe : largeur 10 par caractère, hauteur de ligne 20. */
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
            return 15f;
        }

        @Override
        public float descent(FontSpec spec) {
            return 5f;
        }
    }

    /**
     * PIEGE-11 — inlayOccupiesExactInkCells_onCodeBaseline : le
     * fantôme, indice en ligne éphémère, occupe exactement une cellule
     * d'encre pleine par caractère — la première ligne se pose à
     * l'ancre, sur la ligne de base de SA rangée, les suivantes au bord
     * gauche du texte, une rangée chacune. Ni espace fantôme, ni
     * chevauchement des cellules.
     */
    @Test
    void inlayOccupiesExactInkCells_onCodeBaseline() {
        FantomePainter peintre = new FantomePainter(new FabriqueSimple());
        SurfaceEnregistreuse surface = new SurfaceEnregistreuse();
        Document document = Document.of("val reponse = \n");
        EditorMetrics metrics =
                EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 0);
        FrameState etat = new FrameState();
        etat.beginFrame(document, MultiSelection.empty(), projection, 3L,
                metrics, THEME, 0, 4, 40f, 0f, 0f);

        peintre.setSuggestion(
                new SuggestionFantome("lireAuClavier()\n    .premier()", 14, 3));
        peintre.paint(surface, etat);

        assertEquals(2, surface.dessins().size(), "deux lignes, deux tracés");
        Dessin premiere = surface.dessins().get(0);
        // L'ancre vit sur la première ligne (offset 14, colonne 14) :
        // 40 (gouttière) + 14 × 10 (cellules d'encre) = 180.
        assertEquals(180f, premiere.x(), "une cellule d'encre par caractère, à l'ancre");
        assertEquals(metrics.rowBaseline(0), premiere.y(),
                "la même ligne de base que le code");
        Dessin seconde = surface.dessins().get(1);
        assertEquals(40f, seconde.x(),
                "les lignes suivantes repartent au bord gauche du texte");
        assertEquals(metrics.rowBaseline(0) + metrics.lineHeight(), seconde.y(),
                "une rangée chacune, lignes de base alignées au code");
        assertEquals("lireAuClavier()", premiere.texte());
        assertEquals("    .premier()", seconde.texte());
    }

    /**
     * F-01 — l'encre fanée : le fantôme se lit sans se confondre avec
     * le code — l'opacité voile l'encre de base du thème, la teinte
     * reste.
     */
    @Test
    void encreFaneeDuTheme() {
        FantomePainter peintre = new FantomePainter(new FabriqueSimple());
        SurfaceEnregistreuse surface = new SurfaceEnregistreuse();
        Document document = Document.of("val x = ");
        EditorMetrics metrics =
                EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 0);
        FrameState etat = new FrameState();
        etat.beginFrame(document, MultiSelection.empty(), projection, 0L,
                metrics, THEME, 0, 2, 40f, 0f, 0f);

        peintre.setSuggestion(new SuggestionFantome("1", 8, 0));
        peintre.paint(surface, etat);

        assertEquals(1, surface.dessins().size());
        int couleur = surface.dessins().get(0).couleur();
        assertEquals(0x99, (couleur >>> 24) & 0xFF,
                "l'opacité voile, sans changer la teinte");
        assertEquals(THEME.baseTextColor() & 0x00FFFFFF, couleur & 0x00FFFFFF);
    }

    /**
     * F-01 — le fantôme fermé ou ancré hors document ne trace rien ;
     * la cible du geste suit la première ligne, bord à bord.
     */
    @Test
    void fantomeFermeEtCibleDuGeste() {
        FantomePainter peintre = new FantomePainter(new FabriqueSimple());
        SurfaceEnregistreuse surface = new SurfaceEnregistreuse();
        Document document = Document.of("val x = ");
        EditorMetrics metrics =
                EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 0);
        FrameState etat = new FrameState();
        etat.beginFrame(document, MultiSelection.empty(), projection, 0L,
                metrics, THEME, 0, 2, 40f, 0f, 0f);

        peintre.paint(surface, etat);
        assertTrue(surface.dessins().isEmpty(), "sans suggestion, rien ne se trace");

        peintre.setSuggestion(new SuggestionFantome("1", 99, 0));
        peintre.paint(surface, etat);
        assertTrue(surface.dessins().isEmpty(),
                "l'ancre hors document ne se trace pas");

        peintre.setSuggestion(new SuggestionFantome("calcul()", 8, 0));
        // L'ancre vit à la colonne 8 : la première ligne couvre
        // [120, 200] horizontalement, [0, 20] verticalement.
        assertTrue(peintre.premiereLigneTouchee(etat, 120f + 4f * 10f + 2f, 8f),
                "le milieu de la première ligne se touche");
        assertTrue(!peintre.premiereLigneTouchee(etat, 205f, 8f),
                "au-delà de la dernière cellule, le geste vise le code");
    }
}
