package jo.celeris.core.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;

/**
 * L'import automatique (B-05) : le candidat d'import s'insère en tête
 * du bloc des imports — après la déclaration de paquet — par une
 * ÉDITION DE POSITION UNIQUE. L'import déjà présent ne se re-insère
 * jamais.
 */
class ImportAutoTest {

    /** L'insertion après le paquet : une seule édition, à la position exacte. */
    @Test
    void linsertionApresLePaquet() {
        String texte = "package demo;\n\nclass A {}\n";
        Document doc = Document.of(texte);
        EditPlan plan = ImportAuto.importer(doc, "import java.util.List;\n");
        assertNotNull(plan);
        assertEquals(1, plan.transaction().edits().size());
        var edition = plan.transaction().edits().get(0);
        // la tête : le début de la ligne qui suit le paquet
        assertEquals(14, edition.start());
        assertEquals("", edition.removed());
        assertEquals("import java.util.List;\n", edition.inserted());
        // la sélection d'après : le curseur au début de l'import inséré
        MultiSelection selection = plan.selection();
        assertEquals(14, selection.ranges().get(0).caret());
    }

    /** Sans paquet : l'insertion à la toute première position du document. */
    @Test
    void linsertionSansPaquet() {
        String texte = "class A {}\n";
        Document doc = Document.of(texte);
        EditPlan plan = ImportAuto.importer(doc, "import java.util.Map;\n");
        assertNotNull(plan);
        assertEquals(0, plan.transaction().edits().get(0).start());
        assertEquals("import java.util.Map;\n",
                plan.transaction().edits().get(0).inserted());
    }

    /** Avec des imports existants : l'insertion en tête du bloc, avant le premier. */
    @Test
    void linsertionEnTeteDuBloc() {
        String texte = "package demo;\n\nimport java.util.Map;\n\nimport java.util.Set;\n\nclass A {}\n";
        Document doc = Document.of(texte);
        EditPlan plan = ImportAuto.importer(doc, "import java.util.List;\n");
        assertNotNull(plan);
        // la tête du bloc : le début de la ligne du premier import (ligne 2)
        assertEquals(texte.indexOf("import java.util.Map;"),
                plan.transaction().edits().get(0).start());
        assertEquals("import java.util.List;\n",
                plan.transaction().edits().get(0).inserted());
    }

    /** L'import déjà présent : rien à faire — null, jamais de doublon. */
    @Test
    void limportDejaPresent() {
        String texte = "package demo;\n\nimport java.util.List;\n\nclass A {}\n";
        Document doc = Document.of(texte);
        assertNull(ImportAuto.importer(doc, "import java.util.List;\n"));
    }

    /** L'import déjà présent ailleurs dans le texte : la ligne entière compte. */
    @Test
    void laPresenceSeVerifieALaLigneEntiere() {
        String texte = """
                package demo;

                import java.util.List;

                class A { Object o = "import java.util.List;"; }
                """;
        Document doc = Document.of(texte);
        // présent à la ligne des imports : rien à faire
        assertNull(ImportAuto.importer(doc, "import java.util.List;\n"));
        // un autre import : il s'insère, la chaîne ne compte pas comme présence
        EditPlan plan = ImportAuto.importer(doc, "import java.util.Map;\n");
        assertNotNull(plan);
        assertEquals(texte.indexOf("import java.util.List;"),
                plan.transaction().edits().get(0).start());
    }
}
