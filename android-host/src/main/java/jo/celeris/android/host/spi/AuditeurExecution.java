package jo.celeris.android.host.spi;

/**
 * L'auditeur des changements d'état d'exécution (E-04) : chaque
 * lancement et chaque fin se signalent — les repères de gouttière
 * suivent.
 */
@FunctionalInterface
public interface AuditeurExecution {

    /**
     * Appelé à chaque changement d'état d'un fichier lancé.
     *
     * @param fichier le fichier dont l'état change
     * @param etat    le nouvel état
     */
    void surChangement(CheminDoc fichier, EtatExecution etat);
}
