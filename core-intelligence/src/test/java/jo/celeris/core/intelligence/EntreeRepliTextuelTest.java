package jo.celeris.core.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.TextSession;

/**
 * Le repli textuel de l'Entrée (la discipline du dépôt de départ) :
 * l'arbre suit la frappe — la publication ne rattrape l'édition qu'à
 * la ré-analyse (la rétention du moteur hors fil, 250 ms), l'Entrée
 * qui tombe DANS la fenêtre ne peut compter que sur le texte.
 *
 * <p>Écrits ROUGES sur le code d'avant redressement : le repli ne
 * gagnait jamais le cran de l'ouvrant (l'Entrée après
 * « {@code class A {} } » collait le corps au cran courant), et le
 * fermant de la paire vide descendait à la COLONNE ZÉRO — la
 * profondeur lue sans arbre rendait toujours zéro, le cran moins un
 * n'existait pas. Au premier niveau, la garde
 * {@code !indentation.isEmpty()} tuait le geste deux-sauts tout
 * court : une seule ligne nue naissait.</p>
 */
class EntreeRepliTextuelTest {

    /** Le style canonique de la démonstration. */
    private static final IndentStyle STYLE = IndentStyle.spaces(4);

    /**
     * LE CAS SIGNALÉ — l'Entrée entre accolades d'une méthode à la
     * frappe : deux sauts, le corps au cran plus un, le fermant au
     * cran de la ligne d'ouverture. Sans arbre.
     */
    @Test
    void entreeEntreAccoladesDeMethodeSansArbre() {
        Document doc = Document.of("class A {\n    void m() {}\n}");
        // caret entre « { » et « } » de la méthode (offset 24)
        EditPlan plan = SmartTyping.enter(doc, null,
                MultiSelection.caretsAt(24), STYLE);
        assertEquals("\n        \n    ", plan.transaction().edits().get(0).inserted());
        assertEquals(List.of(33), plan.selection().carets());
    }

    /** Paire vide au premier niveau : le geste deux-sauts vit aussi sans arbre. */
    @Test
    void paireVideAuPremierNiveauSansArbre() {
        Document doc = Document.of("class A {}");
        EditPlan plan = SmartTyping.enter(doc, null,
                MultiSelection.caretsAt(9), STYLE);
        assertEquals("\n    \n", plan.transaction().edits().get(0).inserted());
        assertEquals(List.of(14), plan.selection().carets());
    }

    /** Les crochets suivent la même discipline que les accolades. */
    @Test
    void paireVideCrochetsSansArbre() {
        Document doc = Document.of("x = []");
        // caret entre « [ » (offset 4) et « ] » (offset 5)
        EditPlan plan = SmartTyping.enter(doc, null,
                MultiSelection.caretsAt(5), STYLE);
        assertEquals("\n    \n", plan.transaction().edits().get(0).inserted());
        assertEquals(List.of(10), plan.selection().carets());
    }

    /** Les parenthèses suivent la même discipline que les accolades. */
    @Test
    void paireVideParenthesesSansArbre() {
        Document doc = Document.of("f()");
        // caret entre « ( » (offset 1) et « ) » (offset 2)
        EditPlan plan = SmartTyping.enter(doc, null,
                MultiSelection.caretsAt(2), STYLE);
        assertEquals("\n    \n", plan.transaction().edits().get(0).inserted());
    }

    /**
     * L'ouvrant en fin de ligne gagne son cran — sans arbre comme
     * avec : « class A { » + Entrée pose le corps au cran un.
     */
    @Test
    void ouvrantEnFinDeLigneGagneSonCranSansArbre() {
        Document doc = Document.of("class A {");
        EditPlan plan = SmartTyping.enter(doc, null,
                MultiSelection.caretsAt(9), STYLE);
        assertEquals("\n    ", plan.transaction().edits().get(0).inserted());
        assertEquals(List.of(14), plan.selection().carets());
    }

    /** L'ouvreur imbriqué gagne son cran sur le retrait de sa ligne. */
    @Test
    void ouvrantImbriqueGagneSonCranSansArbre() {
        Document doc = Document.of("class A {\n    void m() {");
        // caret en fin de « void m() { » (offset 24)
        EditPlan plan = SmartTyping.enter(doc, null,
                MultiSelection.caretsAt(24), STYLE);
        assertEquals("\n        ", plan.transaction().edits().get(0).inserted());
    }

    /** Sans ouvrant collé, la ligne prête son retrait — pas de cran fantôme. */
    @Test
    void sansOuvrantLaLignePreteSonRetrait() {
        Document doc = Document.of("    int x = 1;");
        EditPlan plan = SmartTyping.enter(doc, null,
                MultiSelection.caretsAt(14), STYLE);
        assertEquals("\n    ", plan.transaction().edits().get(0).inserted());
    }

    /**
     * Le fermant après des blancs — ni paire vide ni arbre : le saut
     * simple, jamais le fermant à la colonne zéro (l'ancien repli
     * posait « \n + retrait + \n » : le fermant tombait nu).
     */
    @Test
    void fermantTardifSansPaireSautSimple() {
        Document doc = Document.of("    x = f(a)");
        // caret avant « ) » — l'argument reste, pas de paire vide
        EditPlan plan = SmartTyping.enter(doc, null,
                MultiSelection.caretsAt(11), STYLE);
        assertEquals("\n    ", plan.transaction().edits().get(0).inserted());
    }

    /** La coupe dans un littéral ne gagne ni cran ni descente. */
    @Test
    void dansLitteralPasDeCran() {
        Document doc = Document.of("s = \"{}\"");
        // caret entre « { » (offset 5) et « } » (offset 6), DANS la chaîne
        EditPlan plan = SmartTyping.enter(doc, null,
                MultiSelection.caretsAt(6), STYLE);
        assertEquals("\n", plan.transaction().edits().get(0).inserted());
    }

    /** Le plan s'applique et s'annule par la voie unique. */
    @Test
    void planDeRepliAppliqueEtAnnule() {
        String texte = "class A {\n    void m() {}\n}";
        Document doc = Document.of(texte);
        EditPlan plan = SmartTyping.enter(doc, null,
                MultiSelection.caretsAt(24), STYLE);
        TextSession session = TextSession.of(texte, () -> 0L);
        session.setSelection(MultiSelection.caretsAt(24));
        assertTrue(session.apply(plan.transaction(), plan.selection()));
        assertEquals("class A {\n    void m() {\n        \n    }\n}",
                session.text());
        assertTrue(session.undo());
        assertEquals(texte, session.text());
    }
}
