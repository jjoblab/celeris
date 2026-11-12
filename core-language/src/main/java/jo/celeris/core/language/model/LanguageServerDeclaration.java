package jo.celeris.core.language.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jo.celeris.core.language.JsonReader;
import jo.celeris.core.language.packs.JsonChamps;
import org.jspecify.annotations.Nullable;

/**
 * La déclaration d'un serveur de langage (M7.6, H-13) : de la DONNÉE
 * pure, lue d'un fichier {@code servers.json} — celui du paquet de
 * langage ou celui de l'hôte qui assemble son registre. Elle dit
 * comment joindre un serveur, jamais comment le monter : l'éditeur ne
 * connaît AUCUN serveur, il porte la déclaration ; l'hôte décide de la
 * disponibilité, monte ce qu'il peut servir et replie sur ses moyens
 * propres (ADR-005 : l'index répond vite et hors ligne, le serveur
 * répond juste, la vue ne sait pas qui a répondu).
 *
 * <p>Trois transports se déclarent : {@code embedded} — le serveur vit
 * dans le processus de l'hôte, rejoint par sa classe et son interface
 * cliente —, {@code stdio} — un processus lancé par commande — et
 * {@code tcp} — une prise réseau. Chaque transport exige ses champs ;
 * toute méprise se refuse au chargement avec le champ en cause, jamais
 * à moitié lue.</p>
 *
 * @param language              l'identifiant du langage servi
 * @param transport             le transport : {@code embedded},
 *                              {@code stdio} ou {@code tcp}
 * @param serverClass           la classe du serveur embarqué, ou null
 * @param clientInterface       l'interface cliente du serveur embarqué,
 *                              ou null
 * @param clientName            le nom annoncé à la poignée de main
 * @param command               la commande du processus stdio,
 *                              éventuellement vide
 * @param host                  l'hôte de la prise tcp, ou null
 * @param port                  le port de la prise tcp, -1 sans prise
 * @param initializationOptions les options d'initialisation,
 *                              éventuellement vides
 */
