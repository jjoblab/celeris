package jo.celeris.core.lsp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Les flux du client (B-02) : jetons sémantiques et diagnostics sont
 * deux sources indépendantes, jamais chaînées.
 *
 * <p>PIEGE-18 (hérité du dépôt de départ) : le tir sémantique ne doit
 * pas dépendre de l'état des diagnostics. Dans le dépôt de départ, les
 * jetons n'étaient rafraîchis qu'à l'arrivée des diagnostics — un
 * serveur silencieux sur les erreurs laissait la coloration sémantique
 * périmée. Ici, les jetons se TIRENT par requête, les diagnostics se
 * POUSSENT par notification : deux flux distincts, l'un ne conditionne
 * jamais l'autre.</p>
 */
class JetonsSemantiquesFluxTest {

    private TransportMemoire.Paire paire;
    private ServeurFactice serveur;
    private ClientLsp client;

    @BeforeEach
    void monter() {
        paire = TransportMemoire.creerPaire();
        serveur = ServeurFactice.brancher(paire);
        client = ClientLsp.sur(paire.boutA(), "tests-celeris");
        client.initialiser(JsonObjet.vide());
    }

    @AfterEach
    void demonter() {
        client.close();
        serveur.close();
    }

    /**
     * PIEGE-18 — le tir sémantique ne dépend pas de l'état des
     * diagnostics : sans qu'AUCUN diagnostic ne soit jamais émis, les
     * jetons se tirent, se stockent et se publient ; une tempête de
     * diagnostics ensuite ne les change pas — et le tir continue après.
     */
    @Test
    void semanticTokens_independentOfDiagnostics() {
        List<String> urisJetons = new ArrayList<>();
        client.ajouterAuditeurJetons((uri, jetons) -> urisJetons.add(uri));
        // AUCUN diagnostic n'est jamais émis pour ce document : le tir
        // marche quand même — stocké et publié
        scripterJetons("jetons-1", 0, 4, 5, 0, 0);
        JetonsSemantiques jetons = client.tirerJetons("file:///A.java");
        assertEquals(List.of("file:///A.java"), urisJetons);
        assertEquals(List.of(new JetonSemantique(0, 4, 5, 0, 0)), jetons.jetons());
        assertEquals(List.of(new JetonSemantique(0, 4, 5, 0, 0)),
                client.jetonsDe("file:///A.java").jetons());

        // une tempête de diagnostics pour le même document : les jetons ne
        // changent pas d'un iota — même liste, même résultat
        servirDiagnostics("file:///A.java");
        attendreCondition(() -> client.diagnosticsDe("file:///A.java").size() == 1);
        assertEquals(List.of(new JetonSemantique(0, 4, 5, 0, 0)),
                client.jetonsDe("file:///A.java").jetons());
        // et le tir continue d'arriver après les diagnostics, sans y passer
        scripterJetons("jetons-2", 1, 0, 3, 1, 0);
        JetonsSemantiques seconds = client.tirerJetons("file:///A.java");
        assertEquals("jetons-2", seconds.identifiantResultat());
        assertEquals(List.of(new JetonSemantique(1, 0, 3, 1, 0)),
                client.jetonsDe("file:///A.java").jetons());
        // le flux jetons vit sans auditeur diagnostics — et réciproquement
        assertTrue(client.diagnosticsDe("file:///A.java").size() == 1);
    }

    /** Chaque uri vit sa vie : les jetons d'un document ne débordent pas sur l'autre. */
    @Test
    void lesJetonsParDocument() {
        scripterJetons("jetons-A", 0, 0, 1, 0, 0);
        client.tirerJetons("file:///A.java");
        scripterJetons("jetons-B", 2, 1, 2, 1, 0);
        client.tirerJetons("file:///B.kt");
        assertEquals(List.of(new JetonSemantique(0, 0, 1, 0, 0)),
                client.jetonsDe("file:///A.java").jetons());
        assertEquals(List.of(new JetonSemantique(2, 1, 2, 1, 0)),
                client.jetonsDe("file:///B.kt").jetons());
        assertTrue(client.jetonsDe("file:///inconnu.kt").jetons().isEmpty());
    }

    /** Le serveur sans jetons : réponse vide, tir vide — jamais d'exception. */
    @Test
    void leTirVide() {
        serveur.reponseScriptee("textDocument/semanticTokens/full", JsonNul.nul());
        JetonsSemantiques jetons = client.tirerJetons("file:///A.java");
        assertTrue(jetons.jetons().isEmpty());
        assertTrue(client.jetonsDe("file:///A.java").jetons().isEmpty());
    }

    // ── Aides ─────────────────────────────────────────────────────────

    /** Scripte la réponse de tir de jetons — delta d'un seul jeton. */
    private void scripterJetons(String identifiant, int... delta) {
        JsonTableau data = JsonTableau.vide();
        for (int valeur : delta) {
            data = data.ajouter(JsonNombre.of(valeur));
        }
        serveur.reponseScriptee("textDocument/semanticTokens/full", JsonObjet.vide()
                .ajouter("resultId", JsonChaine.of(identifiant))
                .ajouter("data", data));
    }

    /** Sert une tempête de diagnostics — une erreur en ligne 0. */
    private void servirDiagnostics(String uri) {
        serveur.notifier("textDocument/publishDiagnostics", JsonObjet.vide()
                .ajouter("uri", JsonChaine.of(uri))
                .ajouter("diagnostics", JsonTableau.vide()
                        .ajouter(JsonObjet.vide()
                                .ajouter("range", JsonObjet.vide()
                                        .ajouter("start", JsonObjet.vide()
                                                .ajouter("line", JsonNombre.of(0))
                                                .ajouter("character", JsonNombre.of(1)))
                                        .ajouter("end", JsonObjet.vide()
                                                .ajouter("line", JsonNombre.of(0))
                                                .ajouter("character", JsonNombre.of(2))))
                                .ajouter("severity", JsonNombre.of(1))
                                .ajouter("message", JsonChaine.of("erreur")))));
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
