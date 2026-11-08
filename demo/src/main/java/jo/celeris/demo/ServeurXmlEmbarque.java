package jo.celeris.demo;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

import jo.celeris.android.widgets.CompletionDocModel;
import jo.celeris.android.widgets.SignatureAideModel;
import jo.celeris.core.lsp.ClientLsp;
import jo.celeris.core.lsp.DiagnosticLsp;
import jo.celeris.core.lsp.JsonObjet;
import jo.celeris.core.lsp.ListeCompletion;
import jo.celeris.core.lsp.OrdonnanceurLsp;
import jo.celeris.core.lsp.PositionsLsp;
import jo.celeris.core.lsp.TransportFlux;
import jo.celeris.core.text.Document;

import org.eclipse.lemminx.XMLLanguageServer;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.jspecify.annotations.Nullable;

/**
 * Le serveur de langage XML RÉEL, embarqué et lancé EN PROCESSUS
 * (M8.6 — LemMinX, voir NOTICE pour l'identité et la licence) : la
 * démonstration y branche son client LSP par le transport à flux sur
 * des tubes mémoire — cadrage Content-Length pour de vrai, poignée de
 * main, documents versionnés, complétion, survol, définition et
 * diagnostics de validation, tout le protocole y passe.
 *
 * <p>Pourquoi ce pont : les tubes du JDK couplent la lecture à la
 * VIVACITÉ du fil écrivain — les réponses du serveur partent de fils
 * de piscine éphémères, et leur mort brisait la liaison
 * ({@link TubesMemoire} documente le piège). Le serveur se lance par
 * son API — jamais par son {@code main()}, qui attendrait la mort du
 * processus PARENT : sur appareil, le parent EST l'application.</p>
 *
 * <p>La fermeture ne lui envoie JAMAIS {@code exit} : l'implémentation
 * y appelle la sortie de machine virtuelle, fatale au processus hôte.
 * Les transports se ferment, la piscine s'arrête — les fils sont des
 * démons, ils meurent avec le processus.</p>
 */
public final class ServeurXmlEmbarque implements LanguageSession {

    private final TransportFlux transport;
    private final ClientLsp client;
    private final OrdonnanceurLsp ordonnanceur;
    private final ExecutorService executeur;
    private final TubesMemoire.Direction versServeur;
    private final TubesMemoire.Direction duServeur;

    private ServeurXmlEmbarque(TransportFlux transport, ClientLsp client,
            OrdonnanceurLsp ordonnanceur, ExecutorService executeur,
            TubesMemoire.Direction versServeur,
            TubesMemoire.Direction duServeur) {
        this.transport = transport;
        this.client = client;
        this.ordonnanceur = ordonnanceur;
        this.executeur = executeur;
        this.versServeur = versServeur;
        this.duServeur = duServeur;
    }

    /**
     * Démarre le serveur XML en processus : les tubes mémoire, le
     * serveur sur ses deux bouts, le client poignée-de-main sur les
     * nôtres — le protocole circule par le cadrage du transport à flux.
     *
     * @return la session démarrée, prête à ouvrir un document
     */
    public static ServeurXmlEmbarque demarrer() {
        TubesMemoire.Direction versServeur = TubesMemoire.creer();
        TubesMemoire.Direction duServeur = TubesMemoire.creer();
        ExecutorService executeur = Executors.newCachedThreadPool(fil -> {
            Thread daemon = new Thread(fil, "serveur-xml-embarque");
            daemon.setDaemon(true);
            return daemon;
        });
        XMLLanguageServer serveur = new XMLLanguageServer();
        Launcher<LanguageClient> lanceur =
                new LSPLauncher.Builder<LanguageClient>()
                .setLocalService(serveur)
                .setRemoteInterface(
                        org.eclipse.lemminx.customservice.XMLLanguageClientAPI.class)
                .setInput(versServeur.entree())
                .setOutput(duServeur.sortie())
                .setExecutorService(executeur)
                .create();
        serveur.setClient(lanceur.getRemoteProxy());
        lanceur.startListening();
        TransportFlux transport = TransportFlux.sur(duServeur.entree(),
                versServeur.sortie(), "serveur-xml-embarque");
        ClientLsp client = ClientLsp.sur(transport, "celeris-demo");
        client.initialiser(JsonObjet.vide());
        return new ServeurXmlEmbarque(transport, client,
                OrdonnanceurLsp.sur(client), executeur, versServeur,
                duServeur);
    }

    @Override
    public void ouvrir(String uri, String langue, String texte) {
        client.ouvrir(uri, langue, texte);
    }

    @Override
    public void synchroniser(String uri, String texte) {
        // le client du cœur porte la version ; la synchronisation reste complète
        client.changer(uri, client.versionDe(uri) + 1, texte);
    }

    @Override
    public CompletableFuture<@Nullable SignatureAideModel> signature(
            String uri, Document doc, int caret) {
        // le XML n'a pas d'appel de fonction : jamais de signature
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<@Nullable CompletionDocModel> survol(String uri,
            Document doc, String symbole, int caret) {
        return ordonnanceur.survol(uri, PositionsLsp.position(doc, caret))
                .thenApply(survol -> ServeurLangage.enModele(survol, symbole));
    }

    @Override
    public boolean sertCompletion() {
        return true;
    }

    @Override
    public CompletableFuture<ListeCompletion> completion(String uri,
            Document doc, int caret) {
        return ordonnanceur.completion(uri,
                PositionsLsp.position(doc, caret));
    }

    @Override
    public void poserAuditeurDiagnostics(
            BiConsumer<String, java.util.List<DiagnosticLsp>> auditeur) {
        client.ajouterAuditeurDiagnostics(new ClientLsp.AuditeurDiagnostics() {
            @Override
            public void diagnostics(String uri,
                    java.util.List<DiagnosticLsp> diagnostics) {
                auditeur.accept(uri, diagnostics);
            }
        });
    }

    @Override
    public void close() {
        // JAMAIS de notification exit : elle tuerait le processus hôte.
        // Les transports se ferment, la piscine s'arrête — les fils
        // démons ne survivent pas au processus.
        transport.close();
        versServeur.fermer();
        duServeur.fermer();
        executeur.shutdownNow();
    }
}
