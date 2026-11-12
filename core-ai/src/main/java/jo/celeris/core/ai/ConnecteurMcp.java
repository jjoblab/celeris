package jo.celeris.core.ai;

import java.util.List;

import jo.celeris.core.syntax.structure.OutlineEntry;
import jo.celeris.core.syntax.SyntaxIssue;

/**
 * Le connecteur MCP de l'API d'outils (F-06, ADR-012) : les mêmes
 * outils qu'en direct, exposés sous leurs noms de serveur — le
 * catalogue se liste, les appels se rendent en JSON, le transport
 * reste à l'hôte.
 *
 * <p>Le cœur ne parle aucun fil réseau (ADR-012) : le connecteur est
 * la couche de surface compatible avec les serveurs d'outils du
 * protocole — noms, descriptions, arguments et réponses rendues en
 * JSON — l'hôte qui parle JSON-RPC branche ces méthodes derrière les
 * siennes. La parité se prouve : chaque appel du connecteur traverse
 * la même méthode que l'appel direct.</p>
 */
public final class ConnecteurMcp {

    /** Les outils exposés, dans l'ordre du catalogue. */
    public enum Outil {

        /** Lit une plage de l’instantané courant — arguments : debut, fin. */
        LIRE("celeris.lire", "Lit une plage de l'instantane courant, revision comprise"),

        /** Rend l'instantané entier — aucun argument. */
        INSTANTANE("celeris.instantane", "Rend l'instantane entier, revision comprise"),

        /** Liste les symboles — aucun argument. */
        SYMBOLES("celeris.symboles", "Liste les symboles du document"),

        /** Liste les diagnostics — aucun argument. */
        DIAGNOSTICS("celeris.diagnostics", "Liste les diagnostics courants"),

        /** Sert les instructions d'agent du dépôt — aucun argument. */
        INSTRUCTIONS("celeris.instructions",
                "Sert les instructions d'agent du depot, telles quelles"),

        /** Applique une transaction — refus si l'instantané lu a bougé. */
        APPLIQUER("celeris.appliquer",
                "Applique une transaction — refus explicite si l'instantane de reference a bouge");

        private final String nom;
        private final String description;

        Outil(String nom, String description) {
            this.nom = nom;
            this.description = description;
        }

        /** Le nom de l’outil côté serveur. */
        public String nom() {
            return nom;
        }

        /** La description de l’outil, lue par le serveur. */
        public String description() {
            return description;
        }
    }

    /** La description d'un outil : nom, description, arguments. */
    public record DescriptionOutil(String nom, String description,
            List<String> arguments) {
    }

    private static final List<DescriptionOutil> CATALOGUE = List.of(
            new DescriptionOutil(Outil.LIRE.nom(), Outil.LIRE.description(),
                    List.of("debut", "fin")),
            new DescriptionOutil(Outil.INSTANTANE.nom(),
                    Outil.INSTANTANE.description(), List.of()),
            new DescriptionOutil(Outil.SYMBOLES.nom(),
                    Outil.SYMBOLES.description(), List.of()),
            new DescriptionOutil(Outil.DIAGNOSTICS.nom(),
                    Outil.DIAGNOSTICS.description(), List.of()),
            new DescriptionOutil(Outil.INSTRUCTIONS.nom(),
                    Outil.INSTRUCTIONS.description(), List.of()),
            new DescriptionOutil(Outil.APPLIQUER.nom(),
                    Outil.APPLIQUER.description(),
                    List.of("revision", "editions")));

    private ConnecteurMcp() {
    }

    /**
     * Le catalogue des outils exposés — la surface d'un serveur d'outils.
     *
     * @return les descriptions, dans l'ordre
     */
    public static List<DescriptionOutil> catalogue() {
        return CATALOGUE;
    }

    /**
     * Lit une plage par le connecteur — le même {@code lire} qu'en
     * direct, la réponse se rend en JSON.
     *
     * @param outils les outils de l'éditeur
     * @param debut  le début de la plage, inclusif
     * @param fin    la fin de la plage, exclusive
     * @return la réponse JSON : révision et texte
     */
    public static String lire(OutilsEditeur outils, int debut, int fin) {
        PlageLue plage = outils.lire(debut, fin);
        return "{\"revision\":" + plage.revision() + ",\"texte\":\""
                + echapper(plage.texte()) + "\"}";
    }

