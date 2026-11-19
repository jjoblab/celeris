package jo.celeris.lsp.protocole;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import jo.celeris.core.lsp.types.CodeActionLsp;
import jo.celeris.core.lsp.types.DiagnosticLsp;
import jo.celeris.core.lsp.types.WorkspaceEditLsp;
import jo.celeris.core.lsp.types.TextEditLsp;
import jo.celeris.core.lsp.types.InlayHintLsp;
import jo.celeris.core.lsp.types.CompletionListLsp;
import jo.celeris.core.lsp.types.LocationLsp;
import jo.celeris.core.lsp.types.FormatOptionsLsp;
import jo.celeris.core.lsp.types.RangeLsp;
import jo.celeris.core.lsp.types.FoldLsp;
import jo.celeris.core.lsp.types.LspPosition;
import jo.celeris.core.lsp.types.RenamePrepareLsp;
import jo.celeris.core.lsp.types.SemanticTokensLsp;
import jo.celeris.core.lsp.types.SignatureLsp;
import jo.celeris.core.lsp.types.HighlightLsp;
import jo.celeris.core.lsp.types.HoverLsp;
import jo.celeris.core.lsp.types.SymbolLsp;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.jspecify.annotations.Nullable;

















/**
 * Le montage du serveur EMBARQUÉ derrière la frontière (ADR-015) : un
 * serveur de langage lancé EN PROCESSUS, rejoint par une session sur
 * des tubes à file — le banc du dépôt de départ y monte son serveur
 * de référence, sans que la bibliothèque paraisse dans son code :
 * l'objet serveur et l'interface cliente se donnent opaques, la
 * session rend les types du domaine.
 *
 * <p>Pourquoi ici : le montage utilise le lanceur de la bibliothèque
 * (cadrage, fils de lecture), donc vit du bon côté de la frontière —
 * le seul module qui connaît la bibliothèque. Le serveur se lance par
 * son API, JAMAIS par sa ligne de commande : les points d'entrée en
 * ligne de commande attendent la mort du processus PARENT (constat
 * M8.6) — sur appareil, le parent EST l'application.</p>
 *
 * <p>L'injection du proxy client suit la convention des serveurs
 * embarqués : la méthode {@code setClient(LanguageClient)} d'abord
 * (l'idiome des serveurs tiers étendus, qui transtype ensuite vers sa propre
 * interface étendue — d'où l'interface cliente à passer au montage),
 * puis {@code connect(LanguageClient)} de l'interface standard de la
 * bibliothèque. Sans l'un ni l'autre, le serveur vit sans rappels du
 * client.</p>
 *
 * <p>La fermeture ne lui envoie JAMAIS {@code exit} : l'implémentation
 * d'un serveur réel y éteint la machine (constat M8.6) — la session
 * demande {@code shutdown}, les tubes se ferment, la piscine du
 * serveur s'arrête ; les fils sont des démons.</p>
 */
public final class EmbeddedServer implements AutoCloseable {

    /**
     * Le service de documents d'un serveur de référence : la
     * synchronisation suivie, le survol et l'aide aux paramètres — les
     * types du domaine, jamais ceux de la bibliothèque. Les méthodes
     * ont leurs replis : un service qui ne sert pas le survol rend
     * null, la demande reçoit sa réponse vide du protocole.
     */
    public interface Docs {

        /**
         * Un document s'ouvre — synchronisation complète, version 1.
         *
         * @param uri    l'uri du document
         * @param language l'identifiant de langage
         * @param text  le texte entier
         */
        default void open(String uri, String language, String text) {
        }

        /**
         * Un document change — le texte entier remplace l'état.
         *
         * @param uri     l'uri du document
         * @param version la version annoncée par l'hôte
         * @param text   le texte entier
         */
        default void change(String uri, int version, String text) {
        }

        /** Un document se ferme — l'état suivi s'arrête. */
        default void close(String uri) {
        }

