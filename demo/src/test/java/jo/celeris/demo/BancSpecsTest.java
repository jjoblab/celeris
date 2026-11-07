package jo.celeris.demo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Le banc des vérifications automatiques (M7.5, §9) : les
 * spécifications de la démonstration passent — la démo qui affiche
 * ses preuves ne peut pas en afficher de fausses —, chaque
 * identifiant cite un scénario CONNU du catalogue, aucun doublon.
 */
class BancSpecsTest {

    /** Les verdicts du banc : toutes les spécifications tiennent. */
    @Test
    void everySpecHolds() {
        Map<String, Boolean> verdicts = BancSpecs.verdicts();
        assertFalse(verdicts.isEmpty(), "le banc porte des spécifications");
        for (Map.Entry<String, Boolean> verdict : verdicts.entrySet()) {
            assertTrue(verdict.getValue(),
                    "la spécification " + verdict.getKey() + " doit tenir");
        }
    }

    /** Chaque spécification cite un scénario connu du catalogue — jamais un doublon. */
    @Test
    void everySpecCitesAKnownScenario() {
        Set<String> connus = new HashSet<>();
        for (Scenario scenario : ScenarioCatalog.scenarios()) {
            connus.add(scenario.identifiant());
        }
        Set<String> vus = new HashSet<>();
        for (ScenarioSpec spec : BancSpecs.specs()) {
            assertTrue(connus.contains(spec.identifiant()),
                    "la spécification " + spec.identifiant()
                            + " cite un scénario inconnu du catalogue");
            assertTrue(vus.add(spec.identifiant()),
                    "la spécification " + spec.identifiant() + " est en double");
        }
    }
}
