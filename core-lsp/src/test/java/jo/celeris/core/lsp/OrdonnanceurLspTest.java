package jo.celeris.core.lsp;

import jo.celeris.core.lsp.types.PositionLsp;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

/**
 * L'ordonnancement des requêtes (ADR-005) sur la voie générique de la
 * frontière protocolaire (ADR-015) : annulation, priorités, fusion,
 * resynchronisation — les quatre règles du domaine, éprouvées sur des
 * futurs opaques sans aucun fil ni aucune trame. L'émetteur rend un
 * futur que le banc tient : c'est le banc qui fait office de serveur,
 * l'annulation au fil s'observe sur le futur émis.
 */
class OrdonnanceurLspTest {

    private static final String URI = "file:///A.java";

    /** Le suivi des versions du banc : une carte que le banc fait vivre. */
    private static final class Versions implements SuiviVersions {
        private final Map<String, Integer> versions = new ConcurrentHashMap<>();

        @Override
        public int versionDe(String uri) {
            return versions.getOrDefault(uri, -1);
        }
    }

    private final Versions versions = new Versions();

    /** Ordonnance une demande de complétion dont l'émission rend un futur tenu. */
    private CompletableFuture<String> completion(
            OrdonnanceurLsp ordonnanceur, Object identite,
            Supplier<CompletableFuture<String>> emission) {
        return ordonnanceur.ordonnancer("completion", URI,
                OrdonnanceurLsp.Priorite.INTERACTIF, identite, emission);
    }

    private static <T> T joindre(CompletableFuture<T> futur) throws Exception {
        return futur.get(5, TimeUnit.SECONDS);
    }

    private static RequeteAnnuleeException annulationDe(CompletableFuture<?> futur)
            throws Exception {
        ExecutionException echec = assertThrows(ExecutionException.class,
                () -> joindre(futur));
        return assertInstanceOf(RequeteAnnuleeException.class, echec.getCause());
    }

    /**
     * La fusion : deux demandes identiques en vol, UNE émission —
     * les deux appelants servis par le même futur.
     */
    @Test
    void laFusionDesRequetesIdentiques() throws Exception {
        OrdonnanceurLsp ordonnanceur = OrdonnanceurLsp.sur(versions);
        AtomicInteger emissions = new AtomicInteger();
        CompletableFuture<String> tenue = new CompletableFuture<>();
        record Position(int ligne, int colonne) {
        }
        Position identite = new Position(3, 7);
        CompletableFuture<String> premiere = completion(ordonnanceur, identite, () -> {
            emissions.incrementAndGet();
            return tenue;
        });
        CompletableFuture<String> seconde = completion(ordonnanceur, identite, () -> {
            emissions.incrementAndGet();
            return new CompletableFuture<>();
        });
        assertEquals(1, emissions.get(),
                "une seule émission pour deux demandes identiques en vol");
        tenue.complete("un");
        assertEquals("un", joindre(premiere));
        assertEquals("un", joindre(seconde));
    }

    /**
     * L'annulation au remplacement : une complétion en vol remplacée
     * par une plus fraîche — le futur émis est ANNULÉ (la voie y
     * traduit $/cancelRequest), le futur visible échoue REMPLACÉE, la
     * nouvelle part.
     */
    @Test
    void lannulationAuRemplacement() throws Exception {
        OrdonnanceurLsp ordonnanceur = OrdonnanceurLsp.sur(versions);
        CompletableFuture<String> vieuxVol = new CompletableFuture<>();
        CompletableFuture<String> neuve = completion(ordonnanceur,
                new PositionLsp(3, 7), () -> vieuxVol);
        CompletableFuture<String> fraiche = completion(ordonnanceur,
                new PositionLsp(3, 8), () -> new CompletableFuture<>());
        assertEquals(RequeteAnnuleeException.Raison.REMPLACEE,
                annulationDe(neuve).raison());
        assertTrue(vieuxVol.isCancelled(),
                "le futur émis de la remplacée est annulé — la voie y"
                        + " traduit $/cancelRequest");
        // le serveur achève la remplacée : sa réponse tardive tombe, ignorée
        assertFalse(fraiche.isDone(), "la fraîche tient son vol");
    }

