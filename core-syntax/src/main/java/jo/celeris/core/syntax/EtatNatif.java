package jo.celeris.core.syntax;

/**
 * L'état de lexicographie du niveau 2 (ADR-020) : l'arbre natif porte
 * la structure ENTIERE — les lignes n'y cascadent aucun état, la
 * comparaison d'états du socle s'y arrête à la première (la voie
 * re-matérialise l'arbre à chaque édition, jamais elle ne relit les
 * lignes épargnées).
 */
enum EtatNatif implements LexState {

    /** L'état unique de la voie native. */
    PREMIER
}
