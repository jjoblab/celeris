package jo.celeris.core.lsp;

/**
 * Le message JSON-RPC 2.0 : requête, notification ou réponse — l'union
 * close des trois formes qui voyagent sur le transport. L'analyse
 * {@link #de} classe le message ou refuse l'enveloppe : version
 * absente ou mauvaise, méthode non textuelle, réponse sans résultat ni
 * erreur, ou avec les deux.
 */
public sealed interface MessageRpc permits RequeteRpc, NotificationRpc, ReponseRpc {

    /** La forme JSON du message — complète, prête au cadrage. */
    ValeurJson versJson();

    /**
     * Analyse et classe un message.
     *
     * @param json la valeur lue sur le transport
     * @return le message classé
     * @throws IllegalArgumentException si l'enveloppe n'est aucune des
     *         trois formes valides
     */
    static MessageRpc de(ValeurJson json) {
        if (!(json instanceof JsonObjet objet)) {
            throw new IllegalArgumentException("message must be an object");
        }
        String version = objet.membreTexte("jsonrpc");
        if (!"2.0".equals(version)) {
            throw new IllegalArgumentException("jsonrpc must be \"2.0\"");
        }
        if (objet.membre("id") != null && objet.membre("method") != null) {
            return RequeteRpc.de(objet);
        }
        if (objet.membre("method") != null) {
            return NotificationRpc.de(objet);
        }
        return ReponseRpc.de(objet);
    }
}
