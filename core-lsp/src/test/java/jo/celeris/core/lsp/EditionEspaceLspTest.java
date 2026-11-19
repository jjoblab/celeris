package jo.celeris.core.lsp;

import jo.celeris.core.lsp.types.DocumentEditLsp;
import jo.celeris.core.lsp.types.TextEditLsp;
import jo.celeris.core.lsp.types.RangeLsp;
import jo.celeris.core.lsp.types.LspPosition;
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
        DocumentEditLsp edit = new DocumentEditLsp("file:///A.java", 1, List.of(
                new TextEditLsp(new RangeLsp(new LspPosition(0, 6), new LspPosition(0, 12)),
                        "Acheteur")));
        TextSession session = TextSession.of("class Client {}\n", System::nanoTime);
        session.apply(WorkspaceEditsLsp.transaction(doc, edit));
        assertEquals("class Acheteur {}\n", session.text());
        // annulable : le retour arrière restitue l'original (B-17)
        session.undo();
        assertEquals("class Client {}\n", session.text());
    }

    /** Les éditions multiples : ordre descendant d'application, offsets originaux intacts. */
    @Test
    void lesEditionsMultiples() {
        Document doc = Document.of("un deux trois\n");
        DocumentEditLsp edit = new DocumentEditLsp("file:///A.txt", 1, List.of(
                new TextEditLsp(new RangeLsp(new LspPosition(0, 0), new LspPosition(0, 2)), "1"),
                new TextEditLsp(new RangeLsp(new LspPosition(0, 8), new LspPosition(0, 13)), "3"),
                new TextEditLsp(new RangeLsp(new LspPosition(0, 3), new LspPosition(0, 7)), "2")));
        TextSession session = TextSession.of("un deux trois\n", System::nanoTime);
        session.apply(WorkspaceEditsLsp.transaction(doc, edit));
        assertEquals("1 2 3\n", session.text());
        session.undo();
        assertEquals("un deux trois\n", session.text());
    }
}
