package jo.celeris.core.lsp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextSession;

/**
 * Les éditions d'espace de travail (B-17, B-19) : le modèle LSP —
 * éditions de document ET édits de gabarit, le bloqué historique du
 * dépôt de départ — analysées, sérialisées, transposées en transaction
 * annulable.
 */
class EditionEspaceLspTest {

    /** Une édition d'espace de travail en vraie grandeur : un document édité, un gabarit posé. */
    private static String traceEspace() {
        return """
                {
                  "documentChanges": [
                    {
                      "textDocument": {"uri": "file:///A.java", "version": 3},
                      "edits": [
                        {"range": {"start": {"line": 0, "character": 6},
                                   "end": {"line": 0, "character": 12}},
                         "newText": "Acheteur"}
                      ]
                    },
                    {
                      "kind": "celeris/templateEdit",
                      "textDocument": {"uri": "file:///B.kt"},
                      "position": {"line": 1, "character": 0},
                      "template": "fun ${nom:nouvelleFonction}() {\\n    ${corps}\\n}"
                    }
                  ]
                }
                """;
    }

    /** L'analyse : les éditions de document et les édits de gabarit, chacun son champ. */
    @Test
    void lanalyseDesDeuxSortes() {
        EditionEspaceLsp espace = EditionEspaceLsp.de(JsonLecteur.lire(traceEspace()));
        assertEquals(1, espace.documents().size());
        EditionDocumentLsp document = espace.documents().get(0);
        assertEquals("file:///A.java", document.uri());
        assertEquals(3, document.version());
        assertEquals(1, document.editions().size());
        EditionTexteLsp edition = document.editions().get(0);
        assertEquals(new PositionLsp(0, 6), edition.plage().debut());
        assertEquals(new PositionLsp(0, 12), edition.plage().fin());
        assertEquals("Acheteur", edition.nouveauTexte());
        assertEquals(1, espace.gabarits().size());
        EditionGabaritLsp gabarit = espace.gabarits().get(0);
        assertEquals("file:///B.kt", gabarit.uri());
        assertEquals(new PositionLsp(1, 0), gabarit.position());
        assertEquals("fun ${nom:nouvelleFonction}() {\n    ${corps}\n}", gabarit.corps());
    }

    /** La forme héritée « changes » : la carte uri → éditions, pliée dans les documents. */
    @Test
    void laFormeHeritee() {
        EditionEspaceLsp espace = EditionEspaceLsp.de(JsonLecteur.lire("""
                {"changes": {"file:///A.java": [
                    {"range": {"start": {"line": 0, "character": 0},
                               "end": {"line": 0, "character": 1}},
                     "newText": "X"}]}}
                """));
        assertEquals(1, espace.documents().size());
        assertEquals("file:///A.java", espace.documents().get(0).uri());
        assertEquals(-1, espace.documents().get(0).version());
        assertEquals("X", espace.documents().get(0).editions().get(0).nouveauTexte());
        assertTrue(espace.gabarits().isEmpty());
    }

    /** L'aller-retour : la forme sérialisée redit les deux sortes. */
    @Test
    void lallerRetour() {
        EditionEspaceLsp espace = EditionEspaceLsp.de(JsonLecteur.lire(traceEspace()));
        String json = JsonEcrivain.ecrire(espace.versJson());
        EditionEspaceLsp relue = EditionEspaceLsp.de(JsonLecteur.lire(json));
        assertEquals(espace, relue);
    }

    /** La tolérance : réponse nulle ou vide — jamais d'exception, un espace vide. */
    @Test
    void laTolerance() {
        assertEquals(EditionEspaceLsp.de(JsonNul.nul()), EditionEspaceLsp.VIDE);
        assertTrue(EditionEspaceLsp.de(JsonObjet.vide()).documents().isEmpty());
        assertTrue(EditionEspaceLsp.de(JsonObjet.vide()).gabarits().isEmpty());
    }

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
