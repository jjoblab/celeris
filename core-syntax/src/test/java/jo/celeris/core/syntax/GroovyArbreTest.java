package jo.celeris.core.syntax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextSession;

/**
 * L'analyseur Groovy tolérant (ADR-004, dialecte Gradle — C-07) : le
 * script se découpe par appel de premier niveau en tête de ligne, les
 * déclarations de type et les def ouvrent leur chunk ; le plan liste
 * types, méthodes et champs ; les corps — fermetures comprises — se
 * plient par bloc ; la malformation devient un nœud d'erreur, jamais
 * une exception ; l'édition incrémentale égale l'analyse complète.
 */
class GroovyArbreTest {

    private static final String SCRIPT = """
            plugins {
                id 'java-library'
            }

            def salut() {
                println 'bonjour'
            }

            class Etat {
                int compteur

                def avance() {
                    compteur++
                }
            }

            salut()
            """;

    private final SyntaxService service = SyntaxService.of(GroovySyntax.INSTANCE);

    /** Le plan du script liste méthodes, types et méthodes de type. */
    @Test
    void planDuScript() {
        Document doc = Document.of(SCRIPT);
        List<OutlineEntry> plan = TreeOutline.entries(doc, service.analyseFull(doc));
        assertTrue(plan.stream().anyMatch(e -> e.category() == NodeCategory.FUNCTION
                && "salut".equals(e.name())));
        assertTrue(plan.stream().anyMatch(e -> e.category() == NodeCategory.TYPE
                && "Etat".equals(e.name())));
        assertTrue(plan.stream().anyMatch(e -> e.category() == NodeCategory.FUNCTION
                && "avance".equals(e.name())));
    }

    /** Les chunks : chaque appel de premier niveau et chaque déclaration. */
    @Test
    void chunksParAppelDePremierNiveau() {
        Document doc = Document.of(SCRIPT);
        List<Chunk> chunks = service.analyseFull(doc).tree().chunks();
        assertTrue(chunks.size() >= 4, "plugins, def salut, class Etat, salut() — "
                + chunks.size() + " chunks");
        assertEquals(0, chunks.get(0).startLine(), "plugins ouvre le premier chunk");
    }

    /** Les plis par bloc : fermetures de script et corps de déclarations. */
    @Test
    void plisParBloc() {
        Document doc = Document.of(SCRIPT);
        List<FoldSpan> plis = TreeFolds.regions(doc, service.analyseFull(doc));
        assertTrue(plis.size() >= 4, "plugins, salut, Etat, avance — " + plis.size()
                + " plis");
        assertEquals(0, plis.get(0).startLine());
        assertEquals(2, plis.get(0).endLine(), "le pli de plugins couvre sa fermeture");
    }

    /** Une accolade manquante devient un diagnostic, jamais une exception. */
    @Test
    void accoladeManquanteDiagnostic() {
        String source = """
                class Cassee {
                    def f() {
                }
                """;
        Document doc = Document.of(source);
        List<SyntaxIssue> problemes = TreeDiagnostics.issues(doc, service.analyseFull(doc));
        assertFalse(problemes.isEmpty());
    }

    /** Un source sain ne produit aucun diagnostic. */
    @Test
    void sourceSainSansDiagnostics() {
        Document doc = Document.of(SCRIPT);
        assertTrue(TreeDiagnostics.issues(doc, service.analyseFull(doc)).isEmpty());
    }

    /** Le gabarit et la chaîne triple dans un corps ne cassent pas le découpage. */
    @Test
    void gabaritsEtTriplesDansLeCorps() {
        String source = """
                task resume {
                    def version = "1.2.${revision}"
                    def doc = '''sur
                    plusieurs lignes'''
                    println "${doc} ${version}"
                }
                """;
        Document doc = Document.of(source);
        List<FoldSpan> plis = TreeFolds.regions(doc, service.analyseFull(doc));
        assertEquals(1, plis.size(), "le corps de la tâche se plie");
        assertEquals(0, plis.get(0).startLine());
        assertEquals(5, plis.get(0).endLine());
        assertTrue(TreeDiagnostics.issues(doc, service.analyseFull(doc)).isEmpty());
    }

    /** L'édition incrémentale égale l'analyse complète, sous-arbres conservés. */
    @Test
    void editionIncrementaleEgaleComplet() {
        TextSession session = TextSession.of(SCRIPT, () -> 0L);
        Document avant = session.document();
        Analysis premiere = service.analyseFull(avant);
        int cible = SCRIPT.indexOf("compteur");
        session.replaceRange(cible, cible + 8, "total");
        Document edite = session.document();
        Analysis complet = service.analyseFull(edite);
        EditSite site = EditSite.of(avant, edite, cible, cible + 8, "total");
        Analysis diff = service.analyseIncremental(edite, premiere, site);
        Arbres.verifierEquivalence(diff.tree(), complet.tree(), edite);
    }

    /** Le dialecte porte son identifiant propre. */
    @Test
    void leDialectePorteSonIdentifiant() {
        assertEquals("groovy", GroovySyntax.INSTANCE.id());
    }
}
