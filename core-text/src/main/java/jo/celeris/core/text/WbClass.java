package jo.celeris.core.text;

/**
 * Classe de rupture de mot (propriété Word_Break d'UAX #29) — visible du
 * paquet seulement : la surface publique est {@link Word}.
 */
enum WbClass {
    NEWLINE, CR, LF, WS, EXTEND, FORMAT, ZWJ, LETTER, NUMERIC,
    KATAKANA, EXTENDNUMLET, MIDLETTER, MIDNUM, MIDNUMLET, EXTPICT, ANY
}
