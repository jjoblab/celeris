package jo.celeris.core.syntax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;

/**
 * Le dialecte Gradle KTS (ADR-004) : le même lexeur et le même parseur
 * que Kotlin — seul l'identifiant change, le registre des langages
 * distingue build.gradle.kts de build.gradle par l'extension composée.
 */
class KtsArbreTest {

    private final SyntaxService service = SyntaxService.of(KtsSyntax.INSTANCE);

    /** Le dialecte porte son identifiant propre. */
    @Test
    void identifiant() {
        assertEquals("kotlin", KotlinSyntax.INSTANCE.id());
        assertEquals("kts", KtsSyntax.INSTANCE.id());
    }

    /** Les blocs du script se replient — un seul chunk, pas de déclaration. */
    @Test
    void blocsDuScript() {
        Document doc = Document.of(CorpusChargeur.exempleKts());
        List<Chunk> chunks = service.analyseFull(doc).tree().chunks();
        assertEquals(1, chunks.size(), "un script sans déclaration tient en un chunk");
        List<FoldSpan> plis = TreeFolds.regions(doc, service.analyseFull(doc));
        assertEquals(5, plis.size(), "plugins, repositories, dependencies, tâche, doLast");
        assertEquals(0, plis.get(0).startLine());
        assertEquals(2, plis.get(0).endLine());
        assertTrue(TreeOutline.entries(doc, service.analyseFull(doc)).isEmpty(),
                "aucune déclaration nommée dans un script");
        assertTrue(TreeDiagnostics.issues(doc, service.analyseFull(doc)).isEmpty());
    }
}
