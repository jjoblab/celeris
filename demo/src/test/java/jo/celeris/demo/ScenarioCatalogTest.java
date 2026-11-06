package jo.celeris.demo;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Test de fumée du catalogue de scénarios : le jalon M7 y ajoute les
 * contrats d'intégration de l'hôte — E-01 à E-06 (système de fichiers
 * virtuel, VCS, débogueur, exécution, problèmes de build, traces
 * d'appel) — en plus des cinquante-neuf des jalons M2 à M6 (dont la
 * sémantique M6 : B-02 à B-05, B-06 à B-11, B-13, B-17, B-19, B-24 à
 * B-27), dans l'ordre de la matrice de parité, uniques et complets.
 * Les facettes non livrées (D-14 ligatures) rejoignent leur jalon.
 */
final class ScenarioCatalogTest {

    /** Identifiant de la matrice de parité : lettre(s), tiret, nombre. */
    private static final Pattern IDENTIFIANT = Pattern.compile("[A-Z]+-\\d+");

    @Test
    void m6ScenariosAreAllPresentInParityOrder() {
        List<String> identifiants = ScenarioCatalog.scenarios().stream()
                .map(Scenario::identifiant).toList();
        assertEquals(List.of(
                "A-01", "A-02", "A-03", "A-05", "A-06", "A-07", "A-08",
                "A-09", "A-10", "A-11", "A-12", "A-13", "A-15", "A-16",
                "A-18", "A-19", "A-20", "A-22", "A-24", "A-25",
                "B-01", "B-02", "B-03", "B-04", "B-05", "B-06", "B-07",
                "B-08", "B-09", "B-10", "B-11", "B-13", "B-14", "B-15",
                "B-16", "B-17", "B-19", "B-24", "B-25", "B-26", "B-27",
                "C-01", "C-07", "C-14", "C-15", "C-16",
                "D-01", "D-06", "D-08", "D-09", "D-10", "D-13", "D-15",
                "D-16", "D-17", "D-18",
                "E-01", "E-02", "E-03", "E-04", "E-05", "E-06",
                "G-01", "G-02", "G-04", "G-05"), identifiants);
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
