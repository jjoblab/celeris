package jo.celeris.core.text.internal.segmentation;

import jo.celeris.core.text.Word;

/**
 * Classe de rupture de mot (propriété Word_Break d'UAX #29) — visible du
 * paquet seulement : la surface publique est {@link Word}.
 */
public enum WbClass {
    NEWLINE, CR, LF, WS, EXTEND, FORMAT, ZWJ, LETTER, NUMERIC,
    KATAKANA, EXTENDNUMLET, MIDLETTER, MIDNUM, MIDNUMLET, EXTPICT, ANY
}
