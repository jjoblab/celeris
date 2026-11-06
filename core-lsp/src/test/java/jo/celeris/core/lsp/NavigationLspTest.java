package jo.celeris.core.lsp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * La navigation en ligne (B-10) et le raffinement des usages (B-11) : la
 * définition et les références passent par le serveur — l'index hors
 * ligne répond vite, le serveur répond juste, jamais de chaînage
 * (ADR-005, règle des rôles). Le lieu décodé (uri + plage) est ce que
 * l'historique de navigation M5-t4 consomme pour le retour au point de
 * départ.
 */
class NavigationLspTest {

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

    /** Le lieu : uri et plage lues, la plage absente reste nulle, jamais devinée. */
    @Test
    void leLieuDecode() {
        LieuLsp lieu = LieuLsp.de(JsonObjet.vide()
                .ajouter("uri", JsonChaine.of("file:///B.java"))
                .ajouter("range", new PlageLsp(new PositionLsp(4, 2),
                        new PositionLsp(4, 11)).versJson()));
        assertEquals("file:///B.java", lieu.uri());
        assertEquals(new PositionLsp(4, 2), lieu.plage().debut());
        assertEquals(new PositionLsp(4, 11), lieu.plage().fin());
        LieuLsp sansPlage = LieuLsp.de(JsonObjet.vide()
                .ajouter("uri", JsonChaine.of("file:///C.java")));
        assertEquals("file:///C.java", sansPlage.uri());
        assertNull(sansPlage.plage());
    }

    /** La liste : le tableau des lieux dans l'ordre, l'objet seul toléré, le vide sans casse. */
    @Test
    void laListeDesLieux() {
        assertEquals(0, LieuLsp.liste(JsonNul.nul()).size());
        assertEquals(0, LieuLsp.liste(JsonTableau.vide()).size());
        List<LieuLsp> deux = LieuLsp.liste(JsonTableau.vide()
                .ajouter(lieuJson("file:///B.java", 4, 2, 4, 11))
                .ajouter(lieuJson("file:///A.java", 0, 6, 0, 12)));
        assertEquals(2, deux.size());
        assertEquals("file:///B.java", deux.get(0).uri());
        assertEquals("file:///A.java", deux.get(1).uri());
        List<LieuLsp> seul = LieuLsp.liste(lieuJson("file:///D.java", 1, 0, 1, 3));
        assertEquals(1, seul.size());
        assertEquals("file:///D.java", seul.get(0).uri());
    }

    /** La définition : requête au serveur par la clé du document, les lieux décodés. */
    @Test
    void laDefinitionAuServeur() throws Exception {
        serveur.reponseScriptee("textDocument/definition", JsonTableau.vide()
                .ajouter(lieuJson("file:///B.java", 4, 2, 4, 11)));
        CompletableFuture<List<LieuLsp>> futur = ordonnanceur.definition(
                "file:///A.java", new PositionLsp(9, 14));
        List<LieuLsp> lieux = futur.get(5, TimeUnit.SECONDS);
        assertEquals(1, lieux.size());
        assertEquals("file:///B.java", lieux.get(0).uri());
        assertEquals(new PositionLsp(4, 2), lieux.get(0).plage().debut());
        String trame = serveur.tramesRecues().stream()
                .filter(t -> t.contains("textDocument/definition")).findFirst().orElse("");
        assertTrue(trame.contains("file:///A.java"));
        assertTrue(trame.contains("\"line\":9"));
        assertTrue(trame.contains("\"character\":14"));
    }

    /** Les références : le contexte exige les déclarations, les lieux décodés. */
    @Test
    void lesReferencesAuServeur() throws Exception {
        serveur.reponseScriptee("textDocument/references", JsonTableau.vide()
                .ajouter(lieuJson("file:///A.java", 3, 8, 3, 12))
                .ajouter(lieuJson("file:///A.java", 17, 4, 17, 8)));
        CompletableFuture<List<LieuLsp>> futur = ordonnanceur.references(
                "file:///A.java", new PositionLsp(3, 10));
        List<LieuLsp> lieux = futur.get(5, TimeUnit.SECONDS);
        assertEquals(2, lieux.size());
        assertEquals(new PositionLsp(17, 8), lieux.get(1).plage().fin());
        String trame = serveur.tramesRecues().stream()
                .filter(t -> t.contains("textDocument/references")).findFirst().orElse("");
        assertTrue(trame.contains("includeDeclaration"));
        assertTrue(trame.contains("true"));
    }

    private static JsonObjet lieuJson(String uri, int ld, int cd, int lf, int cf) {
        return JsonObjet.vide()
                .ajouter("uri", JsonChaine.of(uri))
                .ajouter("range", new PlageLsp(new PositionLsp(ld, cd),
                        new PositionLsp(lf, cf)).versJson());
    }
}
