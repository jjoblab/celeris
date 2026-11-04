package jo.celeris.core.text;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Segmentation en clusters de graphèmes selon UAX #29 (ADR-003 : la
 * norme vit dans le cœur, jamais dans la vue).
 *
 * <p>Le cœur ne manipule jamais des caractères isolés : le curseur se
 * déplace, sélectionne et supprime par graphème — un « é » accentué, un
 * drapeau, une famille d'emoji unie par sélecteur de largeur nulle
 * forment chacun un seul cluster. Les règles GB3 à GB999 de la
 * révision 15 sont implémentées ; les classes se dérivent des
 * catégories générales Unicode ({@link Character#getType(int)})
 * complétées par les plages propres à UAX #29 (préfixes, indicateurs
 * régionaux, jamo hangûl, pictogrammes étendus, modificateurs
 * d'emoji).</p>
 *
 * <p>Limite documentée : l'ensemble des pictogrammes étendus couvre les
 * plages usuelles jusqu'à U+1FFFD ; les pictogrammes attribués
 * ultérieurement tombent en classe {@code ANY} et se segmentent
 * caractère par caractère. Fil d'exécution : utilitaire pur.</p>
 */
public final class Grapheme {

    private Grapheme() {
    }

    /** Classe de rupture de graphème (propriété Grapheme_Cluster_Break). */
    public enum ClusterClass {
        /** Retour chariot U+000D. */
        CR,
        /** Saut de ligne U+000A. */
        LF,
        /** Caractères de contrôle ou de format. */
        CONTROL,
        /** Marques combinantes (Mn, Me) et modificateurs d'emoji. */
        EXTEND,
        /** Sélecteur de largeur nulle U+200D. */
        ZWJ,
        /** Indicateurs régionaux (drapeaux). */
        RI,
        /** Caractères préfixes (arabe, indiens du sud). */
        PREPEND,
        /** Marques d'espacement (Mc). */
        SPACING_MARK,
        /** Consonnes hangûl de départ. */
        HANGUL_L,
        /** Voyelles hangûl. */
        HANGUL_V,
        /** Consonnes hangûl finales. */
        HANGUL_T,
        /** Syllabe hangûl complète LV. */
        HANGUL_LV,
        /** Syllabe hangûl complète LVT. */
        HANGUL_LVT,
        /** Pictogrammes étendus (emoji). */
        EXTPICT,
        /** Tout le reste. */
        ANY
    }

    private static final int[][] PREPEND = {
            {0x0600, 0x0605}, {0x06DD, 0x06DD}, {0x070F, 0x070F}, {0x0890, 0x0891},
            {0x08E2, 0x08E2}, {0x0D4E, 0x0D4E}, {0x110BD, 0x110BD}, {0x110CD, 0x110CD},
            {0x111C2, 0x111C3}, {0x1193F, 0x1193F}, {0x11941, 0x11941}, {0x11A3A, 0x11A3A},
            {0x11A84, 0x11A89}, {0x11D46, 0x11D46}, {0x11F02, 0x11F02},
    };

    private static final int[][] EXTPICT = {
            {0x00A9, 0x00A9}, {0x00AE, 0x00AE}, {0x203C, 0x203C}, {0x2049, 0x2049},
            {0x2122, 0x2122}, {0x2139, 0x2139}, {0x2194, 0x21AA}, {0x231A, 0x231B},
            {0x2328, 0x2328}, {0x23CF, 0x23CF}, {0x23E9, 0x23FA}, {0x24C2, 0x24C2},
            {0x25AA, 0x25FE}, {0x2600, 0x27BF}, {0x2934, 0x2935}, {0x2B00, 0x2B55},
            {0x3030, 0x3030}, {0x303D, 0x303D}, {0x3297, 0x3297}, {0x3299, 0x3299},
            {0x1F000, 0x1F0FF}, {0x1F10D, 0x1F10F}, {0x1F12F, 0x1F12F},
            {0x1F16C, 0x1F171}, {0x1F17E, 0x1F17F}, {0x1F18E, 0x1F18E},
            {0x1F191, 0x1F19A}, {0x1F1AD, 0x1F1E5}, {0x1F201, 0x1F20F},
            {0x1F21A, 0x1F21A}, {0x1F22F, 0x1F22F}, {0x1F232, 0x1F23A},
            {0x1F23C, 0x1F23F}, {0x1F249, 0x1F3FA}, {0x1F400, 0x1F53D},
            {0x1F546, 0x1F64F}, {0x1F680, 0x1F6FF}, {0x1F774, 0x1F77F},
            {0x1F7D5, 0x1F7FF}, {0x1F80C, 0x1F80F}, {0x1F848, 0x1F84F},
            {0x1F85A, 0x1F85F}, {0x1F888, 0x1F88F}, {0x1F8AE, 0x1F8FF},
            {0x1F90C, 0x1F93A}, {0x1F93C, 0x1F945}, {0x1F947, 0x1FAFF},
            {0x1FC00, 0x1FFFD},
    };

    private static final int[][] HANGUL_L = {{0x1100, 0x115F}, {0xA960, 0xA97C}};

    private static final int[][] HANGUL_V = {{0x1160, 0x11A7}, {0xD7B0, 0xD7C6}};

    private static final int[][] HANGUL_T = {{0x11A8, 0x11FF}, {0xD7CB, 0xD7FB}};

    /** Ce qui suit une consonne de départ (GB6). */
    private static final Set<ClusterClass> APRES_L =
            EnumSet.of(ClusterClass.HANGUL_L, ClusterClass.HANGUL_V,
                    ClusterClass.HANGUL_LV, ClusterClass.HANGUL_LVT);

    /** Ce qui suit une voyelle ou une syllabe LV (GB7). */
    private static final Set<ClusterClass> APRES_VOYELLE =
            EnumSet.of(ClusterClass.HANGUL_V, ClusterClass.HANGUL_T);

    /** Ce qui suit une consonne finale ou une syllabe LVT (GB8). */
    private static final Set<ClusterClass> FINALES =
            EnumSet.of(ClusterClass.HANGUL_LVT, ClusterClass.HANGUL_T);

    /**
     * Renvoie la classe de rupture de graphème d'un point de code.
     *
     * @param codePoint point de code Unicode
     * @return classe de rupture
     */
    public static ClusterClass classOf(int codePoint) {
        if (codePoint == 0x000D) {
            return ClusterClass.CR;
        }
        if (codePoint == 0x000A) {
            return ClusterClass.LF;
        }
        if (codePoint == 0x200D) {
            return ClusterClass.ZWJ;
        }
        if (codePoint == 0x200C) {
            return ClusterClass.EXTEND;
        }
        if (inRanges(codePoint, PREPEND)) {
            return ClusterClass.PREPEND;
        }
        ClusterClass classe = hangulOrFlagOrEmojiClass(codePoint);
        if (classe != ClusterClass.ANY) {
            return classe;
        }
        return categoryClassOf(Character.getType(codePoint));
    }

    /** Le reste, par catégorie générale Unicode. */
    private static ClusterClass categoryClassOf(int type) {
        return switch (type) {
            case Character.NON_SPACING_MARK, Character.ENCLOSING_MARK -> ClusterClass.EXTEND;
            case Character.COMBINING_SPACING_MARK -> ClusterClass.SPACING_MARK;
            case Character.CONTROL, Character.FORMAT, Character.LINE_SEPARATOR,
                    Character.PARAGRAPH_SEPARATOR, Character.SURROGATE, Character.UNASSIGNED
                    -> ClusterClass.CONTROL;
            default -> ClusterClass.ANY;
        };
    }

    /**
     * Les classes hors catégorie générale : indicateurs régionaux, jamo
     * et syllabes hangûl, modificateurs d'emoji et pictogrammes étendus.
     */
    private static ClusterClass hangulOrFlagOrEmojiClass(int codePoint) {
        if (codePoint >= 0x1F1E6 && codePoint <= 0x1F1FF) {
            return ClusterClass.RI;
        }
        ClusterClass hangul = hangulClassOf(codePoint);
        if (hangul != ClusterClass.ANY) {
            return hangul;
        }
        if (codePoint >= 0x1F3FB && codePoint <= 0x1F3FF) {
            return ClusterClass.EXTEND;
        }
        if (inRanges(codePoint, EXTPICT)) {
            return ClusterClass.EXTPICT;
        }
        return ClusterClass.ANY;
    }

    /** Jamo et syllabes hangûl (les syllabes se distinguent par la forme). */
    private static ClusterClass hangulClassOf(int codePoint) {
        if (inRanges(codePoint, HANGUL_L)) {
            return ClusterClass.HANGUL_L;
        }
        if (inRanges(codePoint, HANGUL_V)) {
            return ClusterClass.HANGUL_V;
        }
        if (inRanges(codePoint, HANGUL_T)) {
            return ClusterClass.HANGUL_T;
        }
        if (codePoint >= 0xAC00 && codePoint <= 0xD7A3) {
            return (codePoint - 0xAC00) % 28 == 0 ? ClusterClass.HANGUL_LV : ClusterClass.HANGUL_LVT;
        }
        return ClusterClass.ANY;
    }

    /**
     * Renvoie la première frontière de graphème strictement après la
     * position donnée.
     *
     * @param text texte segmenté
     * @param from position de départ, frontière ou non
     * @return décalage de la frontière suivante, ou longueur du texte
     * @throws IndexOutOfBoundsException si la position est hors bornes
     */
    public static int nextBoundary(String text, int from) {
        int length = text.length();
        if (from < 0 || from > length) {
            throw new IndexOutOfBoundsException("from " + from + " out of [0, " + length + "]");
        }
        if (from == length) {
            return length;
        }
        int i = from + Character.charCount(text.codePointAt(from));
        while (i < length && !isBreak(text, i)) {
            i += Character.charCount(text.codePointAt(i));
        }
        return i;
    }

    /**
     * Renvoie la dernière frontière de graphème strictement avant la
     * position donnée.
     *
     * @param text texte segmenté
     * @param from position de départ, frontière ou non
     * @return décalage de la frontière précédente, ou 0
     * @throws IndexOutOfBoundsException si la position est hors bornes
     */
    public static int previousBoundary(String text, int from) {
        int length = text.length();
        if (from < 0 || from > length) {
            throw new IndexOutOfBoundsException("from " + from + " out of [0, " + length + "]");
        }
        int i = from;
        do {
            if (i == 0) {
                return 0;
            }
            i -= Character.charCount(text.codePointBefore(i));
        } while (i > 0 && !isBreak(text, i));
        return i;
    }

    /**
     * Renvoie toutes les frontières de graphèmes du texte.
     *
     * @param text texte segmenté
     * @return liste croissante commençant à 0 et finissant à la longueur
     */
    public static List<Integer> boundaries(String text) {
        List<Integer> result = new ArrayList<>();
        int i = 0;
        result.add(0);
        while (i < text.length()) {
            i = nextBoundary(text, i);
            result.add(i);
        }
        return result;
    }

    private static boolean isBreak(String text, int position) {
        ClusterClass previous = classOf(text.codePointBefore(position));
        ClusterClass next = classOf(text.codePointAt(position));
        // GB3 : CR × LF ; GB4 et GB5 : contrôles isolés.
        if (separatedByControls(previous, next)) {
            return previous != ClusterClass.CR || next != ClusterClass.LF;
        }
        // GB6 à GB8 : composition hangûl.
        if (hangulJoins(previous, next)) {
            return false;
        }
        // GB9, GB9a, GB9b.
        if (next == ClusterClass.EXTEND || next == ClusterClass.ZWJ
                || next == ClusterClass.SPACING_MARK || previous == ClusterClass.PREPEND) {
            return false;
        }
        // GB11 : pictogramme, extensions, sélecteur, pictogramme.
        if (next == ClusterClass.EXTPICT && precededByExtPictZwj(text, position)) {
            return false;
        }
        // GB12-13 : paire d'indicateurs régionaux.
        return !(previous == ClusterClass.RI && next == ClusterClass.RI
                && oddRegionalIndicatorCount(text, position));
    }

    /** GB3/GB4/GB5 : ruptures autour des contrôles, sauf CR × LF. */
    private static boolean separatedByControls(ClusterClass previous, ClusterClass next) {
        boolean previousIsControl = previous == ClusterClass.CR || previous == ClusterClass.LF
                || previous == ClusterClass.CONTROL;
        boolean nextIsControl = next == ClusterClass.CR || next == ClusterClass.LF
                || next == ClusterClass.CONTROL;
        return previousIsControl || nextIsControl;
    }

    /** GB6 à GB8 : les jamo et syllabes hangûl se composent. */
    private static boolean hangulJoins(ClusterClass previous, ClusterClass next) {
        if (previous == ClusterClass.HANGUL_L && APRES_L.contains(next)) {
            return true;
        }
        if ((previous == ClusterClass.HANGUL_LV || previous == ClusterClass.HANGUL_V)
                && APRES_VOYELLE.contains(next)) {
            return true;
        }
        return FINALES.contains(previous) && next == ClusterClass.HANGUL_T;
    }

    private static boolean precededByExtPictZwj(String text, int position) {
        int i = position;
        while (i > 0 && classOf(text.codePointBefore(i)) == ClusterClass.EXTEND) {
            i -= Character.charCount(text.codePointBefore(i));
        }
        if (i == 0) {
            return false;
        }
        if (classOf(text.codePointBefore(i)) != ClusterClass.ZWJ) {
            return false;
        }
        i -= Character.charCount(text.codePointBefore(i));
        // Les attaches entre le pictogramme de base et le sélecteur
        // font partie de la même séquence GB11.
        while (i > 0 && classOf(text.codePointBefore(i)) == ClusterClass.EXTEND) {
            i -= Character.charCount(text.codePointBefore(i));
        }
        return i > 0 && classOf(text.codePointBefore(i)) == ClusterClass.EXTPICT;
    }

    private static boolean oddRegionalIndicatorCount(String text, int position) {
        int count = 0;
        int i = position;
        while (i > 0) {
            int codePoint = text.codePointBefore(i);
            if (classOf(codePoint) != ClusterClass.RI) {
                break;
            }
            count++;
            i -= Character.charCount(codePoint);
        }
        return count % 2 == 1;
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
