package jo.celeris.core.lsp;

/**
 * Le transport d'un bout de la liaison JSON-RPC (ADR-005) : tube stdio,
 * prise réseau ou appel direct en processus — injectable, le client ne
 * connaît que cette interface. Les messages voyagent en charges JSON
 * nues ; le cadrage appartient aux implémentations à flux.
 */
public interface Transport extends AutoCloseable {

    /**
     * Envoie un message au pair.
     *
     * @param trame la charge JSON complète
     * @throws IllegalStateException si le transport est fermé ou sans
     *         récepteur pair branché
     */
    void envoyer(String trame);

    /**
     * Branche le récepteur des messages arrivants — le précédent, s'il
     * y en avait un, est remplacé.
     *
     * @param recepteur le récepteur des messages du pair
     */
    void brancher(Recepteur recepteur);

    /** Ferme ce bout — le pair en est prévenu une fois ; idempotent. */
    @Override
    void close();
}
