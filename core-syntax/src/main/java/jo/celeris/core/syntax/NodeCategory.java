package jo.celeris.core.syntax;

/**
 * Catégories de nœuds consommables par les outils génériques (plan, plis,
 * diagnostics) : indépendantes des langages, elles décident de la
 * présentation — icône du plan, pliabilité, gravité.
 */
public enum NodeCategory {

    /** Déclaration de type (classe, interface, enum, record). */
    TYPE,

    /** Déclaration de fonction (méthode, fun, def). */
    FUNCTION,

    /** Déclaration de champ ou propriété. */
    FIELD,

    /** Élément de balisage (XML) ou membre de JSON. */
    TAG,

    /** Bloc d'accolades ou de balises — pliable. */
    BLOCK,

    /** Instruction — rarement exposée, jamais pliable seule. */
    STATEMENT,

    /** Structure malformée : source de diagnostic. */
    ERROR,

    /** Trivia ou structure sans intérêt générique. */
    OTHER
}
