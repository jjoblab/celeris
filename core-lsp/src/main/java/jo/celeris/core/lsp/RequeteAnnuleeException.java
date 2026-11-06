package jo.celeris.core.lsp;

/**
 * La requête annulée par l'ordonnanceur : remplacée par une plus
 * fraîche, devancée par une interactive, ou périmée — la réponse
 * serait celle d'un document qui n'existe plus. L'appelant l'apprend
 * ici, il re-émet s'il veut.
 */
public final class RequeteAnnuleeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Pourquoi la requête est morte. */
    public enum Raison {

        /** Une requête plus fraîche de la même famille l'a remplacée. */
        REMPLACEE,

        /** Une requête interactive du même document l'a devancée. */
        DEVANCEE,

        /** Le document a changé pendant le vol — la réponse serait périmée. */
        PERIMEE
    }

    private final Raison raison;

    /**
     * L'annulation, pour une raison.
     *
     * @param raison pourquoi la requête est morte
     */
    public RequeteAnnuleeException(Raison raison) {
        super("request cancelled: " + raison);
        this.raison = raison;
    }

    /** La raison de l'annulation. */
    public Raison raison() {
        return raison;
    }
}
