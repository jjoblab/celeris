package jo.celeris.core.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests de la complétion du manifeste (C-06) : les attributs connus
 * de chaque balise du manifeste — manifeste, application, activité,
 * service, récepteur, permission — et les permissions suggérées par
 * leur préfixe.
 */
final class CompletionsManifesteTest {

    /** Les attributs de la balise application répondent au préfixe. */
    @Test
    void attributsDeApplicationRepondentAuPrefixe() {
        List<String> candidats = CompletionsManifeste.attributs("application", "andro");
        assertFalse(candidats.isEmpty());
        for (String candidat : candidats) {
            assertTrue(candidat.startsWith("andro"), "chaque candidat porte le préfixe : "
                    + candidat);
        }
        assertTrue(candidats.contains("android:label"), "le libellé est connu : " + candidats);
        assertTrue(candidats.contains("android:theme"), "le thème est connu : " + candidats);
    }

    /** Les attributs de la balise manifeste incluent le paquet et les versions. */
    @Test
    void attributsDeManifesteIncluentPaquetEtVersions() {
        List<String> candidats = CompletionsManifeste.attributs("manifest", "");
        assertTrue(candidats.contains("package"), "le paquet est connu : " + candidats);
        assertTrue(candidats.contains("android:versionCode"),
                "le code de version est connu : " + candidats);
    }

    /** Les attributs de l'activité incluent l'export et la configuration. */
    @Test
    void attributsDeActiviteIncluentExport() {
        List<String> candidats = CompletionsManifeste.attributs("activity", "");
        assertTrue(candidats.contains("android:exported"), "l'export est connu : " + candidats);
        assertTrue(candidats.contains("android:name"), "le nom est connu : " + candidats);
    }

    /** La balise inconnue retombe sur les attributs communs. */
    @Test
    void baliseInconnueRetombeSurLesCommuns() {
        List<String> candidats = CompletionsManifeste.attributs("inconnue", "android:");
        assertFalse(candidats.isEmpty(), "les attributs communs répondent toujours");
        assertTrue(candidats.contains("android:name"));
    }

    /** Les permissions se suggèrent par préfixe, cas respecté. */
    @Test
    void permissionsSeSuggerentParPrefixe() {
        List<String> toutes = CompletionsManifeste.permissions("");
        assertTrue(toutes.contains("android.permission.INTERNET"),
                "INTERNET est suggéré : " + toutes);
        List<String> camera = CompletionsManifeste.permissions("android.permission.CAMERA");
        assertEquals(List.of("android.permission.CAMERA"), camera,
                "le préfixe filtre exactement");
    }

    /** Les préfixes vides rendent la liste entière, jamais null. */
    @Test
    void prefixeVideRendLaListeEntiere() {
        assertTrue(CompletionsManifeste.permissions("").size() > 1,
                "plusieurs permissions connues");
        assertTrue(CompletionsManifeste.attributs("service", "").size() > 1,
                "plusieurs attributs du service connus");
    }
}
