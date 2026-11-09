package jo.celeris.lsp.protocole;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.ClientInfo;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.SignatureHelpParams;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageServer;

import jo.celeris.core.lsp.CapacitesServeur;
import jo.celeris.core.lsp.ClientLsp;
import jo.celeris.core.lsp.DiagnosticLsp;
import jo.celeris.core.lsp.ListeCompletion;
import jo.celeris.core.lsp.LieuLsp;
import jo.celeris.core.lsp.PositionLsp;
import jo.celeris.core.lsp.SignatureLsp;
import jo.celeris.core.lsp.SurvolLsp;

/**
 * La session LSP de la frontière protocolaire (ADR-015) : le protocole
 * et le JSON-RPC viennent de la bibliothèque, les types rendus sont
 * ceux du domaine de core-lsp — aucun type de la bibliothèque ne
 * paraît dans l'API.
 *
 * <p>Modèle d'exécution : les fils de la piscine sont des démons — la
 * lecture des trames vit chez la bibliothèque sur ces fils, la session
 * meurt avec le processus hôte. Les documents suivent la convention du
 * client maison : ouverture à la version 1, chaque changement à la
 * version donnée par l'hôte, fermeture qui oublie ; la synchronisation
 * reste complète (le texte entier voyage à chaque changement).</p>
 *
 * <p>La fermeture n'appelle JAMAIS {@code exit} — l'implémentation d'un
 * serveur réel y éteint la machine (constat M8.6) : la requête
 * {@code shutdown} part, la piscine s'arrête, l'appelant ferme ses
 * flux. L'ordonnancement (priorités, annulation, fusion) reste au
 * domaine (ADR-005) et s'assoit au-dessus de cette session à la
 * parité.</p>
 */
public final class LspSession implements AutoCloseable {

    private static final long DELAI_INITIALISATION = 30;
    private static final long DELAI_EXTINCTION = 5;

    private final LanguageServer serveur;
    private final RecepteurServeur recepteur;
    private final ExecutorService executeur;
    private final Future<Void> ecoute;
    private final String nomClient;
    private final Map<String, Integer> versions = new ConcurrentHashMap<>();
    private CapacitesServeur capacites = CapacitesServeur.vides();
    private volatile boolean initialises;
    private volatile boolean fermee;

    private LspSession(Launcher<LanguageServer> lanceur, RecepteurServeur recepteur,
            ExecutorService executeur, String nomClient) {
        this.serveur = lanceur.getRemoteProxy();
        this.recepteur = recepteur;
        this.executeur = executeur;
        this.ecoute = lanceur.startListening();
        this.nomClient = nomClient;
    }

    /**
     * Crée la session sur des flux.
     *
     * @param entree   le flux qui sort du serveur
     * @param sortie   le flux qui entre au serveur
     * @param nomClient le nom annoncé à la poignée de main
     * @return la session prête à s'initialiser
     */
    public static LspSession sur(InputStream entree, OutputStream sortie,
            String nomClient) {
        ExecutorService executeur = Executors.newCachedThreadPool(fil -> {
            Thread daemon = new Thread(fil, "celeris-lsp-protocole");
            daemon.setDaemon(true);
            return daemon;
        });
        RecepteurServeur recepteur = new RecepteurServeur();
        Launcher<LanguageServer> lanceur = LSPLauncher.createClientLauncher(
                recepteur, entree, sortie, executeur, Function.identity());
        return new LspSession(lanceur, recepteur, executeur, nomClient);
    }

    /**
     * La poignée de main : requête initialize (bloquante, délai borné)
     * puis notification initialized — le pid du processus n'est jamais
     * annoncé : la session vit embarquée, le serveur n'a pas à surveiller
     * l'hôte.
     *
     * @return les capacités du serveur
     */
    public CapacitesServeur initialiser() {
        exigerOuverte();
        InitializeParams params = new InitializeParams();
        params.setProcessId(null);
        params.setClientInfo(new ClientInfo(nomClient));
        params.setCapabilities(new ClientCapabilities());
        capacites = CorrespondanceLsp.lireCapacites(
                attendre(serveur.initialize(params), DELAI_INITIALISATION)
                        .getCapabilities());
        serveur.initialized(new InitializedParams());
        initialises = true;
        return capacites;
    }

    /** Les capacités du serveur, lues à l'initialisation. */
    public CapacitesServeur capacites() {
        return capacites;
    }

