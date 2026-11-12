package jo.celeris.demo;

import jo.celeris.core.syntax.clike.JavaSyntax;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jo.celeris.core.intelligence.EditPlan;

import jo.celeris.core.syntax.SyntaxCoordinator;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextSession;

import org.junit.jupiter.api.Test;

/**
 * L'atelier de refactorisation de la démonstration (M7.5, §8) : le
 * gabarit pose son premier trou sélectionné (A-22), le formatage
 * redresse l'indentation en transaction annulable (B-25), le
 * renommage porte les usages ENTIERS du symbole (B-19, B-11), l'import
 * se pose à la tête du bloc et refuse les doublons (B-27). Pur, sans
 * appareil.
 */
class AtelierRefactorTest {

    private static final String URI = "file:///exemple.java";

    private static final String DOCUMENT_MAL_INDENTE = """
            class Exemple {
            void corps() {
            int nombre = 1;
            }
            }
            """;

    private static final String DOCUMENT_A_RENOMMER = """
            class Exemple {
                int total = 0;
                void corps() {
                    int nombre = total;
                    nombre = nombre + total;
                }
            }
            """;

    /**
     * A-22 — le gabarit rend ses défauts, le premier trou vit sélectionné.
     * C7 — la macro FICHIER se résout au rendu, le trou à choix du genre
     * rend son premier choix.
     */
    @Test
    void templatePlanSelectsItsFirstHole() {
        TextSession session = TextSession.of("class Exemple {\n}\n", () -> 0L);
        int caret = session.document().text().indexOf('\n') + 1;
        session.setSelection(MultiSelection.of(SelRange.caretAt(caret)));
        EditPlan plan = AtelierRefactor.planGabarit(session.document(),
                session.selection(), AtelierRefactor.GABARIT_METHODE,
                java.util.Map.of("FICHIER", "Exemple.java"));
        session.apply(plan.transaction(), plan.selection());
        String attendu = "class Exemple {\n"
                + "// Exemple.java — posée par le gabarit de la démonstration\n"
                + "private void nouvelleMethode() {\n    \n}}\n";
        assertEquals(attendu, session.document().text(),
                "le gabarit rend ses défauts, macro résolue, indenté tel quel");
        SelRange premiere = session.selection().ranges().get(0);
        assertEquals("void",
                session.document().subText(premiere.start(), premiere.end()),
                "le premier trou — le trou à choix du genre — vit sélectionné");
    }

    /** B-25 — le formatage redresse l'indentation, en une transaction annulable. */
    @Test
    void formatPlanReindentsAndUndoesInOneGesture() {
        TextSession session = TextSession.of(DOCUMENT_MAL_INDENTE, () -> 0L);
        SyntaxCoordinator coordinateur = SyntaxCoordinator.on(session,
                JavaSyntax.INSTANCE);
        EditPlan plan;
        try {
            plan = AtelierRefactor.planFormat(session.document(),
                    coordinateur.analysis());
        } finally {
            coordinateur.close();
        }
        assertNotNull(plan, "le document mal indenté a un plan");
        assertTrue(session.apply(plan.transaction(), plan.selection()),
                "la transaction s'applique");
        String texte = session.document().text();
        assertTrue(texte.contains("\n    void corps() {"),
                "la méthode gagne son cran");
        assertTrue(texte.contains("\n        int nombre = 1;"),
                "le corps de la méthode gagne le sien");
        session.undo();
        assertEquals(DOCUMENT_MAL_INDENTE, session.document().text(),
                "un geste défait le formatage entier");
    }

    /** B-25 — le document déjà en forme ne crée pas d'étape muette. */
    @Test
    void formatPlanStaysSilentOnShapedDocument() {
        String enForme = "class Exemple {\n    void corps() {\n"
                + "        int nombre = 1;\n    }\n}\n";
        TextSession session = TextSession.of(enForme, () -> 0L);
        SyntaxCoordinator coordinateur = SyntaxCoordinator.on(session,
                JavaSyntax.INSTANCE);
        EditPlan plan;
        try {
            plan = AtelierRefactor.planFormat(session.document(),
                    coordinateur.analysis());
        } finally {
            coordinateur.close();
        }
        assertNull(plan, "rien à changer : pas de plan, pas d'étape");
    }

    /** B-19, B-11 — le renommage porte les usages entiers, annulable d'un geste. */
    @Test
    void renameCarriesWholeUsagesAndUndoes() {
        TextSession session = TextSession.of(DOCUMENT_A_RENOMMER, () -> 0L);
        SyntaxCoordinator coordinateur = SyntaxCoordinator.on(session,
                JavaSyntax.INSTANCE);
        jo.celeris.core.intelligence.Renommage.PlanRenommage plan;
        try {
            plan = AtelierRefactor.planRenommage(session.document(),
                    coordinateur.analysis(), URI, "total", "somme");
        } finally {
            coordinateur.close();
        }
        assertTrue(plan.conflits().isEmpty(), "aucun conflit au renommage");
        assertTrue(AtelierRefactor.appliquerRenommage(session, URI, plan),
                "les éditions du fichier s'appliquent");
        assertFalse(session.document().text().contains("total"),
                "aucun usage de l'ancien nom ne reste");
        assertEquals(3, session.document().text().split("somme", -1).length - 1,
                "les trois usages portent le nouveau nom");
        session.undo();
        assertEquals(DOCUMENT_A_RENOMMER, session.document().text(),
                "un geste défait le renommage entier");
    }

    /** B-27 — l'import se pose à la tête du bloc, le doublon se refuse. */
    @Test
    void importInsertsAtHeadAndRefusesDuplicates() {
        TextSession session = TextSession.of(
                "package jo.exemple;\n\nclass Exemple {\n}\n", () -> 0L);
        EditPlan plan = AtelierRefactor.planImport(session.document(),
                "import java.util.List;\n");
        assertNotNull(plan, "l'import absent a son plan");
        assertTrue(session.apply(plan.transaction(), plan.selection()));
        String texte = session.document().text();
        assertTrue(texte.startsWith("package jo.exemple;\nimport java.util.List;\n"),
                "l'import se pose juste sous le paquet");
        assertNull(AtelierRefactor.planImport(session.document(),
                        "import java.util.List;\n"),
                "le doublon se refuse — rien ne bouge");
    }
}
