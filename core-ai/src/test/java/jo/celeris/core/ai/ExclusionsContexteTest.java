package jo.celeris.core.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextSession;

/**
 * Tests des exclusions de contexte et du fichier d'instructions
 * d'agent (F-07, ADR-012) : un chemin exclu ne part jamais vers un
 * modèle — l'assertion se prend sur le journal du faux — et les
 * instructions du dépôt se servent par la voie des outils, jamais
 * inventées.
 */
class ExclusionsContexteTest {

    /**
     * F-07, le format du fichier : chemins exacts, dossiers, jokers —
     * commentaires et lignes vides ignorés.
     */
    @Test
    void leFichierDExclusionSeLit() {
        ExclusionsContexte exclusions = ExclusionsContexte.de("""
                # Les secrets ne partent jamais
                secrets.properties
                keystore/
                *.jks
                cle-*.pem

                # (fin)
                """);

        assertTrue(exclusions.estExclu("secrets.properties"),
                "le chemin exact se refuse");
        assertTrue(exclusions.estExclu("keystore/release.keystore"),
                "le dossier refuse tout son contenu");
        assertTrue(exclusions.estExclu("certs/debug.jks"),
                "le joker prend les chemins qui y collent");
        assertTrue(exclusions.estExclu("config/cle-prod.pem"),
                "l'asterisque traverse les barres obliques");
        assertTrue(!exclusions.estExclu("build.gradle.kts"),
                "les fichiers ordinaires passent");
        assertTrue(!exclusions.estExclu("secrets-doc.txt"),
                "le prefixe seul ne refuse pas : le dossier tient a sa barre");
    }

    /**
     * F-07, le dossier sans barre : un chemin exact refuse aussi tout
     * son contenu — «\u202fsecrets » couvre «\u202fsecrets/interne.txt ».
     */
    @Test
    void leDossierSansBarreCouvreSonContenu() {
        ExclusionsContexte exclusions = ExclusionsContexte.de("secrets\n");
        assertTrue(exclusions.estExclu("secrets"));
        assertTrue(exclusions.estExclu("secrets/interne.txt"));
        assertTrue(!ExclusionsContexte.aucune().estExclu("secrets.properties"),
                "sans regles, rien ne se refuse");
    }

    /**
     * F-07, le critère de la matrice : le chemin exclu n'est jamais
     * transmis — l'assertion se prend sur le journal du faux modèle,
     * fantôme et prédiction compris ; le chemin admis traverse.
     */
    @Test
    void cheminExcluJamaisTransmis() {
        FauxModeleLocal modele = new FauxModeleLocal();
        modele.fantome(new SuggestionFantome(" suggestion", 8, 0));
        GardeContexte garde = new GardeContexte(modele, modele,
                "secrets.properties", ExclusionsContexte.de("secrets.properties"));
        SessionTexteFantome session = new SessionTexteFantome(garde, Runnable::run);
        Document document = Document.of("cle = xx");

        session.demander(document, 8);
        assertTrue(modele.requetes().isEmpty(),
                "le chemin exclu ne part jamais : le journal du faux le prouve");
        assertNull(session.suggestionActive(),
                "aucune suggestion ne s'affiche : rien n'est parti");

        GardeContexte gardeAdmis = new GardeContexte(modele, modele,
                "build.gradle.kts", ExclusionsContexte.de("secrets.properties"));
        SessionTexteFantome sessionAdmise =
                new SessionTexteFantome(gardeAdmis, Runnable::run);
        sessionAdmise.demander(document, 8);
        assertEquals(1, modele.requetes().size(),
                "le chemin admis traverse le garde");
        assertNotNull(sessionAdmise.suggestionActive(),
                "la suggestion du chemin admis s'affiche");
        assertEquals(" suggestion", sessionAdmise.suggestionActive().texte());
    }

    /**
     * F-07 — les instructions d'agent du dépôt se servent par la voie
     * des outils, telles quelles ; le dépôt sans fichier répond
     * honnêtement qu'il n'y a rien.
     */
    @Test
    void instructionsServiesParLesOutils() {
        TextSession session = TextSession.of("fun principal() {}", System::nanoTime);
        InstructionsAgent declarees = InstructionsAgent.de(
                "Toujours passer par la transaction.\nJamais toucher aux secrets.");
        OutilsEditeur outils = new OutilsEditeurSurSession(session,
                document -> List.of(), List::of, declarees);

        assertEquals(declarees, outils.instructions());
        assertTrue(ConnecteurMcp.instructions(outils).contains("transaction"));
        assertTrue(ConnecteurMcp.instructions(outils).contains("\"absentes\":false"));

        OutilsEditeur sansFichier = new OutilsEditeurSurSession(session,
                document -> List.of(), List::of);
        assertTrue(sansFichier.instructions().estAbsente(),
                "le depot sans fichier n'invente pas de conventions");
        assertTrue(ConnecteurMcp.instructions(sansFichier)
                        .contains("\"absentes\":true"));
    }

    /**
     * F-06 suit F-07 — le catalogue compte l'outil d'instructions :
     * six outils exposés.
     */
    @Test
    void leCatalogueCompteLesInstructions() {
        assertEquals(6, ConnecteurMcp.catalogue().size());
        assertEquals("celeris.instructions",
                ConnecteurMcp.catalogue().get(4).nom());
    }
}
