package jo.celeris.core.syntax;

/**
 * Sortes de nœuds propres à Kotlin (ADR-004) : le plan, les plis et les
 * diagnostics lisent la catégorie générique, la sorte sert aux requêtes
 * propres au langage.
 */
enum KotlinKind implements NodeKind {

    /** Déclaration de type (classe, interface, objet, objet compagnon). */
    TYPE,

    /** Fonction. */
    METHOD,

    /** Propriété ou constante. */
    FIELD,

    /** Bloc d'initialisation (init) ou bloc nu. */
    INITIALIZER,

    /** Instruction avec bloc (contrôle). */
    STATEMENT,

    /** Bloc d'accolades dans un corps. */
    BLOC,

    /** Structure malformée. */
    ERREUR,

    /** Trivia ou construction non structurée. */
    AUTRE
}
