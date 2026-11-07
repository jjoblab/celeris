package jo.celeris.core.ai;

/**
 * Le contrat du modèle de prédiction (F-02, ADR-012) : distinct de la
 * complétion — le modèle prédit une opération d'édition, pas un texte à
 * afficher.
 *
 * <p>Le cœur ne donne au modèle aucune session à muter : la prédiction
 * qui arrive par le récepteur est une donnée, son application est le
 * geste explicite de l'hôte (voir {@link AppliquerPrediction}) — jamais
 * une application silencieuse. Le contrat est asynchrone, comme celui du
 * texte fantôme : le jeton de la requête dit si la réponse intéresse
 * encore quelqu'un.</p>
 */
public interface ModelePrediction {

    /**
     * Demande une prédiction d'édition pour la requête donnée.
     *
     * <p>Ne bloque jamais l'appelant : la réponse arrive par le
     * récepteur, ou pas — le jeton mort enterre la requête.</p>
     *
     * @param requete   l'instantané, le curseur et le jeton du vol
     * @param recepteur le récepteur de l'unique réponse
     */
    void predire(RequeteModele requete, Recepteur recepteur);

    /** Le récepteur de la réponse d'une prédiction. */
    interface Recepteur {

        /**
         * Une prédiction — datée de la révision de la requête.
         *
         * @param prediction l'opération prédite
         */
        void prediction(PredictionEdition prediction);

        /** L'aveu qu'il n'y a rien à prédire pour cette requête. */
        void aucune();
    }
}
