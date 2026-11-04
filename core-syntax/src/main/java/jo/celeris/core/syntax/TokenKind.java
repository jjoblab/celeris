package jo.celeris.core.syntax;

/**
 * Sortes de jetons communes aux langages (ADR-004) : le socle ne connaît
 * que des catégories de coloration et de structure, chaque analyseur
 * affecte la sienne. {@link #OPEN_BRACKET} et {@link #CLOSE_BRACKET}
 * portent les crochets appariables — la profondeur, les plis et la visée
 * s'en servent ; les langages à balises n'en émettent pas.
 */
public enum TokenKind {

    /** Mot réservé du langage. */
    KEYWORD,

    /** Nom de type (classe, interface, balise). */
    TYPE,

    /** Chaîne de caractères, gabarit compris. */
    STRING,

    /** Littéral numérique. */
    NUMBER,

    /** Commentaire (ligne ou bloc). */
    COMMENT,

    /** Documentation (javadoc, kdoc). */
    DOC,

    /** Annotation (@ […] ou [ ]). */
    ANNOTATION,

    /** Opérateur. */
    OPERATOR,

    /** Ponctuation non appariable. */
    PUNCTUATION,

    /** Identificateur simple. */
    IDENTIFIER,

    /** Constante nommée. */
    CONSTANT,

    /** Crochet ouvrant appariant : ( { [. */
    OPEN_BRACKET,

    /** Crochet fermant appariant : ) } ]. */
    CLOSE_BRACKET,

    /** Jeton malformé (chaîne non fermée, surprise lexicale). */
    ERROR
}
