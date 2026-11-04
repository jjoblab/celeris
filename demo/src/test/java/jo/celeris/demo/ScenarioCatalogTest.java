package jo.celeris.demo;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Test de fumée du catalogue de scénarios : le jalon M3 exige les
 * scénarios des facettes livrées — A-01 (pilule multi-curseur), A-18
 * (retour à la ligne, modèle et rangées), D-01 (gouttière à numéros),
 * D-13 (thèmes à contraste vérifié), D-15 (zoom au pincement), D-16
 * (poignées, pilule, loupe), D-17 (clavier matériel) et G-01, G-02,
 * G-05 — en plus des cinq du jalon M2, dans l'ordre de la matrice de
 * parité, uniques et complets. Les facettes non livrées (D-02
 * diagnostics, D-07 flis, D-14 ligatures) rejoignent leur jalon.
 */
final class ScenarioCatalogTest {

    /** Identifiant de la matrice de parité : lettre(s), tiret, nombre. */
    private static final Pattern IDENTIFIANT = Pattern.compile("[A-Z]+-\\d+");

    @Test
    void m3ScenariosAreAllPresentInParityOrder() {
        List<String> identifiants = ScenarioCatalog.scenarios().stream()
                .map(Scenario::identifiant).toList();
        assertEquals(List.of(
                "A-01", "A-13", "A-18", "A-20", "A-24", "A-25",
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
