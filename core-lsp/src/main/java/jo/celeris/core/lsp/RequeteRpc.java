package jo.celeris.core.lsp;

import org.jspecify.annotations.Nullable;

/**
 * La requête JSON-RPC : un appel qui attend une réponse — identifiée
 * par son numéro, portée par la couche d'ordonnancement (annulation,
 * priorité, fusion).
 *
 * @param id      l'identifiant de la requête, numérique — le client n'en
 *                émet jamais d'autre forme
 * @param methode la méthode invoquée
 * @param params  les paramètres, ou null pour l'omission
 */
public record RequeteRpc(long id, String methode, @Nullable ValeurJson params)
        implements MessageRpc {

    @Override
    public JsonObjet versJson() {
        JsonObjet objet = JsonObjet.vide()
                .ajouter("jsonrpc", JsonChaine.of("2.0"))
                .ajouter("id", JsonNombre.of(id))
                .ajouter("method", JsonChaine.of(methode));
        return params == null ? objet : objet.ajouter("params", params);
    }

    /**
     * Analyse la requête depuis sa forme JSON.
     *
     * @param objet l'objet message
     * @return la requête lue
     * @throws IllegalArgumentException si la forme est invalide
     */
    public static RequeteRpc de(JsonObjet objet) {
        Long id = objet.membreEntier("id");
        String methode = objet.membreTexte("method");
        if (id == null || methode == null) {
            throw new IllegalArgumentException("request requires numeric id and method");
        }
        return new RequeteRpc(id, methode, objet.membre("params"));
    }
}
