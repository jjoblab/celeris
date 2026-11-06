package jo.celeris.core.lsp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Les indices en ligne (B-08) : le serveur les rend pour une plage —
 * le visible —, le client les tire pour cette plage, priorité DE FOND :
 * l'interactif du même document les devance (ADR-007). Le libellé vient
 * en deux formes bornées : la chaîne nue ou les parties typées, chaque
 * partie pouvant porter sa cible de navigation.
 */
class IndicesLspTest {

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

    /** L'indice à libellé chaîne : une partie sans cible, les défauts tenus. */
    @Test
    void lIndiceAChaine() {
        IndiceLsp indice = IndiceLsp.de(indiceJson(new PositionLsp(3, 8),
                JsonChaine.of(" : Int"), 1, false, false));
        assertEquals(new PositionLsp(3, 8), indice.position());
        assertEquals(1, indice.parties().size());
        assertEquals(" : Int", indice.parties().get(0).valeur());
        assertNull(indice.parties().get(0).cible(), "la cible absente reste nulle");
        assertEquals(1, indice.genre());
        assertFalse(indice.remplissageGauche());
        assertFalse(indice.remplissageDroite());
    }

    /** L'indice à parties : chaque partie porte sa valeur, la cible quand le serveur la donne. */
    @Test
    void lIndiceAParties() {
        IndiceLsp indice = IndiceLsp.de(indiceJson(new PositionLsp(7, 0),
                JsonTableau.vide()
                        .ajouter(JsonObjet.vide()
                                .ajouter("value", JsonChaine.of("noms"))
                                .ajouter("location", JsonObjet.vide()
                                        .ajouter("uri", JsonChaine.of("file:///A.java"))
                                        .ajouter("range", new PlageLsp(
                                                new PositionLsp(1, 6),
                                                new PositionLsp(1, 10)).versJson())))
                        .ajouter(JsonObjet.vide()
                                .ajouter("value", JsonChaine.of(": List<String>"))),
                null, true, true));
        assertEquals(2, indice.parties().size());
        assertEquals("noms", indice.parties().get(0).valeur());
        assertEquals("file:///A.java", indice.parties().get(0).cible().uri());
        assertEquals(new PositionLsp(1, 6), indice.parties().get(0).cible().plage().debut());
        assertNull(indice.parties().get(1).cible(), "sans location, aucune cible");
        assertEquals(0, indice.genre(), "le genre absent vaut zero");
        assertTrue(indice.remplissageGauche());
        assertTrue(indice.remplissageDroite());
    }

    /** La liste : le tableau dans l'ordre, le vide sans casse. */
    @Test
    void laListeDesIndices() {
        assertEquals(0, IndiceLsp.liste(JsonNul.nul()).size());
        List<IndiceLsp> deux = IndiceLsp.liste(JsonTableau.vide()
                .ajouter(indiceJson(new PositionLsp(3, 8), JsonChaine.of(" : Int"),
                        1, false, false))
                .ajouter(indiceJson(new PositionLsp(9, 4), JsonChaine.of("a ="),
                        null, true, false)));
        assertEquals(2, deux.size());
        assertEquals(new PositionLsp(9, 4), deux.get(1).position());
    }

    /** La requête : la plage visible passe au serveur, priorité de fond — l'interactif devance. */
    @Test
    void laRequeteDeFondDevancee() throws Exception {
        serveur.reponseDifferee("textDocument/inlayHint");
        CompletableFuture<List<IndiceLsp>> indices = ordonnanceur.indices(
                "file:///A.java", new PlageLsp(new PositionLsp(0, 0),
                        new PositionLsp(40, 0)));
        assertEquals(1, serveur.requetesEnAttente("textDocument/inlayHint"));
        String trame = serveur.tramesRecues().stream()
                .filter(t -> t.contains("textDocument/inlayHint")).findFirst().orElse("");
        assertTrue(trame.contains("file:///A.java"));
        assertTrue(trame.contains("\"start\""));
        // l'interactif du même document devance le fond : la complétion part,
        // les indices en vol échouent DEVANCÉE
        serveur.reponseScriptee("textDocument/completion", JsonTableau.vide());
        CompletableFuture<ListeCompletion> completion = ordonnanceur.completion(
                "file:///A.java", new PositionLsp(10, 3));
        assertInstanceOf(RequeteAnnuleeException.class, echecDe(indices));
        assertFalse(completion.isCompletedExceptionally());
    }

    private static JsonObjet indiceJson(PositionLsp position, ValeurJson libelle,
            Integer genre, boolean gauche, boolean droite) {
        JsonObjet objet = JsonObjet.vide()
                .ajouter("position", position.versJson())
                .ajouter("label", libelle);
        if (genre != null) {
            objet = objet.ajouter("kind", JsonNombre.of(genre));
        }
        if (gauche) {
            objet = objet.ajouter("paddingLeft", JsonBooleen.of(gauche));
        }
        if (droite) {
            objet = objet.ajouter("paddingRight", JsonBooleen.of(droite));
        }
        return objet;
    }

    private static Throwable echecDe(CompletableFuture<?> futur) throws Exception {
        try {
            futur.get(5, TimeUnit.SECONDS);
            throw new IllegalStateException("le futur devait echouer");
        } catch (ExecutionException e) {
            return e.getCause();
        }
    }
}
