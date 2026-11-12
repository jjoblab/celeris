package jo.celeris.serveur.java;

import jo.celeris.core.lsp.types.DiagnosticLsp;
import jo.celeris.core.lsp.types.EditionTexteLsp;
import jo.celeris.core.lsp.types.ListeCompletion;
import jo.celeris.core.lsp.types.OptionsFormatageLsp;
import jo.celeris.core.lsp.types.PliLsp;
import jo.celeris.core.lsp.types.PositionLsp;
import jo.celeris.core.lsp.types.SurvolLsp;
import jo.celeris.core.lsp.types.SymboleLsp;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jo.celeris.core.lsp.CapacitesServeur;








import jo.celeris.lsp.protocole.LspSession;
import jo.celeris.lsp.protocole.ServeurEmbarque;
import jo.celeris.lsp.protocole.TubesMemoire;

import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;

/**
 * Le banc de bout en bout du mini serveur : monté EN PROCESSUS sur
 * des tubes à file, rejoint par la session cliente de la frontière
 * protocolaire — la poignée de main, les diagnostics poussés, la
 * complétion, le survol, les symboles, les plis et le formatage y
 * traversent les deux bouts de la même bibliothèque, exactement comme
 * l'éditeur les vivra.
 */
class JavaLanguageServerTest {

    private static final String URI = "file:///exemple.java";

    private static final String DOCUMENT_BRISE = """
            class Compte {
                private int solde;
                void deposer(int montant) {
                    solde = solde + montant
            """;

    private static final String DOCUMENT_PROPRE = """
            class Compte {
                private int solde;
                void deposer(int montant) {
                    solde = solde + montant;
                }
            }
            """;

    private TubesMemoire.Direction versServeur;

    private TubesMemoire.Direction duServeur;

    private LspSession session;

    @BeforeEach
    @SuppressWarnings("FutureReturnValueIgnored")
    void monter() {
        versServeur = TubesMemoire.creer();
        duServeur = TubesMemoire.creer();
        LanguageServer serveur = new JavaLanguageServer();
        ExecutorService executeur = Executors.newCachedThreadPool(fil -> {
            Thread daemon = new Thread(fil, "banc-serveur-java");
            daemon.setDaemon(true);
            return daemon;
        });
        // le proxy client s'injecte AVANT l'écoute : les diagnostics
        // poussés dès la première ouverture doivent trouver leur fil
        org.eclipse.lsp4j.jsonrpc.Launcher<LanguageClient> lanceur =
                LSPLauncher.createServerLauncher(serveur,
                        versServeur.entree(), duServeur.sortie(), executeur,
                        Function.identity());
        ((JavaLanguageServer) serveur).setClient(
                lanceur.getRemoteProxy());
        // le futur d'écoute vit autant que le fil démon qui le porte :
        // le banc ne l'annule jamais, il meurt avec le processus
        lanceur.startListening();
        session = LspSession.sur(duServeur.entree(), versServeur.sortie(),
                "banc-serveur-java");
        session.initialiser();
    }

    @AfterEach
    void demonter() {
        session.close();
        versServeur.fermer();
        duServeur.fermer();
    }

    @Test
    void laPoigneeDeMainNegocieLesCapacitesAnnoncees() {
        CapacitesServeur capacites = session.capacites();
        assertTrue(capacites.sertCompletion());
        assertTrue(capacites.sertSurvol());
        assertTrue(capacites.sertSymboles());
        assertTrue(capacites.sertPlis());
        assertTrue(capacites.sertFormatage());
        assertEquals(1, capacites.genreSynchro(),
                "la synchronisation annoncée est complète");
    }

    @Test
    void lesDiagnosticsPoussesArriventJusquauClient() {
        session.ouvrir(URI, "java", DOCUMENT_BRISE);
        List<DiagnosticLsp> diagnostics = attendreDiagnostics();
        assertEquals(3, diagnostics.size(),
                "les deux accolades ouvertes et le point-virgule manquant");
        assertEquals(1, diagnostics.get(0).gravite());
        assertEquals("accolade ouvrante non fermée",
                diagnostics.get(0).message());
        assertEquals(0, diagnostics.get(0).plage().debut().ligne(),
                "l'accolade de la classe porte le cran");
        assertEquals(2, diagnostics.get(2).gravite(),
                "le point-virgule reste une piste d'avertissement");
        assertTrue(diagnostics.get(2).message()
                .startsWith("point-virgule"));
    }

    @Test
    void laResynchronisationRetireLesDiagnostics() throws Exception {
        session.ouvrir(URI, "java", DOCUMENT_BRISE);
        attendreDiagnostics();
        session.changer(URI, 2, DOCUMENT_PROPRE);
        List<DiagnosticLsp> apres = attendreDiagnostics();
        assertTrue(apres.isEmpty(), () -> apres.toString());
        session.fermer(URI);
        List<DiagnosticLsp> vidange = attendreDiagnostics();
        assertTrue(vidange.isEmpty(), "la fermeture publie la liste vide");
    }

