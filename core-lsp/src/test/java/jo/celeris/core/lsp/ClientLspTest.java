package jo.celeris.core.lsp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Le client LSP (ADR-005) : poignée de main, documents versionnés à
 * synchronisation complète, diagnostics publiés. Le client vit sur un
 * fil de travail propre — les auditeurs sont appelés dessus, les
 * requêtes rendent des futurs.
 */
class ClientLspTest {

    private TransportMemoire.Paire paire;
    private ServeurFactice serveur;
    private ClientLsp client;

    @BeforeEach
    void monter() {
        paire = TransportMemoire.creerPaire();
        serveur = ServeurFactice.brancher(paire);
        client = ClientLsp.sur(paire.boutA(), "tests-celeris");
    }

    @AfterEach
    void demonter() {
        client.close();
        serveur.close();
    }

    /** Les capacités du serveur factice : complétion, survol, jetons avec légende. */
    private static ValeurJson capacitesServeur() {
        return JsonObjet.vide()
                .ajouter("textDocumentSync", JsonNombre.of(1))
                .ajouter("completionProvider", JsonObjet.vide())
                .ajouter("hoverProvider", JsonBooleen.of(true))
                .ajouter("semanticTokensProvider", JsonObjet.vide()
                        .ajouter("legend", JsonObjet.vide()
                                .ajouter("tokenTypes", JsonTableau.vide()
                                        .ajouter(JsonChaine.of("classe"))
                                        .ajouter(JsonChaine.of("methode")))
                                .ajouter("tokenModifiers", JsonTableau.vide()
                                        .ajouter(JsonChaine.of("lectureSeule")))));
    }

    /** La poignée de main : initialize puis initialized, capacités analysées. */
    @Test
    void laPoigneeDeMain() {
        serveur.capacites(capacitesServeur());
        CapacitesServeur capacites = client.initialiser(JsonObjet.vide());
        assertTrue(capacites.sertCompletion());
        assertTrue(capacites.sertSurvol());
        assertTrue(!capacites.sertRennomage());
        assertEquals(1, capacites.genreSynchro());
        assertEquals(List.of("classe", "methode"), capacites.typesJetons());
        assertEquals(List.of("lectureSeule"), capacites.modificateursJetons());
        // le serveur a vu la requête initialize puis la notification initialized
        List<MessageRpc> messages = messagesRecus();
        assertEquals("initialize", ((RequeteRpc) messages.get(0)).methode());
        assertEquals("initialized", ((NotificationRpc) messages.get(1)).methode());
    }

    /** Les paramètres d'initialisation : le nom du client et ses capacités. */
    @Test
    void lesParametresDInitialisation() {
        client.initialiser(JsonObjet.vide()
                .ajouter("generale", JsonBooleen.of(true)));
        RequeteRpc initialize = (RequeteRpc) messagesRecus().get(0);
        JsonObjet params = assertInstanceOf(JsonObjet.class, initialize.params());
        assertEquals("tests-celeris",
                assertInstanceOf(JsonObjet.class, params.membre("clientInfo")).membreTexte("name"));
    }

