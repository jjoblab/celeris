package jo.celeris.android.widgets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Le modèle de documentation de complétion (B-09) : la documentation
 * structurée du serveur — des blocs typés balisés par des clôtures
 * Markdown — devient des sections distinctes, jamais fondues en un
 * bloc unique.
 *
 * <p>PIEGE-16 (hérité du dépôt de départ) : le panneau de documentation
 * de complétion fusionnait titre et élément en ignorant la signature
 * (clôture typée) du serveur : les blocs typés doivent rester distincts.
 * Le modèle de départ affichait la signature collée au libellé, sans
 * séparation de genre — illisible et trompeur.</p>
 */
class CompletionDocModelTest {

    /** La documentation factice du serveur : signature clôturée, description clôturée, texte libre. */
    private static final String DOC_SERVEUR = "Some text before.\n"
            + "```signature\n"
            + "java.lang.String substring(int beginIndex)\n"
            + "```\n"
            + "Some text between.\n"
            + "```description\n"
            + "Returns a string that is a substring.\n"
            + "```\n"
            + "Some text after.";

    /**
     * PIEGE-16 — les blocs typés restent distincts : chaque clôture
     * porte son genre, le titre ne fusionne jamais avec le corps.
     */
    @Test
    void completionDocModelKeepsTypedSections() {
        CompletionDocModel model = CompletionDocModel.of("substring", DOC_SERVEUR);
        // le titre est distinct : il ne commence ni ne finit dans une section
        assertEquals("substring", model.titre());
        // les blocs clôturés sortent en sections typées séparées, dans l'ordre
        List<CompletionDocModel.Section> sections = model.sections();
        assertEquals(2, sections.size());
        assertEquals("signature", sections.get(0).kind());
        assertEquals("java.lang.String substring(int beginIndex)",
                sections.get(0).text());
        assertEquals("description", sections.get(1).kind());
        assertEquals("Returns a string that is a substring.",
                sections.get(1).text());
        // aucun marqueur de clôture ne fuit dans le texte des sections
        for (CompletionDocModel.Section section : sections) {
            assertTrue(!section.text().contains("```"));
            assertTrue(!section.text().contains(section.kind()));
        }
    }

    /** Le texte hors clôtures : il vit à part, jamais mêlé aux sections typées. */
    @Test
    void leTexteHorsClotures() {
        CompletionDocModel model = CompletionDocModel.of("x", DOC_SERVEUR);
        String libre = model.freeText();
        assertTrue(libre.contains("Some text before."));
        assertTrue(libre.contains("Some text between."));
        assertTrue(libre.contains("Some text after."));
        // le contenu des clôtures ne fuit pas dans le texte libre
        assertTrue(!libre.contains("substring(int"));
        assertTrue(!libre.contains("Returns a string"));
    }

    /** Sans clôture : tout est texte libre, aucune section inventée. */
    @Test
    void sansClotureToutEstLibre() {
        CompletionDocModel model = CompletionDocModel.of("ret",
                "Just plain documentation, no fences.");
        assertTrue(model.sections().isEmpty());
        assertEquals("Just plain documentation, no fences.", model.freeText());
    }

    /** La clôture non fermée : le reste du texte en est le contenu — jamais perdu. */
    @Test
    void laClotureNonFermee() {
        CompletionDocModel model = CompletionDocModel.of("y",
                "```signature\nint run()\nno closing fence");
        assertEquals(1, model.sections().size());
        assertEquals("signature", model.sections().get(0).kind());
        assertTrue(model.sections().get(0).text().contains("int run()"));
        assertEquals("", model.freeText());
    }

    /** Le genre inconnu : la clôture porte ce qu'elle porte, le modèle ne devine pas. */
    @Test
    void leGenreInconnuEstConserve() {
        CompletionDocModel model = CompletionDocModel.of("z",
                "```example\nexemple\n```");
        assertEquals("example", model.sections().get(0).kind());
        assertEquals("exemple", model.sections().get(0).text());
    }

    /** La documentation vide : titre seul, ni section ni texte libre. */
    @Test
    void laDocumentationVide() {
        CompletionDocModel model = CompletionDocModel.of("seul", "");
        assertEquals("seul", model.titre());
        assertTrue(model.sections().isEmpty());
        assertEquals("", model.freeText());
    }

    /** La convention des serveurs LSP (audit Android Studio du
     *  2026-11-14) : « ```java », « **Parameters:** », « **Returns:** »
     *  et les listes à tirets — la doc rapide y affiche ses sections
     *  « Parameters », « Returns », « Throws » ; le modèle en fait des
     *  sections au même rang que les clôtures typées, les tirets
     *  restent au corps de leur section. */
    @Test
    void lesEnTetesMarkdownStandardOuvrentLesSections() {
        String doc = "Returns a string that is a substring.\n"
                + "```java\n"
                + "java.lang.String substring(int beginIndex)\n"
                + "```\n"
                + "**Parameters:**\n"
                + "- beginIndex — the beginning index\n"
                + "- endIndex — the ending index\n"
                + "**Returns:**\n"
                + "the substring";
        CompletionDocModel model = CompletionDocModel.of("substring", doc);
        assertEquals("substring", model.titre());
        assertEquals("Returns a string that is a substring.",
                model.freeText());
        assertEquals(3, model.sections().size());
        assertEquals("java", model.sections().get(0).kind());
        assertEquals("java.lang.String substring(int beginIndex)",
                model.sections().get(0).text());
        assertEquals("Parameters:", model.sections().get(1).kind());
        assertEquals("- beginIndex — the beginning index\n"
                        + "- endIndex — the ending index",
                model.sections().get(1).text());
        assertEquals("Returns:", model.sections().get(2).kind());
        assertEquals("the substring", model.sections().get(2).text());
    }

    /** Les titres Markdown (« # Titre ») ouvrent aussi la section —
     *  le corps vit jusqu'au titre ou à la clôture suivante. */
    @Test
    void lesTitresMarkdownOuvrentLesSections() {
        String doc = "intro\n# Details\nla description\n# See Also\nrien";
        CompletionDocModel model = CompletionDocModel.of("x", doc);
        assertEquals("intro", model.freeText());
        assertEquals(2, model.sections().size());
        assertEquals("Details", model.sections().get(0).kind());
        assertEquals("la description", model.sections().get(0).text());
        assertEquals("See Also", model.sections().get(1).kind());
        assertEquals("rien", model.sections().get(1).text());
    }

    /** Le gras EN LIGNE n'ouvre RIEN : seule la ligne PORTÉE par le
     *  gras entier ouvre une section — la phrase qui porte un mot en
     *  gras reste au texte libre, marqueurs compris. */
    @Test
    void leGrasEnLigneResteAuCorps() {
        String doc = "le mot **important** reste au corps";
        CompletionDocModel model = CompletionDocModel.of("y", doc);
        assertTrue(model.sections().isEmpty());
        assertEquals(doc, model.freeText());
    }
}
