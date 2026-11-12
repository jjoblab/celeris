package jo.celeris.core.syntax;

import jo.celeris.core.syntax.structure.FoldSpan;
import jo.celeris.core.syntax.structure.OutlineEntry;
import jo.celeris.core.syntax.structure.TreeDiagnostics;
import jo.celeris.core.syntax.structure.TreeFolds;
import jo.celeris.core.syntax.structure.TreeIndent;
import jo.celeris.core.syntax.structure.TreeOutline;
import jo.celeris.core.syntax.xml.XmlSyntax;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;

/**
 * L'analyseur XML tolérant (ADR-004) : chunks de haut niveau (prologue et
 * commentaires, puis l'élément racine), arbre d'éléments imbriqués, plan et
 * plis du même arbre, tolérance aux balises non fermées, et maintenance
 * incrémentale — une édition dans un élément ne ré-analyse que le chunk
 * propriétaire.
 */
class XmlArbreTest {

    private final SyntaxService service = SyntaxService.of(XmlSyntax.INSTANCE);

    /** Le plan liste les éléments, imbriqués dans l'ordre du source. */
    @Test
    void planDesElements() {
        Document doc = Document.of(CorpusChargeur.exempleXml());
        List<OutlineEntry> plan = TreeOutline.entries(doc, service.analyseFull(doc));
        assertTrue(plan.stream().anyMatch(e -> e.category() == NodeCategory.TAG
                && "catalogue".equals(e.name())));
        assertTrue(plan.stream().anyMatch(e -> e.category() == NodeCategory.TAG
                && "produit".equals(e.name())));
        assertTrue(plan.stream().anyMatch(e -> e.category() == NodeCategory.TAG
                && "description".equals(e.name())));
        assertTrue(plan.stream().anyMatch(e -> e.category() == NodeCategory.TAG
                && "tag".equals(e.name())));
        assertEquals(2, plan.stream()
                .filter(e -> "produit".equals(e.name())).count());
    }

    /** Deux chunks : prologue et commentaires, puis l'élément racine. */
    @Test
    void chunksDeHautNiveau() {
        Document doc = Document.of(CorpusChargeur.exempleXml());
        List<Chunk> chunks = service.analyseFull(doc).tree().chunks();
        assertEquals(2, chunks.size());
        assertEquals(0, chunks.get(0).startLine());
        assertEquals(2, chunks.get(1).startLine(),
                "l'élément racine ouvre son chunk à sa balise");
    }

    /** Les plis couvrent les éléments multi-lignes, pas les feuilles. */
    @Test
    void plisDesElements() {
        Document doc = Document.of(CorpusChargeur.exempleXml());
        List<FoldSpan> plis = TreeFolds.regions(doc, service.analyseFull(doc));
        FoldSpan premier = plis.get(0);
        assertEquals(0, premier.startLine(),
                "le prologue et son commentaire se replient ensemble");
        assertEquals(1, premier.endLine());
        FoldSpan racine = plis.get(1);
        assertEquals(2, racine.startLine(), "le pli de la racine s'ouvre à sa balise");
        assertEquals(21, racine.endLine(), "et couvre tout le document");
        assertTrue(plis.stream().anyMatch(p -> p.startLine() == 4 && p.endLine() == 10),
                "le premier produit se replie de sa balise à sa fermeture");
    }

    /** Une balise non fermée devient une erreur localisée. */
    @Test
    void baliseNonFermee() {
        String source = "<racine>\n    <enfant>\n";
        Document doc = Document.of(source);
        List<SyntaxIssue> problemes = TreeDiagnostics.issues(doc, service.analyseFull(doc));
        assertFalse(problemes.isEmpty());
        assertTrue(problemes.get(0).startOffset() >= 0);
        assertTrue(problemes.get(0).length() > 0);
    }

    /** Un source équilibré ne produit aucun diagnostic. */
    @Test
    void sourceSainSansDiagnostic() {
        Document doc = Document.of(CorpusChargeur.exempleXml());
        List<SyntaxIssue> problemes = TreeDiagnostics.issues(doc, service.analyseFull(doc));
        assertTrue(problemes.isEmpty());
    }

    /** L'auto-fermeture est une feuille, pas un bloc à replier. */
    @Test
    void autoFermetureFeuille() {
        String source = "<racine><feuille/></racine>";
        Document doc = Document.of(source);
        List<OutlineEntry> plan = TreeOutline.entries(doc, service.analyseFull(doc));
        assertEquals(2, plan.size(), "la racine et sa feuille, rien de plus");
        List<FoldSpan> plis = TreeFolds.regions(doc, service.analyseFull(doc));
        assertTrue(plis.isEmpty(), "une seule ligne : aucun pli");
    }

    /** Les niveaux d'indentation suivent l'imbrication des éléments. */
    @Test
    void niveauxIndentation() {
        Document doc = Document.of(CorpusChargeur.exempleXml());
        Analysis analyse = service.analyseFull(doc);
        assertEquals(0, TreeIndent.levelAt(doc, analyse, 0));
        assertEquals(0, TreeIndent.levelAt(doc, analyse, 2), "la racine ouvre, elle n'est pas imbriquée");
        assertEquals(1, TreeIndent.levelAt(doc, analyse, 3));
        assertEquals(2, TreeIndent.levelAt(doc, analyse, 5));
        assertEquals(3, TreeIndent.levelAt(doc, analyse, 15), "les balises tag sont à trois niveaux");
    }

    /** Une édition dans un élément ne ré-analyse que le chunk racine. */
    @Test
    void editionDansUnElement() {
        String source = CorpusChargeur.exempleXml();
        Document doc = Document.of(source);
        Analysis complet = service.analyseFull(doc);
        int cible = source.indexOf("électronique");
        assertTrue(cible > 0);
        Document edite = doc.replace(cible, cible + 12, "numerique");
        Analysis diff = service.analyseIncremental(edite, complet,
                EditSite.of(doc, edite, cible, cible + 12, "numerique"));
        Arbres.verifierEquivalence(diff.tree(),
                service.analyseFull(edite).tree(), edite);
    }
}
