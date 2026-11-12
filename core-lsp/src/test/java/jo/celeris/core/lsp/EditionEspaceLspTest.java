package jo.celeris.core.lsp;

import jo.celeris.core.lsp.types.EditionDocumentLsp;
import jo.celeris.core.lsp.types.EditionTexteLsp;
import jo.celeris.core.lsp.types.PlageLsp;
import jo.celeris.core.lsp.types.PositionLsp;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextSession;

/**
 * Les éditions d'espace de travail (B-17, B-19) : le modèle LSP —
 * éditions de document ET édits de gabarit, le bloqué historique du
 * dépôt de départ — transposées en transaction annulable. Le modèle se
 * construit ici par ses champs résolus ; la lecture de la réponse du
 * serveur (l'objet WorkspaceEdit de la bibliothèque) vivra derrière la
 * frontière protocolaire à la couverture nouvelle (ADR-015, H-12).
 */
class EditionEspaceLspTest {

    /** La transposition : les éditions LSP deviennent une transaction, annulable. */
    @Test
    void laTranspositionEnTransaction() {
        Document doc = Document.of("class Client {}\n");
        EditionDocumentLsp edition = new EditionDocumentLsp("file:///A.java", 1, List.of(
                new EditionTexteLsp(new PlageLsp(new PositionLsp(0, 6), new PositionLsp(0, 12)),
                        "Acheteur")));
        TextSession session = TextSession.of("class Client {}\n", System::nanoTime);
        session.apply(EditsEspaceLsp.transaction(doc, edition));
        assertEquals("class Acheteur {}\n", session.text());
        // annulable : le retour arrière restitue l'original (B-17)
        session.undo();
        assertEquals("class Client {}\n", session.text());
    }

    /** Les éditions multiples : ordre descendant d'application, offsets originaux intacts. */
    @Test
    void lesEditionsMultiples() {
        Document doc = Document.of("un deux trois\n");
        EditionDocumentLsp edition = new EditionDocumentLsp("file:///A.txt", 1, List.of(
                new EditionTexteLsp(new PlageLsp(new PositionLsp(0, 0), new PositionLsp(0, 2)), "1"),
                new EditionTexteLsp(new PlageLsp(new PositionLsp(0, 8), new PositionLsp(0, 13)), "3"),
                new EditionTexteLsp(new PlageLsp(new PositionLsp(0, 3), new PositionLsp(0, 7)), "2")));
        TextSession session = TextSession.of("un deux trois\n", System::nanoTime);
        session.apply(EditsEspaceLsp.transaction(doc, edition));
        assertEquals("1 2 3\n", session.text());
        session.undo();
        assertEquals("un deux trois\n", session.text());
    }
}
