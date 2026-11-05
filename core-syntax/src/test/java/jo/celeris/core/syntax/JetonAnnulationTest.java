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
class JetonAnnulationTest {

    /** Un jeton frais est actif : la vérification passe. */
    @Test
    void jetonActifVerifieSansLever() {
        JetonAnnulation jeton = new JetonAnnulation();
        assertFalse(jeton.estAnnule());
        jeton.verifier();
    }

    /** Un jeton annulé lève AnalyseAnnulee à la vérification. */
    @Test
    void jetonAnnuleLeveAnalyseAnnulee() {
        JetonAnnulation jeton = new JetonAnnulation();
        jeton.annuler();
        assertTrue(jeton.estAnnule());
        assertThrows(AnalyseAnnulee.class, jeton::verifier);
    }

    /** L'annulation depuis un autre fil devient visible : moniteur et précédence. */
    @Test
    void annulationDepuisAutreFilDevientVisible() throws InterruptedException {
        JetonAnnulation jeton = new JetonAnnulation();
        Thread fil = new Thread(jeton::annuler);
        fil.start();
        fil.join(5_000);
        assertTrue(jeton.estAnnule());
    }

    /** Annuler deux fois reste idempotent. */
    @Test
    void annulationIdempotente() {
        JetonAnnulation jeton = new JetonAnnulation();
        jeton.annuler();
        jeton.annuler();
        assertTrue(jeton.estAnnule());
    }
}
