package jo.celeris.core.syntax;

/**
 * Sortes de nœuds propres à Java (ADR-004) : le plan, les plis et les
 * diagnostics lisent la catégorie générique, la sorte sert aux requêtes
 * propres au langage.
 */
enum JavaKind implements NodeKind {

    /** Paquet ou import. */
    PAQUET_IMPORT,

    /** Déclaration de type (classe, interface, énumération, record). */
    TYPE,

    /** Méthode ou constructeur. */
    METHOD,

    /** Champ ou constante. */
    FIELD,

    /** Bloc d'initialisation ou bloc nu. */
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
