package jo.celeris.core.ai;

/**
 * Le contrat du modèle de texte fantôme (F-01, ADR-012) : l'hôte fournit
 * l'implémentation — modèle local ou distant — le cœur n'appelle jamais
 * un réseau.
 *
 * <p>La complétion est asynchrone par nature : la réponse arrive par le
 * récepteur, sur le fil d'où le modèle l'envoie. La session qui l'écoute
 * ramène les livraisons à son fil de publication (ADR-007) — le modèle
 * n'a aucune contrainte de fil. Chaque requête reçoit au plus une
 * réponse : une suggestion, ou l'aveu qu'il n'y en a pas. Le jeton de la
 * requête dit si la réponse intéresse encore quelqu'un : annulée, la
 * réponse ne sert rien — le modèle qui l'a lu se tait.</p>
 */
public interface ModeleTexteFantome {

    /**
     * Demande une complétion en ligne pour la requête donnée.
     *
     * <p>Ne bloque jamais l'appelant : le contrat est asynchrone, la
     * réponse arrive par le récepteur — ou pas, si le jeton meurt avant
     * (l'attente ne bloque jamais la frappe, ADR-012).</p>
     *
     * @param requete  l'instantané, le curseur et le jeton du vol
     * @param recepteur le récepteur de l'unique réponse
     */
    void completer(RequeteModele requete, Recepteur recepteur);

    /** Le récepteur de la réponse d'une complétion. */
    interface Recepteur {

        /**
         * Une suggestion — datée de la révision de la requête.
         *
         * @param suggestion la suggestion calculée
         */
        void suggestion(SuggestionFantome suggestion);

        /** L'aveu qu'il n'y a rien à suggérer pour cette requête. */
        void aucune();
    }
}
