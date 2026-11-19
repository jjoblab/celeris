package jo.celeris.core.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.jspecify.annotations.Nullable;

import jo.celeris.core.syntax.TokenCancellation;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextEdit;

/**
 * Les faux modèles de référence (F-08, ADR-012) : le local livre
 * synchronement, le distant ne livre que par son exécuteur — le même
 * contrat pour l'un et l'autre, branchés par l'hôte.
 *
 * <p>Le journal des requêtes se lit pareil des deux côtés : le jour des
 * exclusions de contexte (F-07), c'est lui qui prouvera qu'un chemin
 * exclu ne part jamais.</p>
 */
class FauxModelesTest {

    /** Le récepteur de test : ce qui est arrivé, et sur quel fil. */
    private static final class Enregistreur implements GhostTextModel.Receiver {
        private @Nullable GhostSuggestion recue;
        private boolean aucune;
        private @Nullable Thread filLivraison;

        @Override
        public void suggestion(GhostSuggestion suggestion) {
            recue = suggestion;
            filLivraison = Thread.currentThread();
        }

        @Override
        public void aucune() {
            aucune = true;
        }
    }

    /** Le récepteur de prédiction de test. */
    private static final class EnregistreurPrediction implements PredictionModel.Receiver {
        private @Nullable PredictionEdit recue;
        private boolean aucune;

        @Override
        public void prediction(PredictionEdit prediction) {
            recue = prediction;
        }

        @Override
        public void aucune() {
            aucune = true;
        }
    }

    /** Le local livre synchronement, sur le fil de l'appelant. */
    @Test
    void localLivresseSynchroneSurLeFilAppelant() {
        FakeLocalModel modele = new FakeLocalModel();
        modele.ghost(new GhostSuggestion("42;", 8, 0));
        Enregistreur receiver = new Enregistreur();
        modele.completer(new ModelRequest(Document.of("int x = "), 8,
                new TokenCancellation()), receiver);
        assertEquals(new GhostSuggestion("42;", 8, 0), receiver.recue,
                "la livraison locale est immédiate");
        assertEquals(Thread.currentThread(), receiver.filLivraison,
                "le modèle local ne change pas de fil");
    }

    /** Le local sans avis rend l'aveu, pas de suggestion. */
    @Test
    void localSansAvisRendLeaveu() {
        FakeLocalModel modele = new FakeLocalModel();
        Enregistreur receiver = new Enregistreur();
        modele.completer(new ModelRequest(Document.of("x"), 0,
                new TokenCancellation()), receiver);
        assertNull(receiver.recue);
        assertTrue(receiver.aucune);
    }

    /** La requête déjà annulée ne livre rien — annuler coûte moins cher. */
    @Test
    void requeteAnnuleeNeLivreRien() {
        FakeLocalModel modele = new FakeLocalModel();
        modele.ghost(new GhostSuggestion("42;", 8, 0));
        TokenCancellation mort = new TokenCancellation();
        mort.annuler();
        Enregistreur receiver = new Enregistreur();
        modele.completer(new ModelRequest(Document.of("int x = "), 8, mort),
                receiver);
        assertNull(receiver.recue, "un vol mort ne livre pas");
        assertFalse(receiver.aucune, "un vol mort ne dit même pas son aveu");
        assertEquals(1, modele.requetes().size(), "la requête morte est journalisée");
    }

    /** Le local journalise chaque requête, fantôme et prédiction. */
    @Test
    void localJournaliseChaqueRequete() {
        FakeLocalModel modele = new FakeLocalModel();
        Document document = Document.of("int x = ");
        modele.completer(new ModelRequest(document, 8, new TokenCancellation()),
                new Enregistreur());
        modele.predire(new ModelRequest(document, 4, new TokenCancellation()),
                new EnregistreurPrediction());
        assertEquals(2, modele.requetes().size());
        assertEquals(document, modele.requetes().get(0).document());
        assertEquals(8, modele.requetes().get(0).offset());
        assertEquals(4, modele.requetes().get(1).offset());
    }

    /** Le distant ne livre que par son exécuteur — jamais avant. */
    @Test
    void distantNeLivreQueParSonExecuteur() {
        PompeManuelle reseau = new PompeManuelle();
        FakeRemoteModel modele = new FakeRemoteModel(reseau);
        modele.ghost(new GhostSuggestion("42;", 8, 0));
        Enregistreur receiver = new Enregistreur();
        modele.completer(new ModelRequest(Document.of("int x = "), 8,
                new TokenCancellation()), receiver);
        assertNull(receiver.recue, "rien ne livre avant le réseau");
        assertEquals(1, reseau.enAttente(), "la livraison attend dans le réseau");
        reseau.executerUne();
        assertEquals(new GhostSuggestion("42;", 8, 0), receiver.recue,
                "le réseau livre");
    }

