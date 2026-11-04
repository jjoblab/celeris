package jo.celeris.core.syntax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;

/**
 * L'analyseur Java tolérant (ADR-004) : chunks de haut niveau (paquet,
 * imports, types), plan avec méthodes et champs, plis des corps, tolérance
 * aux sources malformés, et maintenance incrémentale — une édition dans
 * une méthode ne ré-analyse que le chunk propriétaire.
 */
class JavaArbreTest {

    private final SyntaxService service = SyntaxService.of(JavaSyntax.INSTANCE);

    /** Le plan du fichier liste types, méthodes et champs, imbriqués. */
    @Test
    void planDuFichier() {
        Document doc = Document.of(CorpusChargeur.exempleJava());
        List<OutlineEntry> plan = TreeOutline.entries(doc, service.analyseFull(doc));
        assertTrue(plan.stream().anyMatch(e -> e.category() == NodeCategory.TYPE
                && "ExempleJava".equals(e.name())));
        assertTrue(plan.stream().anyMatch(e -> e.category() == NodeCategory.FUNCTION
                && "additionne".equals(e.name())));
        assertTrue(plan.stream().anyMatch(e -> e.category() == NodeCategory.FUNCTION
                && "reindexe".equals(e.name())));
        assertTrue(plan.stream().anyMatch(e -> e.category() == NodeCategory.FIELD
                && "index".equals(e.name())));
        assertTrue(plan.stream().anyMatch(e -> e.category() == NodeCategory.TYPE
                && "Etat".equals(e.name())));
        assertTrue(plan.stream().anyMatch(e -> e.category() == NodeCategory.TYPE
                && "Point".equals(e.name())));
        assertTrue(plan.stream().anyMatch(e -> e.category() == NodeCategory.FUNCTION
                && "norme".equals(e.name())));
    }

    /** Les chunks de haut niveau : paquet+imports, puis chaque type. */
    @Test
    void chunksDeHautNiveau() {
        String source = """
                package a.b;

                import java.util.List;

                class Premier {
                    int x;
                }

                class Second {
                }
                """;
        Document doc = Document.of(source);
        SyntaxTree arbre = service.analyseFull(doc).tree();
        assertEquals(3, arbre.chunks().size());
        assertEquals(0, arbre.chunks().get(0).startLine());
        assertTrue(arbre.chunks().get(1).startLine() < arbre.chunks().get(2).startLine());
        List<OutlineEntry> plan = TreeOutline.entries(doc, service.analyseFull(doc));
        assertEquals(2, plan.stream()
                .filter(e -> e.category() == NodeCategory.TYPE).count());
    }

    /** Les plis couvrent les corps de types, de méthodes et les blocs. */
    @Test
    void plisDesCorps() {
        Document doc = Document.of(CorpusChargeur.exempleJava());
        List<FoldSpan> plis = TreeFolds.regions(doc, service.analyseFull(doc));
        assertFalse(plis.isEmpty());
        FoldSpan premier = plis.get(0);
        assertEquals(5, premier.startLine(),
                "le pli de la classe s'ouvre à sa documentation, en-tête de sa déclaration");
        assertTrue(premier.endLine() > 60, "et couvre tout le corps");
        assertTrue(plis.stream().anyMatch(p -> p.endLine() > p.startLine() + 3),
                "plusieurs régions de plusieurs lignes");
    }

    /** Une édition dans une méthode ne ré-analyse que le chunk du type. */
    @Test
    void editionDansUneMethode() {
        String source = CorpusChargeur.exempleJava();
        Document doc = Document.of(source);
        Analysis complet = service.analyseFull(doc);
        int cible = source.indexOf("somme += valeur;");
        assertTrue(cible > 0);
        Document edite = doc.replace(cible + 7, cible + 14, "alpha");
        Analysis diff = service.analyseIncremental(edite, complet,
                EditSite.of(doc, edite, cible + 7, cible + 14, "alpha"));
        Arbres.verifierEquivalence(diff.tree(),
                service.analyseFull(edite).tree(), edite);
    }

    /** Un crochet fermant manquant devient une erreur localisée. */
    @Test
    void crochetFermantManquant() {
        String source = """
                class Casse {
                    void marche() {
                        int x = 1;
                }
                """;
        Document doc = Document.of(source);
        List<SyntaxIssue> problemes = TreeDiagnostics.issues(doc, service.analyseFull(doc));
        assertFalse(problemes.isEmpty(), "l'accolade du méthode non fermée est signalée");
        SyntaxIssue premier = problemes.get(0);
        assertTrue(premier.startOffset() >= 0);
        assertTrue(premier.length() > 0);
    }

    /** Un source sain ne produit aucun diagnostic. */
    @Test
    void sourceSainSansDiagnostic() {
        Document doc = Document.of(CorpusChargeur.exempleJava());
        List<SyntaxIssue> problemes = TreeDiagnostics.issues(doc, service.analyseFull(doc));
        assertTrue(problemes.isEmpty());
    }

    /** L'énumération, le record et la classe interne apparaissent au plan. */
    @Test
    void membresImbriquesAuPlan() {
        Document doc = Document.of(CorpusChargeur.exempleJava());
        List<OutlineEntry> plan = TreeOutline.entries(doc, service.analyseFull(doc));
        List<OutlineEntry> dansEtat = plan.stream()
                .filter(e -> "Etat".equals(e.name())).toList();
        assertEquals(1, dansEtat.size());
        OutlineEntry etat = dansEtat.get(0);
        assertTrue(etat.endLine() > etat.startLine(),
                "l'énumération couvre ses constantes");
    }

    /** Les niveaux d'indentation suivent les blocs de l'arbre. */
    @Test
    void niveauxIndentation() {
        String source = """
                class A {
                    void b() {
                        if (true) {
                            int x;
                        }
                    }
                }
                """;
        Document doc = Document.of(source);
        Analysis analyse = service.analyseFull(doc);
        assertEquals(0, TreeIndent.levelAt(doc, analyse, 0));
        assertEquals(1, TreeIndent.levelAt(doc, analyse, 1));
        assertEquals(2, TreeIndent.levelAt(doc, analyse, 2));
        assertEquals(3, TreeIndent.levelAt(doc, analyse, 3));
        assertEquals(4, TreeIndent.levelAt(doc, analyse, 4));
        assertEquals(3, TreeIndent.levelAt(doc, analyse, 5));
    }

    /** La coordination suit les éditions de session sur le corpus Java. */
    @Test
    void coordinationSurCorpus() {
        String source = CorpusChargeur.exempleJava();
        jo.celeris.core.text.TextSession session =
                jo.celeris.core.text.TextSession.of(source, () -> 0L);
        SyntaxCoordinator coordinateur = SyntaxCoordinator.on(session, JavaSyntax.INSTANCE);
        Analysis premiere = coordinateur.analysis();
        int cible = source.indexOf("compteur;");
        session.replaceRange(cible, cible + 9, "totalite");
        Analysis seconde = coordinateur.analysis();
        assertSame(premiere.lex().line(0), seconde.lex().line(0));
        Arbres.verifierEquivalence(seconde.tree(),
                service.analyseFull(session.document()).tree(), session.document());
        coordinateur.close();
    }
}
