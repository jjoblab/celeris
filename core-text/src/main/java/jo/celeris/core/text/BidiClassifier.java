package jo.celeris.core.text;

import org.jspecify.annotations.Nullable;

/**
 * Classification directionnelle des points de code (UAX #9) — visible du
 * paquet seulement, la surface publique étant {@link Bidi}.
 *
 * <p>Les marques (Mn, Me, Mc) se vérifient avant les plages arabes : les
 * diacritiques arabes (U+064B…) sont des marques, pas des lettres
 * fortes.</p>
 */
final class BidiClassifier {

    private static final int[][] WHITESPACE = {
            {0x0009, 0x0009}, {0x0020, 0x0020}, {0x00A0, 0x00A0},
            {0x2000, 0x200A}, {0x202F, 0x202F}, {0x205F, 0x205F}, {0x3000, 0x3000},
    };

    private static final int[][] HEBREW = {{0x0590, 0x05FF}, {0xFB1D, 0xFB4F}};

    private static final int[][] ARABIC_LETTERS = {
            {0x0700, 0x08FF}, {0xFB50, 0xFDFF}, {0xFE70, 0xFEFF},
    };

    private static final int[][] EUROPEAN_DIGITS = {{0x0030, 0x0039}, {0xFF10, 0xFF19}};

    private BidiClassifier() {
    }

    /** La catégorie directionnelle d'un point de code. */
    static BidiCategory classOf(int codePoint) {
        BidiCategory simple = simpleClassOf(codePoint);
        if (simple != null) {
            return simple;
        }
        int type = Character.getType(codePoint);
        if (type == Character.NON_SPACING_MARK || type == Character.ENCLOSING_MARK
                || type == Character.COMBINING_SPACING_MARK) {
            return BidiCategory.NSM;
        }
        BidiCategory arabic = arabicClassOf(codePoint);
        if (arabic != null) {
            return arabic;
        }
        BidiCategory numeric = numericClassOf(codePoint);
        if (numeric != null) {
            return numeric;
        }
        return typeClassOf(type);
    }

    /** Vrai si la catégorie est neutre (WS, ON, BN). */
    static boolean isNeutral(BidiCategory cls) {
        return cls == BidiCategory.WS || cls == BidiCategory.ON || cls == BidiCategory.BN;
    }

    /** Vrai si la catégorie agit comme du droite à gauche (R, AN, EN). */
    static boolean actsAsRtl(BidiCategory cls) {
        return cls == BidiCategory.R || cls == BidiCategory.AN || cls == BidiCategory.EN;
    }

    /** Blancs, marqueurs directionnels et contrôles explicites. */
    private static @Nullable BidiCategory simpleClassOf(int codePoint) {
        if (inRanges(codePoint, WHITESPACE)) {
            return BidiCategory.WS;
        }
        if (codePoint == 0x200E) {
            return BidiCategory.L;
        }
        if (codePoint == 0x200F || codePoint == 0x061C) {
            return BidiCategory.R;
        }
        return switch (codePoint) {
            case 0x202A -> BidiCategory.LRE;
            case 0x202B -> BidiCategory.RLE;
            case 0x202C -> BidiCategory.PDF;
            case 0x202D -> BidiCategory.LRO;
            case 0x202E -> BidiCategory.RLO;
            case 0x2066 -> BidiCategory.LRI;
            case 0x2067 -> BidiCategory.RLI;
            case 0x2068 -> BidiCategory.FSI;
            case 0x2069 -> BidiCategory.PDI;
            default -> null;
        };
    }

    /** Hébreu et arabe : lettres fortes droite à gauche. */
    private static @Nullable BidiCategory arabicClassOf(int codePoint) {
        if (inRanges(codePoint, HEBREW)) {
            return BidiCategory.R;
        }
        if (codePoint >= 0x0600 && codePoint <= 0x06FF) {
            return arabicBlockClassOf(codePoint);
        }
        if (inRanges(codePoint, ARABIC_LETTERS)) {
            return BidiCategory.AL;
        }
        return null;
    }

    /** Le bloc arabe : chiffres arabes, chiffres européens étendus, lettres. */
    private static BidiCategory arabicBlockClassOf(int codePoint) {
        if (codePoint >= 0x0660 && codePoint <= 0x0669) {
            return BidiCategory.AN;
        }
        if (codePoint >= 0x06F0 && codePoint <= 0x06F9) {
            return BidiCategory.EN;
        }
        if (codePoint == 0x066B || codePoint == 0x066C) {
            return BidiCategory.CS;
        }
        return BidiCategory.AL;
    }

    /** Chiffres européens et séparateurs numériques latins. */
    private static @Nullable BidiCategory numericClassOf(int codePoint) {
        if (inRanges(codePoint, EUROPEAN_DIGITS)) {
            return BidiCategory.EN;
        }
        if (codePoint == 0x002B || codePoint == 0x002D) {
            return BidiCategory.ES;
        }
        if (codePoint == 0x0024 || codePoint == 0x0025 || codePoint == 0x00B0) {
            return BidiCategory.ET;
        }
        if (codePoint == 0x002C || codePoint == 0x002E || codePoint == 0x003A) {
            return BidiCategory.CS;
        }
        return null;
    }

    /** Le reste, par catégorie générale. */
    private static BidiCategory typeClassOf(int type) {
        return switch (type) {
            case Character.UPPERCASE_LETTER, Character.LOWERCASE_LETTER,
                    Character.TITLECASE_LETTER, Character.MODIFIER_LETTER,
                    Character.OTHER_LETTER -> BidiCategory.L;
            case Character.CONTROL, Character.FORMAT, Character.SURROGATE,
                    Character.UNASSIGNED, Character.PRIVATE_USE -> BidiCategory.BN;
            default -> BidiCategory.ON;
        };
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