    /**
     * Rend l'instantané entier par le connecteur — le même
     * {@code instantane} qu'en direct.
     *
     * @param outils les outils de l'éditeur
     * @return la réponse JSON : révision et texte
     */
    public static String instantane(OutilsEditeur outils) {
        PlageLue plage = outils.instantane();
        return "{\"revision\":" + plage.revision() + ",\"texte\":\""
                + echapper(plage.texte()) + "\"}";
    }

    /**
     * Liste les symboles par le connecteur — le même {@code symboles}
     * qu'en direct.
     *
     * @param outils les outils de l'éditeur
     * @return la réponse JSON : les symboles, bornes comprises
     */
    public static String symboles(OutilsEditeur outils) {
        StringBuilder json = new StringBuilder("{\"symboles\":[");
        List<OutlineEntry> symboles = outils.symboles();
        for (int i = 0; i < symboles.size(); i++) {
            OutlineEntry symbole = symboles.get(i);
            if (i > 0) {
                json.append(',');
            }
            json.append("{\"categorie\":\"").append(symbole.category().name())
                    .append("\",\"nom\":\"").append(echapper(symbole.name()))
                    .append("\",\"debut\":").append(symbole.startOffset())
                    .append(",\"fin\":").append(symbole.endOffset())
                    .append(",\"ligneDebut\":").append(symbole.startLine())
                    .append(",\"ligneFin\":").append(symbole.endLine())
                    .append('}');
        }
        return json.append("]}").toString();
    }

    /**
     * Liste les diagnostics par le connecteur — le même
     * {@code diagnostics} qu'en direct.
     *
     * @param outils les outils de l'éditeur
     * @return la réponse JSON : les diagnostics, plages comprises
     */
    public static String diagnostics(OutilsEditeur outils) {
        StringBuilder json = new StringBuilder("{\"diagnostics\":[");
        List<SyntaxIssue> diagnostics = outils.diagnostics();
        for (int i = 0; i < diagnostics.size(); i++) {
            SyntaxIssue diagnostic = diagnostics.get(i);
            if (i > 0) {
                json.append(',');
            }
            json.append("{\"debut\":").append(diagnostic.startOffset())
                    .append(",\"longueur\":").append(diagnostic.length())
                    .append(",\"message\":\"")
                    .append(echapper(diagnostic.message())).append("\"}");
        }
        return json.append("]}").toString();
    }

    /**
     * Sert les instructions d'agent par le connecteur — le même
     * {@code instructions} qu'en direct.
     *
     * @param outils les outils de l'éditeur
     * @return la réponse JSON : les instructions, ou absence honnête
     */
    public static String instructions(OutilsEditeur outils) {
        InstructionsAgent instructions = outils.instructions();
        return "{\"instructions\":\"" + echapper(instructions.texte())
                + "\",\"absentes\":" + instructions.estAbsente() + "}";
    }

    /**
     * Applique une transaction par le connecteur — le même
     * {@code appliquer} qu'en direct, détection de conflit comprise.
     *
     * @param outils      les outils de l'éditeur
     * @param transaction la transaction et sa révision de référence
     * @return la réponse JSON : sortie, message et révision courante
     */
    public static String appliquer(OutilsEditeur outils,
            TransactionOutil transaction) {
        TransactionOutilReponse reponse = outils.appliquer(transaction);
        return "{\"sortie\":\"" + reponse.sortie().name() + "\",\"message\":\""
                + echapper(reponse.message()) + "\",\"revision\":"
                + reponse.revision() + "}";
    }

    /**
     * L'échappement JSON minimal : guillemet, barre oblique inverse et
     * contrôles — le texte servi reste lisible par tout serveur.
     */
    private static String echapper(String texte) {
        StringBuilder json = new StringBuilder(texte.length() + 8);
        for (int i = 0; i < texte.length(); i++) {
            char c = texte.charAt(i);
            switch (c) {
                case '"' -> json.append("\\\"");
                case '\\' -> json.append("\\\\");
                case '\n' -> json.append("\\n");
                case '\r' -> json.append("\\r");
                case '\t' -> json.append("\\t");
                default -> {
                    if (c < 0x20) {
                        json.append(String.format("\\u%04x", (int) c));
                    } else {
                        json.append(c);
                    }
                }
            }
        }
        return json.toString();
    }
}
