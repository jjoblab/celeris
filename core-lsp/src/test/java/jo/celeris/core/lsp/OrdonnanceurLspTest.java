package jo.celeris.core.lsp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * L'ordonnancement des requêtes (ADR-005) : annulation, priorités,
 * fusion, resynchronisation — la couche qui manque à toute liaison
 * tierce et la raison d'être du client propre. Le serveur factice
 * retient ses réponses : les requêtes vivent en vol, l'ordonnanceur
 * décide.
 */
class OrdonnanceurLspTest {

    private TransportMemoire.Paire paire;
    private ServeurFactice serveur;
    private ClientLsp client;
    private OrdonnanceurLsp ordonnanceur;

    @BeforeEach
    void monter() {
        paire = TransportMemoire.creerPaire();
        serveur = ServeurFactice.brancher(paire);
        client = ClientLsp.sur(paire.boutA(), "tests-celeris");
        client.initialiser(JsonObjet.vide());
        ordonnanceur = OrdonnanceurLsp.sur(client);
    }

    @AfterEach
    void demonter() {
        client.close();
        serveur.close();
    }

    /** La fusion : deux demandes identiques en vol, UNE requête sur le fil — les deux servies. */
    @Test
    void laFusionDesRequetesIdentiques() throws Exception {
        serveur.reponseDifferee("textDocument/completion");
        JsonObjet params = parametresCompletion("file:///A.java", 3, 7);
        CompletableFuture<ValeurJson> premiere = ordonnanceur.demander(
                "textDocument/completion", params,
                OrdonnanceurLsp.Priorite.INTERACTIF, cle("file:///A.java"));
        CompletableFuture<ValeurJson> seconde = ordonnanceur.demander(
                "textDocument/completion", params,
                OrdonnanceurLsp.Priorite.INTERACTIF, cle("file:///A.java"));
        assertEquals(1, serveur.requetesEnAttente("textDocument/completion"),
                "une seule requete doit etre en vol");
        serveur.acheverRequete("textDocument/completion", JsonTableau.vide()
                .ajouter(JsonObjet.vide().ajouter("label", JsonChaine.of("un"))));
        assertEquals("un", libelle(premiere.get(5, java.util.concurrent.TimeUnit.SECONDS)));
        assertEquals("un", libelle(seconde.get(5, java.util.concurrent.TimeUnit.SECONDS)));
        // les deux futurs servis, une seule requête au fil — comptée chez le serveur
        assertTrue(serveur.tramesRecues().stream()
                .filter(t -> t.contains("textDocument/completion")).count() == 1);
    }

    /**
     * L'annulation au remplacement : une complétion en vol remplacée par
     * une plus fraîche — l'ancienne est annulée au fil ($/cancelRequest),
     * son futur échoue, la nouvelle part.
     */
    @Test
    void lannulationAuRemplacement() throws Exception {
        serveur.reponseDifferee("textDocument/completion");
        CompletableFuture<ValeurJson> vieille = ordonnanceur.demander(
                "textDocument/completion", parametresCompletion("file:///A.java", 3, 7),
                OrdonnanceurLsp.Priorite.INTERACTIF, cle("file:///A.java"));
        CompletableFuture<ValeurJson> neuve = ordonnanceur.demander(
                "textDocument/completion", parametresCompletion("file:///A.java", 3, 8),
                OrdonnanceurLsp.Priorite.INTERACTIF, cle("file:///A.java"));
        // la vieille a échoué sur l'annulation
        ExecutionException annulee = assertInstanceOf(ExecutionException.class,
                assertThrowsExecution(vieille));
        RequeteAnnuleeException cause = assertInstanceOf(RequeteAnnuleeException.class,
                annulee.getCause());
        assertEquals(RequeteAnnuleeException.Raison.REMPLACEE, cause.raison());
        // le fil porte l'annulation de l'ancienne : la notification $/cancelRequest
        attendreCondition(() -> serveur.tramesRecues().stream()
                .anyMatch(t -> t.contains("$/cancelRequest")));
        // la nouvelle est en vol
        assertEquals(2, serveur.requetesEnAttente("textDocument/completion"));
        // le serveur achève la plus ancienne — la remplacée — sa réponse tardive tombe, ignorée
        serveur.acheverRequete("textDocument/completion", JsonChaine.of("trop tard"));
        serveur.acheverRequete("textDocument/completion", JsonTableau.vide()
                .ajouter(JsonObjet.vide().ajouter("label", JsonChaine.of("fraiche"))));
        assertEquals("fraiche", libelle(neuve.get(5, java.util.concurrent.TimeUnit.SECONDS)));
    }

