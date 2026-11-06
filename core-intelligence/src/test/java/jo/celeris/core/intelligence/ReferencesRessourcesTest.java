package jo.celeris.core.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * Tests de la navigation des références de ressources (C-04) : aller à
 * la ressource définie — {@code @string/x} vers le {@code <string
 * name="x">} du fichier de valeurs, {@code @layout/x} vers le fichier
 * de mise en page — et aller à l'usage — chaque occurrence de la
 * référence dans le projet.
 */
final class ReferencesRessourcesTest {

    private static final String STRINGS =
            "<resources>\n  <string name=\"app_name\">Celeris</string>\n"
                    + "  <string name=\"accueil\">Bienvenue</string>\n</resources>";

    private static final String PRINCIPAL =
            "<LinearLayout>\n  <TextView android:text=\"@string/app_name\"/>\n"
                    + "  <Button android:text=\"@string/accueil\"/>\n</LinearLayout>";

    private static final String SECOND =
            "<TextView android:text=\"@string/app_name\"/>";

    private static final Map<String, String> PROJET = Map.of(
            "res/values/strings.xml", STRINGS,
            "res/layout/main.xml", PRINCIPAL,
            "res/layout/second.xml", SECOND);

    /** La définition d'une ressource de valeurs se trouve à sa balise. */
    @Test
    void definitionRessourceDeValeurs() {
        Optional<ReferencesRessources.Lieu> lieu = ReferencesRessources.definition(
                PROJET, "string", "app_name");
        assertTrue(lieu.isPresent(), "la ressource définie se trouve");
        assertEquals("res/values/strings.xml", lieu.get().chemin());
        assertEquals(STRINGS.indexOf("<string name=\"app_name\""), lieu.get().offset(),
                "l'offset pointe la balise de définition");
    }

    /** La définition d'une mise en page est son fichier propre. */
    @Test
    void definitionMiseEnPage() {
        Optional<ReferencesRessources.Lieu> lieu = ReferencesRessources.definition(
                PROJET, "layout", "main");
        assertTrue(lieu.isPresent());
        assertEquals("res/layout/main.xml", lieu.get().chemin());
        assertEquals(PRINCIPAL.indexOf('<'), lieu.get().offset(),
                "l'offset pointe la première balise du fichier");
    }

    /** La ressource inconnue n'a pas de définition. */
    @Test
    void ressourceInconnueSansDefinition() {
        assertTrue(ReferencesRessources.definition(PROJET, "string", "absente").isEmpty());
    }

    /** Les usages couvrent le projet entier, aux offsets exacts. */
    @Test
    void usagesCouvrentLeProjetEntier() {
        List<ReferencesRessources.Lieu> usages = ReferencesRessources.usages(
                PROJET, "string", "app_name");
        assertEquals(2, usages.size(), "deux usages dans le projet");
        assertEquals(PRINCIPAL.indexOf("@string/app_name"),
                usages.stream().filter(u -> u.chemin().endsWith("main.xml"))
                        .findFirst().orElseThrow().offset(),
                "l'usage du fichier principal est à l'offset exact");
        assertEquals(SECOND.indexOf("@string/app_name"),
                usages.stream().filter(u -> u.chemin().endsWith("second.xml"))
                        .findFirst().orElseThrow().offset(),
                "l'usage du second fichier est à l'offset exact");
    }

    /** La ressource sans usage rend une liste vide, jamais null. */
    @Test
    void ressourceSansUsageRendListeVide() {
        assertTrue(ReferencesRessources.usages(
                Map.of("res/values/strings.xml",
                        "<string name=\"seule\">x</string>"),
                "string", "seule").isEmpty());
    }

    /** La référence suivie d'un identifiant ne compte pas comme usage. */
    @Test
    void referenceProlongeeNeComptePas() {
        List<ReferencesRessources.Lieu> usages = ReferencesRessources.usages(
                Map.of("res/layout/x.xml", "<TextView android:text=\"@string/app_name2\"/>"),
                "string", "app_name");
        assertTrue(usages.isEmpty(), "@string/app_name2 n'est pas un usage de app_name");
    }
}
