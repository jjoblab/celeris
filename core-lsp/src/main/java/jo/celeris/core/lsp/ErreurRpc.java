package jo.celeris.core.lsp;

import org.jspecify.annotations.Nullable;

/**
 * L'erreur JSON-RPC : code, message, données optionnelles. Les codes
 * standards du protocole et de LSP vivent en constantes — les
 * annulations du serveur (-32800, -32801) y sont distinguées, la
 * couche d'ordonnancement s'en sert pour requalifier une panne en
 * annulation.
 *
 * @param code     le code numérique de l'erreur
 * @param message  le message humain, en anglais
 * @param donnees  la charge utile additionnelle, ou null
 */
public record ErreurRpc(int code, String message, @Nullable ValeurJson donnees) {

    /** Analyse d'un document impossible. */
    public static final int PARSE_ERROR = -32700;

    /** Enveloppe invalide. */
    public static final int INVALID_REQUEST = -32600;

    /** Méthode inconnue du serveur. */
    public static final int METHOD_NOT_FOUND = -32601;

    /** Paramètres invalides. */
    public static final int INVALID_PARAMS = -32602;

    /** Erreur interne du serveur. */
    public static final int INTERNAL_ERROR = -32603;

    /** Requête annulée par le client ou supplantée côté serveur. */
    public static final int REQUEST_CANCELLED = -32800;

    /** Le document a changé pendant le traitement — la requête est périmée. */
    public static final int CONTENT_MODIFIED = -32801;

    /**
     * Analyse l'erreur depuis sa forme JSON.
     *
     * @param json l'objet « error » d'une réponse
     * @return l'erreur lue
     * @throws IllegalArgumentException si la forme est invalide
     */
    public static ErreurRpc de(ValeurJson json) {
        if (!(json instanceof JsonObjet objet)) {
            throw new IllegalArgumentException("error must be an object");
        }
        Long code = objet.membreEntier("code");
        String message = objet.membreTexte("message");
        if (code == null || message == null) {
            throw new IllegalArgumentException("error requires code and message");
        }
        return new ErreurRpc(code.intValue(), message, objet.membre("data"));
    }

    /** La forme JSON de l'erreur. */
    public JsonObjet versJson() {
        JsonObjet objet = JsonObjet.vide()
                .ajouter("code", JsonNombre.of(code))
                .ajouter("message", JsonChaine.of(message));
        return donnees == null ? objet : objet.ajouter("data", donnees);
    }
}
