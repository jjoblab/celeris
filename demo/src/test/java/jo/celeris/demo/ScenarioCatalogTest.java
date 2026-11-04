package jo.celeris.demo;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Test de fumée du catalogue de scénarios : le jalon M2 exige la
 * présence des scénarios A-13, A-18, A-20, A-24 et A-25, dans l'ordre
 * de la matrice de parité, uniques et complets (libellé affiché et
 * critère d'acceptation rappelés).
 */
final class ScenarioCatalogTest {

    /** Identifiant de la matrice de parité : lettre(s), tiret, nombre. */
    private static final Pattern IDENTIFIANT = Pattern.compile("[A-Z]+-\\d+");

    @Test
    void m2ScenariosAreAllPresentInParityOrder() {
        List<String> identifiants = ScenarioCatalog.scenarios().stream()
                .map(Scenario::identifiant).toList();
        assertEquals(List.of("A-13", "A-18", "A-20", "A-24", "A-25"), identifiants);
    }

    @Test
    void identifiersAreUniqueAndWellFormed() {
        List<String> identifiants = ScenarioCatalog.scenarios().stream()
                .map(Scenario::identifiant).toList();
        assertEquals(identifiants.size(), identifiants.stream().distinct().count());
        for (String identifiant : identifiants) {
            assertTrue(IDENTIFIANT.matcher(identifiant).matches(),
                    "identifiant mal formé : " + identifiant);
        }
    }

    @Test
    void everyScenarioCarriesLabelAndAcceptanceCriterion() {
        for (Scenario scenario : ScenarioCatalog.scenarios()) {
            assertFalse(scenario.libelle().isBlank(),
                    scenario.identifiant() + " sans libellé");
            assertFalse(scenario.objectif().isBlank(),
                    scenario.identifiant() + " sans critère d'acceptation");
            assertTrue(scenario.objectif().endsWith("."),
                    scenario.identifiant() + " : critère d'acceptation inachevé");
        }
    }
}