    /** Ouvre un document — version 1. */
    public void ouvrir(String uri, String langue, String texte) {
        exigerInitialisee();
        versions.put(uri, 1);
        serveur.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(
                new TextDocumentItem(uri, langue, 1, texte)));
    }

    /** Change un document — synchronisation complète, à la version donnée. */
    public void changer(String uri, int version, String texte) {
        exigerInitialisee();
        versions.put(uri, version);
        serveur.getTextDocumentService().didChange(new DidChangeTextDocumentParams(
                new VersionedTextDocumentIdentifier(uri, version),
                List.of(new TextDocumentContentChangeEvent(texte))));
    }

    /** Ferme un document — l'état suivi est oublié. */
    public void fermer(String uri) {
        exigerInitialisee();
        versions.remove(uri);
        serveur.getTextDocumentService().didClose(new DidCloseTextDocumentParams(
                new TextDocumentIdentifier(uri)));
    }

    /**
     * La dernière version envoyée pour un document.
     *
     * @param uri l'uri du document
     * @return la version, ou -1 si le document n'est pas suivi
     */
    public int versionDe(String uri) {
        Integer version = versions.get(uri);
        return version == null ? -1 : version;
    }

    /** Les derniers diagnostics publiés pour un document — vide par défaut. */
    public List<DiagnosticLsp> diagnosticsDe(String uri) {
        return recepteur.diagnosticsDe(uri);
    }

    /** Branche un auditeur de diagnostics — le contrat du domaine (ADR-005). */
    public void ajouterAuditeurDiagnostics(ClientLsp.AuditeurDiagnostics auditeur) {
        recepteur.ajouterAuditeurDiagnostics(auditeur);
    }

    /** La complétion typée — le futur du domaine, la coupure du serveur honorée. */
    public CompletableFuture<ListeCompletion> completion(String uri,
            PositionLsp position) {
        exigerInitialisee();
        return serveur.getTextDocumentService().completion(
                new CompletionParams(identifiant(uri),
                        CorrespondanceLsp.ecrirePosition(position)))
                .thenApply(CorrespondanceDocuments::lireListe);
    }

    /** Le survol — la documentation se résout à la demande, jamais avant. */
    public CompletableFuture<SurvolLsp> survol(String uri, PositionLsp position) {
        exigerInitialisee();
        return serveur.getTextDocumentService().hover(new HoverParams(
                identifiant(uri), CorrespondanceLsp.ecrirePosition(position)))
                .thenApply(CorrespondanceDocuments::lireSurvol);
    }

    /** L'aide aux paramètres — aucune signature quand le serveur n'en sert pas. */
    public CompletableFuture<SignatureLsp> signature(String uri,
            PositionLsp position) {
        exigerInitialisee();
        return serveur.getTextDocumentService().signatureHelp(
                new SignatureHelpParams(identifiant(uri),
                        CorrespondanceLsp.ecrirePosition(position)))
                .thenApply(CorrespondanceDocuments::lireSignature);
    }

    /** La définition — les lieux donnés par le serveur. */
    public CompletableFuture<List<LieuLsp>> definition(String uri,
            PositionLsp position) {
        exigerInitialisee();
        return serveur.getTextDocumentService().definition(new DefinitionParams(
                identifiant(uri), CorrespondanceLsp.ecrirePosition(position)))
                .thenApply(CorrespondanceDocuments::lireLieux);
    }

    @Override
    public void close() {
        if (fermee) {
            return;
        }
        if (initialises) {
            try {
                attendre(serveur.shutdown(), DELAI_EXTINCTION);
            } catch (RuntimeException e) {
                // le serveur ne répond plus : la fermeture continue son chemin
                ignorerExtinctionSansReponse();
            }
        }
        fermee = true;
        ecoute.cancel(true);
        executeur.shutdown();
    }

    /** Marque la fermeture silencieuse : le serveur ne répond plus, rien à faire. */
    private static void ignorerExtinctionSansReponse() {
        // volontairement vide — le protocole ne prévoit rien de mieux
    }

    private TextDocumentIdentifier identifiant(String uri) {
        return new TextDocumentIdentifier(uri);
    }

    private static <T> T attendre(CompletableFuture<T> futur, long secondes) {
        try {
            return futur.get(secondes, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while waiting for server", e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException("server call failed", e);
        } catch (TimeoutException e) {
            throw new IllegalStateException("server did not answer in time", e);
        }
    }

    private void exigerOuverte() {
        if (fermee) {
            throw new IllegalStateException("session is closed");
        }
    }

    private void exigerInitialisee() {
        exigerOuverte();
        if (!initialises) {
            throw new IllegalStateException("session is not initialized");
        }
    }
}
