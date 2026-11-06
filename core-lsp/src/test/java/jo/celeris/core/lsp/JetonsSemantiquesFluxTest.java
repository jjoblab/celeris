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
 * périmée. Ici, chaque flux vit sa vie : les jetons arrivent, se
 * stockent et se publient seuls ; les diagnostics n'y peuvent rien,
 * ni leur absence, ni leur tempête.</p>
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
     * PIEGE-18 — les jetons ne dépendent pas de l'état des diagnostics :
     * sans aucun diagnostic émis, les jetons arrivent, se stockent et se
     * publient ; une tempête de diagnostics ensuite ne les change pas —
     * et les deux flux vivent sans auditeur l'un de l'autre.
     */
    @Test
    void semanticTokens_independentOfDiagnostics() {
        List<String> urisJetons = new ArrayList<>();
        client.ajouterAuditeurJetons((uri, jetons) -> urisJetons.add(uri));
        // AUCUN diagnostic n'est jamais émis pour ce document : les jetons
        // arrivent quand même — publiés et stockés
        serveur.notifier("textDocument/semanticTokens/full", JsonObjet.vide()
                .ajouter("textDocument", JsonObjet.vide()
                        .ajouter("uri", JsonChaine.of("file:///A.java")))
                .ajouter("resultId", JsonChaine.of("jetons-1"))
                .ajouter("data", JsonTableau.vide()
                        .ajouter(JsonNombre.of(0)).ajouter(JsonNombre.of(4))
                        .ajouter(JsonNombre.of(5)).ajouter(JsonNombre.of(0))
                        .ajouter(JsonNombre.of(0))));
        attendreCondition(() -> !urisJetons.isEmpty());
        assertEquals(List.of("file:///A.java"), urisJetons);
        assertEquals(1, client.jetonsDe("file:///A.java").jetons().size());
        assertEquals(new JetonSemantique(0, 4, 5, 0, 0),
                client.jetonsDe("file:///A.java").jetons().get(0));

        // une tempête de diagnostics pour le même document : les jetons ne
        // changent pas d'un iota — même liste, même résultat
        serveur.notifier("textDocument/publishDiagnostics", JsonObjet.vide()
                .ajouter("uri", JsonChaine.of("file:///A.java"))
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
        attendreCondition(() -> client.diagnosticsDe("file:///A.java").size() == 1);
        assertEquals(1, client.jetonsDe("file:///A.java").jetons().size());
        assertEquals(new JetonSemantique(0, 4, 5, 0, 0),
                client.jetonsDe("file:///A.java").jetons().get(0));
        // et les jetons continuent d'arriver après les diagnostics
        serveur.notifier("textDocument/semanticTokens/full", JsonObjet.vide()
                .ajouter("textDocument", JsonObjet.vide()
                        .ajouter("uri", JsonChaine.of("file:///A.java")))
                .ajouter("resultId", JsonChaine.of("jetons-2"))
                .ajouter("data", JsonTableau.vide()
                        .ajouter(JsonNombre.of(1)).ajouter(JsonNombre.of(0))
                        .ajouter(JsonNombre.of(3)).ajouter(JsonNombre.of(1))
                        .ajouter(JsonNombre.of(0))));
        attendreCondition(() -> "jetons-2".equals(
                client.jetonsDe("file:///A.java").identifiantResultat()));
        assertEquals(new JetonSemantique(1, 0, 3, 1, 0),
                client.jetonsDe("file:///A.java").jetons().get(0));
    }

    /** Chaque uri vit sa vie : les jetons d'un document ne débordent pas sur l'autre. */
    @Test
    void lesJetonsParDocument() {
        serveur.notifier("textDocument/semanticTokens/full", JsonObjet.vide()
                .ajouter("textDocument", JsonObjet.vide()
                        .ajouter("uri", JsonChaine.of("file:///A.java")))
                .ajouter("data", JsonTableau.vide()
                        .ajouter(JsonNombre.of(0)).ajouter(JsonNombre.of(0))
                        .ajouter(JsonNombre.of(1)).ajouter(JsonNombre.of(0))
                        .ajouter(JsonNombre.of(0))));
        serveur.notifier("textDocument/semanticTokens/full", JsonObjet.vide()
                .ajouter("textDocument", JsonObjet.vide()
                        .ajouter("uri", JsonChaine.of("file:///B.kt")))
                .ajouter("data", JsonTableau.vide()
                        .ajouter(JsonNombre.of(2)).ajouter(JsonNombre.of(1))
                        .ajouter(JsonNombre.of(2)).ajouter(JsonNombre.of(1))
                        .ajouter(JsonNombre.of(0))));
        attendreCondition(() -> client.jetonsDe("file:///B.kt").jetons().size() == 1);
        assertEquals(new JetonSemantique(0, 0, 1, 0, 0),
                client.jetonsDe("file:///A.java").jetons().get(0));
        assertEquals(new JetonSemantique(2, 1, 2, 1, 0),
                client.jetonsDe("file:///B.kt").jetons().get(0));
        assertTrue(client.jetonsDe("file:///inconnu.kt").jetons().isEmpty());
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
