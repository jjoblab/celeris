package jo.celeris.core.lsp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * L'aide aux paramètres (B-07) : les signatures du serveur décodées, la
 * signature active et le paramètre courant exposés — BORNÉ à l'arité de
 * la signature active, les serveurs généreux qui envoient un paramètre
 * au-delà ne cassent pas le surlignage. Les champs absents restent nuls,
 * jamais devinés.
 */
class SignatureLspTest {

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

    /** Les signatures décodées : libellés, paramètres en chaînes, actifs lus. */
    @Test
    void lesSignaturesDecodees() {
        SignatureLsp aide = SignatureLsp.de(aideDeuxSignatures());
        assertEquals(2, aide.entrees().size());
        SignatureLsp.Entree premiere = aide.entrees().get(0);
        assertEquals("ajouter(int a, int b)", premiere.libelle());
        assertEquals(2, premiere.parametres().size());
        assertEquals("a", premiere.parametres().get(0).libelle());
        assertEquals("b", premiere.parametres().get(1).libelle());
        assertNull(premiere.parametres().get(0).documentation());
        // la signature active : la seconde, le paramètre courant : le premier
        assertEquals(1, aide.signatureActiveIndex());
        SignatureLsp.Entree active = aide.signatureActive();
        assertEquals("ajouter(int a, int b, int c)", active.libelle());
        assertEquals(0, aide.parametreCourant());
    }

    /** Le libellé de paramètre par bornes : la tranche du libellé de signature. */
    @Test
    void leLibelleDeParametreParBornes() {
        // « ajouter(int a, int b) » : « a » vit de 12 à 13, « b » de 18 à 19
        SignatureLsp aide = SignatureLsp.de(JsonObjet.vide()
                .ajouter("signatures", JsonTableau.vide()
                        .ajouter(JsonObjet.vide()
                                .ajouter("label", JsonChaine.of("ajouter(int a, int b)"))
                                .ajouter("parameters", JsonTableau.vide()
                                        .ajouter(JsonObjet.vide().ajouter("label",
                                                JsonTableau.vide()
                                                        .ajouter(JsonNombre.of(12))
                                                        .ajouter(JsonNombre.of(13))))
                                        .ajouter(JsonObjet.vide().ajouter("label",
                                                JsonTableau.vide()
                                                        .ajouter(JsonNombre.of(19))
                                                        .ajouter(JsonNombre.of(20))))))));
        assertEquals("a", aide.entrees().get(0).parametres().get(0).libelle());
        assertEquals("b", aide.entrees().get(0).parametres().get(1).libelle());
    }

    /** Le paramètre courant BORNÉ : au-delà de l'arité, le dernier ; absent, -1. */
    @Test
    void leParametreCourantBorne() {
        SignatureLsp genereux = SignatureLsp.de(JsonObjet.vide()
                .ajouter("signatures", JsonTableau.vide()
                        .ajouter(JsonObjet.vide()
                                .ajouter("label", JsonChaine.of("ajouter(int a, int b)"))
                                .ajouter("parameters", JsonTableau.vide()
                                        .ajouter(parametreTexte("a"))
                                        .ajouter(parametreTexte("b")))))
                .ajouter("activeSignature", JsonNombre.of(0))
                .ajouter("activeParameter", JsonNombre.of(7)));
        assertEquals(1, genereux.parametreCourant(), "borne au dernier parametre");
        SignatureLsp absent = SignatureLsp.de(JsonObjet.vide()
                .ajouter("signatures", JsonTableau.vide()
                        .ajouter(JsonObjet.vide()
                                .ajouter("label", JsonChaine.of("ajouter(int a, int b)"))
                                .ajouter("parameters", JsonTableau.vide()
                                        .ajouter(parametreTexte("a"))
                                        .ajouter(parametreTexte("b"))))));
        assertEquals(-1, absent.signatureActiveIndex());
        assertNull(absent.signatureActive());
        assertEquals(-1, absent.parametreCourant());
        // sans paramètre : aucun courant, même si le serveur en annonce un
        SignatureLsp vide = SignatureLsp.de(JsonObjet.vide()
                .ajouter("signatures", JsonTableau.vide()
                        .ajouter(JsonObjet.vide()
                                .ajouter("label", JsonChaine.of("fermer()"))))
                .ajouter("activeSignature", JsonNombre.of(0))
                .ajouter("activeParameter", JsonNombre.of(0)));
        assertEquals(-1, vide.parametreCourant());
    }

    /** La réponse vide : aucune signature, jamais une exception. */
    @Test
    void laReponseVide() {
        assertEquals(0, SignatureLsp.de(JsonNul.nul()).entrees().size());
    }

    /** La requête : signatureHelp par la clé du document, la réponse décodée. */
    @Test
    void laRequeteDeSignature() throws Exception {
        serveur.reponseScriptee("textDocument/signatureHelp", aideDeuxSignatures());
        CompletableFuture<SignatureLsp> futur = ordonnanceur.signature(
                "file:///A.java", new PositionLsp(5, 18));
        SignatureLsp aide = futur.get(5, TimeUnit.SECONDS);
        assertEquals(2, aide.entrees().size());
        String trame = serveur.tramesRecues().stream()
                .filter(t -> t.contains("textDocument/signatureHelp")).findFirst().orElse("");
        assertTrue(trame.contains("file:///A.java"));
        assertTrue(trame.contains("\"line\":5"));
        assertTrue(trame.contains("\"character\":18"));
    }

    private static JsonObjet aideDeuxSignatures() {
        return JsonObjet.vide()
                .ajouter("signatures", JsonTableau.vide()
                        .ajouter(JsonObjet.vide()
                                .ajouter("label", JsonChaine.of("ajouter(int a, int b)"))
                                .ajouter("parameters", JsonTableau.vide()
                                        .ajouter(parametreTexte("a"))
                                        .ajouter(parametreTexte("b"))))
                        .ajouter(JsonObjet.vide()
                                .ajouter("label", JsonChaine.of("ajouter(int a, int b, int c)"))
                                .ajouter("parameters", JsonTableau.vide()
                                        .ajouter(parametreTexte("a"))
                                        .ajouter(parametreTexte("b"))
                                        .ajouter(parametreTexte("c")))))
                .ajouter("activeSignature", JsonNombre.of(1))
                .ajouter("activeParameter", JsonNombre.of(0));
    }

    private static JsonObjet parametreTexte(String libelle) {
        return JsonObjet.vide().ajouter("label", JsonChaine.of(libelle));
    }
}
