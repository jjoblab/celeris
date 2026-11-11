package jo.celeris.core.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.JavaSyntax;
import jo.celeris.core.syntax.SyntaxService;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextSession;

/**
 * La frappe intelligente dédiée à Java (chantier C8) : le découpage des
 * chaînes à l'Entrée — le caret dans un littéral ferme la chaîne, pose
 * la concaténation et la rouvre à la ligne — et le cran des labels de
 * cas — l'Entrée qui suit « case … : » gagne un cran, le corps du cas
 * vit sous son label. Le reste retombe sur le moteur générique,
 * geste pour geste.
 */
class JavaSmartTypingTest {

    private final SyntaxService service = SyntaxService.of(JavaSyntax.INSTANCE);

    // ------------------------------------------------ découpage de chaîne

    /** Le caret dans une chaîne : elle se ferme, se concatène, se rouvre. */
    @Test
    void decoupeChaineAuMilieu() {
        Document doc = Document.of("        String s = \"abcdef\";");
        // la citation ouvre en 19, « abc » vit de 20 à 22 : le caret en 23
        EditPlan plan = JavaSmartTyping.enter(doc, analyse(doc),
                MultiSelection.caretsAt(23), IndentStyle.spaces(4));
        assertEquals("\" +\n            \"",
                plan.transaction().edits().get(0).inserted());
        assertEquals(23, plan.transaction().edits().get(0).start());
        TextSession session = TextSession.of(
                "        String s = \"abcdef\";", () -> 0L);
        session.apply(plan.transaction(), plan.selection());
        assertEquals("        String s = \"abc\" +\n            \"def\";",
                session.text());
        assertEquals(23 + 17, session.selection().carets().get(0));
    }

    /** Le caret juste après l'ouvrante : la chaîne vide se pose, le + suit. */
    @Test
    void decoupeChaineApresLOuvrante() {
        Document doc = Document.of("        String s = \"abcdef\";");
        EditPlan plan = JavaSmartTyping.enter(doc, analyse(doc),
                MultiSelection.caretsAt(20), IndentStyle.spaces(4));
        assertEquals("\" +\n            \"",
                plan.transaction().edits().get(0).inserted());
    }

    /** Le caret juste avant la fermante : la suite rouverte est vide. */
    @Test
    void decoupeChaineAvantLaFermante() {
        Document doc = Document.of("String s = \"abcdef\";");
        EditPlan plan = JavaSmartTyping.enter(doc, analyse(doc),
                MultiSelection.caretsAt(18), IndentStyle.spaces(4));
        TextSession session = TextSession.of("String s = \"abcdef\";", () -> 0L);
        session.apply(plan.transaction(), plan.selection());
        assertEquals("String s = \"abcdef\" +\n    \"\";", session.text());
    }

    /** Le style à tabulations : la ligne rouverte gagne une tabulation. */
    @Test
    void decoupeChaineStyleTabulations() {
        Document doc = Document.of("String s = \"abcdef\";");
        EditPlan plan = JavaSmartTyping.enter(doc, analyse(doc),
                MultiSelection.caretsAt(15), IndentStyle.tabs(4));
        assertEquals("\" +\n\t\"", plan.transaction().edits().get(0).inserted());
    }

    /** La citation échappée ne compte pas : le caret reste dans la chaîne. */
    @Test
    void citationEchappeeNeComptePas() {
        Document doc = Document.of("String s = \"a\\\"bc\";");
        // la citation ouvre en 11, l'échappée vit en 13-14, b en 15 : le caret en 16
        EditPlan plan = JavaSmartTyping.enter(doc, analyse(doc),
                MultiSelection.caretsAt(16), IndentStyle.spaces(4));
        TextSession session = TextSession.of("String s = \"a\\\"bc\";", () -> 0L);
        session.apply(plan.transaction(), plan.selection());
        assertEquals("String s = \"a\\\"b\" +\n    \"c\";", session.text());
    }