    /**
     * La priorité : une requête de fond en vol est devancée par une
     * interactive du même document — jamais l'inverse.
     */
    @Test
    void laPrioriteInteractive() throws Exception {
        OrdonnanceurLsp ordonnanceur = OrdonnanceurLsp.sur(versions);
        CompletableFuture<String> volFond = new CompletableFuture<>();
        CompletableFuture<String> fond = ordonnanceur.ordonnancer("formatage",
                URI, OrdonnanceurLsp.Priorite.FOND, "tout", () -> volFond);
        CompletableFuture<String> interactive = ordonnanceur.ordonnancer(
                "survol", URI, OrdonnanceurLsp.Priorite.INTERACTIF, "ici",
                () -> new CompletableFuture<>());
        assertEquals(RequeteAnnuleeException.Raison.DEVANCEE,
                annulationDe(fond).raison());
        assertTrue(volFond.isCancelled(),
                "le vol de fond est annulé au fil quand l'interactive arrive");
        assertFalse(interactive.isDone(), "l'interactive vit sa vie");
        // et l'inverse ne déloge pas : le fond n'annule jamais l'interactif
        CompletableFuture<String> volInteractif = new CompletableFuture<>();
        CompletableFuture<String> interactiveDeux = ordonnanceur.ordonnancer(
                "survol", URI, OrdonnanceurLsp.Priorite.INTERACTIF, "la-bas",
                () -> volInteractif);
        CompletableFuture<String> volFondDeux = new CompletableFuture<>();
        CompletableFuture<String> fondDeux = ordonnanceur.ordonnancer(
                "formatage", URI, OrdonnanceurLsp.Priorite.FOND, "encore",
                () -> volFondDeux);
        assertFalse(interactiveDeux.isCompletedExceptionally(),
                "le fond ne devance jamais l'interactif");
        assertFalse(volInteractif.isCancelled(),
                "le vol interactif tient, délogé par personne");
        assertFalse(fondDeux.isDone(), "le fond tient son vol, délogé par personne");
        assertFalse(volFondDeux.isCancelled(), "le vol de fond tient aussi");
    }

    /**
     * La resynchronisation : la réponse d'une requête partie à la
     * version 1 arrive après le passage à la version 2 — elle est
     * périmée, l'appelant l'apprend, la requête fraîche passe.
     */
    @Test
    void laResynchronisation() throws Exception {
        OrdonnanceurLsp ordonnanceur = OrdonnanceurLsp.sur(versions);
        versions.versions.put(URI, 1);
        CompletableFuture<String> vol = new CompletableFuture<>();
        CompletableFuture<String> aLaVersionUne = completion(ordonnanceur,
                new PositionLsp(0, 5), () -> vol);
        // le document change pendant le vol : la réponse qui arrive est périmée
        versions.versions.put(URI, 2);
        vol.complete("tardif");
        assertEquals(RequeteAnnuleeException.Raison.PERIMEE,
                annulationDe(aLaVersionUne).raison());
        // la demande fraîche, partie après le changement, est servie
        CompletableFuture<String> volFrais = new CompletableFuture<>();
        CompletableFuture<String> fraiche = completion(ordonnanceur,
                new PositionLsp(0, 6), () -> volFrais);
        volFrais.complete("frais");
        assertEquals("frais", joindre(fraiche));
    }

    /** La réponse tardive d'une requête annulée : ignorée, l'ordonnanceur a déjà rendu son verdict. */
    @Test
    void laReponseTardiveEstIgnoree() throws Exception {
        OrdonnanceurLsp ordonnanceur = OrdonnanceurLsp.sur(versions);
        CompletableFuture<String> vieuxVol = new CompletableFuture<>();
        CompletableFuture<String> vieille = completion(ordonnanceur,
                new PositionLsp(1, 1), () -> vieuxVol);
        CompletableFuture<String> volNeuf = new CompletableFuture<>();
        CompletableFuture<String> neuve = completion(ordonnanceur,
                new PositionLsp(2, 2), () -> volNeuf);
        assertTrue(vieille.isCompletedExceptionally(),
                "la remplacée a déjà son verdict");
        // le serveur achève la vieille requête annulée : la réponse tombe, rien ne casse
        volNeuf.complete("la bonne");
        assertEquals("la bonne", joindre(neuve));
        assertEquals(RequeteAnnuleeException.Raison.REMPLACEE,
                annulationDe(vieille).raison());
    }

    /** L'émission qui échoue sur place : le futur visible porte l'échec, sans vol mort. */
    @Test
    void lemissionQuiEchoueSeVoit() {
        OrdonnanceurLsp ordonnanceur = OrdonnanceurLsp.sur(versions);
        IllegalStateException panne = new IllegalStateException("panne d'émission");
        CompletableFuture<String> visible = completion(ordonnanceur,
                new PositionLsp(4, 4), () -> {
                    throw panne;
                });
        java.util.concurrent.CompletionException echec =
                assertThrows(java.util.concurrent.CompletionException.class,
                        () -> visible.join());
        assertEquals(panne, echec.getCause());
    }
}
