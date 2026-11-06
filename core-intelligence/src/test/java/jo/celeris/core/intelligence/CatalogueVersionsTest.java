package jo.celeris.core.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests des catalogues de versions (C-08) : la lecture d'un
 * {@code libs.versions.toml} rend ses sections — versions,
 * bibliothèques, extensions, greffons — et les alias suggérés dans
 * les fichiers de build suivent l'accès Gradle : les tirets deviennent
 * des points, {@code bundles} ouvre son espace de noms.
 */
final class CatalogueVersionsTest {

    private static final String TOML = """
            [versions]
            kotlin = "2.1.0"
            androidx = "1.13.0"

            [libraries]
            androidx-core = { module = "androidx.core:core", version.ref = "androidx" }
            reseau-moshi = { module = "exemple:moshi", version = "1" }

            [bundles]
            reseau = ["reseau-moshi"]

            [plugins]
            application-android = { id = "exemple.application", version.ref = "androidx" }

            # un commentaire ignoré
            """;

    /** La lecture rend les alias de chaque section. */
    @Test
    void lectureRendLesAliasDeChaqueSection() {
        CatalogueVersions catalogue = CatalogueVersions.lire(TOML);
        assertTrue(catalogue.bibliotheques().contains("androidx-core"),
                "les bibliothèques sont lues");
        assertTrue(catalogue.bibliotheques().contains("reseau-moshi"),
                "les bibliothèques à version en dur sont lues");
        assertTrue(catalogue.versions().contains("kotlin"), "les versions sont lues");
        assertTrue(catalogue.greffons().contains("application-android"),
                "les greffons sont lues");
        assertTrue(catalogue.extensions().contains("reseau"),
                "les bundles (extensions) sont lus");
    }

    /** Les alias suggérés suivent l'accès Gradle : tirets en points. */
    @Test
    void aliasSuggeresSuiventLAccesGradle() {
        CatalogueVersions catalogue = CatalogueVersions.lire(TOML);
        List<String> candidats = CatalogueVersions.completerAlias(catalogue, "libs.");
        assertTrue(candidats.contains("libs.androidx.core"),
                "les tirets deviennent des points : " + candidats);
        assertTrue(candidats.contains("libs.reseau.moshi"),
                "chaque bibliothèque est suggérée : " + candidats);
        for (String candidat : candidats) {
            assertTrue(candidat.startsWith("libs."), "chaque candidat garde le préfixe : "
                    + candidat);
        }
    }

    /** Les bundles ouvrent leur espace de noms propre. */
    @Test
    void bundlesOuvrentLeurEspaceDeNoms() {
        CatalogueVersions catalogue = CatalogueVersions.lire(TOML);
        List<String> candidats = CatalogueVersions.completerAlias(catalogue,
                "libs.bundles.");
        assertEquals(List.of("libs.bundles.reseau"), candidats,
                "seuls les bundles vivent sous bundles");
    }

    /** Le préfixe inexistant ne suggère rien, jamais null. */
    @Test
    void prefixeInexistantNeSuggerereRien() {
        CatalogueVersions catalogue = CatalogueVersions.lire(TOML);
        assertTrue(CatalogueVersions.completerAlias(catalogue, "libs.aucun.").isEmpty());
        assertTrue(CatalogueVersions.completerAlias(catalogue, "autre.").isEmpty());
    }

    /** Le catalogue vide reste un catalogue valide. */
    @Test
    void catalogueVideResteValide() {
        CatalogueVersions catalogue = CatalogueVersions.lire("");
        assertTrue(catalogue.versions().isEmpty());
        assertTrue(CatalogueVersions.completerAlias(catalogue, "libs.").isEmpty());
    }

    /** Ligne sans égal ou en dehors d'une section : ignorée. */
    @Test
    void lignesHorsSectionsIgnorees() {
        CatalogueVersions catalogue = CatalogueVersions.lire("version = \"1\"\n[libraries]\n"
                + "androidx-core = { module = \"a:b\" }\nbibre sans rien");
        assertEquals(1, catalogue.bibliotheques().size(), "seule la section libraries lit");
        assertTrue(catalogue.versions().isEmpty(), "hors section, rien n'est lu");
    }

}