    /** Le caret dans une séquence d'échappement refuse : jamais couper une échappée. */
    @Test
    void caretDansUneEchappeeRefuse() {
        Document doc = Document.of("String s = \"ab\\ncd\";");
        // la barre oblique inverse vit en 14 : le caret en 15, dans l'échappée
        EditPlan plan = JavaSmartTyping.enter(doc, analyse(doc),
                MultiSelection.caretsAt(15), IndentStyle.spaces(4));
        assertFalse(plan.transaction().edits().get(0).inserted().contains(" +"));
    }

    /** La chaîne non fermée sur la ligne refuse — on ne découpe pas l'ouvert. */
    @Test
    void chaineNonFermeeRefuse() {
        Document doc = Document.of("String s = \"abcdef;");
        EditPlan plan = JavaSmartTyping.enter(doc, analyse(doc),
                MultiSelection.caretsAt(15), IndentStyle.spaces(4));
        assertFalse(plan.transaction().edits().get(0).inserted().contains(" +"));
    }

    /** Le bloc de texte refuse : sa coupe n'est pas une concaténation. */
    @Test
    void blocDeTexteRefuse() {
        Document doc = Document.of("String s = \"\"\"\n        corps\n        \"\"\";");
        EditPlan plan = JavaSmartTyping.enter(doc, analyse(doc),
                MultiSelection.caretsAt(14), IndentStyle.spaces(4));
        // retombée générique : saut indenté, jamais un découpage
        assertFalse(plan.transaction().edits().get(0).inserted().contains(" +"));
    }

    /** Le caret hors chaîne retombe sur le moteur générique. */
    @Test
    void horsChaineRetombeSurGenerique() {
        Document doc = Document.of("class A {\n}");
        EditPlan plan = JavaSmartTyping.enter(doc, analyse(doc),
                MultiSelection.caretsAt(9), IndentStyle.spaces(4));
        assertEquals("\n    ", plan.transaction().edits().get(0).inserted());
        assertEquals(List.of(14), plan.selection().carets());
    }

    // ------------------------------------------------- cran des labels de cas

    /** « case X: » suivi de l'Entrée : la ligne du corps gagne un cran. */
    @Test
    void labelDeCasGagneUnCran() {
        Document doc = Document.of("    switch (x) {\n        case 1:\n    }");
        // le label vit en ligne 1, son deux-points en 31 : le caret en 32
        EditPlan plan = JavaSmartTyping.enter(doc, analyse(doc),
                MultiSelection.caretsAt(32), IndentStyle.spaces(4));
        assertEquals("\n            ", plan.transaction().edits().get(0).inserted());
        assertEquals(List.of(45), plan.selection().carets());
    }

    /** Le label flèche « case X -> » gagne son cran aussi. */
    @Test
    void labelFlecheGagneUnCran() {
        Document doc = Document.of("        case 1 ->");
        EditPlan plan = JavaSmartTyping.enter(doc, analyse(doc),
                MultiSelection.caretsAt(17), IndentStyle.spaces(4));
        assertEquals("\n            ", plan.transaction().edits().get(0).inserted());
    }

    /** « default: » gagne le même cran que ses cases. */
    @Test
    void defaultGagneUnCran() {
        Document doc = Document.of("        default:");
        EditPlan plan = JavaSmartTyping.enter(doc, analyse(doc),
                MultiSelection.caretsAt(16), IndentStyle.spaces(4));
        assertEquals("\n            ", plan.transaction().edits().get(0).inserted());
    }

    /** Les blancs de queue avant l'Entrée ne masquent pas le label. */
    @Test
    void blancsDeQueueNeMasquentPas() {
        Document doc = Document.of("        case 1:  ");
        EditPlan plan = JavaSmartTyping.enter(doc, analyse(doc),
                MultiSelection.caretsAt(17), IndentStyle.spaces(4));
        assertEquals("\n            ", plan.transaction().edits().get(0).inserted());
    }

