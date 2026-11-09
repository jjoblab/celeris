package jo.celeris.core.syntax.grammars;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import jo.celeris.core.language.LanguageConfiguration;
import jo.celeris.core.language.LanguageRegistry;
import jo.celeris.core.language.LanguageSupport;
import jo.celeris.core.language.LanguagePlatform;
import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.FoldSpan;
import jo.celeris.core.syntax.IndentationFolds;
import jo.celeris.core.syntax.NodeCategory;
import jo.celeris.core.syntax.OutlineEntry;
import jo.celeris.core.syntax.RuleOutline;
import jo.celeris.core.syntax.SyntaxService;
import jo.celeris.core.syntax.TreeFolds;
import jo.celeris.core.text.Document;

import org.jspecify.annotations.Nullable;

import org.junit.jupiter.api.Test;

/**
 * L'analyse structurelle niveau 1 par les données (M7.6, H-08,
 * ADR-016) : Python, YAML et SQL ont plis et plan sans une ligne de
 * Java dédiée — la plateforme assemble les paquets, la grammaire
 * colore, les plis de l'arbre générique et de l'indentation se
 * calculent par la configuration, le plan sort des règles
 * d'{@code outline.json}. Aucun analyseur écrit ne les touche : la
 * donnée parle, le socle écoute.
 */
final class DataStructureTest {

    private static LanguageRegistry registre() {
        return LanguagePlatform.with(PackGrammars.INSTANCE).builtinPacks().build();
    }

    /** L'analyse complète d'un document par la source syntaxique de son langage. */
    private static Analysis analyse(LanguageSupport support, String source) {
        assertNotNull(support.syntax(), support.id() + " a sa source syntaxique");
        return SyntaxService.of(support.syntax()).analyseFull(Document.of(source));
    }

    /** Les plis d'un document : l'arbre, les commentaires, l'indentation de la configuration. */
    private static List<FoldSpan> plis(LanguageSupport support, Analysis analyse,
            Document doc) {
        LanguageConfiguration.Folding folding = support.configuration().folding();
        IndentationFolds.@Nullable Settings settings = folding.byIndentation()
                ? new IndentationFolds.Settings(
                        support.configuration().indentSize(), folding.offSide())
                : null;
        return TreeFolds.regions(doc, analyse, settings);
    }

    /** Le plan d'un document : les règles de son paquet, prolongées à ses plis. */
    private static List<OutlineEntry> plan(LanguageSupport support, Document doc,
            List<FoldSpan> plis) {
        assertFalse(support.outlineRules().isEmpty(),
                support.id() + " porte ses règles de plan");
        return RuleOutline.entries(doc, support.outlineRules(), plis);
    }

    /** Python : les plis de l'indentation, le plan des définitions — rien de Java. */
    @Test
    void pythonPlisEtPlanParLesDonnees() {
        String source = """
                # Outils d'outillage : comptage de lignes par catégorie.
                CATEGORIES = {
                    "code": 0,
                }

                def classifier(ligne):
                    if not ligne.strip():
                        return "vide"
                    if ligne.lstrip().startswith("#"):
                        return "commentaire"
                    return "code"

                def compter(texte):
                    for ligne in texte.splitlines():
                        CATEGORIES[classifier(ligne)] += 1
                    return dict(CATEGORIES)
                """;
        LanguageSupport python = registre().byId("python");
        assertNotNull(python, "python est là");
        Document doc = Document.of(source);
        Analysis analyse = analyse(python, source);
        List<FoldSpan> plis = plis(python, analyse, doc);
        assertTrue(plis.contains(new FoldSpan(5, 10)), "classifier se replie : " + plis);
        assertTrue(plis.contains(new FoldSpan(12, 15)), "compter se replie : " + plis);
        List<OutlineEntry> plan = plan(python, doc, plis);
        assertEquals(3, plan.size(), "le plan : " + plan);
        assertEquals("CATEGORIES", plan.get(0).name());
        assertEquals(NodeCategory.FIELD, plan.get(0).category());
        assertEquals("classifier", plan.get(1).name());
        assertEquals(NodeCategory.FUNCTION, plan.get(1).category());
        assertEquals(5, plan.get(1).startLine());
        assertEquals(10, plan.get(1).endLine());
        assertEquals("compter", plan.get(2).name());
    }

    /** YAML : les plis du décalage, le plan des clés — rien de Java. */
    @Test
    void yamlPlisEtPlanParLesDonnees() {
        String source = """
                catalogue:
                  nom: exemples
                  entrees:
                    - id: A-13
                      libelle: annulation groupee
                      priorite: P0
                  actif: true
                version: 3
                """;
        LanguageSupport yaml = registre().byId("yaml");
        assertNotNull(yaml, "yaml est là");
        Document doc = Document.of(source);
        Analysis analyse = analyse(yaml, source);
        List<FoldSpan> plis = plis(yaml, analyse, doc);
        assertTrue(plis.contains(new FoldSpan(0, 6)), "catalogue se replie : " + plis);
        assertTrue(plis.contains(new FoldSpan(2, 5)), "entrees se replie : " + plis);
        List<OutlineEntry> plan = plan(yaml, doc, plis);
        assertTrue(plan.stream().anyMatch(e -> "catalogue".equals(e.name())),
                "le plan porte catalogue : " + plan);
        assertTrue(plan.stream().anyMatch(e -> "entrees".equals(e.name())),
                "le plan porte entrees : " + plan);
        assertTrue(plan.stream().anyMatch(e -> "version".equals(e.name())),
                "le plan porte version : " + plan);
    }

    /** SQL : les plis des crochets du lexique, le plan des CREATE — rien de Java. */
    @Test
    void sqlPlisEtPlanParLesDonnees() {
        String source = """
                -- L'inventaire de la démonstration : le plan sort des règles.
                CREATE TABLE clients (
                    id INTEGER PRIMARY KEY,
                    nom TEXT NOT NULL,
                    ville TEXT
                );

                CREATE VIEW clients_de_paris AS
                    SELECT id, nom FROM clients WHERE ville = 'Paris';

                CREATE UNIQUE INDEX clients_nom ON clients (nom);
                """;
        LanguageSupport sql = registre().byId("sql");
        assertNotNull(sql, "sql est là");
        Document doc = Document.of(source);
        Analysis analyse = analyse(sql, source);
        List<FoldSpan> plis = plis(sql, analyse, doc);
        assertTrue(plis.stream().anyMatch(r -> r.startLine() == 1 && r.endLine() == 5),
                "la table se replie par ses crochets : " + plis);
        List<OutlineEntry> plan = plan(sql, doc, plis);
        assertEquals(3, plan.size(), "le plan : " + plan);
        assertEquals("clients", plan.get(0).name());
        assertEquals(NodeCategory.TYPE, plan.get(0).category());
        assertEquals("clients_de_paris", plan.get(1).name());
        assertEquals("clients_nom", plan.get(2).name());
        assertEquals(NodeCategory.FIELD, plan.get(2).category());
    }

    /** Les langages à analyseur écrit gardent le plan de l'arbre : règles vides. */
    @Test
    void langagesEcritsSansRegles() {
        LanguageRegistry registre = registre();
        for (String id : List.of("java", "kotlin", "kts", "groovy", "xml", "json")) {
            LanguageSupport support = registre.byId(id);
            assertNotNull(support, id + " est là");
            assertTrue(support.outlineRules().isEmpty(),
                    id + " vit du plan de son arbre, pas de règles");
        }
    }
}
