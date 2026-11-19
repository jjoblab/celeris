package jo.celeris.android.widgets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.EditSite;
import jo.celeris.core.syntax.clike.JavaSyntax;
import jo.celeris.core.syntax.NodeCategory;
import jo.celeris.core.syntax.structure.OutlineEntry;
import jo.celeris.core.syntax.SyntaxService;
import jo.celeris.core.syntax.structure.TreeOutline;
import jo.celeris.core.text.Document;

/**
 * Le modèle du plan consommateur (B-16, tranche 8 M4) : les entrées
 * sortent du même arbre que la coloration, la profondeur d'affichage se
 * déduit de l'imbrication des bornes, et la mise à jour est
 * incrémentale — le préfixe intact d'un plan à l'autre se conserve, le
 * consommateur ne reçoit que le compte de ce qui change.
 */
class OutlineModelTest {

    private static final String SOURCE = """
            package exemple;

            class Premiere {
                int champ;

                void methode() {
                }
            }

            class Deuxieme {
                void autre() {
                }
            }
            """;

    private static OutlineEntry input(NodeCategory categorie, String nom,
            int start, int end, int line) {
        return new OutlineEntry(categorie, nom, start, end, line, line);
    }

    /** Les profondeurs se déduisent de l'imbrication des bornes. */
    @Test
    void profondeursDeduitesDeLimbrication() {
        OutlineModel model = new OutlineModel();
        model.appliquer(List.of(
                input(NodeCategory.TYPE, "Exemple", 0, 100, 0),
                input(NodeCategory.FIELD, "champ", 20, 30, 1),
                input(NodeCategory.FUNCTION, "premiere", 40, 60, 2),
                input(NodeCategory.FUNCTION, "sibling", 70, 90, 3),
                input(NodeCategory.TYPE, "Autre", 120, 180, 5)));

        List<OutlineModel.Row> lines = model.lines();
        assertEquals(5, lines.size());
        assertEquals(0, lines.get(0).profondeur(), "la classe racine est à plat");
        assertEquals(1, lines.get(1).profondeur(), "le champ vit dans la classe");
        assertEquals(1, lines.get(2).profondeur(), "la méthode vit dans la classe");
        assertEquals(1, lines.get(3).profondeur(), "la sœur aussi");
        assertEquals(0, lines.get(4).profondeur(), "la classe suivante retourne à plat");
    }

    /** Le préfixe intact d'un plan à l'autre se conserve (mise à jour incrémentale). */
    @Test
    void miseAJourConserveLePrefixeIntact() {
        OutlineModel model = new OutlineModel();
        OutlineChange premier = model.appliquer(List.of(
                input(NodeCategory.TYPE, "Exemple", 0, 100, 0),
                input(NodeCategory.FUNCTION, "premiere", 40, 60, 2)));
        assertEquals(new OutlineChange(0, 0, 2), premier, "première application : tout est ajouté");

        OutlineChange renommee = model.appliquer(List.of(
                input(NodeCategory.TYPE, "Exemple", 0, 100, 0),
                input(NodeCategory.FUNCTION, "renommee", 40, 60, 2)));
        assertEquals(new OutlineChange(1, 1, 1), renommee,
                "le préfixe intact se conserve, la fin remplace");

        OutlineChange ajoutee = model.appliquer(List.of(
                input(NodeCategory.TYPE, "Exemple", 0, 100, 0),
                input(NodeCategory.FUNCTION, "renommee", 40, 60, 2),
                input(NodeCategory.FUNCTION, "ajoutee", 120, 140, 5)));
        assertEquals(new OutlineChange(2, 0, 1), ajoutee, "l'ajout en queue ne remplace rien");
        assertEquals(3, model.nombre());
    }

    /**
     * Le modèle consomme le vrai plan du socle : l'analyse du même
     * instantané, l'édition incrémentale, les entrées recomposées — le
     * préfixe avant le site d'édition se conserve.
     */
    @Test
    void consommeLePlanDuSocleEtLeMetAJourIncidentiellement() {
        SyntaxService service = SyntaxService.of(JavaSyntax.INSTANCE);
        Document doc = Document.of(SOURCE);
        Analysis complet = service.analyseFull(doc);
        List<OutlineEntry> planAvant = TreeOutline.entries(doc, complet);
        assertTrue(planAvant.size() >= 4, "deux classes, un champ, deux méthodes");

        OutlineModel model = new OutlineModel();
        model.appliquer(planAvant);
        assertEquals("Premiere", model.lines().get(0).input().name());

        int target = SOURCE.indexOf("autre");
        Document edite = doc.replace(target, target + 5, "derniere");
        EditSite site = EditSite.of(doc, edite, target, target + 5, "derniere");
        Analysis diff = service.analyseIncremental(edite, complet, site);
        List<OutlineEntry> planApres = TreeOutline.entries(edite, diff);

        OutlineChange changement = model.appliquer(planApres);
        assertTrue(changement.prefixeConserve() >= 2,
                "les déclarations d'avant le site d'édition sont conservées");
        assertEquals("Premiere", model.lines().get(0).input().name());
        assertTrue(model.lines().stream()
                .anyMatch(l -> "derniere".equals(l.input().name())),
                "le renommé figure au plan mis à jour");
    }

    /** Le test au point résout la ligne visée, null hors bornes. */
    @Test
    void auPointResoutLaLigneVisee() {
        OutlineModel model = new OutlineModel();
        List<OutlineEntry> entrees = List.of(
                input(NodeCategory.TYPE, "Exemple", 0, 100, 0),
                input(NodeCategory.FUNCTION, "premiere", 40, 60, 2));
        model.appliquer(entrees);

        assertSame(entrees.get(0), model.auPoint(0f, 40f), "la première rangée");
        assertSame(entrees.get(0), model.auPoint(39.9f, 40f), "encore la première");
        assertSame(entrees.get(1), model.auPoint(40f, 40f), "la deuxième rangée");
        assertNull(model.auPoint(80f, 40f), "au-delà du plan : rien");
        assertNull(model.auPoint(10f, 0f), "hauteur nulle : rien");
    }
}
