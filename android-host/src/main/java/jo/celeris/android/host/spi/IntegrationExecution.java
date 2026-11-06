package jo.celeris.android.host.spi;

import java.util.List;

/**
 * L'intégration d'exécution de l'hôte (E-04) : lancer un fichier (run
 * ou test), suivre son état — les repères de gouttière s'y abreuvent.
 */
public interface IntegrationExecution {

    /**
     * Lance le fichier : le lancement se signale aux auditeurs avec
     * l'état en cours.
     *
     * @param fichier le chemin du fichier à lancer
     * @param test    vrai pour une exécution de test
     * @return le lancement créé, identifiant unique
     */
    Lancement lancer(CheminDoc fichier, boolean test);

    /**
     * L'état courant du fichier.
     *
     * @param fichier le chemin du fichier
     * @return l'état, {@link EtatExecution#PRET} si jamais lancé
     */
    EtatExecution etat(CheminDoc fichier);

    /**
     * Les fichiers lancés et leur état courant.
     *
     * @return les états par fichier, non modifiables
     */
    List<CheminDoc> etats();

    /**
     * Ajoute un auditeur des changements d'état.
     *
     * @param auditeur l'auditeur à prévenir
     */
    void ajouterAuditeur(AuditeurExecution auditeur);
}
