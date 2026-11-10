package jo.celeris.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import jo.celeris.core.ai.ExclusionsContexte;
import jo.celeris.core.ai.FauxModeleLocal;
import jo.celeris.core.ai.GardeContexte;
import jo.celeris.core.ai.SessionTexteFantome;
import jo.celeris.core.ai.SuggestionFantome;
import jo.celeris.core.text.Document;

/**
 * Le test CI de la neutralité de la démonstration (F-10, ADR-012) :
 * aucune requête réseau en mode par défaut — la preuve structurelle
 * d'abord (le manifeste ne déclare pas la permission INTERNET : le
 * paquet ne peut pas ouvrir de prise), la preuve du garde ensuite (le
 * consentement coupé par défaut ne produit aucun départ, même vers le
 * modèle local ; les secrets de la démonstration restent au sol
 * pareil).
 */
class DemoNeutreTest {

    /**
     * F-10 — le manifeste de la démonstration ne déclare pas la
     * permission INTERNET : sans elle, le paquet ne peut ouvrir aucune
     * prise — la neutralité par défaut est structurelle, pas une
     * promesse de code.
     */
    @Test
    void leManifesteNeDeclareAucunePriseReseau() throws Exception {
        String manifeste = new String(Files.readAllBytes(
                Path.of("src", "main", "AndroidManifest.xml")),
                StandardCharsets.UTF_8);
        assertFalse(manifeste.contains("android.permission.INTERNET"),
                "la démonstration ne déclare pas la permission INTERNET");
        assertFalse(manifeste.contains("uses-permission"),
                "aucune permission du tout — le paquet reste neutre");
    }

    /**
     * Revue de sécurité M9 — la sauvegarde nuage est interdite : sans
     * {@code allowBackup="false"}, les préférences et les fichiers de
     * l'application partiraient au transport Android par défaut — la
     * démonstration n'y consent pas.
     */
    @Test
    void leManifesteInterditLaSauvegardeNuage() throws Exception {
        String manifeste = new String(Files.readAllBytes(
                Path.of("src", "main", "AndroidManifest.xml")),
                StandardCharsets.UTF_8);
        assertTrue(manifeste.contains("android:allowBackup=\"false\""),
                "la démonstration interdit la sauvegarde de ses données");
    }

    /**
     * F-10 — le consentement coupé par défaut ne produit aucun départ :
     * la demande reste au sol, le journal du faux modèle en témoigne —
     * même le modèle local n'est pas consulté sans l'activation
     * explicite.
     */
    @Test
    void sansConsentementAucunDepartMemeLocal() {
        FauxModeleLocal modele = new FauxModeleLocal();
        GardeContexte garde = new GardeContexte(modele, modele,
                "/App.kt", ExclusionsContexte.aucune(), () -> false);
        SessionTexteFantome session = new SessionTexteFantome(garde, Runnable::run);
        Document document = Document.of("fun principal() {");

        session.demander(document, 17);
        assertTrue(modele.requetes().isEmpty(),
                "sans consentement, la requête ne part pas — journal vide");

        GardeContexte consentant = new GardeContexte(modele, modele,
                "/App.kt", ExclusionsContexte.aucune(), () -> true);
        SessionTexteFantome sessionConsentante =
                new SessionTexteFantome(consentant, Runnable::run);
        sessionConsentante.demander(document, 17);
        assertEquals(1, modele.requetes().size(),
                "l'activation explicite ouvre la voie");
    }

    /**
     * F-07 — le scénario de la démonstration respecte ses exclusions :
     * le fichier d'exclusions embarqué refuse les secrets, la
     * complétion sur un fichier exclu ne part jamais — journal vide,
     * même consentant.
     */
    @Test
    void leScenarioRespecteLesExclusionsDeLaDemonstration() {
        ExclusionsContexte exclusions =
                ExclusionsContexte.de(AtelierIA.EXCLUSIONS);
        assertTrue(exclusions.estExclu("/secrets/cles.api"),
                "le dossier des secrets ne part jamais");
        assertTrue(exclusions.estExclu("certs/release.jks"),
                "les magasins de clés ne partent jamais");

        FauxModeleLocal modele = new FauxModeleLocal();
        modele.fantome(new SuggestionFantome("furtif()", 0, 0));
        GardeContexte garde = new GardeContexte(modele, modele,
                "/secrets/cles.api", exclusions, () -> true);
        SessionTexteFantome session = new SessionTexteFantome(garde, Runnable::run);
        Document document = Document.of("cle = ");

        session.demander(document, 6);
        assertTrue(modele.requetes().isEmpty(),
                "le chemin exclu ne part jamais — le journal du faux le prouve");
    }
}