    /** Les documents : ouverture, changement versionné à synchro complète, fermeture. */
    @Test
    void lesDocumentsVersionnes() {
        client.initialiser(JsonObjet.vide());
        client.ouvrir("file:///A.java", "java", "class A {}\n");
        client.changer("file:///A.java", 2, "class A { void f() {} }\n");
        client.changer("file:///A.java", 3, "class A {}\n");
        client.fermer("file:///A.java");
        List<MessageRpc> messages = messagesRecus();
        NotificationRpc ouverture = (NotificationRpc) messages.get(2);
        assertEquals("textDocument/didOpen", ouverture.methode());
        JsonObjet docOuverture = assertInstanceOf(JsonObjet.class,
                assertInstanceOf(JsonObjet.class, ouverture.params())
                        .membreOu("textDocument", JsonObjet.vide()));
        assertEquals("file:///A.java", docOuverture.membreTexte("uri"));
        assertEquals("java", docOuverture.membreTexte("languageId"));
        assertEquals("class A {}\n", docOuverture.membreTexte("text"));
        NotificationRpc changement = (NotificationRpc) messages.get(3);
        JsonObjet docChangement = assertInstanceOf(JsonObjet.class,
                assertInstanceOf(JsonObjet.class, changement.params())
                        .membreOu("textDocument", JsonObjet.vide()));
        assertEquals(2, assertInstanceOf(JsonNombre.class,
                docChangement.membre("version")).entier());
        JsonTableau evenements = assertInstanceOf(JsonTableau.class,
                assertInstanceOf(JsonObjet.class, changement.params())
                        .membre("contentChanges"));
        assertEquals("class A { void f() {} }\n",
                assertInstanceOf(JsonObjet.class, evenements.element(0)).membreTexte("text"));
        assertEquals("textDocument/didClose", methode(messagesRecus().get(5)));
        // le client suit les versions : dernière version envoyée, puis état oublié à la fermeture
        client.ouvrir("file:///A.java", "java", "x");
        assertEquals(1, client.versionDe("file:///A.java"));
        client.fermer("file:///A.java");
        assertEquals(-1, client.versionDe("file:///A.java"));
    }

    /** La version d'un document inconnu : -1, jamais d'exception. */
    @Test
    void laVersionInconnue() {
        assertEquals(-1, client.versionDe("file:///inconnu.kt"));
    }

    /** Les diagnostics publiés : l'auditeur prévenu, le dernier état par uri. */
    @Test
    void lesDiagnosticsPublies() {
        client.initialiser(JsonObjet.vide());
        List<String> uris = new ArrayList<>();
        client.ajouterAuditeurDiagnostics((uri, diags) -> uris.add(uri + ":" + diags.size()));
        serveur.notifier("textDocument/publishDiagnostics", JsonObjet.vide()
                .ajouter("uri", JsonChaine.of("file:///A.java"))
                .ajouter("diagnostics", JsonTableau.vide()
                        .ajouter(diagnostic("(2,4)-(2,9)", 1, "missing return"))
                        .ajouter(diagnostic("(3,0)-(3,1)", 2, "unused variable"))));
        serveur.notifier("textDocument/publishDiagnostics", JsonObjet.vide()
                .ajouter("uri", JsonChaine.of("file:///A.java"))
                .ajouter("diagnostics", JsonTableau.vide()
                        .ajouter(diagnostic("(1,0)-(1,5)", 1, "autre"))));
        serveur.notifier("textDocument/publishDiagnostics", JsonObjet.vide()
                .ajouter("uri", JsonChaine.of("file:///B.kt"))
                .ajouter("diagnostics", JsonTableau.vide()));
        attendreCondition(() -> uris.size() == 3);
        assertEquals(List.of("file:///A.java:2", "file:///A.java:1", "file:///B.kt:0"), uris);
        assertEquals(1, client.diagnosticsDe("file:///A.java").size());
        assertEquals("autre", client.diagnosticsDe("file:///A.java").get(0).message());
        assertEquals(0, client.diagnosticsDe("file:///B.kt").size());
        assertEquals(0, client.diagnosticsDe("file:///inconnu.kt").size());
    }

    /** Le diagnostic décodé : plage, gravité, message, code et source. */
    @Test
    void leDiagnosticDecode() {
        client.initialiser(JsonObjet.vide());
        serveur.notifier("textDocument/publishDiagnostics", JsonObjet.vide()
                .ajouter("uri", JsonChaine.of("file:///A.java"))
                .ajouter("diagnostics", JsonTableau.vide()
                        .ajouter(diagnostic("(2,4)-(2,9)", 1, "missing return"))));
        attendreCondition(() -> !client.diagnosticsDe("file:///A.java").isEmpty());
        DiagnosticLsp diagnostic = client.diagnosticsDe("file:///A.java").get(0);
        assertEquals(new PositionLsp(2, 4), diagnostic.plage().debut());
        assertEquals(new PositionLsp(2, 9), diagnostic.plage().fin());
        assertEquals(1, diagnostic.gravite());
        assertEquals("missing return", diagnostic.message());
        assertEquals("Java", diagnostic.source());
        assertEquals("retour", diagnostic.code());
    }

