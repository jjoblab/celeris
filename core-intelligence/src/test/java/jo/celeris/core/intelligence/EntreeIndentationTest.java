package jo.celeris.core.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.JavaSyntax;
import jo.celeris.core.syntax.SyntaxService;
import jo.celeris.core.syntax.XmlSyntax;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;

/**
 * L'indentation automatique à l'Entrée (A-06, M5) : le retrait attendu de
 * la nouvelle ligne se calcule depuis l'arbre — le nombre de nœuds
 * structurants qui englobent le caret — avec les options de style
 * (espaces ou tabulations, largeur). Le fermant qui reste sur la ligne
 * descend seul sous le curseur : l'Entrée entre accolades produit les deux
 * sauts. Sans arbre, le repli conserve l'indentation de la ligne courante.
 */
class EntreeIndentationTest {

    private final SyntaxService service = SyntaxService.of(JavaSyntax.INSTANCE);

    /** Entrée en fin de ligne ouvrante : la nouvelle ligne gagne un cran. */
    @Test
    void entreeApresOuvrantGagneUnCran() {
        Document doc = Document.of("class A {\n}");
        Analysis analyse = service.analyseFull(doc);
        EditPlan plan = SmartTyping.enter(doc, analyse,
                MultiSelection.caretsAt(9), IndentStyle.spaces(4));
        assertEquals(1, plan.transaction().edits().size());
        assertEquals("\n    ", plan.transaction().edits().get(0).inserted());
        assertEquals(9, plan.transaction().edits().get(0).start());
        assertEquals(List.of(14), plan.selection().carets());
    }

    /** Entrée entre accolades : deux sauts, le fermant descend au cran moins un. */
    @Test
    void entreeEntreAccoladesDescendLeFermant() {
        Document doc = Document.of("class A {}");
        Analysis analyse = service.analyseFull(doc);
        EditPlan plan = SmartTyping.enter(doc, analyse,
                MultiSelection.caretsAt(9), IndentStyle.spaces(4));
        assertEquals("\n    \n", plan.transaction().edits().get(0).inserted());
        assertEquals(List.of(14), plan.selection().carets());
    }

    /** Le retrait suit l'imbrication : le corps de méthode gagne deux crans. */
    @Test
    void corpsDeMethodeDeuxCrans() {
        String texte = "class A {\n    void m() {\n    }\n}";
        Document doc = Document.of(texte);
        Analysis analyse = service.analyseFull(doc);
        // caret en fin de « void m() { » (offset 24)
        EditPlan plan = SmartTyping.enter(doc, analyse,
                MultiSelection.caretsAt(24), IndentStyle.spaces(4));
        assertEquals("\n        ", plan.transaction().edits().get(0).inserted());
        assertEquals(List.of(33), plan.selection().carets());
    }

    /** Une ligne simple garde son niveau : pas de cran fantôme. */
    @Test
    void ligneSimpleGardeSonNiveau() {
        String texte = "class A {\n    int x;\n}";
        Document doc = Document.of(texte);
        Analysis analyse = service.analyseFull(doc);
        // caret en fin de « int x; » (offset 20, avant le saut)
        EditPlan plan = SmartTyping.enter(doc, analyse,
                MultiSelection.caretsAt(20), IndentStyle.spaces(4));
        assertEquals("\n    ", plan.transaction().edits().get(0).inserted());
    }

    /** Le style à tabulations produit des tabulations. */
    @Test
    void styleTabulations() {
        Document doc = Document.of("class A {\n}");
        Analysis analyse = service.analyseFull(doc);
        EditPlan plan = SmartTyping.enter(doc, analyse,
                MultiSelection.caretsAt(9), IndentStyle.tabs(4));
        assertEquals("\n\t", plan.transaction().edits().get(0).inserted());
    }

    /** XML : l'imbrication des éléments compte, sans crochet lexicographique. */
    @Test
    void xmlElementImbriqueCompte() {
        Document doc = Document.of("<a>\n</a>");
        Analysis analyse = SyntaxService.of(XmlSyntax.INSTANCE).analyseFull(doc);
        EditPlan plan = SmartTyping.enter(doc, analyse,
                MultiSelection.caretsAt(3), IndentStyle.spaces(2));
        assertEquals("\n  ", plan.transaction().edits().get(0).inserted());
        assertEquals(List.of(6), plan.selection().carets());
    }

    /** Sans arbre, le repli conserve l'indentation de la ligne courante. */
    @Test
    void sansArbreConserveIndentationCourante() {
        Document doc = Document.of("  x");
        EditPlan plan = SmartTyping.enter(doc, null,
                MultiSelection.caretsAt(3), IndentStyle.spaces(4));
        assertEquals("\n  ", plan.transaction().edits().get(0).inserted());
        assertEquals(List.of(6), plan.selection().carets());
    }

    /** Une sélection non vide est remplacée par le saut indenté. */
    @Test
    void selectionRemplaceeParSautIndente() {
        Document doc = Document.of("class A {\n    int x;\n}");
        Analysis analyse = service.analyseFull(doc);
        EditPlan plan = SmartTyping.enter(doc, analyse,
                MultiSelection.of(new SelRange(14, 20)), IndentStyle.spaces(4));
        assertEquals("int x;", plan.transaction().edits().get(0).removed());
        assertEquals("\n    ", plan.transaction().edits().get(0).inserted());
    }

    /** Multi-curseurs : chaque saut est indenté, les deltas s'ajustent. */
    @Test
    void multiCurseursChacunSonSaut() {
        String texte = "class A {\n    int x;\n}";
        Document doc = Document.of(texte);
        Analysis analyse = service.analyseFull(doc);
        // fin de ligne 0 (offset 9) et fin de ligne 1 (offset 20)
        EditPlan plan = SmartTyping.enter(doc, analyse,
                MultiSelection.caretsAt(9, 20), IndentStyle.spaces(4));
        assertEquals(2, plan.transaction().edits().size());
        assertEquals(9, plan.transaction().edits().get(0).start());
        assertEquals(25, plan.transaction().edits().get(1).start());
        assertEquals(List.of(14, 30), plan.selection().carets());
    }

    /** Le plan s'applique par la voie unique, l'annulation restitue tout. */
    @Test
    void planAppliqueEtAnnule() {
        String texte = "class A {\n    int x;\n}";
        Document doc = Document.of(texte);
        Analysis analyse = service.analyseFull(doc);
        EditPlan plan = SmartTyping.enter(doc, analyse,
                MultiSelection.caretsAt(20), IndentStyle.spaces(4));
        jo.celeris.core.text.TextSession session =
                jo.celeris.core.text.TextSession.of(texte, () -> 0L);
        session.setSelection(MultiSelection.caretsAt(20));
        assertTrue(session.apply(plan.transaction(), plan.selection()));
        assertEquals("class A {\n    int x;\n    \n}", session.text());
        assertTrue(session.undo());
        assertEquals(texte, session.text());
        assertEquals(List.of(20), session.selection().carets());
    }
}
