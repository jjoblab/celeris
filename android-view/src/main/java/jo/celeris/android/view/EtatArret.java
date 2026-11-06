package jo.celeris.android.view;

/**
 * L'état d'un point d'arrêt sur une ligne de la vue (D-04) : posé et
 * actif, posé mais désactivé — l'arrêt reste en place —, ou absent.
 */
public enum EtatArret {

    /** La ligne ne porte aucun point d'arrêt. */
    AUCUN,

    /** Le point d'arrêt est posé et interrompt l'exécution. */
    POSE,

    /** Le point d'arrêt est posé mais désactivé : il reste en place. */
    DESACTIVE
}
