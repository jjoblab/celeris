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

import jo.celeris.core.syntax.JetonAnnulation;
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
    private static final class Enregistreur implements ModeleTexteFantome.Recepteur {
        private @Nullable SuggestionFantome recue;
        private boolean aucune;
        private @Nullable Thread filLivraison;

        @Override
        public void suggestion(SuggestionFantome suggestion) {
            recue = suggestion;
            filLivraison = Thread.currentThread();
        }

        @Override
        public void aucune() {
            aucune = true;
        }
    }

    /** Le récepteur de prédiction de test. */
    private static final class EnregistreurPrediction implements ModelePrediction.Recepteur {
        private @Nullable PredictionEdition recue;
        private boolean aucune;

        @Override
        public void prediction(PredictionEdition prediction) {
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
        FauxModeleLocal modele = new FauxModeleLocal();
        modele.fantome(new SuggestionFantome("42;", 8, 0));
        Enregistreur recepteur = new Enregistreur();
        modele.completer(new RequeteModele(Document.of("int x = "), 8,
                new JetonAnnulation()), recepteur);
        assertEquals(new SuggestionFantome("42;", 8, 0), recepteur.recue,
                "la livraison locale est immédiate");
        assertEquals(Thread.currentThread(), recepteur.filLivraison,
                "le modèle local ne change pas de fil");
    }

    /** Le local sans avis rend l'aveu, pas de suggestion. */
    @Test
    void localSansAvisRendLeaveu() {
        FauxModeleLocal modele = new FauxModeleLocal();
        Enregistreur recepteur = new Enregistreur();
        modele.completer(new RequeteModele(Document.of("x"), 0,
                new JetonAnnulation()), recepteur);
        assertNull(recepteur.recue);
        assertTrue(recepteur.aucune);
    }

    /** La requête déjà annulée ne livre rien — annuler coûte moins cher. */
    @Test
    void requeteAnnuleeNeLivreRien() {
        FauxModeleLocal modele = new FauxModeleLocal();
        modele.fantome(new SuggestionFantome("42;", 8, 0));
        JetonAnnulation mort = new JetonAnnulation();
        mort.annuler();
        Enregistreur recepteur = new Enregistreur();
        modele.completer(new RequeteModele(Document.of("int x = "), 8, mort),
                recepteur);
        assertNull(recepteur.recue, "un vol mort ne livre pas");
        assertFalse(recepteur.aucune, "un vol mort ne dit même pas son aveu");
        assertEquals(1, modele.requetes().size(), "la requête morte est journalisée");
    }

    /** Le local journalise chaque requête, fantôme et prédiction. */
    @Test
    void localJournaliseChaqueRequete() {
        FauxModeleLocal modele = new FauxModeleLocal();
        Document document = Document.of("int x = ");
        modele.completer(new RequeteModele(document, 8, new JetonAnnulation()),
                new Enregistreur());
        modele.predire(new RequeteModele(document, 4, new JetonAnnulation()),
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
        FauxModeleDistant modele = new FauxModeleDistant(reseau);
        modele.fantome(new SuggestionFantome("42;", 8, 0));
        Enregistreur recepteur = new Enregistreur();
        modele.completer(new RequeteModele(Document.of("int x = "), 8,
                new JetonAnnulation()), recepteur);
        assertNull(recepteur.recue, "rien ne livre avant le réseau");
        assertEquals(1, reseau.enAttente(), "la livraison attend dans le réseau");
        reseau.executerUne();
        assertEquals(new SuggestionFantome("42;", 8, 0), recepteur.recue,
                "le réseau livre");
    }

    /** Le vol mort en route ne livre pas, même parti. */
    @Test
    void distantVolMortEnRouteNeLivrePas() {
        PompeManuelle reseau = new PompeManuelle();
        FauxModeleDistant modele = new FauxModeleDistant(reseau);
        modele.fantome(new SuggestionFantome("42;", 8, 0));
        Enregistreur recepteur = new Enregistreur();
        RequeteModele requete = new RequeteModele(Document.of("int x = "), 8,
                new JetonAnnulation());
        modele.completer(requete, recepteur);
        requete.jeton().annuler();
        reseau.executerTout();
        assertNull(recepteur.recue, "le jeton relu au moment de livrer");
        assertFalse(recepteur.aucune);
    }

    /** La requête déjà annulée ne part même pas sur le réseau. */
    @Test
    void distantRequeteAnnuleeNePartPas() {
        PompeManuelle reseau = new PompeManuelle();
        FauxModeleDistant modele = new FauxModeleDistant(reseau);
        JetonAnnulation mort = new JetonAnnulation();
        mort.annuler();
        modele.completer(new RequeteModele(Document.of("x"), 0, mort),
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
        SuggestionFantome avis = new SuggestionFantome("42;", 8, 0);
        Document document = Document.of("int x = ");

        FauxModeleLocal local = new FauxModeleLocal();
        local.fantome(avis);
        Enregistreur recepteurLocal = new Enregistreur();
        local.completer(new RequeteModele(document, 8, new JetonAnnulation()),
                recepteurLocal);

        PompeManuelle reseau = new PompeManuelle();
        FauxModeleDistant distant = new FauxModeleDistant(reseau);
        distant.fantome(avis);
        Enregistreur recepteurDistant = new Enregistreur();
        distant.completer(new RequeteModele(document, 8, new JetonAnnulation()),
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

        local.fantome(null);
        distant.fantome(null);
        Enregistreur aveuLocal = new Enregistreur();
        local.completer(new RequeteModele(document, 8, new JetonAnnulation()),
                aveuLocal);
        Enregistreur aveuDistant = new Enregistreur();
        distant.completer(new RequeteModele(document, 8, new JetonAnnulation()),
                aveuDistant);
        reseau.executerTout();
        assertEquals(aveuLocal.aucune, aveuDistant.aucune, "le même aveu sans avis");
    }

    /** La prédiction suit le même contrat que le fantôme. */
    @Test
    void memeContratPourLaPrediction() {
        PredictionEdition avis = new PredictionEdition("initialise",
                List.of(new TextEdit(8, "0", "42")), 0);
        Document document = Document.of("int x = 0;");

        FauxModeleLocal local = new FauxModeleLocal();
        local.prediction(avis);
        EnregistreurPrediction recepteurLocal = new EnregistreurPrediction();
        local.predire(new RequeteModele(document, 8, new JetonAnnulation()),
                recepteurLocal);

        PompeManuelle reseau = new PompeManuelle();
        FauxModeleDistant distant = new FauxModeleDistant(reseau);
        distant.prediction(avis);
        EnregistreurPrediction recepteurDistant = new EnregistreurPrediction();
        distant.predire(new RequeteModele(document, 8, new JetonAnnulation()),
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
            Thread fil = new Thread(tache);
            fil.start();
            try {
                fil.join(5_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            livraisons.incrementAndGet();
        };
        FauxModeleDistant modele = new FauxModeleDistant(autreFil);
        modele.fantome(new SuggestionFantome("42;", 8, 0));
        Enregistreur recepteur = new Enregistreur();
        modele.completer(new RequeteModele(Document.of("int x = "), 8,
                new JetonAnnulation()), recepteur);
        assertEquals(new SuggestionFantome("42;", 8, 0), recepteur.recue,
                "la livraison d'un autre fil arrive pareil");
        assertNotEquals(Thread.currentThread(), recepteur.filLivraison,
                "la livraison a bien voyagé");
        assertEquals(1, livraisons.get());
    }
}
