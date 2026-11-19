package jo.celeris.core.ai;

import java.util.List;

import jo.celeris.core.syntax.structure.OutlineEntry;
import jo.celeris.core.syntax.SyntaxIssue;

/**
 * L'API d'outils de l'éditeur pour agents (F-05, ADR-012) : lire des
 * plages et des instantanés, lister les symboles, lire les
 * diagnostics, appliquer une transaction avec détection de conflit.
 *
 * <p>Le cœur ne connaît aucun transport : la surface est compatible
 * avec les serveurs d'outils du protocole MCP (voir
 * {@link McpConnector} pour les noms et le catalogue), le fil reste à
 * l'hôte. L'application d'une transaction refuse explicitement si
 * l'instantané de référence a bougé — jamais de fusion silencieuse,
 * le refus porte son message.</p>
 */
public interface EditorTools {

    /**
     * Lit la plage {@code [debut, fin)} de l'instantané courant — la
     * réponse porte la révision servie.
     *
     * @param start le début de la plage, inclusif
     * @param end   la fin de la plage, exclusive
     * @return la plage lue, révision comprise
     * @throws IndexOutOfBoundsException si la plage sort du document
     */
    ReadRange read(int start, int end);

    /**
     * Lit l'instantané entier — la réponse porte la révision servie.
     *
     * @return le document entier, révision comprise
     */
    ReadRange instantane();

    /**
     * Liste les symboles du document (l'index de l'ADR-005, la vue du
     * plan B-16) — la source se fournit à la construction.
     *
     * @return les symboles, dans l'ordre du document
     */
    List<OutlineEntry> symbols();

    /**
     * Liste les diagnostics courants — la source se fournit à la
     * construction.
     *
     * @return les diagnostics, plages exactes comprises
     */
    List<SyntaxIssue> diagnostics();

    /**
     * Les instructions d'agent du dépôt (F-07) — servies telles
     * quelles aux agents qui demandent, jamais inventées : le dépôt
     * sans fichier répond qu'il n'y a rien.
     *
     * @return les instructions, vides si le dépôt n'en déclare pas
     */
    AgentInstructions instructions();

    /**
     * Applique une transaction d'agent : la détection de conflit
     * d'abord — l'instantané de référence doit correspondre, sinon
     * refus explicite — puis la voie de transaction ordinaire, donc
     * annulable et diffable.
     *
     * @param transaction la transaction et sa révision de référence
     * @return le sort, un message lisible et la révision courante
     */
    ToolTransactionReply appliquer(ToolTransaction transaction);
}