    @Test
    void laCompletionParvientDocumentee() throws Exception {
        session.ouvrir(URI, "java", DOCUMENT_PROPRE);
        int finDuMot = DOCUMENT_PROPRE.indexOf("solde") + "so".length();
        int colonne = finDuMot - DOCUMENT_PROPRE.indexOf('\n') - 1;
        ListeCompletion completion = session.completion(URI,
                new PositionLsp(1, colonne))
                .get(5, TimeUnit.SECONDS);
        List<String> libelles = completion.elements().stream()
                .map(element -> element.libelle()).toList();
        assertTrue(libelles.contains("solde"));
        assertTrue(libelles.stream().anyMatch(l -> l.startsWith("so")));
        completion.elements().stream()
                .filter(element -> element.libelle().equals("solde"))
                .forEach(element -> assertEquals(
                        "champ déclaré dans le document", element.detail()));
    }

    @Test
    void leSurvolParvientDocumente() throws Exception {
        session.ouvrir(URI, "java", DOCUMENT_PROPRE);
        SurvolLsp survol = session.survol(URI, new PositionLsp(0, 2))
                .get(5, TimeUnit.SECONDS);
        assertNotNull(survol);
        assertEquals("markdown", survol.genre());
        assertTrue(survol.texte().startsWith("**class**"));
        jo.celeris.core.lsp.types.PlageLsp plage = survol.plage();
        assertNotNull(plage, "le survol porte sa plage");
        assertEquals(0, plage.debut().ligne());
    }

    @Test
    void lesSymbolesParviennentHierarchiques() throws Exception {
        session.ouvrir(URI, "java", DOCUMENT_PROPRE);
        List<SymboleLsp> symboles = session.symboles(URI)
                .get(5, TimeUnit.SECONDS);
        assertEquals(1, symboles.size());
        assertEquals("Compte", symboles.get(0).nom());
        assertEquals(5, symboles.get(0).genre());
        List<String> enfants = symboles.get(0).enfants().stream()
                .map(SymboleLsp::nom).toList();
        assertTrue(enfants.contains("solde"));
        assertTrue(enfants.contains("deposer"));
    }

    @Test
    void lesPlisParviennentAFinExclusive() throws Exception {
        session.ouvrir(URI, "java", DOCUMENT_PROPRE);
        List<PliLsp> plis = session.plis(URI).get(5, TimeUnit.SECONDS);
        assertEquals(2, plis.size());
        assertEquals(0, plis.get(0).ligneDebut());
        assertEquals(5, plis.get(0).ligneFin(),
                "la fin du domaine est exclusive : la fermante reste visible");
        assertEquals(2, plis.get(1).ligneDebut());
        assertEquals(4, plis.get(1).ligneFin());
    }

    @Test
    void leFormatageParvientEnEditionComplete() throws Exception {
        session.ouvrir(URI, "java",
                DOCUMENT_PROPRE.replace("    private", "private"));
        List<EditionTexteLsp> editions = session.formatage(URI,
                OptionsFormatageLsp.DEFAUT).get(5, TimeUnit.SECONDS);
        assertEquals(1, editions.size());
        assertEquals(new PositionLsp(0, 0), editions.get(0).plage().debut());
        assertTrue(editions.get(0).nouveauTexte().contains("    private"),
                "le champ regagne son retrait");
    }

    @Test
    void leMontageParDesignationSuitLaConvention() throws Exception {
        // la voie des DÉCLARATIONS (servers.json) : la classe et
        // l'interface cliente se nomment, le montage instancie et
        // injecte le proxy par réflexion
        try (ServeurEmbarque montage = ServeurEmbarque.monter(
                "jo.celeris.serveur.java.JavaLanguageServer",
                "org.eclipse.lsp4j.services.LanguageClient",
                "banc-designation")) {
            CapacitesServeur capacites = montage.session().initialiser();
            assertTrue(capacites.sertCompletion());
        }
    }

    /** Sonde les diagnostics poussés : le dernier état publié gagne
     * — deux lectures égales suffisent, les tubes à file livrent en
     * quelques millisecondes. */
    private List<DiagnosticLsp> attendreDiagnostics() {
        List<DiagnosticLsp> precedent = session.diagnosticsDe(URI);
        for (int tour = 0; tour < 100; tour++) {
            dormir(10);
            List<DiagnosticLsp> lu = session.diagnosticsDe(URI);
            if (lu.equals(precedent) && (tour > 2 || !lu.isEmpty())) {
                return lu;
            }
            precedent = lu;
        }
        return precedent;
    }

    private static void dormir(long millisecondes) {
        try {
            Thread.sleep(millisecondes);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
