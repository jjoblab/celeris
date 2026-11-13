package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;

/**
 * Tests de la couche des valeurs de débogage en ligne (E-03) : le
 * rendu se dessine en fin de ligne UTILE (les blancs de queue ne
 * comptent pas), une respiration de deux cellules le sépare du code,
 * sur la même ligne de base (PIEGE-11), à l'encre de gouttière — la
 * ligne hors document ne se trace pas, jamais de valeur devinée.
 */
class ValeursPainterTest {

    private static final EditorTheme THEME =
            new EditorTheme(1, 0xFF101014, 0xFFE3E3E6, 0xFF1A73E8,
            0xFFE3E3E6, 0xFF9AA0A6);

    /** Un dessin croisé : le texte, la position et la couleur au moment
     *  de l'appel. */
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
            // la valeur ne trace pas de rectangle
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
     * PIEGE-11 — la valeur vit en fin de ligne UTILE : la ligne
     * « int total = 0;   » porte ses blancs de queue — le rendu se
     * pose à la colonne du POINT-VIRGULE plus une respiration de deux
     * cellules, sur la ligne de base de SA rangée, à l'encre de
     * gouttière (la valeur se lit sans se confondre avec le code).
     */
    @Test
    void laValeurVitEnFinDeLigneUtile() {
        ValeursPainter peintre = new ValeursPainter(new FabriqueSimple());
        SurfaceEnregistreuse surface = new SurfaceEnregistreuse();
        Document document = Document.of("int total = 0;   \nint copie;\n");
        EditorMetrics metrics =
                EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 0);
        FrameState etat = new FrameState();
        etat.beginFrame(document, MultiSelection.empty(), projection, 3L,
                metrics, THEME, 0, 2, 40f, 0f, 0f);

        peintre.setValeurs(List.of(new ValeurLigne(1, "total = 0")));
        peintre.paint(surface, etat);

        assertEquals(1, surface.dessins().size(), "une valeur, un tracé");
        Dessin rendu = surface.dessins().get(0);
        // la ligne utile « int total = 0; » compte 14 caractères :
        // 40 (gouttière) + 14 × 10 (cellules d'encre) + 2 × 10
        // (respiration) = 200
        assertEquals(200f, rendu.x(),
                "en fin de ligne utile, respiration comprise");
        assertEquals(metrics.rowBaseline(0), rendu.y(),
                "la même ligne de base que le code");
        assertEquals("total = 0", rendu.texte());
        assertEquals(THEME.gutterTextColor(), rendu.couleur(),
                "l'encre de gouttière porte la valeur");
    }

    /**
     * E-03 — la couche vide ne trace rien ; la ligne hors document ne
     * se trace pas — jamais de valeur devinée sur une ligne inconnue.
     */
    @Test
    void laCoucheVideEtLaLigneHorsDocumentNeTracentRien() {
        ValeursPainter peintre = new ValeursPainter(new FabriqueSimple());
        SurfaceEnregistreuse surface = new SurfaceEnregistreuse();
        Document document = Document.of("int total = 0;\n");
        EditorMetrics metrics =
                EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        VisualProjection projection =
                new VisualProjection(document, new FoldModel(), 0);
        FrameState etat = new FrameState();
        etat.beginFrame(document, MultiSelection.empty(), projection, 0L,
                metrics, THEME, 0, 2, 40f, 0f, 0f);

        peintre.paint(surface, etat);
        assertTrue(surface.dessins().isEmpty(), "sans valeur, rien ne se trace");
        peintre.setValeurs(List.of(new ValeurLigne(9, "hors = true")));
        peintre.paint(surface, etat);
        assertTrue(surface.dessins().isEmpty(),
                "la ligne hors document ne se trace pas");
        peintre.setValeurs(List.of(new ValeurLigne(0, "zero = false")));
        peintre.paint(surface, etat);
        assertTrue(surface.dessins().isEmpty(),
                "la ligne zéro (hors convention) ne se trace pas");
    }
}
