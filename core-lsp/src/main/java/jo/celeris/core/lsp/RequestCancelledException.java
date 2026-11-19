package jo.celeris.core.lsp;

/**
 * La requête annulée par l'ordonnanceur : remplacée par une plus
 * fraîche, devancée par une interactive, ou périmée — la réponse
 * serait celle d'un document qui n'existe plus. L'appelant l'apprend
 * ici, il re-émet s'il veut.
 */
public final class RequestCancelledException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Pourquoi la requête est morte. */
    public enum Reason {

        /** Une requête plus fraîche de la même famille l'a remplacée. */
        REMPLACEE,

        /** Une requête interactive du même document l'a devancée. */
        SUPERSEDED,

        /** Le document a changé pendant le vol — la réponse serait périmée. */
        STALE
    }

    private final Reason reason;

    /**
     * L'annulation, pour une raison.
     *
     * @param reason pourquoi la requête est morte
     */
    public RequestCancelledException(Reason reason) {
        super("request cancelled: " + reason);
        this.reason = reason;
    }

    /** La raison de l'annulation. */
    public Reason reason() {
        return reason;
    }
}