    /** Le vol mort en route ne livre pas, même parti. */
    @Test
    void distantVolMortEnRouteNeLivrePas() {
        PompeManuelle reseau = new PompeManuelle();
        FakeRemoteModel modele = new FakeRemoteModel(reseau);
        modele.ghost(new GhostSuggestion("42;", 8, 0));
        Enregistreur receiver = new Enregistreur();
        ModelRequest requete = new ModelRequest(Document.of("int x = "), 8,
                new TokenCancellation());
        modele.completer(requete, receiver);
        requete.token().annuler();
        reseau.executerTout();
        assertNull(receiver.recue, "le jeton relu au moment de livrer");
        assertFalse(receiver.aucune);
    }

    /** La requête déjà annulée ne part même pas sur le réseau. */
    @Test
    void distantRequeteAnnuleeNePartPas() {
        PompeManuelle reseau = new PompeManuelle();
        FakeRemoteModel modele = new FakeRemoteModel(reseau);
        TokenCancellation mort = new TokenCancellation();
        mort.annuler();
        modele.completer(new ModelRequest(Document.of("x"), 0, mort),
                new Enregistreur());
        assertEquals(0, reseau.enAttente(), "rien ne voyage pour un vol mort");
    }

    /**
     * F-08 — le même contrat pour le local et le distant : l'avis
     * servi, l'aveu sans avis, le journal — l'un livre sur place,
     * l'autre par son réseau, l'observable est le même.
     */
    @Test
    void memeContratPourLeLocalEtLeDistant() {
        GhostSuggestion avis = new GhostSuggestion("42;", 8, 0);
        Document document = Document.of("int x = ");

        FakeLocalModel local = new FakeLocalModel();
        local.ghost(avis);
        Enregistreur recepteurLocal = new Enregistreur();
        local.completer(new ModelRequest(document, 8, new TokenCancellation()),
                recepteurLocal);

        PompeManuelle reseau = new PompeManuelle();
        FakeRemoteModel distant = new FakeRemoteModel(reseau);
        distant.ghost(avis);
        Enregistreur recepteurDistant = new Enregistreur();
        distant.completer(new ModelRequest(document, 8, new TokenCancellation()),
                recepteurDistant);
        reseau.executerTout();

        assertEquals(recepteurLocal.recue, recepteurDistant.recue,
                "le même avis servi, local ou distant");
        assertEquals(recepteurLocal.aucune, recepteurDistant.aucune);
        assertEquals(local.requetes().size(), distant.requetes().size());
        assertEquals(local.requetes().get(0).document(),
                distant.requetes().get(0).document());
        assertEquals(local.requetes().get(0).offset(),
                distant.requetes().get(0).offset());

        local.ghost(null);
        distant.ghost(null);
        Enregistreur aveuLocal = new Enregistreur();
        local.completer(new ModelRequest(document, 8, new TokenCancellation()),
                aveuLocal);
        Enregistreur aveuDistant = new Enregistreur();
        distant.completer(new ModelRequest(document, 8, new TokenCancellation()),
                aveuDistant);
        reseau.executerTout();
        assertEquals(aveuLocal.aucune, aveuDistant.aucune, "le même aveu sans avis");
    }

    /** La prédiction suit le même contrat que le fantôme. */
    @Test
    void memeContratPourLaPrediction() {
        PredictionEdit avis = new PredictionEdit("initialise",
                List.of(new TextEdit(8, "0", "42")), 0);
        Document document = Document.of("int x = 0;");

        FakeLocalModel local = new FakeLocalModel();
        local.prediction(avis);
        EnregistreurPrediction recepteurLocal = new EnregistreurPrediction();
        local.predire(new ModelRequest(document, 8, new TokenCancellation()),
                recepteurLocal);

        PompeManuelle reseau = new PompeManuelle();
        FakeRemoteModel distant = new FakeRemoteModel(reseau);
        distant.prediction(avis);
        EnregistreurPrediction recepteurDistant = new EnregistreurPrediction();
        distant.predire(new ModelRequest(document, 8, new TokenCancellation()),
                recepteurDistant);
        reseau.executerTout();

        assertEquals(recepteurLocal.recue, recepteurDistant.recue,
                "la même prédiction servie, locale ou distante");
        assertFalse(recepteurLocal.aucune, "un avis était posé, pas d'aveu");
        assertFalse(recepteurDistant.aucune);
    }

    /**
     * Le réseau du faux distant peut livrer depuis un autre fil : la
     * livraison arrive pareille, le contrat de fil est l'affaire de la
     * session qui écoute, pas du modèle qui répond.
     */
    @Test
    void distantLivreeDepuisUnAutreFil() {
        AtomicInteger livraisons = new AtomicInteger();
        Executor autreFil = tache -> {
            Thread thread = new Thread(tache);
            thread.start();
            try {
                thread.join(5_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            livraisons.incrementAndGet();
        };
        FakeRemoteModel modele = new FakeRemoteModel(autreFil);
        modele.ghost(new GhostSuggestion("42;", 8, 0));
        Enregistreur receiver = new Enregistreur();
        modele.completer(new ModelRequest(Document.of("int x = "), 8,
                new TokenCancellation()), receiver);
        assertEquals(new GhostSuggestion("42;", 8, 0), receiver.recue,
                "la livraison d'un autre fil arrive pareil");
        assertNotEquals(Thread.currentThread(), receiver.filLivraison,
                "la livraison a bien voyagé");
        assertEquals(1, livraisons.get());
    }
}