    /** Toute requête de document avant l'initialisation est refusée. */
    @Test
    void rienAvantInitialisation() {
        assertThrows(IllegalStateException.class,
                () -> client.ouvrir("file:///A.java", "java", ""));
    }

    /** La fermeture : shutdown demandé puis exit notifié, le client fermé une fois. */
    @Test
    void laFermeture() {
        client.initialiser(JsonObjet.vide());
        client.ouvrir("file:///A.java", "java", "");
        client.close();
        client.close();
        List<String> methodes = messagesRecus().stream()
                .map(m -> m instanceof RequeteRpc r ? r.methode()
                        : ((NotificationRpc) m).methode())
                .toList();
        assertTrue(methodes.contains("shutdown"), "shutdown doit etre demande");
        assertTrue(methodes.contains("exit"), "exit doit etre notifie");
        assertEquals("exit", methodes.get(methodes.size() - 1));
    }

    /** La requête du serveur inconnue : le client répond erreur de méthode — jamais muet. */
    @Test
    void laRequeteServeurInconnue() {
        client.initialiser(JsonObjet.vide());
        serveur.envoyerRequete("workspace/configuration", JsonObjet.vide());
        attendreCondition(() -> messagesRecus().stream()
                .anyMatch(message -> message instanceof ReponseRpc));
        ReponseRpc reponse = derniereReponse();
        assertTrue(reponse.erreur() != null);
        assertEquals(ErreurRpc.METHOD_NOT_FOUND, reponse.erreur().code());
    }

    // ── Aides ─────────────────────────────────────────────────────────

    /** Les trames reçues par le serveur, décodées en messages. */
    private List<MessageRpc> messagesRecus() {
        return serveur.tramesRecues().stream()
                .map(trame -> MessageRpc.de(JsonLecteur.lire(trame)))
                .toList();
    }

    /** La méthode d'un message, requête ou notification. */
    private static String methode(MessageRpc message) {
        if (message instanceof RequeteRpc requete) {
            return requete.methode();
        }
        return ((NotificationRpc) message).methode();
    }

    /** La dernière réponse reçue par le serveur factice. */
    private ReponseRpc derniereReponse() {
        List<MessageRpc> messages = messagesRecus();
        return (ReponseRpc) messages.get(messages.size() - 1);
    }

    /** Un diagnostic JSON : plage en texte « (l,c)-(l,c) », gravité, message, code, source. */
    private static ValeurJson diagnostic(String plage, int gravite, String message) {
        String[] bornes = plage.split("-", -1);
        String[] debut = bornes[0].replaceAll("[() ]", "").split(",", -1);
        String[] fin = bornes[1].replaceAll("[() ]", "").split(",", -1);
        return JsonObjet.vide()
                .ajouter("range", JsonObjet.vide()
                        .ajouter("start", JsonObjet.vide()
                                .ajouter("line", JsonNombre.of(Integer.parseInt(debut[0])))
                                .ajouter("character", JsonNombre.of(Integer.parseInt(debut[1]))))
                        .ajouter("end", JsonObjet.vide()
                                .ajouter("line", JsonNombre.of(Integer.parseInt(fin[0])))
                                .ajouter("character", JsonNombre.of(Integer.parseInt(fin[1])))))
                .ajouter("severity", JsonNombre.of(gravite))
                .ajouter("message", JsonChaine.of(message))
                .ajouter("code", JsonChaine.of("retour"))
                .ajouter("source", JsonChaine.of("Java"));
    }

    /** Attend que la condition soit vraie — sonde courte, bornée. */
    private static void attendreCondition(java.util.function.BooleanSupplier condition) {
        long fin = System.nanoTime() + 5_000_000_000L;
        while (System.nanoTime() < fin) {
            if (condition.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
