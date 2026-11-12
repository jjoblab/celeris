package jo.celeris.core.syntax;

import jo.celeris.core.syntax.structure.FoldSpan;
import jo.celeris.core.syntax.structure.OutlineEntry;
import jo.celeris.core.syntax.structure.OutlineRule;
import jo.celeris.core.syntax.structure.RuleOutline;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;

/**
 * Le plan par règles (M7.6, H-08) : la première règle qui matche
 * nomme la ligne, l'entrée couvre sa région de pli, le groupe absent
 * se refuse, la catégorie inconnue aussi — la donnée fausse se dit
 * tout de suite, jamais en silence.
 */
class RuleOutlineTest {

    /** Les règles du plan Python : fonctions, classes, constantes de module. */
    private static final List<OutlineRule> PYTHON = List.of(
            OutlineRule.of("^\\s*(?:async\\s+)?def\\s+([A-Za-z_][A-Za-z0-9_]*)",
                    "function", 1),
            OutlineRule.of("^\\s*class\\s+([A-Za-z_][A-Za-z0-9_]*)", "type", 1),
            OutlineRule.of("^([A-Za-z_][A-Za-z0-9_]*)\\s*=", "field", 1));

    /** Le plan du corps Python : chaque déclaration nommée, dans l'ordre. */
    @Test
    void planPython() {
        String source = """
                CATEGORIES = {
                    "code": 0,
                }

                def classifier(ligne):
                    return "code"

                class Compteur:
                    def compter(self, texte):
                        return len(texte)
                """;
        Document doc = Document.of(source);
        List<OutlineEntry> plan = RuleOutline.entries(doc, PYTHON, List.of());
        assertEquals(4, plan.size());
        assertEquals("CATEGORIES", plan.get(0).name());
        assertEquals(NodeCategory.FIELD, plan.get(0).category());
        assertEquals("classifier", plan.get(1).name());
        assertEquals(NodeCategory.FUNCTION, plan.get(1).category());
        assertEquals("Compteur", plan.get(2).name());
        assertEquals(NodeCategory.TYPE, plan.get(2).category());
        assertEquals("compter", plan.get(3).name());
        assertEquals(8, plan.get(3).startLine());
    }

    /** L'entrée couvre sa région de pli : le fil d'Ariane s'y retrouve. */
    @Test
    void entreeProlongeeASaRegion() {
        String source = """
                def classifier(ligne):
                    if not ligne.strip():
                        return "vide"
                    return "code"

                def compter(texte):
                    return 1
                """;
        Document doc = Document.of(source);
        List<FoldSpan> regions = List.of(new FoldSpan(0, 3), new FoldSpan(5, 6));
        List<OutlineEntry> plan = RuleOutline.entries(doc, PYTHON, regions);
        assertEquals(2, plan.size());
        assertEquals(0, plan.get(0).startLine());
        assertEquals(3, plan.get(0).endLine());
        assertEquals(5, plan.get(1).startLine());
        assertEquals(6, plan.get(1).endLine());
        assertTrue(plan.get(0).endOffset() > plan.get(0).startOffset());
    }

    /** La première règle qui matche gagne : l'ordre du paquet fait la précédence. */
    @Test
    void premiereRegleGagne() {
        Document doc = Document.of("def foo():\n");
        OutlineRule fonction = OutlineRule.of("^def\\s+(\\w+)", "function", 1);
        OutlineRule type = OutlineRule.of("^def\\s+(\\w+)", "type", 1);
        List<OutlineEntry> plan = RuleOutline.entries(doc,
                List.of(fonction, type), List.of());
        assertEquals(NodeCategory.FUNCTION, plan.get(0).category());
    }

    /** Le motif invalide, la catégorie inconnue et le groupe hors bornes se refusent. */
    @Test
    void reglesRefusees() {
        assertThrows(IllegalArgumentException.class,
                () -> OutlineRule.of("^(", "function", 1));
        assertThrows(IllegalArgumentException.class,
                () -> OutlineRule.of("^(\\w+)", "module", 1));
        assertThrows(IllegalArgumentException.class,
                () -> OutlineRule.of("^(\\w+)", "function", 2));
        assertThrows(IllegalArgumentException.class,
                () -> OutlineRule.of("^(\\w+)", "function", 0));
    }

    /** Sans règle, le plan reste vide : un langage sans déclaration est légitime. */
    @Test
    void sansReglePlanVide() {
        Document doc = Document.of("def foo():\n");
        assertEquals(List.of(), RuleOutline.entries(doc, List.of(), List.of()));
    }
}