public record LanguageServerDeclaration(
        String language,
        String transport,
        @Nullable String serverClass,
        @Nullable String clientInterface,
        String clientName,
        List<String> command,
        @Nullable String host,
        int port,
        Map<String, Object> initializationOptions) {

    /** Le serveur vit dans le processus de l'hôte. */
    public static final String EMBEDDED = "embedded";

    /** Le serveur vit derrière un processus lancé par commande. */
    public static final String STDIO = "stdio";

    /** Le serveur vit derrière une prise réseau. */
    public static final String TCP = "tcp";

    /** Constructeur canonique : tout se fige, tout se vérifie. */
    public LanguageServerDeclaration {
        exigerNonVide(language, "language");
        if (!EMBEDDED.equals(transport) && !STDIO.equals(transport)
                && !TCP.equals(transport)) {
            throw new IllegalArgumentException("servers.json: \""
                    + transport + "\" is not a transport (expected "
                    + EMBEDDED + ", " + STDIO + " or " + TCP + ")");
        }
        exigerNonVide(clientName, "clientName");
        command = List.copyOf(command);
        initializationOptions = Map.copyOf(initializationOptions);
        if (EMBEDDED.equals(transport)) {
            exigerNonVide(serverClass, "serverClass");
            exigerNonVide(clientInterface, "clientInterface");
        } else if (STDIO.equals(transport)) {
            if (command.isEmpty()) {
                throw new IllegalArgumentException(
                        "servers.json: stdio transport needs a command");
            }
        } else {
            exigerNonVide(host, "host");
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException(
                        "servers.json: tcp transport needs a port in [1, 65535]");
            }
        }
    }

    /**
     * La déclaration d'un serveur embarqué : classe, interface cliente
     * et nom annoncé — le montage reste à l'hôte.
     *
     * @param language              l'identifiant du langage servi
     * @param serverClass           la classe du serveur
     * @param clientInterface       l'interface cliente du serveur
     * @param clientName            le nom annoncé à la poignée de main
     * @param initializationOptions les options d'initialisation
     * @return la déclaration embarquée
     */
    public static LanguageServerDeclaration embedded(String language,
            String serverClass, String clientInterface, String clientName,
            Map<String, Object> initializationOptions) {
        return new LanguageServerDeclaration(language, EMBEDDED, serverClass,
                clientInterface, clientName, List.of(), null, -1,
                initializationOptions);
    }

    /**
     * La déclaration d'un serveur de processus : la commande qui le
     * lance, arguments compris.
     *
     * @param language              l'identifiant du langage servi
     * @param command               la commande et ses arguments
     * @param clientName            le nom annoncé à la poignée de main
     * @param initializationOptions les options d'initialisation
     * @return la déclaration de processus
     */
    public static LanguageServerDeclaration stdio(String language,
            List<String> command, String clientName,
            Map<String, Object> initializationOptions) {
        return new LanguageServerDeclaration(language, STDIO, null, null,
                clientName, command, null, -1, initializationOptions);
    }

    /**
     * La déclaration d'un serveur de prise réseau.
     *
     * @param language              l'identifiant du langage servi
     * @param host                  l'hôte de la prise
     * @param port                  le port de la prise
     * @param clientName            le nom annoncé à la poignée de main
     * @param initializationOptions les options d'initialisation
     * @return la déclaration de prise
     */
    public static LanguageServerDeclaration tcp(String language, String host,
            int port, String clientName,
            Map<String, Object> initializationOptions) {
        return new LanguageServerDeclaration(language, TCP, null, null,
                clientName, List.of(), host, port, initializationOptions);
    }

    /**
     * Lit un fichier {@code servers.json} : la liste des serveurs
     * déclarés, chacun pour son langage. Un fichier sans serveur rend
     * la liste vide ; chaque entrée se valide entière par son
     * constructeur, la première faute nomme son champ.
     *
     * @param json le texte du fichier
     * @return les déclarations lues, éventuellement vides
     */
    public static List<LanguageServerDeclaration> parse(String json) {
        Map<String, Object> champs =
                JsonChamps.objet(JsonReader.read(json), "servers.json");
        Object brut = champs.get("servers");
        if (brut == null) {
            return List.of();
        }
        List<LanguageServerDeclaration> declarations = new ArrayList<>();
        int indice = 0;
        for (Object element : JsonChamps.liste(brut, "servers")) {
            String ou = "servers[" + indice + "]";
            Map<String, Object> entree = JsonChamps.objet(element, ou);
            declarations.add(new LanguageServerDeclaration(
                    JsonChamps.chaineRequise(entree, "language", ou),
                    JsonChamps.chaineRequise(entree, "transport", ou),
                    JsonChamps.chaineNulle(entree.get("serverClass")),
                    JsonChamps.chaineNulle(entree.get("clientInterface")),
                    JsonChamps.chaineRequise(entree, "clientName", ou),
                    JsonChamps.chaines(entree.get("command")),
                    JsonChamps.chaineNulle(entree.get("host")),
                    portOuMoinsUn(entree, ou),
                    options(entree.get("initializationOptions"), ou)));
            indice++;
        }
        return List.copyOf(declarations);
    }

    /** Le port d'une entrée, -1 absent. */
    private static int portOuMoinsUn(Map<String, Object> entree, String ou) {
        Object brut = entree.get("port");
        if (brut == null) {
            return -1;
        }
        if (!(brut instanceof Double nombre) || nombre != Math.floor(nombre)) {
            throw new IllegalArgumentException(
                    ou + ": port must be an integer");
        }
        return nombre.intValue();
    }

    /** Les options d'initialisation d'une entrée, vides absentes. */
    private static Map<String, Object> options(@Nullable Object brut,
            String ou) {
        if (brut == null) {
            return Map.of();
        }
        if (!(brut instanceof Map<?, ?>)) {
            throw new IllegalArgumentException(
                    ou + ": initializationOptions must be an object");
        }
        return JsonChamps.objet(brut, ou + ".initializationOptions");
    }

    /** Une chaîne exigée non vide — le champ nommé dans l'erreur. */
    private static void exigerNonVide(@Nullable String valeur, String champ) {
        if (valeur == null || valeur.isEmpty()) {
            throw new IllegalArgumentException(
                    "servers.json: \"" + champ + "\" must be a string");
        }
    }
}
