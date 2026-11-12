package jo.celeris.lsp.protocole;

import jo.celeris.core.lsp.types.PositionLsp;
import jo.celeris.core.lsp.types.SignatureLsp;
import jo.celeris.core.lsp.types.SurvolLsp;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;





/**
 * Le montage du serveur embarqué (ADR-015) exercé de bout en bout : un
 * serveur de référence sur le SPI du domaine — la synchronisation
 * suivie, le survol et la signature par le fil, la correspondance
 * inverse du module — et le montage d'un serveur existant de la
 * bibliothèque par l'objet opaque (le serveur factice du banc y joue
 * le serveur réel).
 */
class ServeurEmbarqueTest {

    private static final String URI = "file:///banc/reference.txt";

    /** Un serveur de référence du banc : survol et signature en dur, l'état des documents suivi. */
    private static final class ReferenceDuBanc implements ServeurEmbarque.Documents {
        String dernierTexte = "";
        String derniereLangue = "";
        int versionsVues;

        @Override
        public void ouvrir(String uri, String langue, String texte) {
            derniereLangue = langue;
            suivre(texte);
        }

        @Override
        public void changer(String uri, int version, String texte) {
            versionsVues = version;
            suivre(texte);
        }

        private void suivre(String texte) {
            dernierTexte = texte;
        }

        @Override
        public SurvolLsp survol(String uri, PositionLsp position) {
            return new SurvolLsp("La documentation du banc.", "markdown", null);
        }

        @Override
        public SignatureLsp signature(String uri, PositionLsp position) {
            return SignatureLsp.de(List.of(
                    new SignatureLsp.Entree("mesurer(int a, int b)",
                            List.of(new SignatureLsp.Parametre("a", null),
                                    new SignatureLsp.Parametre("b", null)),
                            null)),
                    0, 1);
        }
    }

    /** Le survol et la signature du terrain, servis par le fil du protocole. */
    @Test
    void leServiceDeDocumentsSertSurvolEtSignature() throws Exception {
        try (ServeurEmbarque montage = ServeurEmbarque.monter(
                new ReferenceDuBanc(), "celeris-banc")) {
            LspSession session = montage.session();
            session.initialiser();
            session.ouvrir(URI, "texte", "mesurer(1, 2)");
            SurvolLsp survol = session.survol(URI, new PositionLsp(0, 3))
                    .get(5, TimeUnit.SECONDS);
            assertEquals("La documentation du banc.", survol.texte());
            assertEquals("markdown", survol.genre());
            SignatureLsp aide = session.signature(URI, new PositionLsp(0, 11))
                    .get(5, TimeUnit.SECONDS);
            SignatureLsp.Entree active = aide.signatureActive();
            assertTrue(active != null, "la signature active est servie");
            assertEquals("mesurer(int a, int b)", active.libelle());
            assertEquals(List.of("a", "b"), active.parametres().stream()
                    .map(SignatureLsp.Parametre::libelle).toList());
            assertEquals(1, aide.parametreCourant());
        }
    }

    /** La synchronisation suivie : ouvrir, changer, fermer — l'état du terrain vit. */
    @Test
    void laSynchronisationSuitLesDocuments() throws Exception {
        ReferenceDuBanc reference = new ReferenceDuBanc();
        try (ServeurEmbarque montage = ServeurEmbarque.monter(
                reference, "celeris-banc")) {
            LspSession session = montage.session();
            session.initialiser();
            session.ouvrir(URI, "texte", "premier");
            session.changer(URI, 2, "second");
            session.fermer(URI);
            // les notifications sont asynchrones : l'état du terrain
            // s'attend borné, jamais supposé déjà là
            long borne = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (!("second".equals(reference.dernierTexte)
                    && reference.versionsVues == 2)
                    && System.nanoTime() < borne) {
                Thread.sleep(10);
            }
            assertEquals("second", reference.dernierTexte,
                    "le texte entier remplace l'état");
            assertEquals("texte", reference.derniereLangue);
            assertEquals(2, reference.versionsVues);
        }
    }

    /** Le repli du SPI : sans survol ni signature, le protocole rend ses vides. */
    @Test
    void leServiceMuertRendLesVidesDuProtocole() throws Exception {
        try (ServeurEmbarque montage = ServeurEmbarque.monter(
                new ServeurEmbarque.Documents() {
                }, "celeris-banc")) {
            LspSession session = montage.session();
            session.initialiser();
            session.ouvrir(URI, "texte", "rien");
            assertEquals("", session.survol(URI, new PositionLsp(0, 1))
                    .get(5, TimeUnit.SECONDS).texte());
            assertNull(session.signature(URI, new PositionLsp(0, 1))
                    .get(5, TimeUnit.SECONDS).signatureActive());
        }
    }