        /**
         * Le survol d'une position : la documentation du symbole —
         * null quand le service n'en sert pas.
         *
         * @param uri      l'uri du document
         * @param position la position survolée
         * @return le survol, ou null
         */
        default @Nullable HoverLsp hover(String uri, LspPosition position) {
            return null;
        }

        /**
         * Les diagnostics courants d'un document — publiés par le
         * canal du protocole après chaque synchronisation ; le
         * service qui n'en sert pas rend vide (rien ne se publie).
         *
         * @param uri l'uri du document synchronisé
         * @return les diagnostics du texte courant
         */
        default List<DiagnosticLsp> diagnosticsOf(String uri) {
            return List.of();
        }

        /**
         * La complétion à une position : les candidats du contexte,
         * le préfixe déjà filtré — null quand le service n'en sert
         * pas (le retour terrain du 2026-11-19 : le serveur factice
         * de l'exemple y sert mots réservés et membres).
         *
         * @param uri      l'uri du document
         * @param position la position du curseur
         * @return la liste de complétion, ou null sans service
         */
        default @Nullable CompletionListLsp completion(String uri,
                LspPosition position) {
            return null;
        }

        /**
         * L'aide aux paramètres à une position : les signatures de
         * l'appel ouvert — null quand le service n'en sert pas.
         *
         * @param uri      l'uri du document
         * @param position la position du curseur dans l'appel
         * @return l'aide aux paramètres, ou null
         */
        default @Nullable SignatureLsp signature(String uri, LspPosition position) {
            return null;
        }

        // ── La couverture du jalon (H-12) : autant de replis vides,
        //    le serveur de référence n'écrit que ce qu'il sert. ──────

        /**
         * Les symboles du document — vide quand le service n'en sert pas.
         *
         * @param uri l'uri du document
         * @return les symboles, hiérarchie du terrain
         */
        default List<SymbolLsp> symbols(String uri) {
            return List.of();
        }

        /**
         * Les plis du serveur — vide quand le service n'en sert pas.
         *
         * @param uri l'uri du document
         * @return les plis, fin exclusive
         */
        default List<FoldLsp> folds(String uri) {
            return List.of();
        }

        /**
         * Le formatage du document — vide quand le service n'en sert pas.
         *
         * @param uri     l'uri du document
         * @param options les options de formatage
         * @return les éditions de texte du formatage
         */
        default List<TextEditLsp> formatage(String uri,
                FormatOptionsLsp options) {
            return List.of();
        }

        /**
         * Les surlignages d'occurrences — vide quand le service n'en sert pas.
         *
         * @param uri      l'uri du document
         * @param position la position du curseur
         * @return les surlignages
         */
        default List<HighlightLsp> highlights(String uri, LspPosition position) {
            return List.of();
        }

        /**
         * La préparation du renommage — null quand le service n'en sert pas.
         *
         * @param uri      l'uri du document
         * @param position la position du nom
         * @return la préparation, ou null
         */
        default @Nullable RenamePrepareLsp preparerRennomage(
                String uri, LspPosition position) {
            return null;
        }

        /**
         * Le renommage — l'édition vide quand le service n'en sert pas.
         *
         * @param uri        l'uri du document
         * @param position   la position du nom
         * @param nouveauNom le nom qui remplace
         * @return l'édition d'espace du renommage
         */
        default WorkspaceEditLsp rename(String uri, LspPosition position,
                String nouveauNom) {
            return WorkspaceEditLsp.EMPTY;
        }

        /**
         * Les correctifs rapides — vide quand le service n'en sert pas.
         *
         * @param uri   l'uri du document
         * @param range la plage porteuse
         * @return les correctifs
         */
        default List<CodeActionLsp> correctifs(String uri, RangeLsp range) {
            return List.of();
        }

        /**
         * Les indices en ligne — vide quand le service n'en sert pas.
         *
         * @param uri   l'uri du document
         * @param range la plage visible
         * @return les indices
         */
        default List<InlayHintLsp> inlayHints(String uri, RangeLsp range) {
            return List.of();
        }

