package jo.celeris.core.syntax;

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
        String texte = "fn grand {\n"
                + "  a\n"
                + "  b\n"
                + "}\n"
                + "/* commentaire\n"
                + "   de deux lignes */\n"
                + "var court ;\n";
        Document doc = Document.of(texte);
        List<FoldSpan> plis = TreeFolds.regions(doc, service.analyseFull(doc));
        assertEquals(2, plis.size(), "le bloc et le commentaire, pas le champ d'une ligne");
        assertEquals(0, plis.get(0).startLine());
        assertEquals(2, plis.get(0).endLine());
        assertEquals(4, plis.get(1).startLine());
        assertEquals(5, plis.get(1).endLine());
        assertTrue(plis.get(0).startLine() < plis.get(0).endLine());
    }

    /** Les blocs imbriqués donnent des régions imbriquées, triées par début. */
    @Test
    void blocsImbriquesRegionsImbriquees() {
        String texte = "fn ext {\n"
                + "  fn milieu {\n"
                + "    x\n"
                + "  }\n"
                + "  y\n"
                + "}\n";
        Document doc = Document.of(texte);
        List<FoldSpan> plis = TreeFolds.regions(doc, service.analyseFull(doc));
        assertEquals(2, plis.size());
        assertEquals(0, plis.get(0).startLine());
        assertEquals(5, plis.get(0).endLine());
        assertEquals(1, plis.get(1).startLine());
        assertEquals(3, plis.get(1).endLine());
        assertTrue(plis.get(0).startLine() <= plis.get(1).startLine(),
                "triées par ligne de début");
    }

    /** Les plis d'une zone non éditée ne bougent pas quand une autre zone est éditée. */
    @Test
    void plisStablesSousEditionLointaine() {
        String texte = "fn a {\n" + "  x\n" + "}\n" + "fn b {\n" + "  y\n" + "}\n";
        Document doc = Document.of(texte);
        Analysis complet = service.analyseFull(doc);
        int cible = texte.indexOf("  y");
        Document edite = doc.replace(cible, cible + 3, "  zzz");
        List<FoldSpan> avant = TreeFolds.regions(doc, complet);
        List<FoldSpan> apres = TreeFolds.regions(edite,
                service.analyseIncremental(edite, complet,
                        EditSite.of(doc, edite, cible, cible + 3, "  zzz")));
        assertEquals(avant.size(), apres.size());
        assertEquals(avant.get(0), apres.get(0), "le plis du chunk non édité est identique");
        assertEquals(avant.get(1), apres.get(1));
    }

    /** Un chunk malformé produit quand même sa région, la preuve de tolérance. */
    @Test
    void chunkMalformeProduitSaRegion() {
        String texte = "fn casse {\n" + "  x\n";
        Document doc = Document.of(texte);
        List<FoldSpan> plis = TreeFolds.regions(doc, service.analyseFull(doc));
        assertEquals(1, plis.size());
        assertEquals(0, plis.get(0).startLine());
        assertEquals(1, plis.get(0).endLine());
    }

    /** Les lignes seules ne se replient pas. */
    @Test
    void ligneSeuleNeSeRepliePas() {
        String texte = "fn une { x }\n" + "var v ;\n";
        Document doc = Document.of(texte);
        List<FoldSpan> plis = TreeFolds.regions(doc, service.analyseFull(doc));
        assertTrue(plis.isEmpty(), "aucune région de deux lignes ou plus");
    }
}
