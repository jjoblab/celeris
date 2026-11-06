package jo.celeris.core.lsp;

import org.jspecify.annotations.Nullable;

/**
 * La notification JSON-RPC : un message sans réponse attendue — les
 * changements de document, les publications de diagnostics, les
 * annulations.
 *
 * @param methode la méthode notifiée
 * @param params  les paramètres, ou null pour l'omission
 */
public record NotificationRpc(String methode, @Nullable ValeurJson params)
        implements MessageRpc {

    @Override
    public JsonObjet versJson() {
        JsonObjet objet = JsonObjet.vide()
                .ajouter("jsonrpc", JsonChaine.of("2.0"))
                .ajouter("method", JsonChaine.of(methode));
        return params == null ? objet : objet.ajouter("params", params);
    }

    /**
     * Analyse la notification depuis sa forme JSON.
     *
     * @param objet l'objet message
     * @return la notification lue
     * @throws IllegalArgumentException si la forme est invalide
     */
    public static NotificationRpc de(JsonObjet objet) {
        String methode = objet.membreTexte("method");
        if (methode == null) {
            throw new IllegalArgumentException("notification requires method");
        }
        return new NotificationRpc(methode, objet.membre("params"));
    }
}
