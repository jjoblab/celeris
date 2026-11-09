package jo.celeris.lsp.protocole;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import jo.celeris.core.lsp.DiagnosticLsp;
import jo.celeris.core.lsp.ListeCompletion;
import jo.celeris.core.lsp.LieuLsp;
import jo.celeris.core.lsp.PlageLsp;
import jo.celeris.core.lsp.PositionLsp;
import jo.celeris.core.lsp.SignatureLsp;
import jo.celeris.core.lsp.SurvolLsp;

/**
 * La session de la frontière exercée de bout en bout contre un serveur
 * factice EN PROCESSUS (ADR-015) : le protocole circule pour de vrai —
 * cadrage, sérialisation, fils de lecture — sans qu'aucun type de la
 * bibliothèque ne paraisse dans les réponses, qui sont du domaine.
 */
public final class LspSessionTest {

    private static final String URI = "file:///banc/exemple.xml";

    /** Une session démarrée avec son serveur factice et son état observable. */
    private static final class Banc implements AutoCloseable {
        private final LspSession session;
        private final ServeurFactice.Etat etat;

        private Banc(LspSession session, ServeurFactice.Etat etat) {
            this.session = session;
            this.etat = etat;
        }

        private LspSession session() {
            return session;
        }

        private ServeurFactice.Etat etat() {
            return etat;
        }

        @Override
        public void close() {
            session.close();
        }
    }

    private static Banc demarrer() throws Exception {
        PipedInputStream lectureServeur = new PipedInputStream();
        PipedOutputStream ecritureClient = new PipedOutputStream(lectureServeur);
        PipedInputStream lectureClient = new PipedInputStream();
        PipedOutputStream ecritureServeur = new PipedOutputStream(lectureClient);
        ServeurFactice.Etat etat = ServeurFactice.demarrer(
                lectureServeur, ecritureServeur);
        LspSession session = LspSession.sur(
                lectureClient, ecritureClient, "celeris-banc");
        session.initialiser();
        return new Banc(session, etat);
    }

    private static <T> List<T> attendreNonVide(Supplier<List<T>> sonde)
            throws Exception {
        long borne = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        List<T> vu = sonde.get();
        while (vu.isEmpty() && System.nanoTime() < borne) {
            Thread.sleep(10);
            vu = sonde.get();
        }
        return vu;
    }

    @Test
    void laPoigneeDeMainNegocieLesCapacites() throws Exception {
        try (Banc banc = demarrer()) {
            assertTrue(banc.session().capacites().sertCompletion());
            assertTrue(banc.session().capacites().sertSurvol());
            assertTrue(banc.session().capacites().sertSignature());
            assertTrue(banc.session().capacites().sertDefinition());
            assertEquals(2, banc.session().capacites().genreSynchro());
            assertEquals(List.of("type", "fonction"),
                    banc.session().capacites().typesJetons());
            assertEquals(List.of("declaration"),
                    banc.session().capacites().modificateursJetons());
        }
    }

    @Test
    void leDocumentValidePublieSesDiagnostics() throws Exception {
        try (Banc banc = demarrer()) {
            List<DiagnosticLsp> entendues = new ArrayList<>();
            banc.session().ajouterAuditeurDiagnostics((uri, diagnostics) ->
                    entendues.addAll(diagnostics));
            banc.session().ouvrir(URI, "xml", "<ex/>");
            List<DiagnosticLsp> publies = attendreNonVide(
                    () -> banc.session().diagnosticsDe(URI));
            assertEquals(1, publies.size());
            assertEquals(4, publies.get(0).gravite());
            assertEquals("document ouvert", publies.get(0).message());
            assertEquals("factice", publies.get(0).source());
            assertEquals(1, banc.session().versionDe(URI));
            List<DiagnosticLsp> entendu = attendreNonVide(() -> entendues);
            assertEquals(1, entendu.size());
        }
    }

    @Test
    void lEditionSuitLeDocumentEtRevalide() throws Exception {
        try (Banc banc = demarrer()) {
            banc.session().ouvrir(URI, "xml", "<ex>");
            banc.session().changer(URI, 2, "<ex>");
            List<DiagnosticLsp> publies = attendreNonVide(() ->
                    banc.session().diagnosticsDe(URI).stream()
                            .filter(d -> d.gravite() == DiagnosticLsp.ERREUR)
                            .toList());
            assertEquals(1, publies.size());
            assertEquals("l'élément **n'est pas déclaré**",
                    publies.get(0).message());
            assertEquals("42", publies.get(0).code());
            assertEquals(2, banc.session().versionDe(URI));
            assertEquals("<ex>", banc.etat().dernierTexte.get());
        }
    }

