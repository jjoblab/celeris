package jo.celeris.demo;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Test de fumée du catalogue de scénarios : le jalon M4 exige les
 * scénarios des facettes livrées — A-15 (plis depuis l'arbre), B-01
 * (coloration du même arbre), B-16 (plan consommateur du même arbre),
 * C-01 (XML), C-07 (Gradle KTS et Groovy) et C-14 (langages P0) — en
 * plus des quatorze des jalons M2 et M3, dans l'ordre de la matrice de
 * parité, uniques et complets. Les facettes non livrées (C-15/C-16
 * longue traîne, D-14 ligatures) rejoignent leur tranche.
 */
final class ScenarioCatalogTest {

    /** Identifiant de la matrice de parité : lettre(s), tiret, nombre. */
    private static final Pattern IDENTIFIANT = Pattern.compile("[A-Z]+-\\d+");

    @Test
    void m4ScenariosAreAllPresentInParityOrder() {
        List<String> identifiants = ScenarioCatalog.scenarios().stream()
                .map(Scenario::identifiant).toList();
        assertEquals(List.of(
                "A-01", "A-13", "A-15", "A-18", "A-20", "A-24", "A-25",
                "B-01", "B-16", "C-01", "C-07", "C-14",
                "D-01", "D-13", "D-15", "D-16", "D-17",
                "G-01", "G-02", "G-05"), identifiants);
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
