package jo.celeris.core.syntax;

import jo.celeris.core.syntax.structure.FoldSpan;
import jo.celeris.core.syntax.structure.TreeFolds;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;

/**
 * Les plis calculés depuis l'arbre (A-15, facette socle) : chaque bloc,
 * déclaration et commentaire multi-lignes devient une région de pli
 * (en-tête visible, lignes suivantes repliées) ; les régions sont triées
 * par ligne de début et ne descendent jamais sous deux lignes. Les plis
 * MANUELS — ceux que la vue conserve à travers les éditions — relèvent du
 * modèle de plis de la vue ; ici vit le calcul structurel qui les alimente.
 */
class PlisSocleTest {

    private final SyntaxService service = SyntaxService.of(MiniLangage.langage());

    /** Les blocs, déclarations et commentaires multi-lignes deviennent des plis. */
    @Test
    void blocsEtCommentairesDeviennentDesPlis() {
        String text = """
                fn grand {
                  a
                  b
                }
                /* commentaire
                   de deux lignes */
                var court ;
                """;
        Document doc = Document.of(text);
        List<FoldSpan> folds = TreeFolds.regions(doc, service.analyseFull(doc));
        assertEquals(2, folds.size(), "le bloc et le commentaire, pas le champ d'une ligne");
        assertEquals(0, folds.get(0).startLine());
        assertEquals(3, folds.get(0).endLine(), "le pli couvre jusqu'à l'accolade fermante");
        assertEquals(4, folds.get(1).startLine());
        assertEquals(5, folds.get(1).endLine());
        assertTrue(folds.get(0).startLine() < folds.get(0).endLine());
    }

    /** Les blocs imbriqués donnent des régions imbriquées, triées par début. */
    @Test
    void blocsImbriquesRegionsImbriquees() {
        String text = """
                fn ext {
                  fn milieu {
                    x
                  }
                  y
                }
                """;
        Document doc = Document.of(text);
        List<FoldSpan> folds = TreeFolds.regions(doc, service.analyseFull(doc));
        assertEquals(2, folds.size());
        assertEquals(0, folds.get(0).startLine());
        assertEquals(5, folds.get(0).endLine());
        assertEquals(1, folds.get(1).startLine());
        assertEquals(3, folds.get(1).endLine());
        assertTrue(folds.get(0).startLine() <= folds.get(1).startLine(),
                "triées par ligne de début");
    }

    /** Les plis d'une zone non éditée ne bougent pas quand une autre zone est éditée. */
    @Test
    void plisStablesSousEditionLointaine() {
        String text = """
                fn a {
                  x
                }
                fn b {
                  y
                }
                """;
        Document doc = Document.of(text);
        Analysis complet = service.analyseFull(doc);
        int target = text.indexOf("  y");
        Document edite = doc.replace(target, target + 3, "  zzz");
        List<FoldSpan> avant = TreeFolds.regions(doc, complet);
        List<FoldSpan> apres = TreeFolds.regions(edite,
                service.analyseIncremental(edite, complet,
                        EditSite.of(doc, edite, target, target + 3, "  zzz")));
        assertEquals(avant.size(), apres.size());
        assertEquals(avant.get(0), apres.get(0), "le plis du chunk non édité est identique");
        assertEquals(avant.get(1), apres.get(1));
    }

    /** Un chunk malformé produit quand même sa région, la preuve de tolérance. */
    @Test
    void chunkMalformeProduitSaRegion() {
        String text = "fn casse {\n  x\n";
        Document doc = Document.of(text);
        List<FoldSpan> folds = TreeFolds.regions(doc, service.analyseFull(doc));
        assertEquals(1, folds.size());
        assertEquals(0, folds.get(0).startLine());
        assertEquals(1, folds.get(0).endLine());
    }

    /** Les lignes seules ne se replient pas. */
    @Test
    void ligneSeuleNeSeRepliePas() {
        String text = "fn une { x }\nvar v ;\n";
        Document doc = Document.of(text);
        List<FoldSpan> folds = TreeFolds.regions(doc, service.analyseFull(doc));
        assertTrue(folds.isEmpty(), "aucune région de deux lignes ou plus");
    }
}