        /**
         * Les jetons sémantiques — vide quand le service n'en sert pas.
         *
         * @param uri l'uri du document
         * @return les jetons
         */
        default SemanticTokensLsp tokens(String uri) {
            return SemanticTokensLsp.of(null, List.of());
        }

        /**
         * Le formatage d'une plage — vide quand le service n'en sert pas.
         *
         * @param uri     l'uri du document
         * @param range   la plage à formater
         * @param options les options de formatage
         * @return les éditions de texte du formatage
         */
        default List<TextEditLsp> rangeFormatting(String uri, RangeLsp range,
                FormatOptionsLsp options) {
            return List.of();
        }

        /**
         * Les plages de sélection — vide quand le service n'en sert pas.
         *
         * @param uri      l'uri du document
         * @param position la position de départ
         * @return les plages englobantes, de la plus intime à la plus large
         */
        default List<RangeLsp> selectionRanges(String uri,
                LspPosition position) {
            return List.of();
        }

        /**
         * Les jetons sémantiques d'une plage — vide quand le service n'en
         * sert pas.
         *
         * @param uri   l'uri du document
         * @param range la plage visible
         * @return les jetons de la plage
         */
        default SemanticTokensLsp tokensRange(String uri, RangeLsp range) {
            return SemanticTokensLsp.of(null, List.of());
        }

        /**
         * Les lieux de référence — vide quand le service n'en sert pas.
         *
         * @param uri      l'uri du document
         * @param position la position du symbole
         * @return les lieux de référence
         */
        default List<LocationLsp> references(String uri, LspPosition position) {
            return List.of();
        }
    }

    private final LspSession session;
    private final ExecutorService executor;
    private final MemoryPipes.Direction toServer;
    private final MemoryPipes.Direction fromServer;

    private EmbeddedServer(LspSession session, ExecutorService executor,
            MemoryPipes.Direction toServer, MemoryPipes.Direction fromServer) {
        this.session = session;
        this.executor = executor;
        this.toServer = toServer;
        this.fromServer = fromServer;
    }

    /**
     * Monte un serveur existant de la bibliothèque — un objet opaque
     * (tout serveur tiers monté par son API) et l'interface cliente que son
     * lanceur doit procurer au proxy : la convention des serveurs
     * réels, qui étendent l'interface standard pour leurs services
     * propres.
     *
     * @param server         l'objet serveur, lancé par son API
     * @param interfaceClient l'interface du proxy client, dérivée de
     *                        l'interface standard de la bibliothèque
     * @param clientName       le nom annoncé à la poignée de main
     * @return le montage, sa session prête à s'initialiser
     */
    @SuppressWarnings("FutureReturnValueIgnored")
    public static EmbeddedServer start(Object server,
            Class<?> interfaceClient, String clientName) {
        MemoryPipes.Direction toServer = MemoryPipes.create();
        MemoryPipes.Direction fromServer = MemoryPipes.create();
        ExecutorService executor = executor();
        @SuppressWarnings("unchecked")
        Class<LanguageClient> interfaceStandard =
                (Class<LanguageClient>) interfaceClient.asSubclass(
                        LanguageClient.class);
        Launcher<LanguageClient> lanceur =
                new LSPLauncher.Builder<LanguageClient>()
                .setLocalService(server)
                .setRemoteInterface(interfaceStandard)
                .setInput(toServer.input())
                .setOutput(fromServer.output())
                .setExecutorService(executor)
                .create();
        injecterClient(server, lanceur.getRemoteProxy());
        lanceur.startListening();
        return achever(toServer, fromServer, executor, clientName);
    }

