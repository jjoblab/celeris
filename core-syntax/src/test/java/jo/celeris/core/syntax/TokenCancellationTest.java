package jo.celeris.core.syntax;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Le jeton d'annulation (ADR-007) : annuler coûte moins cher que
 * terminer, la vérification est un point de contrôle explicite — le
 * moteur d'analyse l'interroge entre deux phases longues, jamais dans
 * la boucle chaude. Sans moniteur ni atomique : le jeton est un
 * booléen gardé par son verrou intrinsèque.
 */
class TokenCancellationTest {

    /** Un jeton frais est actif : la vérification passe. */
    @Test
    void jetonActifVerifieSansLever() {
        TokenCancellation token = new TokenCancellation();
        assertFalse(token.isCancelled());
        token.check();
    }

    /** Un jeton annulé lève AnalyseAnnulee à la vérification. */
    @Test
    void jetonAnnuleLeveAnalyseAnnulee() {
        TokenCancellation token = new TokenCancellation();
        token.annuler();
        assertTrue(token.isCancelled());
        assertThrows(AnalysisCancelled.class, token::check);
    }

    /** L'annulation depuis un autre fil devient visible : moniteur et précédence. */
    @Test
    void annulationDepuisAutreFilDevientVisible() throws InterruptedException {
        TokenCancellation token = new TokenCancellation();
        Thread thread = new Thread(token::annuler);
        thread.start();
        thread.join(5_000);
        assertTrue(token.isCancelled());
    }

    /** Annuler deux fois reste idempotent. */
    @Test
    void annulationIdempotente() {
        TokenCancellation token = new TokenCancellation();
        token.annuler();
        token.annuler();
        assertTrue(token.isCancelled());
    }
}
