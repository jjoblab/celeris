package jo.celeris.core.syntax;

/**
 * Sortes de nœuds propres à JSON (ADR-004) : les objets et tableaux sont
 * des contenants repliables, les paires portent leur clé au plan.
 */
enum JsonKind implements NodeKind {

    /** Objet { clé: valeur }. */
    OBJET,

    /** Tableau [ valeur, … ]. */
    TABLEAU,

    /** Paire clé-valeur. */
    PAIRE,

    /** Valeur scalaire ou plage tolérée. */
    VALEUR,

    /** Structure malformée. */
    ERREUR,

    /** Enveloppe sans structure. */
    AUTRE
}