    /**
     * Monte un serveur de référence sur le service de documents du
     * domaine : le survol et l'aide aux paramètres du terrain, servis
     * par le protocole — la voie du dépôt de départ (M7.5, §8), sans
     * qu'aucun type de la bibliothèque paraisse côté terrain.
     *
     * @param service   le service de documents du domaine
     * @param clientName le nom annoncé à la poignée de main
     * @return le montage, sa session prête à s'initialiser
     */
    @SuppressWarnings("FutureReturnValueIgnored")
    public static EmbeddedServer start(Docs service, String clientName) {
        MemoryPipes.Direction toServer = MemoryPipes.create();
        MemoryPipes.Direction fromServer = MemoryPipes.create();
        ExecutorService executor = executor();
        DocumentsServer server = new DocumentsServer(service);
        Launcher<LanguageClient> lanceur = LSPLauncher.createServerLauncher(
                server, toServer.input(),
                fromServer.output(), executor, Function.identity());
        // Le proxy client au SERVEUR de documents (le retour terrain
        // du 2026-11-19 : le serveur factice de l'exemple PUBLIE ses
        // diagnostics par le protocole) — c'est lui qui publie, le
        // proxy se pose par sa convention setClient, AVANT l'écoute.
        server.setClient(lanceur.getRemoteProxy());
        lanceur.startListening();
        return achever(toServer, fromServer, executor, clientName);
    }

    /**
     * Monte un serveur désigné par ses noms : la classe du serveur et
     * l'interface cliente, en toutes lettres — la voie des
     * DÉCLARATIONS (serveurs des paquets, H-13) : l'appelant ne
     * connaît aucune classe de la bibliothèque ni du serveur, le
     * montage instancie par son constructeur vide et injecte le proxy
     * selon la convention. Le serveur réel du dépôt de départ y
     * monte ainsi, sans qu'un seul type du serveur ou de la
     * bibliothèque paraisse dans son code.
     *
     * @param classeServeur   le nom complet de la classe du serveur
     * @param interfaceClient le nom complet de l'interface du proxy
     *                        client, dérivée de l'interface standard
     * @param clientName       le nom annoncé à la poignée de main
     * @return le montage, sa session prête à s'initialiser
     */
    public static EmbeddedServer start(String classeServeur,
            String interfaceClient, String clientName) {
        try {
            Class<?> server = Class.forName(classeServeur);
            Object instance = server.getDeclaredConstructor().newInstance();
            return start(instance, Class.forName(interfaceClient), clientName);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "le serveur demandé ne monte pas : " + classeServeur, e);
        }
    }

    /** Le corps commun des montages : la session sur l'autre bout des tubes. */
    private static EmbeddedServer achever(MemoryPipes.Direction toServer,
            MemoryPipes.Direction fromServer, ExecutorService executor,
            String clientName) {
        LspSession session = LspSession.on(fromServer.input(),
                toServer.output(), clientName);
        return new EmbeddedServer(session, executor, toServer, fromServer);
    }

    /** La piscine du serveur : des fils démons, ils meurent avec l'hôte. */
    private static ExecutorService executor() {
        return Executors.newCachedThreadPool(thread -> {
            Thread daemon = new Thread(thread, "celeris-serveur-embarque");
            daemon.setDaemon(true);
            return daemon;
        });
    }

    /**
     * L'injection du proxy client chez le serveur : la convention
     * {@code setClient} des serveurs embarqués, puis l'interface
     * standard {@code connect} de la bibliothèque — sans l'un ni
     * l'autre, le serveur vit sans rappels du client.
     */
    private static void injecterClient(Object server, LanguageClient proxy) {
        try {
            Method setClient = server.getClass()
                    .getMethod("setClient", LanguageClient.class);
            setClient.invoke(server, proxy);
        } catch (NoSuchMethodException e) {
            // pas de setClient : la convention standard de la
            // bibliothèque dit peut-être connect
            if (server instanceof LanguageClientAware conscient) {
                conscient.connect(proxy);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "l'injection du client a échoué chez le serveur", e);
        }
    }

    /** La session du montage — sa poignée de main reste à l'appelant. */
    public LspSession session() {
        return session;
    }

    @Override
    public void close() {
        // JAMAIS de notification exit : elle tuerait le processus hôte.
        session.close();
        toServer.close();
        fromServer.close();
        executor.shutdownNow();
    }

}
