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
class ContextExclusionsTest {

    /**
     * F-07, le format du fichier : chemins exacts, dossiers, jokers —
     * commentaires et lignes vides ignorés.
     */
    @Test
    void leFichierDExclusionSeLit() {
        ContextExclusions exclusions = ContextExclusions.of("""
                # Les secrets ne partent jamais
                secrets.properties
                keystore/
                *.jks
                cle-*.pem

                # (fin)
                """);

        assertTrue(exclusions.isExcluded("secrets.properties"),
                "le chemin exact se refuse");
        assertTrue(exclusions.isExcluded("keystore/release.keystore"),
                "le dossier refuse tout son contenu");
        assertTrue(exclusions.isExcluded("certs/debug.jks"),
                "le joker prend les chemins qui y collent");
        assertTrue(exclusions.isExcluded("config/cle-prod.pem"),
                "l'asterisque traverse les barres obliques");
        assertTrue(!exclusions.isExcluded("build.gradle.kts"),
                "les fichiers ordinaires passent");
        assertTrue(!exclusions.isExcluded("secrets-doc.txt"),
                "le prefixe seul ne refuse pas : le dossier tient a sa barre");
    }

    /**
     * F-07, le dossier sans barre : un chemin exact refuse aussi tout
     * son contenu — «\u202fsecrets » couvre «\u202fsecrets/interne.txt ».
     */
    @Test
    void leDossierSansBarreCouvreSonContenu() {
        ContextExclusions exclusions = ContextExclusions.of("secrets\n");
        assertTrue(exclusions.isExcluded("secrets"));
        assertTrue(exclusions.isExcluded("secrets/interne.txt"));
        assertTrue(!ContextExclusions.aucune().isExcluded("secrets.properties"),
                "sans regles, rien ne se refuse");
    }

    /**
     * F-07, le critère de la matrice : le chemin exclu n'est jamais
     * transmis — l'assertion se prend sur le journal du faux modèle,
     * fantôme et prédiction compris ; le chemin admis traverse.
     */
    @Test
    void cheminExcluJamaisTransmis() {
        FakeLocalModel modele = new FakeLocalModel();
        modele.ghost(new GhostSuggestion(" suggestion", 8, 0));
        ContextGuard guard = new ContextGuard(modele, modele,
                "secrets.properties", ContextExclusions.of("secrets.properties"));
        GhostTextSession session = new GhostTextSession(guard, Runnable::run);
        Document document = Document.of("cle = xx");

        session.demander(document, 8);
        assertTrue(modele.requetes().isEmpty(),
                "le chemin exclu ne part jamais : le journal du faux le prouve");
        assertNull(session.suggestionActive(),
                "aucune suggestion ne s'affiche : rien n'est parti");

        ContextGuard gardeAdmis = new ContextGuard(modele, modele,
                "build.gradle.kts", ContextExclusions.of("secrets.properties"));
        GhostTextSession sessionAdmise =
                new GhostTextSession(gardeAdmis, Runnable::run);
        sessionAdmise.demander(document, 8);
        assertEquals(1, modele.requetes().size(),
                "le chemin admis traverse le garde");
        assertNotNull(sessionAdmise.suggestionActive(),
                "la suggestion du chemin admis s'affiche");
        assertEquals(" suggestion", sessionAdmise.suggestionActive().text());
    }

    /**
     * F-07 — les instructions d'agent du dépôt se servent par la voie
     * des outils, telles quelles ; le dépôt sans fichier répond
     * honnêtement qu'il n'y a rien.
     */
    @Test
    void instructionsServiesParLesOutils() {
        TextSession session = TextSession.of("fun principal() {}", System::nanoTime);
        AgentInstructions declarees = AgentInstructions.of(
                "Toujours passer par la transaction.\nJamais toucher aux secrets.");
        EditorTools outils = new PerSessionTools(session,
                document -> List.of(), List::of, declarees);

        assertEquals(declarees, outils.instructions());
        assertTrue(McpConnector.instructions(outils).contains("transaction"));
        assertTrue(McpConnector.instructions(outils).contains("\"absentes\":false"));

        EditorTools sansFichier = new PerSessionTools(session,
                document -> List.of(), List::of);
        assertTrue(sansFichier.instructions().isAbsent(),
                "le depot sans fichier n'invente pas de conventions");
        assertTrue(McpConnector.instructions(sansFichier)
                        .contains("\"absentes\":true"));
    }

    /**
     * F-06 suit F-07 — le catalogue compte l'outil d'instructions :
     * six outils exposés.
     */
    @Test
    void leCatalogueCompteLesInstructions() {
        assertEquals(6, McpConnector.catalogue().size());
        assertEquals("celeris.instructions",
                McpConnector.catalogue().get(4).nom());
    }
}
