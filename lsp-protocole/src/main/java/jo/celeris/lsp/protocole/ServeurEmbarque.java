package jo.celeris.lsp.protocole;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.jspecify.annotations.Nullable;

import jo.celeris.core.lsp.CorrectifLsp;
import jo.celeris.core.lsp.EditionEspaceLsp;
import jo.celeris.core.lsp.EditionTexteLsp;
import jo.celeris.core.lsp.IndiceLsp;
import jo.celeris.core.lsp.SemanticTokensLsp;
import jo.celeris.core.lsp.LieuLsp;
import jo.celeris.core.lsp.OptionsFormatageLsp;
import jo.celeris.core.lsp.PlageLsp;
import jo.celeris.core.lsp.PliLsp;
import jo.celeris.core.lsp.PositionLsp;
import jo.celeris.core.lsp.PreparationRennomageLsp;
import jo.celeris.core.lsp.SignatureLsp;
import jo.celeris.core.lsp.SurlignageLsp;
import jo.celeris.core.lsp.SurvolLsp;
import jo.celeris.core.lsp.SymboleLsp;

/**
 * Le montage du serveur EMBARQUÉ derrière la frontière (ADR-015) : un
 * serveur de langage lancé EN PROCESSUS, rejoint par une session sur
 * des tubes à file — le banc de la démonstration y monte le serveur
 * XML réel comme son serveur de référence, sans que la bibliothèque
 * paraisse dans son code : l'objet serveur et l'interface cliente se
 * donnent opaques, la session rend les types du domaine.
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
 * (l'idiome du serveur XML réel, qui transtype ensuite vers sa propre
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
public final class ServeurEmbarque implements AutoCloseable {

    /**
     * Le service de documents d'un serveur de référence : la
     * synchronisation suivie, le survol et l'aide aux paramètres — les
     * types du domaine, jamais ceux de la bibliothèque. Les méthodes
     * ont leurs replis : un service qui ne sert pas le survol rend
     * null, la demande reçoit sa réponse vide du protocole.
     */
    public interface Documents {

        /**
         * Un document s'ouvre — synchronisation complète, version 1.
         *
         * @param uri    l'uri du document
         * @param langue l'identifiant de langage
         * @param texte  le texte entier
         */
        default void ouvrir(String uri, String langue, String texte) {
        }

        /**
         * Un document change — le texte entier remplace l'état.
         *
         * @param uri     l'uri du document
         * @param version la version annoncée par l'hôte
         * @param texte   le texte entier
         */
        default void changer(String uri, int version, String texte) {
        }

        /** Un document se ferme — l'état suivi s'arrête. */
        default void fermer(String uri) {
        }

        /**
         * Le survol d'une position : la documentation du symbole —
         * null quand le service n'en sert pas.
         *
         * @param uri      l'uri du document
         * @param position la position survolée
         * @return le survol, ou null
         */
        default @Nullable SurvolLsp survol(String uri, PositionLsp position) {
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
        default @Nullable SignatureLsp signature(String uri, PositionLsp position) {
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
        default List<SymboleLsp> symboles(String uri) {
            return List.of();
        }

        /**
         * Les plis du serveur — vide quand le service n'en sert pas.
         *
         * @param uri l'uri du document
         * @return les plis, fin exclusive
         */
        default List<PliLsp> plis(String uri) {
            return List.of();
        }

        /**
         * Le formatage du document — vide quand le service n'en sert pas.
         *
         * @param uri     l'uri du document
         * @param options les options de formatage
         * @return les éditions de texte du formatage
         */
        default List<EditionTexteLsp> formatage(String uri,
                OptionsFormatageLsp options) {
            return List.of();
        }

        /**
         * Les surlignages d'occurrences — vide quand le service n'en sert pas.
         *
         * @param uri      l'uri du document
         * @param position la position du curseur
         * @return les surlignages
         */
        default List<SurlignageLsp> surlignages(String uri, PositionLsp position) {
            return List.of();
        }

        /**
         * La préparation du renommage — null quand le service n'en sert pas.
         *
         * @param uri      l'uri du document
         * @param position la position du nom
         * @return la préparation, ou null
         */
        default @Nullable PreparationRennomageLsp preparerRennomage(
                String uri, PositionLsp position) {
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
        default EditionEspaceLsp rennomage(String uri, PositionLsp position,
                String nouveauNom) {
            return EditionEspaceLsp.VIDE;
        }

        /**
         * Les correctifs rapides — vide quand le service n'en sert pas.
         *
         * @param uri   l'uri du document
         * @param plage la plage porteuse
         * @return les correctifs
         */
        default List<CorrectifLsp> correctifs(String uri, PlageLsp plage) {
            return List.of();
        }

        /**
         * Les indices en ligne — vide quand le service n'en sert pas.
         *
         * @param uri   l'uri du document
         * @param plage la plage visible
         * @return les indices
         */
        default List<IndiceLsp> indices(String uri, PlageLsp plage) {
            return List.of();
        }

        /**
         * Les jetons sémantiques — vide quand le service n'en sert pas.
         *
         * @param uri l'uri du document
         * @return les jetons
         */
        default SemanticTokensLsp jetons(String uri) {
            return SemanticTokensLsp.de(null, List.of());
        }

        /**
         * Le formatage d'une plage — vide quand le service n'en sert pas.
         *
         * @param uri     l'uri du document
         * @param plage   la plage à formater
         * @param options les options de formatage
         * @return les éditions de texte du formatage
         */
        default List<EditionTexteLsp> formatagePlage(String uri, PlageLsp plage,
                OptionsFormatageLsp options) {
            return List.of();
        }

        /**
         * Les plages de sélection — vide quand le service n'en sert pas.
         *
         * @param uri      l'uri du document
         * @param position la position de départ
         * @return les plages englobantes, de la plus intime à la plus large
         */
        default List<PlageLsp> plagesSelection(String uri,
                PositionLsp position) {
            return List.of();
        }

        /**
         * Les jetons sémantiques d'une plage — vide quand le service n'en
         * sert pas.
         *
         * @param uri   l'uri du document
         * @param plage la plage visible
         * @return les jetons de la plage
         */
        default SemanticTokensLsp jetonsPlage(String uri, PlageLsp plage) {
            return SemanticTokensLsp.de(null, List.of());
        }

        /**
         * Les lieux de référence — vide quand le service n'en sert pas.
         *
         * @param uri      l'uri du document
         * @param position la position du symbole
         * @return les lieux de référence
         */
        default List<LieuLsp> references(String uri, PositionLsp position) {
            return List.of();
        }
    }

    private final LspSession session;
    private final ExecutorService executeur;
    private final TubesMemoire.Direction versServeur;
    private final TubesMemoire.Direction duServeur;

    private ServeurEmbarque(LspSession session, ExecutorService executeur,
            TubesMemoire.Direction versServeur, TubesMemoire.Direction duServeur) {
        this.session = session;
        this.executeur = executeur;
        this.versServeur = versServeur;
        this.duServeur = duServeur;
    }

    /**
     * Monte un serveur existant de la bibliothèque — un objet opaque
     * (le serveur XML réel par exemple) et l'interface cliente que son
     * lanceur doit procurer au proxy : la convention des serveurs
     * réels, qui étendent l'interface standard pour leurs services
     * propres.
     *
     * @param serveur         l'objet serveur, lancé par son API
     * @param interfaceClient l'interface du proxy client, dérivée de
     *                        l'interface standard de la bibliothèque
     * @param nomClient       le nom annoncé à la poignée de main
     * @return le montage, sa session prête à s'initialiser
     */
    @SuppressWarnings("FutureReturnValueIgnored")
    public static ServeurEmbarque monter(Object serveur,
            Class<?> interfaceClient, String nomClient) {
        TubesMemoire.Direction versServeur = TubesMemoire.creer();
        TubesMemoire.Direction duServeur = TubesMemoire.creer();
        ExecutorService executeur = executeur();
        @SuppressWarnings("unchecked")
        Class<LanguageClient> interfaceStandard =
                (Class<LanguageClient>) interfaceClient.asSubclass(
                        LanguageClient.class);
        Launcher<LanguageClient> lanceur =
                new LSPLauncher.Builder<LanguageClient>()
                .setLocalService(serveur)
                .setRemoteInterface(interfaceStandard)
                .setInput(versServeur.entree())
                .setOutput(duServeur.sortie())
                .setExecutorService(executeur)
                .create();
        injecterClient(serveur, lanceur.getRemoteProxy());
        lanceur.startListening();
        return achever(versServeur, duServeur, executeur, nomClient);
    }

    /**
     * Monte un serveur de référence sur le service de documents du
     * domaine : le survol et l'aide aux paramètres du terrain, servis
     * par le protocole — la voie de la démonstration (M7.5, §8), sans
     * qu'aucun type de la bibliothèque paraisse côté terrain.
     *
     * @param service   le service de documents du domaine
     * @param nomClient le nom annoncé à la poignée de main
     * @return le montage, sa session prête à s'initialiser
     */
    @SuppressWarnings("FutureReturnValueIgnored")
    public static ServeurEmbarque monter(Documents service, String nomClient) {
        TubesMemoire.Direction versServeur = TubesMemoire.creer();
        TubesMemoire.Direction duServeur = TubesMemoire.creer();
        ExecutorService executeur = executeur();
        Launcher<LanguageClient> lanceur = LSPLauncher.createServerLauncher(
                new ServeurDocuments(service), versServeur.entree(),
                duServeur.sortie(), executeur, Function.identity());
        lanceur.startListening();
        return achever(versServeur, duServeur, executeur, nomClient);
    }

    /**
     * Monte un serveur désigné par ses noms : la classe du serveur et
     * l'interface cliente, en toutes lettres — la voie des
     * DÉCLARATIONS (serveurs des paquets, H-13) : l'appelant ne
     * connaît aucune classe de la bibliothèque ni du serveur, le
     * montage instancie par son constructeur vide et injecte le proxy
     * selon la convention. Le serveur réel de la démonstration y
     * monte ainsi, sans qu'un seul type du serveur ou de la
     * bibliothèque paraisse dans son code.
     *
     * @param classeServeur   le nom complet de la classe du serveur
     * @param interfaceClient le nom complet de l'interface du proxy
     *                        client, dérivée de l'interface standard
     * @param nomClient       le nom annoncé à la poignée de main
     * @return le montage, sa session prête à s'initialiser
     */
    public static ServeurEmbarque monter(String classeServeur,
            String interfaceClient, String nomClient) {
        try {
            Class<?> serveur = Class.forName(classeServeur);
            Object instance = serveur.getDeclaredConstructor().newInstance();
            return monter(instance, Class.forName(interfaceClient), nomClient);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "le serveur demandé ne monte pas : " + classeServeur, e);
        }
    }

    /** Le corps commun des montages : la session sur l'autre bout des tubes. */
    private static ServeurEmbarque achever(TubesMemoire.Direction versServeur,
            TubesMemoire.Direction duServeur, ExecutorService executeur,
            String nomClient) {
        LspSession session = LspSession.sur(duServeur.entree(),
                versServeur.sortie(), nomClient);
        return new ServeurEmbarque(session, executeur, versServeur, duServeur);
    }

    /** La piscine du serveur : des fils démons, ils meurent avec l'hôte. */
    private static ExecutorService executeur() {
        return Executors.newCachedThreadPool(fil -> {
            Thread daemon = new Thread(fil, "celeris-serveur-embarque");
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
    private static void injecterClient(Object serveur, LanguageClient proxy) {
        try {
            Method setClient = serveur.getClass()
                    .getMethod("setClient", LanguageClient.class);
            setClient.invoke(serveur, proxy);
        } catch (NoSuchMethodException e) {
            // pas de setClient : la convention standard de la
            // bibliothèque dit peut-être connect
            if (serveur instanceof LanguageClientAware conscient) {
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
        versServeur.fermer();
        duServeur.fermer();
        executeur.shutdownNow();
    }

}
