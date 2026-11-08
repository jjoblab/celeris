package jo.celeris.android.view;

import static org.junit.Assert.assertEquals;

import android.view.KeyEvent;

import jo.celeris.core.language.LanguageSupport;
import jo.celeris.core.syntax.JavaSyntax;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * La frappe intelligente branchée à la vue (M7.5, §8) : les moteurs
 * purs — {@code SmartTyping}, {@code IndentEngine},
 * {@code PasteReindenter} — répondent enfin aux événements RÉELS de
 * la vue, par la connexion d'entrée et le clavier matériel, quand
 * l'hôte pose un fournisseur (A-05 à A-09). Sans fournisseur, la
 * frappe passe telle quelle : la vue ne décide jamais à la place de
 * l'hôte.
 *
 * <p>Écrits ROUGES sur le code courant : l'ouvrant ne pose pas sa
 * paire, l'Entrée est muette, le retour arrière ne connaît pas les
 * paires vides, le collage multiligne perd son indentation.</p>
 */
@RunWith(RobolectricTestRunner.class)
public class SmartTypingViewTest {

    /** Le banc coloré et branché : Java analysé, fournisseur standard posé. */
    private static EditorTestRig bancBranche(String contenu) {
        EditorTestRig banc = EditorTestRig.sur(
                RuntimeEnvironment.getApplication(), contenu);
        banc.vue().setLanguage(LanguageSupport.of(JavaSyntax.INSTANCE));
        banc.vue().intelligence().setProvider(IntelligenceHub.builtins());
        banc.publierAnalyse();
        return banc;
    }

    private static void curseurA(EditorTestRig banc, int offset) {
        banc.vue().session().setSelection(
                MultiSelection.of(SelRange.caretAt(offset)));
    }

    private static String texte(EditorTestRig banc) {
        return banc.vue().session().document().text();
    }

    private static int caret(EditorTestRig banc) {
        return banc.vue().session().selection().ranges().get(0).caret();
    }

    private static void frappe(EditorTestRig banc, int code) {
        banc.vue().onKeyDown(code,
                new KeyEvent(KeyEvent.ACTION_DOWN, code));
    }

    /** A-05 — l'ouvrant insère sa paire, le curseur vit au milieu. */
    @Test
    public void typedOpenInsertsItsPairWithCaretBetween() {
        EditorTestRig banc = bancBranche("int valeur");
        curseurA(banc, 10);
        banc.validerTexte("(");
        assertEquals("l'ouvrant pose sa paire", "int valeur()", texte(banc));
        assertEquals("le curseur vit entre les deux", 11, caret(banc));
    }

    /** A-08 — l'ouvrant sur une sélection l'entoure, le contenu reste sélectionné. */
    @Test
    public void typedOpenAroundSelectionSurroundsIt() {
        EditorTestRig banc = bancBranche("int valeur");
        banc.vue().session().setSelection(
                MultiSelection.of(new SelRange(4, 10)));
        banc.validerTexte("(");
        assertEquals("la sélection se retrouve entourée",
                "int (valeur)", texte(banc));
        SelRange plage = banc.vue().session().selection().ranges().get(0);
        assertEquals("le contenu reste sélectionné", 5, plage.start());
        assertEquals(11, plage.end());
    }

    /** A-05 — le fermant déjà présent sous le curseur se saute, rien ne se double. */
    @Test
    public void typedClosingSkipsOverTheExistingOne() {
        EditorTestRig banc = bancBranche("f()");
        curseurA(banc, 2);
        banc.validerTexte(")");
        assertEquals("aucune double fermeture", "f()", texte(banc));
        assertEquals("le curseur saute le fermant", 3, caret(banc));
    }

    /** A-06 — l'Entrée entre accolades produit les deux sauts indentés. */
    @Test
    public void enterBetweenBracesMakesTwoIndentedJumps() {
        EditorTestRig banc = bancBranche("class Exemple {}");
        curseurA(banc, 15);
        banc.validerTexte("\n");
        assertEquals("le fermant descend seul sous le curseur",
                "class Exemple {\n    \n}", texte(banc));
        assertEquals("le curseur vit sur la ligne indentée", 20, caret(banc));
    }

    /** A-06 — l'Entrée du clavier matériel suit le même chemin indenté. */
    @Test
    public void hardwareEnterIndentsTheNewLine() {
        EditorTestRig banc = bancBranche("class Exemple {\n    int x;\n}");
        curseurA(banc, 26);
        frappe(banc, KeyEvent.KEYCODE_ENTER);
        assertEquals("la nouvelle ligne porte l'indentation attendue",
                "class Exemple {\n    int x;\n    \n}", texte(banc));
        assertEquals(31, caret(banc));
    }

    /** A-06 — sans fournisseur, l'Entrée reste un saut de ligne nu, jamais muette. */
    @Test
    public void withoutProviderEnterInsertsPlainNewline() {
        EditorTestRig banc = EditorTestRig.sur(
                RuntimeEnvironment.getApplication(), "int x;");
        banc.vue().setLanguage(LanguageSupport.of(JavaSyntax.INSTANCE));
        curseurA(banc, 6);
        frappe(banc, KeyEvent.KEYCODE_ENTER);
        assertEquals("l'Entrée sans fournisseur reste un saut nu",
                "int x;\n", texte(banc));
        assertEquals(7, caret(banc));
    }

    /** A-05 — sans fournisseur, l'ouvrant s'insère seul : la vue ne décide pas. */
    @Test
    public void withoutProviderTypingStaysPlain() {
        EditorTestRig banc = EditorTestRig.sur(
                RuntimeEnvironment.getApplication(), "int valeur");
        banc.vue().setLanguage(LanguageSupport.of(JavaSyntax.INSTANCE));
        curseurA(banc, 10);
        banc.validerTexte("(");
        assertEquals("sans fournisseur, la frappe passe telle quelle",
                "int valeur(", texte(banc));
        assertEquals(11, caret(banc));
    }

    /** A-07 — le retour arrière sur une paire vide retire les deux caractères. */
    @Test
    public void backspaceOverEmptyPairRemovesBoth() {
        EditorTestRig banc = bancBranche("f()");
        curseurA(banc, 2);
        frappe(banc, KeyEvent.KEYCODE_DEL);
        assertEquals("la paire vide disparaît entière", "f", texte(banc));
        assertEquals(1, caret(banc));
    }

    /** A-07 — le retour arrière ailleurs retombe sur la suppression du cœur. */
    @Test
    public void backspaceElsewhereFallsBackToPlainDeletion() {
        EditorTestRig banc = bancBranche("int x;");
        curseurA(banc, 6);
        frappe(banc, KeyEvent.KEYCODE_DEL);
        assertEquals("la suppression par clusters reste",
                "int x", texte(banc));
        assertEquals(5, caret(banc));
    }

    /** A-09 — le collage multiligne s'aligne sur l'indentation d'accueil. */
    @Test
    public void multilineCommitReindentsThePastedBlock() {
        EditorTestRig banc = bancBranche("class Exemple {\n}");
        curseurA(banc, 15);
        banc.validerTexte("int un;\nint deux;\n");
        assertEquals("les lignes suivantes gagnent le cran de la classe",
                "class Exemple {int un;\n    int deux;\n    \n}", texte(banc));
    }
}
