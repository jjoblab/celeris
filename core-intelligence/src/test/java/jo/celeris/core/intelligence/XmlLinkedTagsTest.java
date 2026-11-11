package jo.celeris.core.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextSession;

/**
 * Le renommage lié des balises XML (C8) : l'édition du nom ouvrant se
 * reflète sur la fermante appariée — même caractère, position alignée,
 * un seul lot d'annulation. L'oracle : les éditions liées du dépôt de
 * départ (registre de portage, C8).
 */
class XmlLinkedTagsTest {

    private Optional<EditPlan> frappe(String source, String designation, String ch) {
        int caret = source.indexOf(designation) + designation.length();
        return XmlLinkedTags.typeChar(Document.of(source), caret, ch);
    }

    /** C8 — la frappe en fin de nom ouvrant se reflète en fin de fermante, un seul undo. */
    @Test
    void laFrappeSeReflete() {
        String source = "<catalogue>\n    <livre></livre>\n</catalogue>\n";
        Optional<EditPlan> plan = frappe(source, "<livre", "s");
        assertTrue(plan.isPresent(), "la liaison vit");
        TextSession session = TextSession.of(source, System::nanoTime);
        session.apply(plan.get().transaction());
        assertEquals("<catalogue>\n    <livres></livres>\n</catalogue>\n", session.text(),
                "les deux noms reçoivent le caractère à la position alignée");
        session.undo();
        assertEquals(source, session.text(), "un seul lot d'annulation");
    }

    /** C8 — la frappe au milieu du nom ouvrant se reflète au milieu de la fermante. */
    @Test
    void laFrappeAuMilieuSeReflete() {
        String source = "<liste>\n    <element>texte</element>\n</liste>\n";
        Optional<EditPlan> plan = frappe(source, "<ele", "v");
        assertTrue(plan.isPresent());
        TextSession session = TextSession.of(source, System::nanoTime);
        session.apply(plan.get().transaction());
        assertEquals("<liste>\n    <elevment>texte</elevment>\n</liste>\n", session.text());
    }

    /** C8 — le retour arrière lié : le caractère avant le caret et son miroir partent. */
    @Test
    void leRetourArriereSeReflete() {
        String source = "<liste>\n    <elements>texte</elements>\n</liste>\n";
        int caret = source.indexOf("<elements") + "<elements".length();
        Optional<EditPlan> plan = XmlLinkedTags.backspace(Document.of(source), caret);
        assertTrue(plan.isPresent());
        TextSession session = TextSession.of(source, System::nanoTime);
        session.apply(plan.get().transaction());
        assertEquals("<liste>\n    <element>texte</element>\n</liste>\n", session.text(),
                "le dernier caractère des deux noms part");
    }

    /** C8 — les homonymes imbriqués s'apparient par empilement, les auto-fermantes ne comptent pas. */
    @Test
    void lesHomonymesImbriques() {
        String source = "<a><a></a></a>";
        Optional<EditPlan> plan = frappe(source, "<a", "b");
        assertTrue(plan.isPresent());
        TextSession session = TextSession.of(source, System::nanoTime);
        session.apply(plan.get().transaction());
        assertEquals("<ab><a></a></ab>", session.text(),
                "la fermante APPARIÉE (la dernière) suit, l'homonyme imbriqué reste");

        String autofermante = "<a><a/></a>";
        plan = frappe(autofermante, "<a", "b");
        assertTrue(plan.isPresent());
        session = TextSession.of(autofermante, System::nanoTime);
        session.apply(plan.get().transaction());
        assertEquals("<ab><a/></ab>", session.text(),
                "l'auto-fermante ne compte pas dans l'empilement");
    }

    /** C8 — les refus : auto-fermante, fermante éditée, attribut, texte, début du nom, fermante absente. */
    @Test
    void lesRefus() {
        assertTrue(XmlLinkedTags.typeChar(Document.of("<br/>"), 3, "x").isEmpty(),
                "l'auto-fermante n'a pas de fermante à lier");
        String source = "<a></a>";
        assertTrue(XmlLinkedTags.typeChar(Document.of(source),
                source.indexOf("</a") + 3, "x").isEmpty(),
                "l'édition de la fermante ne touche jamais l'ouvrante");
        assertTrue(XmlLinkedTags.typeChar(Document.of("<a attr=\"x\"></a>"),
                "<a attr=".length(), "z").isEmpty(),
                "le caret dans un attribut écarte la liaison");
        assertTrue(XmlLinkedTags.typeChar(Document.of("texte simple"), 3, "x").isEmpty(),
                "le texte simple n'a pas de liaison");
        assertTrue(XmlLinkedTags.backspace(Document.of("<a></a>"), 1).isEmpty(),
                "le retour arrière au début du nom refuse");
        assertTrue(XmlLinkedTags.typeChar(Document.of("<a>texte"), 2, "x").isEmpty(),
                "la fermante absente écarte la liaison");
    }

    /** C8 — les commentaires et sections CDATA se sautent à l'appariement. */
    @Test
    void lesCommentairesSeSautent() {
        String source = "<a><!-- <a></a> --><![CDATA[ </a> ]]></a>";
        Optional<EditPlan> plan = frappe(source, "<a", "b");
        assertTrue(plan.isPresent(), "la fermante réelle s'apparie au-delà du commentaire");
        TextSession session = TextSession.of(source, System::nanoTime);
        session.apply(plan.get().transaction());
        assertEquals("<ab><!-- <a></a> --><![CDATA[ </a> ]]></ab>", session.text(),
                "le contenu du commentaire et de la CDATA reste intact");
    }
}
