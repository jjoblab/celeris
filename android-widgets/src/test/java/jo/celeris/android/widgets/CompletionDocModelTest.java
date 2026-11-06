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
        CompletionDocModel modele = CompletionDocModel.of("substring", DOC_SERVEUR);
        // le titre est distinct : il ne commence ni ne finit dans une section
        assertEquals("substring", modele.titre());
        // les blocs clôturés sortent en sections typées séparées, dans l'ordre
        List<CompletionDocModel.Section> sections = modele.sections();
        assertEquals(2, sections.size());
        assertEquals("signature", sections.get(0).genre());
        assertEquals("java.lang.String substring(int beginIndex)",
                sections.get(0).texte());
        assertEquals("description", sections.get(1).genre());
        assertEquals("Returns a string that is a substring.",
                sections.get(1).texte());
        // aucun marqueur de clôture ne fuit dans le texte des sections
        for (CompletionDocModel.Section section : sections) {
            assertTrue(!section.texte().contains("```"));
            assertTrue(!section.texte().contains(section.genre()));
        }
    }

    /** Le texte hors clôtures : il vit à part, jamais mêlé aux sections typées. */
    @Test
    void leTexteHorsClotures() {
        CompletionDocModel modele = CompletionDocModel.of("x", DOC_SERVEUR);
        String libre = modele.texteLibre();
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
        CompletionDocModel modele = CompletionDocModel.of("ret",
                "Just plain documentation, no fences.");
        assertTrue(modele.sections().isEmpty());
        assertEquals("Just plain documentation, no fences.", modele.texteLibre());
    }

    /** La clôture non fermée : le reste du texte en est le contenu — jamais perdu. */
    @Test
    void laClotureNonFermee() {
        CompletionDocModel modele = CompletionDocModel.of("y",
                "```signature\nint run()\nno closing fence");
        assertEquals(1, modele.sections().size());
        assertEquals("signature", modele.sections().get(0).genre());
        assertTrue(modele.sections().get(0).texte().contains("int run()"));
        assertEquals("", modele.texteLibre());
    }

    /** Le genre inconnu : la clôture porte ce qu'elle porte, le modèle ne devine pas. */
    @Test
    void leGenreInconnuEstConserve() {
        CompletionDocModel modele = CompletionDocModel.of("z",
                "```example\nexemple\n```");
        assertEquals("example", modele.sections().get(0).genre());
        assertEquals("exemple", modele.sections().get(0).texte());
    }

    /** La documentation vide : titre seul, ni section ni texte libre. */
    @Test
    void laDocumentationVide() {
        CompletionDocModel modele = CompletionDocModel.of("seul", "");
        assertEquals("seul", modele.titre());
        assertTrue(modele.sections().isEmpty());
        assertEquals("", modele.texteLibre());
    }
}