    /**
     * La priorité : une requête de fond en vol est devancée par une
     * interactive du même document — jamais l'inverse.
     */
    @Test
    void laPrioriteInteractive() throws Exception {
        serveur.reponseDifferee("textDocument/formatting");
        serveur.reponseDifferee("textDocument/hover");
        client.ouvrir("file:///A.java", "java", "class A {}\n");
        CompletableFuture<ValeurJson> fond = ordonnanceur.demander(
                "textDocument/formatting", JsonObjet.vide(),
                OrdonnanceurLsp.Priorite.FOND,
                new OrdonnanceurLsp.Cle("file:///A.java", "formatting"));
        CompletableFuture<ValeurJson> interactive = ordonnanceur.demander(
                "textDocument/hover", JsonObjet.vide(),
                OrdonnanceurLsp.Priorite.INTERACTIF,
                new OrdonnanceurLsp.Cle("file:///A.java", "hover"));
        ExecutionException devancee = assertInstanceOf(ExecutionException.class,
                assertThrowsExecution(fond));
        RequeteAnnuleeException causeDevancee = assertInstanceOf(
                RequeteAnnuleeException.class, devancee.getCause());
        assertEquals(RequeteAnnuleeException.Raison.DEVANCEE, causeDevancee.raison());
        // l'interactive vit sa vie
        serveur.acheverRequete("textDocument/hover", JsonObjet.vide()
                .ajouter("contents", JsonChaine.of("le survol")));
        assertTrue(interactive.get(5, java.util.concurrent.TimeUnit.SECONDS) != null);
        // et l'inverse ne déloge pas : le fond n'annule jamais l'interactif
        CompletableFuture<ValeurJson> interactiveDeux = ordonnanceur.demander(
                "textDocument/hover", JsonObjet.vide()
                        .ajouter("autre", JsonBooleen.of(true)),
                OrdonnanceurLsp.Priorite.INTERACTIF,
                new OrdonnanceurLsp.Cle("file:///A.java", "hover"));
        CompletableFuture<ValeurJson> fondDeux = ordonnanceur.demander(
                "textDocument/formatting", JsonObjet.vide(),
                OrdonnanceurLsp.Priorite.FOND,
                new OrdonnanceurLsp.Cle("file:///A.java", "formatting"));
        assertTrue(!interactiveDeux.isCompletedExceptionally(),
                "le fond ne devance jamais l'interactif");
        assertTrue(!fondDeux.isDone(), "le fond tient son vol, devancé par personne");
    }

    /**
     * La resynchronisation : la réponse d'une requête partie à la
     * version 1 arrive après le passage à la version 2 — elle est
     * périmée, l'appelant l'apprend, la requête fraîche passe.
     */
    @Test
    void laResynchronisation() throws Exception {
        serveur.reponseDifferee("textDocument/completion");
        client.ouvrir("file:///A.java", "java", "class A {}\n");
        CompletableFuture<ValeurJson> aLaVersionUne = ordonnanceur.demander(
                "textDocument/completion", parametresCompletion("file:///A.java", 0, 5),
                OrdonnanceurLsp.Priorite.INTERACTIF, cle("file:///A.java"));
        // le document change pendant le vol : la réponse qui arrive est périmée
        client.changer("file:///A.java", 2, "class A { void f() {} }\n");
        serveur.acheverRequete("textDocument/completion", JsonTableau.vide()
                .ajouter(JsonObjet.vide().ajouter("label", JsonChaine.of("tardif"))));
        ExecutionException perimee = assertInstanceOf(ExecutionException.class,
                assertThrowsExecution(aLaVersionUne));
        RequeteAnnuleeException causePerimee = assertInstanceOf(
                RequeteAnnuleeException.class, perimee.getCause());
        assertEquals(RequeteAnnuleeException.Raison.PERIMEE, causePerimee.raison());
        // la requête fraîche, partie après le changement, est servie
        CompletableFuture<ValeurJson> fraiche = ordonnanceur.demander(
                "textDocument/completion", parametresCompletion("file:///A.java", 0, 6),
                OrdonnanceurLsp.Priorite.INTERACTIF, cle("file:///A.java"));
        serveur.acheverRequete("textDocument/completion", JsonTableau.vide()
                .ajouter(JsonObjet.vide().ajouter("label", JsonChaine.of("frais"))));
        assertEquals("frais", libelle(fraiche.get(5, java.util.concurrent.TimeUnit.SECONDS)));
    }

