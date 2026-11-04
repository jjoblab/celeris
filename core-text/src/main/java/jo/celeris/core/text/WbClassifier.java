package jo.celeris.core.text;

/**
 * Classification Word_Break des points de code (UAX #29) — visible du
 * paquet seulement, la surface publique étant {@link Word}.
 *
 * <p>Les classes se dérivent des plages explicites de la norme
 * (séparateurs médians, tirets bas, fins de ligne), des écritures
 * ({@link Character.UnicodeScript}) et des catégories générales.</p>
 */
final class WbClassifier {

    private static final int[][] MIDLETTER = {
            {0x003A, 0x003A}, {0x00B7, 0x00B7}, {0x05F4, 0x05F4}, {0x2027, 0x2027},
            {0xFE13, 0xFE13}, {0xFE55, 0xFE55}, {0xFF1A, 0xFF1A},
    };

    private static final int[][] MIDNUM = {
            {0x002C, 0x002C}, {0x003B, 0x003B}, {0x066C, 0x066C}, {0xFE10, 0xFE10},
            {0xFE50, 0xFE50}, {0xFF0C, 0xFF0C},
    };

    private static final int[][] EXTENDNUMLET = {
            {0x005F, 0x005F}, {0x203F, 0x2040}, {0x2054, 0x2054}, {0xFE33, 0xFE34},
            {0xFE4D, 0xFE4F}, {0xFF3F, 0xFF3F},
    };

    private static final int[][] NEWLINE = {
            {0x000B, 0x000C}, {0x0085, 0x0085}, {0x2028, 0x2029},
    };

    private static final int[][] WHITESPACE = {
            {0x0009, 0x0009}, {0x0020, 0x0020}, {0x00A0, 0x00A0}, {0x1680, 0x1680},
            {0x2000, 0x200A}, {0x202F, 0x202F}, {0x205F, 0x205F}, {0x3000, 0x3000},
    };

    private WbClassifier() {
    }

    /** La classe Word_Break d'un point de code. */
    static WbClass classOf(int codePoint) {
        WbClass simple = explicitClassOf(codePoint);
        if (simple != WbClass.ANY) {
            return simple;
        }
        return switch (Character.getType(codePoint)) {
            case Character.NON_SPACING_MARK, Character.ENCLOSING_MARK,
                    Character.COMBINING_SPACING_MARK, Character.FORMAT -> WbClass.EXTEND;
            case Character.DECIMAL_DIGIT_NUMBER -> WbClass.NUMERIC;
            case Character.UPPERCASE_LETTER, Character.LOWERCASE_LETTER,
                    Character.TITLECASE_LETTER, Character.MODIFIER_LETTER,
                    Character.OTHER_LETTER -> letterClass(codePoint);
            default -> WbClass.ANY;
        };
    }

    /** Les classes décidées par plages explicites de la norme. */
    private static WbClass explicitClassOf(int codePoint) {
        if (codePoint == 0x000D) {
            return WbClass.CR;
        }
        if (codePoint == 0x000A) {
            return WbClass.LF;
        }
        if (codePoint == 0x200D) {
            return WbClass.ZWJ;
        }
        if (inRanges(codePoint, NEWLINE)) {
            return WbClass.NEWLINE;
        }
        if (inRanges(codePoint, WHITESPACE)) {
            return WbClass.WS;
        }
        return separatorClassOf(codePoint);
    }

    /** Séparateurs médians, tirets bas et pictogrammes. */
    private static WbClass separatorClassOf(int codePoint) {
        if (inRanges(codePoint, MIDLETTER)) {
            return WbClass.MIDLETTER;
        }
        if (inRanges(codePoint, MIDNUM)) {
            return WbClass.MIDNUM;
        }
        if (codePoint == 0x0027 || codePoint == 0x002E) {
            return WbClass.MIDNUMLET;
        }
        if (inRanges(codePoint, EXTENDNUMLET)) {
            return WbClass.EXTENDNUMLET;
        }
        if (Grapheme.classOf(codePoint) == Grapheme.ClusterClass.EXTPICT) {
            return WbClass.EXTPICT;
        }
        return WbClass.ANY;
    }

    /**
     * Les lettres par écriture : les katakanas forment leur classe, les
     * écritures segmentées par dictionnaire (thaï, lao, khmer) et les
     * sinogrammes ne sont pas des ALetter — chaque caractère forme son
     * propre mot.
     */
    private static WbClass letterClass(int codePoint) {
        Character.UnicodeScript script = Character.UnicodeScript.of(codePoint);
        if (script == Character.UnicodeScript.KATAKANA) {
            return WbClass.KATAKANA;
        }
        if (script == Character.UnicodeScript.HAN || script == Character.UnicodeScript.HIRAGANA
                || script == Character.UnicodeScript.THAI || script == Character.UnicodeScript.LAO
                || script == Character.UnicodeScript.KHMER) {
            return WbClass.ANY;
        }
        return WbClass.LETTER;
    }

    private static boolean inRanges(int codePoint, int[][] ranges) {
        for (int[] range : ranges) {
            if (codePoint >= range[0] && codePoint <= range[1]) {
                return true;
            }
        }
        return false;
    }
}
