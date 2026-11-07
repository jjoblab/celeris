package jo.celeris.android.view;

/**
 * Le geste demandé sur le texte fantôme (F-01, ADR-012) : l'acceptation
 * se prend par mot, par ligne ou en bloc, le refus ferme — la vue
 * route le geste, l'hôte l'exécute sur sa session de fantôme (le cœur
 * ne lui donne aucune session à toucher).
 */
public enum GesteFantome {

    /** Accepte le premier mot du fantôme — les espaces meneuses et la
     *  séparante comprises, le reste survit ancré pareil. */
    ACCEPTER_MOT,

    /** Accepte la première ligne du fantôme, jusqu'au saut compris. */
    ACCEPTER_LIGNE,

    /** Accepte tout le fantôme. */
    ACCEPTER_TOUT,

    /** Refuse — clavier ou geste : le fantôme se ferme, le vol meurt. */
    REFUSER
}
