package jo.celeris.core.lsp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Les enveloppes JSON-RPC 2.0 du client LSP (ADR-005) : requête, réponse,
 * notification — sérialisation et analyse strictes. La surface est bornée
 * au protocole : les codes d'erreur standards vivent avec l'enveloppe.
 */
class JsonRpcTest {

    /** La requête : jsonrpc, id, méthode, paramètres — l'aller-retour complet. */
    @Test
    void laRequete() {
        JsonObjet params = JsonObjet.vide()
                .ajouter("textDocument", JsonObjet.vide()
                        .ajouter("uri", JsonChaine.of("file:///Main.java"))
                        .ajouter("languageId", JsonChaine.of("java")));
        RequeteRpc requete = new RequeteRpc(7, "textDocument/completion", params);
        assertEquals("{\"jsonrpc\":\"2.0\",\"id\":7,"
                + "\"method\":\"textDocument/completion\","
                + "\"params\":{\"textDocument\":{\"uri\":\"file:///Main.java\","
                + "\"languageId\":\"java\"}}}",
                JsonEcrivain.ecrire(requete.versJson()));
        RequeteRpc relue = (RequeteRpc) MessageRpc.de(JsonLecteur.lire(
                JsonEcrivain.ecrire(requete.versJson())));
        assertEquals(7, relue.id());
        assertEquals("textDocument/completion", relue.methode());
        JsonObjet parametres = assertInstanceOf(JsonObjet.class, relue.params());
        JsonObjet documentTexte = assertInstanceOf(JsonObjet.class,
                parametres.membre("textDocument"));
        assertEquals("file:///Main.java", documentTexte.membreTexte("uri"));
    }

    /** La requête sans paramètres : la clé params est omise, jamais null explicite. */
    @Test
    void laRequeteSansParams() {
        RequeteRpc requete = new RequeteRpc(1, "shutdown", null);
        assertNull(requete.params());
        assertEquals("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"shutdown\"}",
                JsonEcrivain.ecrire(requete.versJson()));
    }

    /** La notification : pas d'identifiant — le changement de document en vraie grandeur. */
    @Test
    void laNotification() {
        NotificationRpc notification = new NotificationRpc("textDocument/didChange",
                JsonObjet.vide().ajouter("version", JsonNombre.of(3)));
        String json = JsonEcrivain.ecrire(notification.versJson());
        assertEquals("{\"jsonrpc\":\"2.0\",\"method\":\"textDocument/didChange\","
                + "\"params\":{\"version\":3}}", json);
        NotificationRpc relue = (NotificationRpc) MessageRpc.de(JsonLecteur.lire(json));
        assertEquals("textDocument/didChange", relue.methode());
        assertEquals(3, assertInstanceOf(JsonNombre.class, assertInstanceOf(JsonObjet.class,
                relue.params()).membre("version")).entier());
    }

    /** La réponse : résultat seul, erreur seule — jamais les deux, jamais aucun. */
    @Test
    void laReponse() {
        ReponseRpc ok = new ReponseRpc(7, JsonChaine.of("résultat"), null);
        assertEquals("{\"jsonrpc\":\"2.0\",\"id\":7,\"result\":\"résultat\"}",
                JsonEcrivain.ecrire(ok.versJson()));
        ReponseRpc relueOk = (ReponseRpc) MessageRpc.de(JsonLecteur.lire(
                "{\"jsonrpc\":\"2.0\",\"id\":7,\"result\":[1,2]}"));
        assertEquals(7, relueOk.id());
        assertNull(relueOk.erreur());
        assertEquals(2, assertInstanceOf(JsonTableau.class, relueOk.resultat()).taille());

        ErreurRpc erreur = new ErreurRpc(ErreurRpc.REQUEST_CANCELLED,
                "request cancelled", null);
        ReponseRpc ko = new ReponseRpc(8, null, erreur);
        assertEquals("{\"jsonrpc\":\"2.0\",\"id\":8,\"error\":"
                + "{\"code\":-32800,\"message\":\"request cancelled\"}}",
                JsonEcrivain.ecrire(ko.versJson()));
        ReponseRpc relueKo = (ReponseRpc) MessageRpc.de(JsonLecteur.lire(
                JsonEcrivain.ecrire(ko.versJson())));
        ErreurRpc erreurRelue = assertInstanceOf(ErreurRpc.class, relueKo.erreur());
        assertEquals(ErreurRpc.REQUEST_CANCELLED, erreurRelue.code());
        assertEquals("request cancelled", erreurRelue.message());
        assertNull(erreurRelue.donnees());
    }

    /** L'erreur avec données : la charge utile survit à l'aller-retour. */
    @Test
    void lerreurAvecDonnees() {
        ErreurRpc erreur = new ErreurRpc(ErreurRpc.CONTENT_MODIFIED,
                "content modified", JsonChaine.of("détail"));
        ReponseRpc reponse = new ReponseRpc(9, null, erreur);
        ReponseRpc relue = (ReponseRpc) MessageRpc.de(JsonLecteur.lire(
                JsonEcrivain.ecrire(reponse.versJson())));
        assertEquals("détail", assertInstanceOf(JsonChaine.class,
                assertInstanceOf(ErreurRpc.class, relue.erreur()).donnees()).texte());
    }

    /** Le tri des messages : requête, notification, réponse — et rien d'autre. */
    @Test
    void leTriDesMessages() {
        assertTrue(MessageRpc.de(JsonLecteur.lire(
                "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"x\"}"))
                instanceof RequeteRpc);
        assertTrue(MessageRpc.de(JsonLecteur.lire(
                "{\"jsonrpc\":\"2.0\",\"method\":\"x\"}"))
                instanceof NotificationRpc);
        assertTrue(MessageRpc.de(JsonLecteur.lire(
                "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":null}"))
                instanceof ReponseRpc);
        assertThrows(IllegalArgumentException.class, () -> MessageRpc.de(JsonLecteur.lire(
                "{\"jsonrpc\":\"2.0\"}")));
        assertThrows(IllegalArgumentException.class, () -> MessageRpc.de(JsonLecteur.lire(
                "{\"jsonrpc\":\"1.0\",\"id\":1,\"method\":\"x\"}")));
        assertThrows(IllegalArgumentException.class, () -> MessageRpc.de(JsonLecteur.lire(
                "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":3}")));
        assertThrows(IllegalArgumentException.class, () -> MessageRpc.de(JsonLecteur.lire(
                "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":1,\"error\":{\"code\":1,"
                + "\"message\":\"m\"}}")));
    }

    /** Les codes d'erreur du protocole : les annulations du serveur sont distinguées. */
    @Test
    void lesCodesDErreur() {
        assertEquals(-32700, ErreurRpc.PARSE_ERROR);
        assertEquals(-32600, ErreurRpc.INVALID_REQUEST);
        assertEquals(-32601, ErreurRpc.METHOD_NOT_FOUND);
        assertEquals(-32602, ErreurRpc.INVALID_PARAMS);
        assertEquals(-32603, ErreurRpc.INTERNAL_ERROR);
        assertEquals(-32800, ErreurRpc.REQUEST_CANCELLED);
        assertEquals(-32801, ErreurRpc.CONTENT_MODIFIED);
    }
}
