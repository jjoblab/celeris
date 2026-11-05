package jo.celeris.core.syntax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;

/**
 * L'analyseur JSON tolérant (ADR-004) : chaque valeur de premier
 * niveau ouvre son chunk (un document à valeur unique n'en a qu'un),
 * objets et tableaux en nœuds repliables, paires
 * clé-valeur au plan, tolérance aux crochets non fermés, et maintenance
 * incrémentale égale à la relecture complète.
 */
class JsonArbreTest {

    private final SyntaxService service = SyntaxService.of(JsonSyntax.INSTANCE);

    /** Le plan liste les clés, imbriquées comprises. */
    @Test
    void planDesCles() {
        Document doc = Document.of(CorpusChargeur.exempleJson());
        List<OutlineEntry> plan = TreeOutline.entries(doc, service.analyseFull(doc));
        assertTrue(plan.stream().anyMatch(e -> e.category() == NodeCategory.FIELD
                && "nom".equals(e.name())));
        assertTrue(plan.stream().anyMatch(e -> e.category() == NodeCategory.FIELD
                && "modules".equals(e.name())));
        assertTrue(plan.stream().anyMatch(e -> e.category() == NodeCategory.FIELD
                && "couverture".equals(e.name())), "les clés imbriquées sont au plan");
        assertTrue(plan.stream().anyMatch(e -> e.category() == NodeCategory.FIELD
                && "notes".equals(e.name())));
    }

    /** Un document à valeur unique vit en un chunk unique. */
    @Test
    void chunkUnique() {
        Document doc = Document.of(CorpusChargeur.exempleJson());
        List<Chunk> chunks = service.analyseFull(doc).tree().chunks();
        assertEquals(1, chunks.size());
        assertEquals(0, chunks.get(0).startLine());
    }

    /** Chaque valeur de premier niveau ouvre son chunk (PIEGE-13). */
    @Test
    void chunksParValeurDePremierNiveau() {
        String source = "{\"a\": 1}\n\n{\"b\": [2, 3]}\n\n42\n";
        Document doc = Document.of(source);
        List<Chunk> chunks = service.analyseFull(doc).tree().chunks();
        assertEquals(3, chunks.size(), "objet, objet, scalaire");
        assertEquals(0, chunks.get(0).startLine());
        assertEquals(2, chunks.get(1).startLine(), "la ligne vide appartient au précédent");
        assertEquals(4, chunks.get(2).startLine());
    }

    /** Les objets et tableaux multi-lignes se replient. */
    @Test
    void plisDesContenants() {
        Document doc = Document.of(CorpusChargeur.exempleJson());
        List<FoldSpan> plis = TreeFolds.regions(doc, service.analyseFull(doc));
        assertEquals(3, plis.size(), "la racine, les modules, les seuils");
        assertEquals(0, plis.get(0).startLine());
        assertEquals(16, plis.get(0).endLine());
        assertTrue(plis.stream().anyMatch(p -> p.startLine() == 3 && p.endLine() == 7),
                "le tableau des modules se replie");
    }

    /** Un crochet non fermé devient une erreur localisée. */
    @Test
    void crochetNonFerme() {
        String source = "{\"a\": [1, 2";
        Document doc = Document.of(source);
        List<SyntaxIssue> problemes = TreeDiagnostics.issues(doc, service.analyseFull(doc));
        assertFalse(problemes.isEmpty());
        assertTrue(problemes.get(0).startOffset() >= 0);
        assertTrue(problemes.get(0).length() > 0);
    }

    /** Un source équilibré ne produit aucun diagnostic. */
    @Test
    void sourceSainSansDiagnostic() {
        Document doc = Document.of(CorpusChargeur.exempleJson());
        List<SyntaxIssue> problemes = TreeDiagnostics.issues(doc, service.analyseFull(doc));
        assertTrue(problemes.isEmpty());
    }

    /** Une édition dans une valeur ne ré-analyse que par incrément, à l'identique. */
    @Test
    void editionIncrementale() {
        String source = CorpusChargeur.exempleJson();
        Document doc = Document.of(source);
        Analysis complet = service.analyseFull(doc);
        int cible = source.indexOf("Celeris");
        assertTrue(cible > 0);
        Document edite = doc.replace(cible, cible + 7, "CelerisX");
        Analysis diff = service.analyseIncremental(edite, complet,
                EditSite.of(doc, edite, cible, cible + 7, "CelerisX"));
        Arbres.verifierEquivalence(diff.tree(),
                service.analyseFull(edite).tree(), edite);
    }

    /** Les niveaux d'indentation suivent l'imbrication des contenants. */
    @Test
    void niveauxIndentation() {
        Document doc = Document.of(CorpusChargeur.exempleJson());
        Analysis analyse = service.analyseFull(doc);
        assertEquals(1, TreeIndent.levelAt(doc, analyse, 1), "une paire dans la racine");
        assertEquals(3, TreeIndent.levelAt(doc, analyse, 4),
                "un module dans la paire, dans son tableau");
        assertEquals(3, TreeIndent.levelAt(doc, analyse, 9),
                "un seuil dans la paire, dans son objet");
        assertEquals(2, TreeIndent.levelAt(doc, analyse, 16), "la fermeture porte un cran de plus");
    }
}
