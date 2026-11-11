package jo.celeris.core.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextSession;

/**
 * L'import automatique adossé au classpath (chantier C8) : le nom
 * simple sous le curseur se résout contre l'index de l'hôte — les
 * candidats se proposent dans l'ordre de déclaration, l'import déjà
 * présent ne se re-propose pas, l'insertion vit à la tête du bloc par
 * une édition de position unique.
 */
class ClasspathImportsTest {

    private static ClasspathIndex classpath() {
        Map<String, List<String>> noms = new LinkedHashMap<>();
        noms.put("List", List.of("java.util.List", "java.awt.List"));
        noms.put("Map", List.of("java.util.Map"));
        return ClasspathIndex.of(noms);
    }

    /** Les candidats du nom sous le curseur, dans l'ordre de l'hôte. */
    @Test
    void candidatsDansLOrdreDeLhote() {
        Document doc = Document.of("class A {\n    List x;\n}");
        List<String> candidats = ClasspathImports.candidats(doc, 14,
                classpath());
        assertEquals(List.of("java.util.List", "java.awt.List"), candidats);
    }

    /** Le curseur aux bornes de l'identifiant le lit pareil. */
    @Test
    void curseurAuxBornes() {
        Document doc = Document.of("List x;");
        assertEquals(List.of("java.util.List", "java.awt.List"),
                ClasspathImports.candidats(doc, 4, classpath()));
        assertEquals(List.of("java.util.List", "java.awt.List"),
                ClasspathImports.candidats(doc, 0, classpath()));
    }

    /** Le nom inconnu de l'index ne donne aucun candidat. */
    @Test
    void nomInconnuSansCandidat() {
        Document doc = Document.of("Inconnu x;");
        assertTrue(ClasspathImports.candidats(doc, 1, classpath()).isEmpty());
    }

    /** Hors identifiant : aucun candidat, jamais d'invention. */
    @Test
    void horsIdentifiantAucunCandidat() {
        Document doc = Document.of("class A { }");
        assertTrue(ClasspathImports.candidats(doc, 7, classpath()).isEmpty());
        assertTrue(ClasspathImports.candidats(doc, 0, classpath()).isEmpty());
    }

    /** L'import déjà présent ne se re-propose pas — les autres restent. */
    @Test
    void importPresentNeSeReproposePas() {
        Document doc = Document.of(
                "import java.util.List;\n\nclass A {\n    List x;\n}");
        // le « List » de la ligne 3 vit de 37 à 40 : le caret en 39
        List<String> candidats = ClasspathImports.candidats(doc, 39,
                classpath());
        assertEquals(List.of("java.awt.List"), candidats);
    }

    /** Le nom résolu par un autre import de même nom simple reste proposé. */
    @Test
    void tousLesFqnImportesDisparaissent() {
        Document doc = Document.of(
                "import java.util.List;\nimport java.awt.List;\n\nclass A {\n    List x;\n}");
        // le « List » de la ligne 4 vit de 59 à 62 : le caret en 60
        assertTrue(ClasspathImports.candidats(doc, 60, classpath()).isEmpty());
    }

    /** L'import choisi se pose en tête du bloc, édition de position unique. */
    @Test
    void importChoisiSePoseEnTete() {
        Document doc = Document.of("import java.util.Map;\n\nclass A {\n}");
        EditPlan plan = ClasspathImports.importer(doc, "java.util.List");
        assertNotNull(plan);
        assertEquals(1, plan.transaction().edits().size());
        // la tête du bloc : avant le premier import
        assertEquals(0, plan.transaction().edits().get(0).start());
        TextSession session = TextSession.of(
                "import java.util.Map;\n\nclass A {\n}", () -> 0L);
        session.apply(plan.transaction(), plan.selection());
        assertEquals("import java.util.List;\nimport java.util.Map;\n"
                + "\nclass A {\n}", session.text());
        assertTrue(session.undo());
    }

    /** L'import déjà présent rend le plan nul — jamais de doublon. */
    @Test
    void importDejaPresentRendPlanNul() {
        Document doc = Document.of("import java.util.List;\n\nclass A {\n}");
        assertNull(ClasspathImports.importer(doc, "java.util.List"));
    }

    /** Sans paquet ni import : l'import naît en tête du document. */
    @Test
    void sansPaquetLimportNaitEnTete() {
        Document doc = Document.of("class A {\n}");
        EditPlan plan = ClasspathImports.importer(doc, "java.util.Map");
        assertNotNull(plan);
        assertEquals(0, plan.transaction().edits().get(0).start());
        assertEquals("import java.util.Map;\n",
                plan.transaction().edits().get(0).inserted());
    }

    // ------------------------------------------------- l'index de classpath

    /** Les doublons se retirent, l'ordre de l'hôte se conserve. */
    @Test
    void indexRetireLesDoublons() {
        Map<String, List<String>> noms = new LinkedHashMap<>();
        noms.put("List", List.of("java.util.List", "java.util.List",
                "java.awt.List"));
        assertEquals(List.of("java.util.List", "java.awt.List"),
                ClasspathIndex.of(noms).fqnDe("List"));
    }

    /** Le nom mal formé refuse d'exception — jamais d'index sale. */
    @Test
    void indexRefuseLesNomsMalFormes() {
        Map<String, List<String>> vides = new LinkedHashMap<>();
        vides.put("List", List.of(""));
        assertThrows(IllegalArgumentException.class,
                () -> ClasspathIndex.of(vides));
        Map<String, List<String>> malForme = new LinkedHashMap<>();
        malForme.put("List", List.of("java..List"));
        assertThrows(IllegalArgumentException.class,
                () -> ClasspathIndex.of(malForme));
        Map<String, List<String>> pointFinal = new LinkedHashMap<>();
        pointFinal.put("List", List.of("java.util."));
        assertThrows(IllegalArgumentException.class,
                () -> ClasspathIndex.of(pointFinal));
    }

    /** Le nom qualifié d'un autre nom simple ne rentre pas dans la clé. */
    @Test
    void fqnDAutreNomSimpleIgnore() {
        Map<String, List<String>> noms = new LinkedHashMap<>();
        noms.put("List", List.of("java.util.ArrayList"));
        assertTrue(ClasspathIndex.of(noms).fqnDe("List").isEmpty());
    }

    /** La taille de l'index compte ses noms simples. */
    @Test
    void tailleCompteLesNomsSimples() {
        assertEquals(2, classpath().taille());
    }
}
