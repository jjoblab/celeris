package jo.celeris.core.syntax.grammars;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.FoldSpan;
import jo.celeris.core.syntax.SyntaxService;
import jo.celeris.core.syntax.TreeFolds;
import jo.celeris.core.text.Document;

/**
 * Le parseur générique de la voie de grammaires (ADR-004) : les chunks
 * se découpent au premier jeton de chaque ligne de premier niveau — la
 * ré-analyse reste locale — et chaque paire de crochets appariés qui
 * traverse les lignes devient une région de pli, consommée du même
 * arbre que la coloration (C-15/C-16).
 */
class GrammaireParseurTest {

    /** Les chunks suivent les lignes de premier niveau. */
    @Test
    void chunksParLignesDePremierNiveau() {
        SyntaxService service = SyntaxService.of(LangagesGrammaires.python());
        Analysis analyse = service.analyseFull(Document.of("""
                x = 1
                y = [
                    2,
                    3,
                ]
                z = x + y[0]
                """));
        assertEquals(3, analyse.tree().chunks().size(),
                "trois lignes de premier niveau, trois chunks — le tableau imbriqué n'en ouvre pas");
        assertTrue(analyse.estComplet(), "l'arbre est complet");
    }

    /** La paire de crochets multi-lignes devient une région de pli. */
    @Test
    void paireMultiLignesEnRegionDePli() {
        SyntaxService service = SyntaxService.of(LangagesGrammaires.python());
        Document doc = Document.of("""
                valeurs = [
                    1,
                    2,
                ]
                """);
        Analysis analyse = service.analyseFull(doc);
        List<FoldSpan> regions = TreeFolds.regions(doc, analyse);
        assertEquals(1, regions.size(), "une région : le tableau multi-lignes");
        assertEquals(0, regions.get(0).startLine(), "l'en-tête ouvre à la ligne du crochet");
        assertEquals(3, regions.get(0).endLine(), "la fermeture referme à sa ligne");
    }

    /** Les crochets fermés dans la ligne ne plient rien. */
    @Test
    void paireMonoLigneNePlieRien() {
        SyntaxService service = SyntaxService.of(LangagesGrammaires.python());
        Document doc = Document.of("valeurs = [1, 2]\n");
        Analysis analyse = service.analyseFull(doc);
        assertTrue(TreeFolds.regions(doc, analyse).isEmpty(),
                "un tableau d'une ligne ne se plie pas");
    }

    /** Le crochet non fermé se tolère : aucune région, jamais d'exception. */
    @Test
    void crochetNonFermeTolere() {
        SyntaxService service = SyntaxService.of(LangagesGrammaires.toml());
        Document doc = Document.of("""
                palette = [
                "clair",
                "sombre",
                """);
        Analysis analyse = service.analyseFull(doc);
        assertFalse(analyse.estComplet(), "le document reste analysé");
        assertTrue(TreeFolds.regions(doc, analyse).isEmpty(),
                "le crochet non fermé n'ouvre aucune région");
    }

    /** La clôture de code du balisage léger se replie d'un pli. */
    @Test
    void clotureDeCodeRepliee() {
        SyntaxService service = SyntaxService.of(LangagesGrammaires.markdown());
        Document doc = Document.of("""
            # Titre

            ```java
            class Exemple {
            }
            ```

            Texte après.
            """);
        Analysis analyse = service.analyseFull(doc);
        List<FoldSpan> regions = TreeFolds.regions(doc, analyse);
        assertEquals(1, regions.size(), "la clôture de code se replie");
        assertEquals(2, regions.get(0).startLine(), "ouverte à la ligne de clôture");
        assertEquals(5, regions.get(0).endLine(), "refermée à sa fermeture");
    }
}
