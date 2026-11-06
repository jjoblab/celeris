package jo.celeris.android.widgets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.EditSite;
import jo.celeris.core.syntax.JavaSyntax;
import jo.celeris.core.syntax.NodeCategory;
import jo.celeris.core.syntax.OutlineEntry;
import jo.celeris.core.syntax.SyntaxService;
import jo.celeris.core.syntax.TreeOutline;
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
            package demo;

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

    private static OutlineEntry entree(NodeCategory categorie, String nom,
            int debut, int fin, int ligne) {
        return new OutlineEntry(categorie, nom, debut, fin, ligne, ligne);
    }

    /** Les profondeurs se déduisent de l'imbrication des bornes. */
    @Test
    void profondeursDeduitesDeLimbrication() {
        OutlineModel modele = new OutlineModel();
        modele.appliquer(List.of(
                entree(NodeCategory.TYPE, "Exemple", 0, 100, 0),
                entree(NodeCategory.FIELD, "champ", 20, 30, 1),
                entree(NodeCategory.FUNCTION, "premiere", 40, 60, 2),
                entree(NodeCategory.FUNCTION, "sibling", 70, 90, 3),
                entree(NodeCategory.TYPE, "Autre", 120, 180, 5)));

        List<OutlineModel.Ligne> lignes = modele.lignes();
        assertEquals(5, lignes.size());
        assertEquals(0, lignes.get(0).profondeur(), "la classe racine est à plat");
        assertEquals(1, lignes.get(1).profondeur(), "le champ vit dans la classe");
        assertEquals(1, lignes.get(2).profondeur(), "la méthode vit dans la classe");
        assertEquals(1, lignes.get(3).profondeur(), "la sœur aussi");
        assertEquals(0, lignes.get(4).profondeur(), "la classe suivante retourne à plat");
    }

    /** Le préfixe intact d'un plan à l'autre se conserve (mise à jour incrémentale). */
    @Test
    void miseAJourConserveLePrefixeIntact() {
        OutlineModel modele = new OutlineModel();
        OutlineChange premier = modele.appliquer(List.of(
                entree(NodeCategory.TYPE, "Exemple", 0, 100, 0),
                entree(NodeCategory.FUNCTION, "premiere", 40, 60, 2)));
        assertEquals(new OutlineChange(0, 0, 2), premier, "première application : tout est ajouté");

        OutlineChange renommee = modele.appliquer(List.of(
                entree(NodeCategory.TYPE, "Exemple", 0, 100, 0),
                entree(NodeCategory.FUNCTION, "renommee", 40, 60, 2)));
        assertEquals(new OutlineChange(1, 1, 1), renommee,
                "le préfixe intact se conserve, la fin remplace");

        OutlineChange ajoutee = modele.appliquer(List.of(
                entree(NodeCategory.TYPE, "Exemple", 0, 100, 0),
                entree(NodeCategory.FUNCTION, "renommee", 40, 60, 2),
                entree(NodeCategory.FUNCTION, "ajoutee", 120, 140, 5)));
        assertEquals(new OutlineChange(2, 0, 1), ajoutee, "l'ajout en queue ne remplace rien");
        assertEquals(3, modele.nombre());
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

        OutlineModel modele = new OutlineModel();
        modele.appliquer(planAvant);
        assertEquals("Premiere", modele.lignes().get(0).entree().name());

        int cible = SOURCE.indexOf("autre");
        Document edite = doc.replace(cible, cible + 5, "derniere");
        EditSite site = EditSite.of(doc, edite, cible, cible + 5, "derniere");
        Analysis diff = service.analyseIncremental(edite, complet, site);
        List<OutlineEntry> planApres = TreeOutline.entries(edite, diff);

        OutlineChange changement = modele.appliquer(planApres);
        assertTrue(changement.prefixeConserve() >= 2,
                "les déclarations d'avant le site d'édition sont conservées");
        assertEquals("Premiere", modele.lignes().get(0).entree().name());
        assertTrue(modele.lignes().stream()
                .anyMatch(l -> "derniere".equals(l.entree().name())),
                "le renommé figure au plan mis à jour");
    }

    /** Le test au point résout la ligne visée, null hors bornes. */
    @Test
    void auPointResoutLaLigneVisee() {
        OutlineModel modele = new OutlineModel();
        List<OutlineEntry> entrees = List.of(
                entree(NodeCategory.TYPE, "Exemple", 0, 100, 0),
                entree(NodeCategory.FUNCTION, "premiere", 40, 60, 2));
        modele.appliquer(entrees);

        assertSame(entrees.get(0), modele.auPoint(0f, 40f), "la première rangée");
        assertSame(entrees.get(0), modele.auPoint(39.9f, 40f), "encore la première");
        assertSame(entrees.get(1), modele.auPoint(40f, 40f), "la deuxième rangée");
        assertNull(modele.auPoint(80f, 40f), "au-delà du plan : rien");
        assertNull(modele.auPoint(10f, 0f), "hauteur nulle : rien");
    }
}
