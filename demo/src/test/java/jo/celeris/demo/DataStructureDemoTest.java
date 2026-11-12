package jo.celeris.demo;

import jo.celeris.core.language.model.LanguageConfiguration;
import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.syntax.structure.FoldSpan;
import jo.celeris.core.syntax.structure.IndentationFolds;
import jo.celeris.core.syntax.structure.OutlineEntry;
import jo.celeris.core.syntax.structure.RuleOutline;
import jo.celeris.core.syntax.structure.TreeFolds;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;



import jo.celeris.core.syntax.Analysis;


import jo.celeris.core.syntax.NodeCategory;


import jo.celeris.core.syntax.SyntaxService;

import jo.celeris.core.text.Document;

import org.jspecify.annotations.Nullable;

import org.junit.jupiter.api.Test;

/**
 * La structure des fichiers d'exemple de la démonstration (M7.6, H-08)
 * : Python, YAML et SQL — les fichiers qu'ouvrent les cartes — ont
 * plis et plan par les seules données de leurs paquets (grammaire,
 * configuration d'indentation, règles d'{@code outline.json}). La
 * démonstration ne porte pas une ligne d'analyseur pour eux : ce test
 * le prouve sur sa vraie matière.
 */
final class DataStructureDemoTest {

    /**
     * Les trois fichiers de données de la démo se plient et se
     * planifient sans analyseur écrit — la matière exacte des cartes.
     */
    @Test
    void pythonYamlSqlPlisEtPlanParLesDonnees() {
        for (String nom : List.of("outils.py", "dependances.yml",
                "inventaire.sql")) {
            FichierExemple fichier = FichiersExemple.parNom(nom);
            assertNotNull(fichier, nom + " figure au catalogue");
            LanguageSupport support = LangagesDemo.langageDe(nom, null,
                    fichier.contenu());
            assertNotNull(support, nom + " se détecte");
            assertFalse(support.outlineRules().isEmpty(),
                    nom + " porte ses règles de plan");
            assertFalse(plis(fichier).isEmpty(), nom + " a des régions pliables");
            assertFalse(plan(fichier).isEmpty(), nom + " a un plan");
        }
    }

    /** Le plan du fichier Python de la démo : constantes et définitions. */
    @Test
    void planDuPythonDeLaDemo() {
        List<OutlineEntry> plan = plan(FichiersExemple.parNom("outils.py"));
        assertTrue(plan.stream().anyMatch(e -> "CATEGORIES".equals(e.name())),
                "la constante figure : " + plan);
        assertTrue(plan.stream().anyMatch(e -> "classifier".equals(e.name())
                && e.category() == NodeCategory.FUNCTION), "classifier figure");
        assertTrue(plan.stream().anyMatch(e -> "compter".equals(e.name())),
                "compter figure");
        assertTrue(plis(FichiersExemple.parNom("outils.py")).stream()
                .anyMatch(r -> r.startLine() < r.endLine()),
                "les définitions se replient");
    }

    /** Le plan du YAML de la démo : les clés de premier niveau. */
    @Test
    void planDuYamlDeLaDemo() {
        List<OutlineEntry> plan = plan(FichiersExemple.parNom("dependances.yml"));
        assertTrue(plan.stream().anyMatch(e -> "catalogue".equals(e.name())),
                "catalogue figure : " + plan);
        assertTrue(plan.stream().anyMatch(e -> "pipeline".equals(e.name())),
                "pipeline figure");
        assertTrue(plis(FichiersExemple.parNom("dependances.yml")).stream()
                .anyMatch(r -> r.startLine() == 2 && r.endLine() >= 12),
                "le bloc catalogue se replie");
    }

    /** Le plan du SQL de la démo : table, vue et index nommés. */
    @Test
    void planDuSqlDeLaDemo() {
        List<OutlineEntry> plan = plan(FichiersExemple.parNom("inventaire.sql"));
        assertEquals(3, plan.size(), "le plan : " + plan);
        assertEquals("clients", plan.get(0).name());
        assertEquals(NodeCategory.TYPE, plan.get(0).category());
        assertEquals("clients_actifs", plan.get(1).name());
        assertEquals("clients_nom", plan.get(2).name());
        List<FoldSpan> plis = plis(FichiersExemple.parNom("inventaire.sql"));
        assertTrue(plis.stream().anyMatch(r -> r.startLine() == 2 && r.endLine() == 6),
                "la table se replie par ses crochets : " + plis);
    }

    /** Les plis d'un fichier de la démo : l'arbre, les commentaires, l'indentation déclarée. */
    private static List<FoldSpan> plis(@Nullable FichierExemple fichier) {
        assertNotNull(fichier);
        LanguageSupport support = LangagesDemo.langageDe(fichier.nom(), null,
                fichier.contenu());
        assertNotNull(support);
        Document doc = Document.of(fichier.contenu());
        Analysis analyse = SyntaxService.of(support.syntax()).analyseFull(doc);
        LanguageConfiguration.Folding pliage = support.configuration().folding();
        IndentationFolds.@Nullable Settings settings = pliage.byIndentation()
                ? new IndentationFolds.Settings(support.configuration().indentSize(),
                        pliage.offSide())
                : null;
        return TreeFolds.regions(doc, analyse, settings);
    }

    /** Le plan d'un fichier de la démo : les règles de son paquet, prolongées à ses plis. */
    private static List<OutlineEntry> plan(@Nullable FichierExemple fichier) {
        assertNotNull(fichier);
        LanguageSupport support = LangagesDemo.langageDe(fichier.nom(), null,
                fichier.contenu());
        assertNotNull(support);
        return RuleOutline.entries(Document.of(fichier.contenu()),
                support.outlineRules(), plis(fichier));
    }
}
