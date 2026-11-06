package jo.celeris.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jo.celeris.core.intelligence.Gabarit;
import jo.celeris.core.intelligence.Gabarits;
import jo.celeris.core.lsp.EditionEspaceLsp;
import jo.celeris.core.lsp.EditionGabaritLsp;
import jo.celeris.core.lsp.EditsEspaceLsp;
import jo.celeris.core.lsp.JsonLecteur;
import jo.celeris.core.lsp.PositionsLsp;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.TextSession;

/**
 * La composition des éditions d'espace de travail (B-17, B-19, B-06) :
 * le bloqué historique du dépôt de départ — les édits de gabarit de la
 * version 1.0.0 de la liaison tierce, jamais absorbés — débloqué de bout
 * en bout. La démo est le point de composition : elle voit le client LSP
 * et le moteur de gabarits, les modules purs restent latéraux (ADR-002).
 */
class EditsEspaceDemoTest {

    /** Une édition d'espace en deux temps : un renommage et un gabarit posé. */
    private static final String TRACE = """
            {
              "documentChanges": [
                {
                  "textDocument": {"uri": "file:///A.java", "version": 2},
                  "edits": [
                    {"range": {"start": {"line": 0, "character": 6},
                               "end": {"line": 0, "character": 12}},
                     "newText": "Acheteur"}
                  ]
                },
                {
                  "kind": "celeris/templateEdit",
                  "textDocument": {"uri": "file:///A.java"},
                  "position": {"line": 1, "character": 0},
                  "template": "void ${nom:nouvelleMethode}(${p1:String arg0}) {\\n    ${corps}\\n}\\n"
                }
              ]
            }
            """;

    /** Le renommage : transaction annulable par la voie unique (B-17, B-19). */
    @Test
    void leRenommageAppliqueEtAnnule() {
        EditionEspaceLsp espace = EditionEspaceLsp.de(JsonLecteur.lire(TRACE));
        assertEquals(1, espace.documents().size());
        TextSession session = TextSession.of("class Client {\n}\n", System::nanoTime);
        Document doc = session.document();
        session.apply(EditsEspaceLsp.transaction(doc, espace.documents().get(0)));
        assertEquals("class Acheteur {\n}\n", session.text());
        session.undo();
        assertEquals("class Client {\n}\n", session.text());
    }

    /**
     * L'édit de gabarit — le bloqué historique : le corps portant ses
     * trous s'insère par la voie unique, les défauts sont posés, le
     * premier trou reçoit le curseur, l'annulation restitue tout.
     */
    @Test
    void leditDeGabaritAppliqueEtAnnule() {
        EditionEspaceLsp espace = EditionEspaceLsp.de(JsonLecteur.lire(TRACE));
        assertEquals(1, espace.gabarits().size());
        EditionGabaritLsp edit = espace.gabarits().get(0);
        TextSession session = TextSession.of("class A {\n}\n", System::nanoTime);
        Document doc = session.document();
        // la position LSP du gabarit devient l'offset du document
        int position = PositionsLsp.offset(doc, edit.position());
        // le moteur de gabarits M5 fait le reste : rendu, trous, sélection
        Gabarit gabarit = Gabarit.of(edit.corps());
        jo.celeris.core.intelligence.EditPlan plan =
                Gabarits.inserer(doc, MultiSelection.caretsAt(position), gabarit);
        session.apply(plan.transaction(), plan.selection());
        assertEquals("class A {\n"
                + "void nouvelleMethode(String arg0) {\n    \n}\n"
                + "}\n", session.text());
        // le premier trou — le nom, son défaut sélectionné — porte le curseur :
        // curseurs aux trous (B-06), prêt à être tapé par-dessus
        jo.celeris.core.text.SelRange premierTrou = session.selection().ranges().get(0);
        assertEquals(position + "void ".length(), premierTrou.anchor());
        assertEquals(position + "void nouvelleMethode".length(), premierTrou.caret());
        session.undo();
        assertEquals("class A {\n}\n", session.text());
        assertTrue(gabarit.trous().size() == 3);
    }
}
