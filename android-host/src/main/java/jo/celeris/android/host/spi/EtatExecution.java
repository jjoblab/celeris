package jo.celeris.android.host.spi;

/**
 * L'état d'exécution d'un fichier (E-04) : ce que le repère de
 * gouttière affiche.
 */
public enum EtatExecution {

    /** Le fichier n'a jamais été lancé : aucun repère. */
    PRET,

    /** L'exécution court : repère actif. */
    EN_COURS,

    /** L'exécution s'est terminée sans échec. */
    TERMINE,

    /** L'exécution a échoué : repère d'erreur. */
    ECHOUE
}
