package jo.celeris.core.syntax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;

/**
 * L'analyseur Kotlin tolérant (ADR-004) : chunks de haut niveau (paquet et
 * imports, puis chaque déclaration avec sa documentation), fonctions,
 * classes, objets et propriétés au plan, plis des corps, tolérance aux
 * accolades manquantes, et maintenance incrémentale égale à la relecture.
 */
class KotlinArbreTest {

    private final SyntaxService service = SyntaxService.of(KotlinSyntax.INSTANCE);

    /** Le plan liste fonctions, classes, objets et propriétés. */
    @Test
    void planDuFichier() {
        Document doc = Document.of(CorpusChargeur.exempleKotlin());
        List<OutlineEntry> plan = TreeOutline.entries(doc, service.analyseFull(doc));
        assertTrue(plan.stream().anyMatch(e -> e.category() == NodeCategory.TYPE
                && "Point".equals(e.name())));
        assertTrue(plan.stream().anyMatch(e -> e.category() == NodeCategory.FUNCTION
                && "norme".equals(e.name())));
        assertTrue(plan.stream().anyMatch(e -> e.category() == NodeCategory.TYPE
                && "Catalogue".equals(e.name())));
        assertTrue(plan.stream().anyMatch(e -> e.category() == NodeCategory.FUNCTION
                && "additionne".equals(e.name())));
        assertTrue(plan.stream().anyMatch(e -> e.category() == NodeCategory.FUNCTION
                && "decrit".equals(e.name())));
        assertTrue(plan.stream().anyMatch(e -> e.category() == NodeCategory.TYPE
                && "Utilitaires".equals(e.name())));
        assertTrue(plan.stream().anyMatch(e -> e.category() == NodeCategory.FIELD
                && "SEUIL_PUBLIC".equals(e.name())),
                "la propriété de premier niveau est au plan");
        assertTrue(plan.stream().anyMatch(e -> e.category() == NodeCategory.FUNCTION
                && "reindexe".equals(e.name())));
    }

    /** Six chunks : paquet et imports, puis chaque déclaration. */
    @Test
    void chunksDeHautNiveau() {
        Document doc = Document.of(CorpusChargeur.exempleKotlin());
        List<Chunk> chunks = service.analyseFull(doc).tree().chunks();
        assertEquals(6, chunks.size());
        assertEquals(0, chunks.get(0).startLine());
        assertEquals(5, chunks.get(1).startLine(),
                "la classe de données ouvre son chunk à sa documentation");
        assertEquals(16, chunks.get(2).startLine());
    }

    /** Les plis couvrent classes, fonctions et blocs. */
    @Test
    void plisDesCorps() {
        Document doc = Document.of(CorpusChargeur.exempleKotlin());
        List<FoldSpan> plis = TreeFolds.regions(doc, service.analyseFull(doc));
        assertTrue(plis.stream().anyMatch(p -> p.startLine() == 10 && p.endLine() == 14),
                "la classe de données se replie");
        assertTrue(plis.stream().anyMatch(p -> p.startLine() == 16 && p.endLine() == 49),
                "la classe catalogue se replie de sa déclaration à sa fermeture");
        assertTrue(plis.stream().anyMatch(p -> p.startLine() == 34 && p.endLine() == 40),
                "la fonction à chaîne brute se replie");
    }

    /** Une accolade fermante manquante devient une erreur localisée. */
    @Test
    void accoladeManquante() {
        String source = "fun marche() {\n    val x = 1\n";
        Document doc = Document.of(source);
        List<SyntaxIssue> problemes = TreeDiagnostics.issues(doc, service.analyseFull(doc));
        assertFalse(problemes.isEmpty());
        assertTrue(problemes.get(0).startOffset() >= 0);
        assertTrue(problemes.get(0).length() > 0);
    }

    /** Un source sain ne produit aucun diagnostic. */
    @Test
    void sourceSainSansDiagnostic() {
        Document doc = Document.of(CorpusChargeur.exempleKotlin());
        List<SyntaxIssue> problemes = TreeDiagnostics.issues(doc, service.analyseFull(doc));
        assertTrue(problemes.isEmpty());
    }

    /** Les niveaux d'indentation suivent les blocs de l'arbre. */
    @Test
    void niveauxIndentation() {
        Document doc = Document.of(CorpusChargeur.exempleKotlin());
        Analysis analyse = service.analyseFull(doc);
        assertEquals(1, TreeIndent.levelAt(doc, analyse, 11), "norme dans Point");
        assertEquals(2, TreeIndent.levelAt(doc, analyse, 12), "son corps");
        assertEquals(3, TreeIndent.levelAt(doc, analyse, 44),
                "les branches de when sont à trois niveaux");
    }

    /** Une édition dans une fonction ne ré-analyse que le chunk propriétaire. */
    @Test
    void editionDansUneFonction() {
        String source = CorpusChargeur.exempleKotlin();
        Document doc = Document.of(source);
        Analysis complet = service.analyseFull(doc);
        int cible = source.indexOf("somme += v");
        assertTrue(cible > 0);
        Document edite = doc.replace(cible + 7, cible + 8, "w");
        Analysis diff = service.analyseIncremental(edite, complet,
                EditSite.of(doc, edite, cible + 7, cible + 8, "w"));
        Arbres.verifierEquivalence(diff.tree(),
                service.analyseFull(edite).tree(), edite);
    }
}
