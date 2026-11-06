package jo.celeris.core.lsp;

/**
 * L'erreur du protocole remontée aux appelants : la réponse d'erreur
 * d'une requête, code et message compris — les annulations serveur
 * (-32800, -32801) y sont distinguables par code.
 */
public final class ErreurLspException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;

    /**
     * L'erreur du protocole.
     *
     * @param erreur l'erreur de la réponse
     */
    public ErreurLspException(ErreurRpc erreur) {
        super("LSP error " + erreur.code() + ": " + erreur.message());
        this.code = erreur.code();
    }

    /** Le code de l'erreur du protocole. */
    public int code() {
        return code;
    }
}
