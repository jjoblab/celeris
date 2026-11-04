package jo.celeris.core.text;

/**
 * Catégorie directionnelle d'un point de code (algorithme bidirectionnel
 * UAX #9) — visible du paquet seulement, la surface publique étant
 * {@link Bidi}.
 */
enum BidiCategory {
    /** Lettre gauche à droite. */
    L,
    /** Lettre droite à gauche. */
    R,
    /** Lettre arabe (forte droite à gauche). */
    AL,
    /** Chiffre européen. */
    EN,
    /** Chiffre arabe-indien. */
    AN,
    /** Séparateur numérique européen. */
    ES,
    /** Symbole numérique européen. */
    ET,
    /** Séparateur numérique commun. */
    CS,
    /** Blanc. */
    WS,
    /** Autre neutre. */
    ON,
    /** Caractère frontière neutre. */
    BN,
    /** Marque non espacente. */
    NSM,
    /** Enchâssement gauche à droite. */
    LRE,
    /** Enchâssement droite à gauche. */
    RLE,
    /** Survol gauche à droite. */
    LRO,
    /** Survol droite à gauche. */
    RLO,
    /** Fin d'enchâssement/survol. */
    PDF,
    /** Isolat gauche à droite. */
    LRI,
    /** Isolat droite à gauche. */
    RLI,
    /** Isolat à sens implicite. */
    FSI,
    /** Fin d'isolat. */
    PDI
}
