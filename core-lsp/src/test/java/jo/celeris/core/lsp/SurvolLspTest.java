package jo.celeris.core.lsp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Le survol du serveur (B-09) : la documentation se RÉSOUT À LA DEMANDE —
 * la requête part quand l'utilisateur survole, jamais avant — et le
 * contenu arrive en deux formes bornées : la chaîne nue ou le contenu
 * balisé (genre + texte). Les champs absents sont nuls, jamais devinés.
 */
class SurvolLspTest {

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

    /** Le contenu balisé : genre et texte lus, la plage présente décodée. */
    @Test
    void leContenuBalise() {
        SurvolLsp survol = SurvolLsp.de(JsonObjet.vide()
                .ajouter("contents", JsonObjet.vide()
                        .ajouter("kind", JsonChaine.of("markdown"))
                        .ajouter("value", JsonChaine.of("Ajoute deux entiers.")))
                .ajouter("range", new PlageLsp(new PositionLsp(1, 4),
                        new PositionLsp(1, 8)).versJson()));
        assertEquals("markdown", survol.genre());
        assertEquals("Ajoute deux entiers.", survol.texte());
        PlageLsp plage = survol.plage();
        assertNotNull(plage);
        assertEquals(new PositionLsp(1, 4), plage.debut());
        assertEquals(new PositionLsp(1, 8), plage.fin());
    }

    /** La chaîne nue : le texte sans genre, aucune plage. */
    @Test
    void laChaineNue() {
        SurvolLsp survol = SurvolLsp.de(JsonChaine.of("Une méthode."));
        assertEquals("Une méthode.", survol.texte());
        assertEquals("", survol.genre());
        assertNull(survol.plage());
    }

    /** La réponse vide : le survol vide, jamais une exception. */
    @Test
    void laReponseVide() {
        assertEquals(SurvolLsp.VIDE, SurvolLsp.de(JsonNul.nul()));
        assertEquals("", SurvolLsp.VIDE.texte());
        assertEquals("", SurvolLsp.VIDE.genre());
        assertNull(SurvolLsp.VIDE.plage());
    }

    /** La résolution à la demande : la requête ne part qu'à l'appel — une seule. */
    @Test
    void laResolutionALaDemande() throws Exception {
        assertTrue(serveur.tramesRecues().stream()
                .noneMatch(t -> t.contains("textDocument/hover")), "aucune requete avant l'appel");
        serveur.reponseScriptee("textDocument/hover", JsonObjet.vide()
                .ajouter("contents", JsonObjet.vide()
                        .ajouter("kind", JsonChaine.of("markdown"))
                        .ajouter("value", JsonChaine.of("La doc."))));
        CompletableFuture<SurvolLsp> futur = ordonnanceur.survol(
                "file:///A.java", new PositionLsp(2, 7));
        SurvolLsp survol = futur.get(5, TimeUnit.SECONDS);
        assertEquals("La doc.", survol.texte());
        assertEquals("markdown", survol.genre());
        assertEquals(1, serveur.tramesRecues().stream()
                .filter(t -> t.contains("textDocument/hover")).count());
        // la requête porte le document et la position
        String trame = serveur.tramesRecues().stream()
                .filter(t -> t.contains("textDocument/hover")).findFirst().orElse("");
        assertTrue(trame.contains("file:///A.java"));
        assertTrue(trame.contains("\"line\":2"));
        assertTrue(trame.contains("\"character\":7"));
    }
}