    @Test
    void laCompletionSertLesElementsDocumentes() throws Exception {
        try (Banc banc = demarrer()) {
            banc.session().ouvrir(URI, "xml", "<ex");
            ListeCompletion liste = banc.session().completion(
                    URI, new PositionLsp(0, 3)).get(5, TimeUnit.SECONDS);
            assertTrue(liste.estIncomplete());
            assertEquals(1, liste.elements().size());
            assertEquals("ex:exemplaire", liste.elements().get(0).libelle());
            assertEquals(5, liste.elements().get(0).genre());
            assertEquals("l'élément exemplaire",
                    liste.elements().get(0).detail());
            assertEquals("La documentation de l'élément exemplaire.",
                    liste.elements().get(0).documentation());
        }
    }

    @Test
    void leSurvolSertLaDocumentationBalisee() throws Exception {
        try (Banc banc = demarrer()) {
            banc.session().ouvrir(URI, "xml", "<ex");
            SurvolLsp survol = banc.session().survol(
                    URI, new PositionLsp(0, 1)).get(5, TimeUnit.SECONDS);
            assertEquals("La documentation survolée.", survol.texte());
            assertEquals("markdown", survol.genre());
            PlageLsp plageSurvol = survol.plage();
            assertNotNull(plageSurvol);
            assertEquals(new PositionLsp(0, 0), plageSurvol.debut());
        }
    }

    @Test
    void laSignatureSertLesParametresADecalages() throws Exception {
        try (Banc banc = demarrer()) {
            banc.session().ouvrir(URI, "xml", "<ex");
            SignatureLsp aide = banc.session().signature(
                    URI, new PositionLsp(0, 1)).get(5, TimeUnit.SECONDS);
            assertEquals(1, aide.entrees().size());
            assertEquals("f(a, b)", aide.entrees().get(0).libelle());
            assertEquals(2, aide.entrees().get(0).parametres().size());
            assertEquals("a", aide.entrees().get(0).parametres().get(0).libelle());
            assertEquals("b", aide.entrees().get(0).parametres().get(1).libelle());
            assertEquals(0, aide.signatureActiveIndex());
            assertEquals(1, aide.parametreCourant());
        }
    }

    @Test
    void laDefinitionSertLesLieux() throws Exception {
        try (Banc banc = demarrer()) {
            banc.session().ouvrir(URI, "xml", "<ex");
            List<LieuLsp> lieux = banc.session().definition(
                    URI, new PositionLsp(0, 1)).get(5, TimeUnit.SECONDS);
            assertEquals(1, lieux.size());
            assertEquals(URI, lieux.get(0).uri());
            PlageLsp plageLieu = lieux.get(0).plage();
            assertNotNull(plageLieu);
            assertEquals(new PositionLsp(0, 0), plageLieu.debut());
        }
    }

    @Test
    void laFermetureDemandeArretJamaisSortie() throws Exception {
        Banc banc = demarrer();
        banc.session().close();
        banc.session().close();
        assertTrue(banc.etat().arretDemande.get());
        assertFalse(banc.etat().sortieDemandee.get());
        assertThrows(IllegalStateException.class,
                () -> banc.session().ouvrir(URI, "xml", "<ex/>"));
    }

    @Test
    void laSessionRefuseSansInitialisation() throws Exception {
        PipedInputStream lectureServeur = new PipedInputStream();
        PipedOutputStream ecritureClient = new PipedOutputStream(lectureServeur);
        PipedInputStream lectureClient = new PipedInputStream();
        PipedOutputStream ecritureServeur = new PipedOutputStream(lectureClient);
        ServeurFactice.demarrer(lectureServeur, ecritureServeur);
        try (LspSession session = LspSession.sur(
                lectureClient, ecritureClient, "celeris-banc")) {
            assertThrows(IllegalStateException.class,
                    () -> session.ouvrir(URI, "xml", "<ex/>"));
        }
    }
}
