package jo.celeris.core.ai;

/**
 * La réponse d'une transaction par la voie des outils (F-05,
 * ADR-012) : le sort, un message lisible — le refus dit pourquoi — et
 * la révision courante du document, pour que l'agent reprenne sur des
 * bases fraîches.
 *
 * @param sortie  le sort de la transaction
 * @param message le message lisible, jamais vide
 * @param revision la révision courante du document au moment de la réponse
 */
public record TransactionOutilReponse(Sortie sortie, String message, int revision) {

    /** Le sort d'une transaction d'outil. */
    public enum Sortie {

        /** Appliquée en transaction ordinaire — annulable, une étape. */
        APPLIQUEE,

        /** Refusée : le document a bougé depuis l'instantané lu —
         *  relire, recalculer, recommencer. */
        REFUSEE_CONFLIT,

        /** Refusée : la session est protégée en écriture. */
        REFUSEE_PROTECTION,

        /** Refusée : les éditions ne collent pas au document lu. */
        REFUSEE_EDITS,

        /** Refusée : la transaction ne porte aucune édition. */
        REFUSEE_VIDE
    }

    /**
     * Valide la réponse : le message se lit toujours, même appliqué.
     *
     * @throws IllegalArgumentException si le message est vide
     */
    public TransactionOutilReponse {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message absent");
        }
    }
}
