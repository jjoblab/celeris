package jo.celeris.core.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.TextSession;

import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * Les continuations et collapses de la frappe (C7/C8/C9, l'analyse
 * croisée) : le retour arrière collapse les lignes vides, la
 * suppression avant mange les paires vides, l'Entrée continue les
 * documented comments ({@code * }), les puces de liste et les
 * opérateurs en suspens.
 */
class FrappeContinuationsTest {

    private String apresBackspace(String texte, int caret) {
        Document doc = Document.of(texte);
        Optional<EditPlan> plan = SmartTyping.backspace(doc,
                MultiSelection.caretsAt(caret), TypingRules.generic(),
                IndentStyle.spaces(4));
        if (plan.isEmpty()) {
            return texte + " (main au coeur)";
        }
        TextSession session = TextSession.of(texte, () -> 0L);
        session.apply(plan.get().transaction(), plan.get().selection());
        return session.text();
    }

    private String apresEnter(String texte, int caret) {
        Document doc = Document.of(texte);
        EditPlan plan = SmartTyping.enter(doc, null,
                MultiSelection.caretsAt(caret), IndentStyle.spaces(4));
        TextSession session = TextSession.of(texte, () -> 0L);
        session.apply(plan.transaction(), plan.selection());
        return session.text();
    }

    /** C7 : le retour arriere entre deux lignes vides les COLLAPSE. */
    @Test
    void leBackspaceCollapseLesLignesVides() {
        // caret au debut de la 3e ligne, deux vides devant
        String apres = apresBackspace("a\n\n\nb", 3);
        assertEquals("a\n\nb", apres);
    }

    /** C8 : la suppression avant sur une paire vide mange les DEUX
     *  caracteres. */
    @Test
    void laSuppressionAvantMangeLaPaireVide() {
        Document doc = Document.of("f()");
        Optional<EditPlan> plan = LineActions.deleteForward(doc,
                MultiSelection.caretsAt(1), TypingRules.generic());
        assertTrue(plan.isPresent(), "le plan existe");
        TextSession session = TextSession.of("f()", () -> 0L);
        session.apply(plan.get().transaction(), plan.get().selection());
        assertEquals("f", session.text());
    }

    /** C9 : l'Entree dans un bloc de documentation continue le
     *  « * » — le corps du commentaire vit aligne. */
    @Test
    void lepoqueContinueLaDocumentation() {
        String apres = apresEnter("    /** toto", 12);
        assertTrue(apres.contains("\n     * "),
                "la documentation se continue : " + apres);
    }

    /** C9 : l'Entree apres une puce de liste continue la puce. */
    @Test
    void lepoqueContinueLaPuce() {
        String apres = apresEnter("- premier", 9);
        assertEquals("- premier\n- ", apres);
    }
}
