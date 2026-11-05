package jo.celeris.core.syntax;

/**
 * Sortes de nœuds propres à Groovy (ADR-004, dialecte Gradle — C-07) :
 * le plan, les plis et les diagnostics lisent la catégorie générique,
 * la sorte sert aux requêtes propres au langage.
 */
enum GroovyKind implements NodeKind {

    /** Déclaration de type (class, interface, enum, trait). */
    TYPE,

    /** Méthode (def nom(...)). */
    METHOD,

    /** Champ ou variable (def nom = …, déclaration typée). */
    FIELD,

    /** Instruction avec bloc (contrôle, fermeture). */
    STATEMENT,

    /** Bloc d'accolades dans un corps. */
    BLOC,

    /** Structure malformée. */
    ERREUR,

    /** Trivia ou construction non structurée. */
    AUTRE
}
