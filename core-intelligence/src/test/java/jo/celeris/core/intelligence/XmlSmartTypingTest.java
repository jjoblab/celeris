package jo.celeris.core.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextSession;

import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * La frappe intelligente XML (C5/C6, l'analyse croisée) : le dépôt
 * de départ ferme la balise sur « > » et renomme LIÉ les balises —
 * le caractère frappé dans l'ouvrante se reflète dans la fermante.
 * Celeris avait porté XmlLinkedTags sans jamais le brancher, et la
 * fermeture de balise n'existait pas.
 */
class XmlSmartTypingTest {

    private String apresPlan(Optional<EditPlan> plan, String texte) {
        assertTrue(plan.isPresent(), "le plan existe");
        TextSession session = TextSession.of(texte, () -> 0L);
        session.apply(plan.get().transaction(), plan.get().selection());
        return session.text();
    }

    /** C5 : « > » après un nom d'élément insère la fermeture
     *  appariée — <div|> devient <div></div>, caret avant le
     *  fermant. */
    @Test
    void leSuperieurFermeLaBalise() {
        Document doc = Document.of("<div");
        Optional<EditPlan> plan = XmlSmartTyping.closeTag(
                doc, 4, TypingRules.generic());
        assertEquals("<div></div>", apresPlan(plan, "<div"));
    }

    /** C5 : la balise auto-fermante ne se double pas — <br|>
     *  reste <br>, et <img … /> aussi. */
    @Test
    void laBaliseAutoFermanteNeSeDoublePas() {
        Document doc = Document.of("<br");
        Optional<EditPlan> plan = XmlSmartTyping.closeTag(
                doc, 3, TypingRules.generic());
        assertTrue(plan.isEmpty() || apresPlan(plan, "<br").equals("<br"),
                "la balise courte ne se ferme pas");
    }

    /** C5 : le « > » d'une balise FERMANTE ne déclenche rien. */
    @Test
    void leSuperieurFermantNeDeclencheRien() {
        Document doc = Document.of("<a></a");
        // caret avant le > de la fermante </a|>
        Optional<EditPlan> plan = XmlSmartTyping.closeTag(
                doc, 7, TypingRules.generic());
        assertTrue(plan.isEmpty() || apresPlan(plan, "<a></a").equals("<a></a"),
                "la fermante ne se referme pas");
    }

    /** C6 : le renommage lié — la frappe dans l'ouvrante se reflète
     *  dans la fermante. */
    @Test
    void leRenommageLieRefleteLaFermante() {
        Document doc = Document.of("<div></div>");
        // caret après « div » dans l'ouvrante : frappe de « 2 »
        Optional<EditPlan> plan = XmlLinkedTags.typeChar(doc, 4, "2");
        assertEquals("<div2></div2>", apresPlan(plan, "<div></div>"));
    }

    /** C6 : le backspace lié — la suppression dans l'ouvrante
     *  retire le miroir dans la fermante. */
    @Test
    void leBackspaceLieRetireLeMiroir() {
        Document doc = Document.of("<div2></div2>");
        Optional<EditPlan> plan = XmlLinkedTags.backspace(doc, 5);
        assertEquals("<div></div>", apresPlan(plan, "<div2></div2>"));
    }
}