    /** Le label multi-constantes « case A, B: » gagne le cran. */
    @Test
    void labelMultiConstantesGagneUnCran() {
        Document doc = Document.of("        case A, B:");
        EditPlan plan = JavaSmartTyping.enter(doc, analyse(doc),
                MultiSelection.caretsAt(18), IndentStyle.spaces(4));
        assertEquals("\n            ", plan.transaction().edits().get(0).inserted());
    }

    /** Une ligne qui n'est pas un label retombe sur le générique. */
    @Test
    void pasUnLabelRetombeSurGenerique() {
        Document doc = Document.of("        int x = 1;");
        EditPlan plan = JavaSmartTyping.enter(doc, analyse(doc),
                MultiSelection.caretsAt(18), IndentStyle.spaces(4));
        assertEquals("\n", plan.transaction().edits().get(0).inserted());
    }

    /** Le ternaire « a ? b : » n'est pas un label : le mot de tête décide. */
    @Test
    void ternaireNEstPasUnLabel() {
        Document doc = Document.of("        int y = a ? b :");
        EditPlan plan = JavaSmartTyping.enter(doc, analyse(doc),
                MultiSelection.caretsAt(22), IndentStyle.spaces(4));
        assertFalse(plan.transaction().edits().get(0).inserted().contains("            "));
    }

    /** La ligne de commentaire « // case X: » n'est pas un label. */
    @Test
    void commentaireNEstPasUnLabel() {
        Document doc = Document.of("        // case X:");
        EditPlan plan = JavaSmartTyping.enter(doc, analyse(doc),
                MultiSelection.caretsAt(17), IndentStyle.spaces(4));
        assertFalse(plan.transaction().edits().get(0).inserted().contains("            "));
    }

    /** Le plan du cran s'applique et s'annule par la voie unique. */
    @Test
    void cranAppliqueEtAnnule() {
        String texte = "        case 1:";
        Document doc = Document.of(texte);
        EditPlan plan = JavaSmartTyping.enter(doc, analyse(doc),
                MultiSelection.caretsAt(15), IndentStyle.spaces(4));
        TextSession session = TextSession.of(texte, () -> 0L);
        assertTrue(session.apply(plan.transaction(), plan.selection()));
        assertEquals("        case 1:\n            ", session.text());
        assertTrue(session.undo());
        assertEquals(texte, session.text());
    }

    // ---------------------------------------------------------- multi-curseurs

    /**
     * Deux curseurs, un dans une chaîne, l'autre en fin de label : chacun
     * son geste dédié, les deltas s'enchaînent.
     */
    @Test
    void multiCurseursGestesMeles() {
        String texte = "String s = \"abcdef\";\n        case 1:";
        Document doc = Document.of(texte);
        // caret 0 : entre c et d de la chaîne (offset 15) ;
        // caret 1 : fin du label (offset 36)
        EditPlan plan = JavaSmartTyping.enter(doc, analyse(doc),
                MultiSelection.caretsAt(15, 36), IndentStyle.spaces(4));
        assertEquals(2, plan.transaction().edits().size());
        assertEquals("\" +\n    \"", plan.transaction().edits().get(0).inserted());
        assertEquals("\n            ", plan.transaction().edits().get(1).inserted());
        assertEquals(36 + 9, plan.transaction().edits().get(1).start());
        TextSession session = TextSession.of(texte, () -> 0L);
        session.apply(plan.transaction(), plan.selection());
        assertEquals("String s = \"abc\" +\n    \"def\";\n        case 1:\n            ",
                session.text());
    }

    /** Une sélection non vide sur un label : remplacée par le saut crâné. */
    @Test
    void selectionSurLabelRemplacee() {
        Document doc = Document.of("        case 1: DUO");
        EditPlan plan = JavaSmartTyping.enter(doc, analyse(doc),
                MultiSelection.of(new SelRange(16, 19)), IndentStyle.spaces(4));
        assertEquals("DUO", plan.transaction().edits().get(0).removed());
        assertEquals("\n            ", plan.transaction().edits().get(0).inserted());
    }

    // --------------------------------------------------------------- outil

    private Analysis analyse(Document doc) {
        return service.analyseFull(doc);
    }
}
