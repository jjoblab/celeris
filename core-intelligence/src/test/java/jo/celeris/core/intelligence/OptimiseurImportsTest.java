package jo.celeris.core.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.JavaSyntax;
import jo.celeris.core.syntax.SyntaxService;
import jo.celeris.core.text.Document;

/**
 * L'optimisation des imports (B-27) : chaque import est rapporté utilisé
 * ou non — SIGNALÉ, jamais supprimé en silence. Le sort d'un import
 * appartient à l'hôte et à l'utilisateur, pas à l'analyseur.
 */
class OptimiseurImportsTest {

    private final SyntaxService service = SyntaxService.of(JavaSyntax.INSTANCE);

    private List<OptimiseurImports.RapportImport> analyser(String texte) {
        Document doc = Document.of(texte);
        Analysis analyse = service.analyseFull(doc);
        return OptimiseurImports.analyser(doc, analyse);
    }

    /** L'import utilisé reste, l'import inutilisé est signalé — tous deux rapportés. */
    @Test
    void lutiliseEtLinutilise() {
        List<OptimiseurImports.RapportImport> rapports = analyser("""
                package demo;

                import java.util.List;
                import java.util.Map;

                class A {
                    List<String> noms;
                }
                """);
        assertEquals(2, rapports.size());
        assertEquals("import java.util.List;", rapports.get(0).importComplet());
        assertEquals(2, rapports.get(0).ligne());
        assertTrue(rapports.get(0).utilise());
        assertEquals("import java.util.Map;", rapports.get(1).importComplet());
        assertTrue(!rapports.get(1).utilise());
    }

    /** L'usage compte dans le code, pas dans les chaînes ni les commentaires. */
    @Test
    void lusageCompteDansLeCode() {
        List<OptimiseurImports.RapportImport> rapports = analyser("""
                import java.util.List;

                class A {
                    // List dans un commentaire
                    String s = "List dans une chaine";
                }
                """);
        assertTrue(!rapports.get(0).utilise(),
                "ni le commentaire ni la chaine ne sauvent l'import");
    }

    /** L'import statique : le nom simple est le dernier segment. */
    @Test
    void limportStatique() {
        List<OptimiseurImports.RapportImport> rapports = analyser("""
                import static java.util.Objects.requireNonNull;

                class A {
                    Object o = requireNonNull(null);
                }
                """);
        assertEquals(1, rapports.size());
        assertTrue(rapports.get(0).utilise());
    }

    /** Sans import : aucun rapport, pas d'exception. */
    @Test
    void sansImport() {
        assertTrue(analyser("class A {}\n").isEmpty());
        assertTrue(analyser("").isEmpty());
    }

    /** Le signalement ne mute jamais le document — rien n'est supprimé en silence. */
    @Test
    void leSignalementNeMuteRien() {
        String texte = "import java.util.Map;\n\nclass A {}\n";
        Document doc = Document.of(texte);
        Analysis analyse = service.analyseFull(doc);
        List<OptimiseurImports.RapportImport> rapports = OptimiseurImports.analyser(doc, analyse);
        assertEquals(1, rapports.size());
        assertTrue(!rapports.get(0).utilise());
        // le document est intact : l'optimiseur ne décide pas à la place de l'hôte
        assertEquals(texte, doc.text());
    }

    /** Les lignes d'édition attendues : le rapport donne la ligne du plan, zéro pour la première. */
    @Test
    void lesLignesDuRapport() {
        List<OptimiseurImports.RapportImport> rapports = analyser("""
                import java.util.List;
                import java.util.Map;
                class A { List x; }
                """);
        assertEquals(0, rapports.get(0).ligne());
        assertEquals(1, rapports.get(1).ligne());
        assertTrue(rapports.get(0).utilise());
        assertTrue(!rapports.get(1).utilise());
    }

}
