package jo.celeris.demo;

import jo.celeris.core.lsp.types.EditionDocumentLsp;
import jo.celeris.core.lsp.types.EditionEspaceLsp;
import jo.celeris.core.lsp.types.EditionGabaritLsp;
import jo.celeris.core.lsp.types.EditionTexteLsp;
import jo.celeris.core.lsp.types.PlageLsp;
import jo.celeris.core.lsp.types.PositionLsp;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.intelligence.Gabarit;
import jo.celeris.core.intelligence.Gabarits;




import jo.celeris.core.lsp.EditsEspaceLsp;


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
 *
 * <p>Le modèle se construit ici par ses champs résolus : la lecture de
 * la réponse du serveur — l'objet {@code WorkspaceEdit} de la
 * bibliothèque, édits de gabarit compris — vit derrière la frontière
 * protocolaire et rejoint la correspondance à la couverture nouvelle
 * (ADR-015, H-12).</p>
 */
class EditsEspaceDemoTest {

    private static final String URI = "file:///A.java";

    /**
     * L'espace en deux temps, construit directement : un renommage de
     * document (version 2) et un édit de gabarit posé à la ligne
     * suivante — la même réponse que le serveur enverrait.
     */
    private static EditionEspaceLsp espace() {
        return new EditionEspaceLsp(
                List.of(new EditionDocumentLsp(URI, 2, List.of(new EditionTexteLsp(
                        new PlageLsp(new PositionLsp(0, 6), new PositionLsp(0, 12)),
                        "Acheteur")))),
                List.of(new EditionGabaritLsp(URI, new PositionLsp(1, 0),
                        "void ${nom:nouvelleMethode}(${p1:String arg0}) {\n"
                                + "    ${corps}\n}\n")));
    }

    /** Le renommage : transaction annulable par la voie unique (B-17, B-19). */
    @Test
    void leRenommageAppliqueEtAnnule() {
        EditionEspaceLsp espace = espace();
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
        EditionEspaceLsp espace = espace();
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