    /** La réponse tardive d'une requête annulée : ignorée, l'ordonnanceur a déjà rendu son verdict. */
    @Test
    void laReponseTardiveEstIgnoree() throws Exception {
        serveur.reponseDifferee("textDocument/completion");
        CompletableFuture<ValeurJson> vieille = ordonnanceur.demander(
                "textDocument/completion", parametresCompletion("file:///A.java", 1, 1),
                OrdonnanceurLsp.Priorite.INTERACTIF, cle("file:///A.java"));
        CompletableFuture<ValeurJson> neuve = ordonnanceur.demander(
                "textDocument/completion",
                parametresCompletion("file:///A.java", 2, 2),
                OrdonnanceurLsp.Priorite.INTERACTIF, cle("file:///A.java"));
        assertTrue(vieille.isCompletedExceptionally());
        // le serveur achève la vieille requête annulée : la réponse tombe, rien ne casse
        serveur.acheverRequete("textDocument/completion", JsonChaine.of("trop tard"));
        serveur.acheverRequete("textDocument/completion", JsonChaine.of("la bonne"));
        // la neuve est servie, le client vit toujours
        assertEquals(JsonChaine.of("la bonne"), attendreValeur(neuve));
    }

    /** La complétion typée (B-04) : les paramètres du protocole, la réponse au modèle — la coupure honorée. */
    @Test
    void laCompletionTypee() throws Exception {
        serveur.reponseScriptee("textDocument/completion",
                JsonObjet.vide()
                        .ajouter("isIncomplete", JsonBooleen.of(true))
                        .ajouter("items", JsonTableau.vide()
                                .ajouter(JsonObjet.vide()
                                        .ajouter("label", JsonChaine.of("zebra")))
                                .ajouter(JsonObjet.vide()
                                        .ajouter("label", JsonChaine.of("apple")))));
        CompletableFuture<ListeCompletion> future = ordonnanceur.completion(
                "file:///A.java", new PositionLsp(3, 7));
        ListeCompletion liste = future.get(5, java.util.concurrent.TimeUnit.SECONDS);
        assertTrue(liste.estIncomplete());
        assertEquals(List.of("zebra", "apple"),
                liste.elements().stream().map(ElementCompletion::libelle).toList());
        // les paramètres partis au fil : textDocument et position
        List<MessageRpc> requetes = serveur.tramesRecues().stream()
                .map(t -> MessageRpc.de(JsonLecteur.lire(t)))
                .filter(m -> m instanceof RequeteRpc r
                        && "textDocument/completion".equals(r.methode()))
                .toList();
        assertEquals(1, requetes.size());
        JsonObjet params = assertInstanceOf(JsonObjet.class,
                ((RequeteRpc) requetes.get(0)).params());
        JsonObjet document = assertInstanceOf(JsonObjet.class, params.membre("textDocument"));
        assertEquals("file:///A.java", document.membreTexte("uri"));
        JsonObjet position = assertInstanceOf(JsonObjet.class, params.membre("position"));
        assertEquals(3, assertInstanceOf(JsonNombre.class,
                position.membre("line")).entier());
        assertEquals(7, assertInstanceOf(JsonNombre.class,
                position.membre("character")).entier());
    }

    // ── Aides ─────────────────────────────────────────────────────────

    /** La clé de remplacement d'un document. */
    private static OrdonnanceurLsp.Cle cle(String uri) {
        return new OrdonnanceurLsp.Cle(uri, "completion");
    }

    /** Les paramètres d'une complétion à une position. */
    private static JsonObjet parametresCompletion(String uri, int ligne, int colonne) {
        return JsonObjet.vide()
                .ajouter("textDocument", JsonObjet.vide()
                        .ajouter("uri", JsonChaine.of(uri)))
                .ajouter("position", new PositionLsp(ligne, colonne).versJson());
    }

    /** Le libellé du premier élément d'une réponse de complétion — tableau nu ou objet items. */
    private static String libelle(ValeurJson reponse) {
        JsonTableau items = reponse instanceof JsonTableau tableau ? tableau
                : assertInstanceOf(JsonTableau.class,
                        assertInstanceOf(JsonObjet.class, reponse).membre("items"));
        String libelle = assertInstanceOf(JsonObjet.class, items.element(0))
                .membreTexte("label");
        return assertInstanceOf(String.class, libelle);
    }

    /** Attend la valeur du futur — l'assertion d'échec est ailleurs. */
    private static ValeurJson attendreValeur(CompletableFuture<ValeurJson> futur)
            throws Exception {
        return futur.get(5, java.util.concurrent.TimeUnit.SECONDS);
    }

    /** Attend l'échec du futur — le retourne en exception d'exécution. */
    private static ExecutionException assertThrowsExecution(CompletableFuture<?> futur) {
        try {
            futur.get(5, java.util.concurrent.TimeUnit.SECONDS);
            throw new AssertionError("le futur devait echouer");
        } catch (java.util.concurrent.ExecutionException e) {
            return e;
        } catch (InterruptedException | java.util.concurrent.TimeoutException e) {
            throw new AssertionError("le futur devait echouer vite", e);
        }
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
