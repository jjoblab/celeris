package jo.celeris.core.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import org.junit.jupiter.api.Test;

import jo.celeris.core.language.model.LanguageConfiguration;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;

/**
 * Commenter et décommenter (A-11, M5) : le préfixe suit le langage —
 * Java et Kotlin au double barre, XML au bloc, Python au dièse, JSON
 * n'a pas de commentaire et l'action est masquée. Le basculement
 * commente tout le bloc si une ligne ne l'est pas, le retire si toutes
 * le sont.
 */
class CommenterTest {

    /** La configuration d'un langage au commentaire de ligne et de bloc. */
    private static LanguageConfiguration configuration(
            @Nullable String ligne, @Nullable String blocDebut,
            @Nullable String blocFin) {
        return new LanguageConfiguration(
                new LanguageConfiguration.Comments(ligne, blocDebut, blocFin),
                java.util.List.of(), java.util.List.of(), java.util.List.of(),
                null, new LanguageConfiguration.Folding(false, false, null, null),
                new LanguageConfiguration.IndentRules(null, null, null, null),
                4, false);
    }

    private static final LanguageConfiguration JAVA =
            configuration("// ", "/* ", " */");
    private static final LanguageConfiguration XML =
            configuration(null, "<!--", "-->");
    private static final LanguageConfiguration JSON =
            configuration(null, null, null);
    private static final LanguageConfiguration PYTHON =
            configuration("# ", null, null);

    /** Commenter une ligne Java : le préfixe s'insère en tête de ligne. */
    @Test
    void commenterUneLigneJava() {
        Document doc = Document.of("int a;\nint b;");
        Optional<EditPlan> plan = Commenter.toggle(doc,
                MultiSelection.of(new SelRange(0, 6)), CommentRules.of(JAVA));
        assertTrue(plan.isPresent());
        EditPlan geste = plan.get();
        assertEquals(1, geste.transaction().edits().size());
        assertEquals(0, geste.transaction().edits().get(0).start());
        assertEquals("// ", geste.transaction().edits().get(0).inserted());
        assertTrue(geste.transaction().edits().get(0).removed().isEmpty());
    }

    /** Décommenter : le préfixe se retire, toutes les lignes le sont. */
    @Test
    void decommenterRetireLePrefixe() {
        Document doc = Document.of("// int a;\n// int b;");
        // la sélection couvre les deux lignes ([0,15) touche les lignes 0 et 1)
        Optional<EditPlan> plan = Commenter.toggle(doc,
                MultiSelection.of(new SelRange(0, 15)), CommentRules.of(JAVA));
        assertTrue(plan.isPresent());
        assertEquals(2, plan.get().transaction().edits().size());
        assertEquals("// ", plan.get().transaction().edits().get(0).removed());
        assertEquals("// ", plan.get().transaction().edits().get(1).removed());
        // décalé du retrait des trois caractères de la première ligne
        assertEquals(7, plan.get().transaction().edits().get(1).start());
    }

    /** Le basculement mixte commente tout : une ligne non commentée suffit. */
    @Test
    void basculementMixteCommenteTout() {
        Document doc = Document.of("int a;\n// int b;");
        Optional<EditPlan> plan = Commenter.toggle(doc,
                MultiSelection.of(new SelRange(0, 15)), CommentRules.of(JAVA));
        assertTrue(plan.isPresent());
        assertEquals(2, plan.get().transaction().edits().size());
        assertEquals("// ", plan.get().transaction().edits().get(0).inserted());
        assertEquals("// ", plan.get().transaction().edits().get(1).inserted());
    }

    /** XML : pas de commentaire de ligne, le bloc entoure la sélection. */
    @Test
    void xmlEntoureLaSelection() {
        Document doc = Document.of("<a>\n  <b/>\n</a>");
        // sélection sur «   <b/> » (offsets 4..10)
        Optional<EditPlan> plan = Commenter.toggle(doc,
                MultiSelection.of(new SelRange(4, 10)), CommentRules.of(XML));
        assertTrue(plan.isPresent());
        EditPlan geste = plan.get();
        assertEquals("  <b/>", geste.transaction().edits().get(0).removed());
        assertEquals("<!--  <b/>-->", geste.transaction().edits().get(0).inserted());
        // le contenu reste sélectionné dans le bloc
        SelRange apres = geste.selection().ranges().get(0);
        assertEquals(8, apres.start());
        assertEquals(14, apres.end());
    }

    /** XML : le bloc commenté se retire au second basculement. */
    @Test
    void xmlRetireLeBlocAuSecondBasculement() {
        Document doc = Document.of("<!--  <b/>-->");
        Optional<EditPlan> plan = Commenter.toggle(doc,
                MultiSelection.of(new SelRange(0, 13)), CommentRules.of(XML));
        assertTrue(plan.isPresent());
        assertEquals("<!--  <b/>-->", plan.get().transaction().edits().get(0).removed());
        assertEquals("  <b/>", plan.get().transaction().edits().get(0).inserted());
    }

    /** JSON n'a pas de commentaire : l'action est masquée (A-11). */
    @Test
    void jsonNaPasDeCommentaire() {
        assertFalse(CommentRules.of(JSON).commentable());
        Document doc = Document.of("{\"a\": 1}");
        Optional<EditPlan> plan = Commenter.toggle(doc,
                MultiSelection.of(new SelRange(0, 8)), CommentRules.of(JSON));
        assertFalse(plan.isPresent());
    }

    /** La longue traîne : Python commente au dièse. */
    @Test
    void pythonCommenteAuDiese() {
        Document doc = Document.of("print(1)");
        Optional<EditPlan> plan = Commenter.toggle(doc,
                MultiSelection.of(new SelRange(0, 8)), CommentRules.of(PYTHON));
        assertTrue(plan.isPresent());
        assertEquals("# ", plan.get().transaction().edits().get(0).inserted());
    }

    /** La configuration vide masque l'action plutôt que de deviner. */
    @Test
    void configurationSansCommentaireMasque() {
        assertFalse(CommentRules.of(LanguageConfiguration.EMPTY).commentable());
    }

    /** La ligne vide ne bloque pas le commentaire du bloc. */
    @Test
    void laLigneVideNeComptePas() {
        Document doc = Document.of("\n// a");
        Optional<EditPlan> plan = Commenter.toggle(doc,
                MultiSelection.of(new SelRange(0, 5)), CommentRules.of(JAVA));
        assertTrue(plan.isPresent());
        // la ligne vide n'est pas commentée, la commentée est décommentée
        assertEquals(1, plan.get().transaction().edits().size());
        assertEquals("// ", plan.get().transaction().edits().get(0).removed());
    }
}
