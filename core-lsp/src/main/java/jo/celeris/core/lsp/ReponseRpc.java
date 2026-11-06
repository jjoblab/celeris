package jo.celeris.core.lsp;

import org.jspecify.annotations.Nullable;

/**
 * La réponse JSON-RPC : un résultat OU une erreur — jamais les deux,
 * jamais aucun (le résultat peut être le nul explicite).
 *
 * @param id       l'identifiant de la requête servie
 * @param resultat le résultat, ou null (le nul du protocole y est
 *                 normalisé)
 * @param erreur   l'erreur, ou null quand la requête a réussi
 */
public record ReponseRpc(long id, @Nullable ValeurJson resultat, @Nullable ErreurRpc erreur)
        implements MessageRpc {

    @Override
    public JsonObjet versJson() {
        JsonObjet objet = JsonObjet.vide()
                .ajouter("jsonrpc", JsonChaine.of("2.0"))
                .ajouter("id", JsonNombre.of(id));
        if (erreur != null) {
            return objet.ajouter("error", erreur.versJson());
        }
        return objet.ajouter("result", resultat == null ? JsonNul.nul() : resultat);
    }

    /**
     * Analyse la réponse depuis sa forme JSON.
     *
     * @param objet l'objet message
     * @return la réponse lue
     * @throws IllegalArgumentException si ni résultat ni erreur, ou les
     *         deux à la fois
     */
    public static ReponseRpc de(JsonObjet objet) {
        Long id = objet.membreEntier("id");
        if (id == null) {
            throw new IllegalArgumentException("response requires numeric id");
        }
        ValeurJson resultat = objet.membre("result");
        ValeurJson erreur = objet.membre("error");
        if (resultat != null && erreur != null) {
            throw new IllegalArgumentException("response cannot carry result and error");
        }
        if (erreur != null) {
            return new ReponseRpc(id, null, ErreurRpc.de(erreur));
        }
        if (resultat == null) {
            throw new IllegalArgumentException("response requires result or error");
        }
        return new ReponseRpc(id, resultat instanceof JsonNul ? null : resultat, null);
    }
}
