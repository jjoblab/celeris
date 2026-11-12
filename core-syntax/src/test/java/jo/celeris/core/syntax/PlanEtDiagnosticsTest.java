package jo.celeris.core.syntax;

import jo.celeris.core.syntax.structure.OutlineEntry;
import jo.celeris.core.syntax.structure.TreeDiagnostics;
import jo.celeris.core.syntax.structure.TreeOutline;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;

/**
 * Le plan du fichier (B-16, facette socle) : les entrées sortent du MÊME
 * arbre que la coloration — déclarations avec nom, catégorie et bornes
 * absolues — et se recalculent par révision. Les diagnostics (D-02) :
 * chaque nœud d'erreur devient une plage source localisée, prête pour la
 * gouttière et les soulignements.
 */
class PlanEtDiagnosticsTest {

    private final SyntaxService service = SyntaxService.of(MiniLangage.langage());

    /** Le plan liste les déclarations avec nom, catégorie et lignes. */
    @Test
    void planListeLesDeclarations() {
        String texte = """
                fn premier {
                  x
                }
                var seul ;
                fn deuxieme {
                  y
                }
                """;
        Document doc = Document.of(texte);
        List<OutlineEntry> plan = TreeOutline.entries(doc, service.analyseFull(doc));
        assertEquals(3, plan.size());
        assertEquals(NodeCategory.FUNCTION, plan.get(0).category());
        assertEquals("premier", plan.get(0).name());
        assertEquals(0, plan.get(0).startLine());
        assertEquals(2, plan.get(0).endLine());
        assertEquals(NodeCategory.FIELD, plan.get(1).category());
        assertEquals("seul", plan.get(1).name());
        assertEquals(3, plan.get(1).startLine());
        assertEquals(NodeCategory.FUNCTION, plan.get(2).category());
        assertEquals("deuxieme", plan.get(2).name());
        assertEquals(4, plan.get(2).startLine());
        assertTrue(plan.get(2).startOffset() < plan.get(2).endOffset());
    }

    /** Le plan suit les déplacements : insertion de lignes en amont. */
    @Test
    void planSuitLesDecalages() {
        String texte = "fn a { x }\nfn b { y }\n";
        Document doc = Document.of(texte);
        Analysis complet = service.analyseFull(doc);
        int cible = texte.indexOf("fn a");
        Document edite = doc.replace(cible, cible, "n1\nn2\nn3\n");
        List<OutlineEntry> plan = TreeOutline.entries(edite,
                service.analyseIncremental(edite, complet,
                        EditSite.of(doc, edite, cible, cible, "n1\nn2\nn3\n")));
        assertEquals(2, plan.size());
        assertEquals("a", plan.get(0).name());
        assertEquals(3, plan.get(0).startLine(), "les trois lignes insérées décalent tout");
        assertEquals("b", plan.get(1).name());
        assertEquals(4, plan.get(1).startLine());
    }

    /** Une déclaration ajoutée entre deux autres apparaît au plan. */
    @Test
    void declarationAjouteeApparait() {
        String texte = "fn a { x }\nfn c { y }\n";
        Document doc = Document.of(texte);
        Analysis complet = service.analyseFull(doc);
        int cible = texte.indexOf("fn c");
        Document edite = doc.replace(cible, cible, "fn b { z }\n");
        List<OutlineEntry> plan = TreeOutline.entries(edite,
                service.analyseIncremental(edite, complet,
                        EditSite.of(doc, edite, cible, cible, "fn b { z }\n")));
        assertEquals(3, plan.size());
        assertEquals("a", plan.get(0).name());
        assertEquals("b", plan.get(1).name());
        assertEquals("c", plan.get(2).name());
    }

    /** Les erreurs de l'arbre deviennent des plages localisées, triées. */
    @Test
    void erreursDeviennentPlagesLocalisees() {
        String texte = "fn casse {\n  x\n";
        Document doc = Document.of(texte);
        List<SyntaxIssue> problemes = TreeDiagnostics.issues(doc, service.analyseFull(doc));
        assertEquals(1, problemes.size(), "un seul chunk malformé");
        SyntaxIssue probleme = problemes.get(0);
        assertTrue(probleme.startOffset() >= 0);
        assertTrue(probleme.length() > 0);
        assertTrue(probleme.message() != null && !probleme.message().isEmpty());
        assertTrue(probleme.startOffset() < doc.length());
    }

    /** Un source sain ne produit aucun diagnostic. */
    @Test
    void sourceSainSansDiagnostics() {
        String texte = "fn a {\n  x\n}\nvar v ;\n";
        Document doc = Document.of(texte);
        List<SyntaxIssue> problemes = TreeDiagnostics.issues(doc, service.analyseFull(doc));
        assertTrue(problemes.isEmpty());
    }
}