    /** Le montage d'un serveur existant : l'objet opaque, l'interface standard du banc. */
    @Test
    void leServeurExistantMonteParObjetOpaque() throws Exception {
        try (ServeurEmbarque montage = ServeurEmbarque.monter(
                new ServeurFactice(), org.eclipse.lsp4j.services.LanguageClient.class,
                "celeris-banc")) {
            LspSession session = montage.session();
            assertTrue(session.initialiser().sertCompletion(),
                    "le serveur factice monte par l'objet opaque et répond");
            session.ouvrir(URI, "xml", "<ex/>");
            List<jo.celeris.core.lsp.types.DiagnosticLsp> publies = new java.util.ArrayList<>();
            session.ajouterAuditeurDiagnostics((uri, diags) -> publies.addAll(diags));
            session.ouvrir(URI + "2", "xml", "<ex/>");
            long borne = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (publies.isEmpty() && System.nanoTime() < borne) {
                Thread.sleep(10);
            }
            assertTrue(!publies.isEmpty(),
                    "l'injection setClient du montage porte : les"
                            + " diagnostics du serveur factice arrivent");
        }
    }

    /** Le montage par désignation : les noms en toutes lettres, la voie des déclarations. */
    @Test
    void leMontageParDesignationInstancieEtRepond() throws Exception {
        try (ServeurEmbarque montage = ServeurEmbarque.monter(
                "jo.celeris.lsp.protocole.ServeurFactice",
                "org.eclipse.lsp4j.services.LanguageClient", "celeris-banc")) {
            assertTrue(montage.session().initialiser().sertSurvol(),
                    "le serveur désigné par ses noms monte et répond");
        }
    }

    /** Le montage par désignation refuse l'introuvable — l'échec se dit, jamais ne se devine. */
    @Test
    void leMontageParDesignationRefuseLintrouvable() {
        assertThrows(IllegalStateException.class, () -> ServeurEmbarque.monter(
                "jo.celeris.introuvable.Serveur", "java.lang.Runnable", "banc"));
    }

    /**
     * Le repli de l'injection : sans méthode setClient, l'interface
     * standard de la bibliothèque ({@code connect}) porte le proxy.
     */
    @Test
    void leRepliConnectDeLaBibliothequePorteLeProxy() throws Exception {
        ServeurConscient serveur = new ServeurConscient();
        try (ServeurEmbarque montage = ServeurEmbarque.monter(serveur,
                org.eclipse.lsp4j.services.LanguageClient.class, "celeris-banc")) {
            montage.session().initialiser();
            assertTrue(serveur.connecte,
                    "le proxy est passé par connect, la convention standard");
        }
    }

    /**
     * Sans setClient ni connect, le serveur vit sans rappels du
     * client — le montage ne l'exige pas.
     */
    @Test
    void leServeurSansRappelsMonteQuandMeme() throws Exception {
        try (ServeurEmbarque montage = ServeurEmbarque.monter(
                new ServeurSansRappels(),
                org.eclipse.lsp4j.services.LanguageClient.class, "celeris-banc")) {
            assertTrue(montage.session().initialiser().sertSurvol(),
                    "le serveur sans rappels répond à la poignée de main");
        }
    }

    /** Un serveur conscient : ni setClient, seulement connect de la bibliothèque. */
    private static final class ServeurConscient
            extends ServeurSansRappels
            implements org.eclipse.lsp4j.services.LanguageClientAware {
        boolean connecte;

        @Override
        public void connect(org.eclipse.lsp4j.services.LanguageClient client) {
            connecte = true;
        }
    }

    /** Un serveur nu : les réponses vides du protocole, aucun rappel du client. */
    private static class ServeurSansRappels implements org.eclipse.lsp4j.services.LanguageServer {
        @Override
        public CompletableFuture<org.eclipse.lsp4j.InitializeResult> initialize(
                org.eclipse.lsp4j.InitializeParams params) {
            org.eclipse.lsp4j.ServerCapabilities capacites =
                    new org.eclipse.lsp4j.ServerCapabilities();
            capacites.setHoverProvider(
                    org.eclipse.lsp4j.jsonrpc.messages.Either.forLeft(Boolean.TRUE));
            return CompletableFuture.completedFuture(
                    new org.eclipse.lsp4j.InitializeResult(capacites));
        }

        @Override
        public CompletableFuture<Object> shutdown() {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void exit() {
            // jamais appelé par la session (constat M8.6)
        }

        @Override
        public org.eclipse.lsp4j.services.TextDocumentService getTextDocumentService() {
            return new org.eclipse.lsp4j.services.TextDocumentService() {
                @Override
                public void didOpen(org.eclipse.lsp4j.DidOpenTextDocumentParams params) {
                    // le banc n'ouvre rien
                }

                @Override
                public void didChange(org.eclipse.lsp4j.DidChangeTextDocumentParams params) {
                    // le banc ne change rien
                }

                @Override
                public void didClose(org.eclipse.lsp4j.DidCloseTextDocumentParams params) {
                    // le banc ne ferme rien
                }

                @Override
                public void didSave(org.eclipse.lsp4j.DidSaveTextDocumentParams params) {
                    // le banc ne sauvegarde rien
                }
            };
        }

        @Override
        public org.eclipse.lsp4j.services.WorkspaceService getWorkspaceService() {
            return new org.eclipse.lsp4j.services.WorkspaceService() {
                @Override
                public void didChangeConfiguration(
                        org.eclipse.lsp4j.DidChangeConfigurationParams params) {
                    // le banc ne configure rien
                }

                @Override
                public void didChangeWatchedFiles(
                        org.eclipse.lsp4j.DidChangeWatchedFilesParams params) {
                    // le banc ne surveille rien
                }
            };
        }
    }
}
