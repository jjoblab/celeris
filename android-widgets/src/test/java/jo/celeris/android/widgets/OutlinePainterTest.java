package jo.celeris.android.widgets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.NodeCategory;
import jo.celeris.core.syntax.OutlineEntry;

/**
 * Le peintre du plan consommateur (B-16, tranche 8 M4) : une rangée par
 * entrée — glyphe de catégorie en primitives, nom en retrait d'un pas
 * par niveau d'imbrication — sans allocation par image (PIEGE-15) : le
 * peintre ne construit rien, il trace.
 */
class OutlinePainterTest {

    /** Surface enregistreuse : chaque appel capture son genre et sa position. */
    private static final class SurfaceEnregistreuse implements OutlineSurface {
        private final List<String> appels = new ArrayList<>();
        private final List<Float> abscissesTexte = new ArrayList<>();

        @Override
        public void drawText(String text, float x, float y, int couleur) {
            appels.add("texte:" + text);
            abscissesTexte.add(x);
        }

        @Override
        public void drawLine(float x1, float y1, float x2, float y2, int couleur) {
            appels.add("trait");
        }

        @Override
        public void drawRect(float left, float top, float right, float bottom, int couleur) {
            appels.add("rectangle");
        }

        @Override
        public void drawCircle(float cx, float cy, float rayon, int couleur) {
            appels.add("cercle");
        }
    }

    private static OutlineEntry entree(NodeCategory categorie, String nom,
            int debut, int fin) {
        return new OutlineEntry(categorie, nom, debut, fin, 0, 0);
    }

    /** Le glyphe suit la catégorie, le nom se retire d'un pas par niveau. */
    @Test
    void glypheParCategorieEtRetraitParNiveau() {
        OutlineModel modele = new OutlineModel();
        modele.appliquer(List.of(
                entree(NodeCategory.TYPE, "Exemple", 0, 100),
                entree(NodeCategory.FUNCTION, "premiere", 10, 90),
                entree(NodeCategory.FIELD, "champ", 20, 30)));

        SurfaceEnregistreuse surface = new SurfaceEnregistreuse();
        OutlinePainter peintre = new OutlinePainter(40f, 24f, 10f, 0xFF000000, 0xFF5F6368);
        peintre.dessiner(modele, surface);

        assertEquals(List.of("rectangle", "texte:Exemple",
                        "cercle", "texte:premiere",
                        "trait", "texte:champ"),
                surface.appels, "type en rectangle, fonction en cercle, champ en trait");
        assertTrue(surface.abscissesTexte.get(1) > surface.abscissesTexte.get(0),
                "la méthode imbriquée se retire d'un pas");
        assertTrue(surface.abscissesTexte.get(2) > surface.abscissesTexte.get(1),
                "le champ profond se retire encore");
    }

    /** Un plan vide ne trace rien ; la hauteur suit le nombre d'entrées. */
    @Test
    void planVideNeTraceRienEtHauteurSuitLesEntrees() {
        OutlineModel modele = new OutlineModel();
        SurfaceEnregistreuse surface = new SurfaceEnregistreuse();
        OutlinePainter peintre = new OutlinePainter(40f, 24f, 10f, 0xFF000000, 0xFF5F6368);

        peintre.dessiner(modele, surface);
        assertEquals(0, surface.appels.size(), "un plan vide ne trace rien");
        assertEquals(0f, peintre.hauteur(0), 0.001f, "hauteur nulle sans entrée");

        modele.appliquer(List.of(entree(NodeCategory.TYPE, "Exemple", 0, 100)));
        assertEquals(40f, peintre.hauteur(modele.nombre()), 0.001f,
                "une entrée, une hauteur de rangée");
    }

    /** Les catégories restantes ont leur glyphe, jamais de trou. */
    @Test
    void chaqueCategorieATraceSonGlyphe() {
        OutlineModel modele = new OutlineModel();
        modele.appliquer(List.of(
                entree(NodeCategory.TAG, "balise", 0, 10),
                entree(NodeCategory.BLOCK, "bloc", 20, 30),
                entree(NodeCategory.STATEMENT, "instruction", 40, 50),
                entree(NodeCategory.OTHER, "autre", 60, 70),
                entree(NodeCategory.ERROR, "rate", 80, 90)));

        SurfaceEnregistreuse surface = new SurfaceEnregistreuse();
        OutlinePainter peintre = new OutlinePainter(40f, 24f, 10f, 0xFF000000, 0xFF5F6368);
        peintre.dessiner(modele, surface);

        assertEquals(10, surface.appels.size(), "cinq entrées, un glyphe et un texte chacune");
        assertEquals("rectangle", surface.appels.get(0), "une balise en rectangle");
        assertTrue(surface.appels.contains("cercle"), "le reste trace son glyphe");
    }
}
