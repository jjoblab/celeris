package jo.celeris.core.text;

import java.util.ArrayList;
import java.util.List;

/**
 * Segmentation en mots selon UAX #29 (sous-ensemble documenté).
 *
 * <p>La navigation par mot du cœur s'appuie sur les règles WB3 à
 * WB999 : lettres et chiffres se groupent, les séparateurs médians
 * (apostrophe, point, virgule, deux-points) collent deux mots de même
 * nature, les tirets bas prolongent (« mot_de_passe » est un mot),
 * les katakanas se suivent, chaque sinogramme reste seul, les espaces
 * blanches se groupent et les fins de ligne coupent toujours.
 * Le saut de mot respecte les clusters : un emoji avec modificateur
 * de tonalité ne se coupe jamais.</p>
 *
 * <p>Limites documentées : les règles propres à l'hébreu (guillemets
 * simples en contraction « ע׳ ») et la segmentation par dictionnaire
 * (thaï, khmer, lao) ne sont pas implémentées : ces écritures se
 * segmentent caractère par caractère, comme le prévoit la norme en
 * l'absence de dictionnaire. La classification vit dans
 * {@link WbClassifier}, visible du paquet. Fil d'exécution : utilitaire
 * pur.</p>
 */
public final class Word {

    private Word() {
    }

    /**
     * Renvoie la première frontière de mot strictement après la
     * position donnée.
     *
     * @param text texte segmenté
     * @param from position de départ
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
     * Renvoie toutes les frontières de mots du texte.
     *
     * @param text texte segmenté
     * @return liste croissante commençant à 0 et finissant à la longueur
     */
    public static List<Integer> boundaries(String text) {
        List<Integer> result = new ArrayList<>();
        result.add(0);
        int i = 0;
        while (i < text.length()) {
            i = nextBoundary(text, i);
            result.add(i);
        }
        return result;
    }

    private static boolean isBreak(String text, int position) {
        WbClass next = WbClassifier.classOf(text.codePointAt(position));
        int directPreviousStart = position - Character.charCount(text.codePointBefore(position));
        WbClass directPrevious = WbClassifier.classOf(text.codePointBefore(position));
        // WB3c : le sélecteur de largeur nulle colle l'emoji suivant.
        if (directPrevious == WbClass.ZWJ && next == WbClass.EXTPICT) {
            return false;
        }
        // WB4 : les attaches se rattachent au caractère précédent ;
        // le précédent effectif est le précédent direct, sauf s'il
        // est lui-même une attache.
        WbClass previous = effectivePrevious(text, directPreviousStart, directPrevious);
        // WB3, WB3a et WB3b : les fins de ligne coupent toujours.
        if (cutByLineEnds(previous, next)) {
            return previous != WbClass.CR || next != WbClass.LF;
        }
        // WB4 : les attaches (extensions, formats, sélecteurs) suivent
        // leur base sans rupture.
        if (next == WbClass.EXTEND || next == WbClass.FORMAT || next == WbClass.ZWJ) {
            return false;
        }
        if (joinsByClass(previous, next)) {
            return false;
        }
        return !joinsThroughMiddleSeparator(text, position, previous, next,
                directPrevious, directPreviousStart);
    }

    /** WB3/WB3a/WB3b : rupture dès qu'une extrémité est une fin de ligne. */
    private static boolean cutByLineEnds(WbClass previous, WbClass next) {
        return previous == WbClass.NEWLINE || previous == WbClass.CR || previous == WbClass.LF
                || next == WbClass.NEWLINE || next == WbClass.CR || next == WbClass.LF;
    }

    /**
     * WB5 à WB13b : groupements par classes — espaces, lettres, chiffres,
     * katakanas et tirets bas.
     */
    private static boolean joinsByClass(WbClass previous, WbClass next) {
        if (joinsSameNature(previous, next)) {
            return true;
        }
        // WB13a et WB13b : tirets bas.
        if ((previous == WbClass.LETTER || previous == WbClass.NUMERIC
                || previous == WbClass.KATAKANA || previous == WbClass.EXTENDNUMLET)
                && next == WbClass.EXTENDNUMLET) {
            return true;
        }
        return previous == WbClass.EXTENDNUMLET && (next == WbClass.LETTER
                || next == WbClass.NUMERIC || next == WbClass.KATAKANA);
    }

    /** WB3d, WB5, WB8 à WB10, WB13 : deux caractères de même nature. */
    private static boolean joinsSameNature(WbClass previous, WbClass next) {
        if (previous == WbClass.WS && next == WbClass.WS) {
            return true;
        }
        if (previous == WbClass.LETTER && next == WbClass.LETTER) {
            return true;
        }
        if (previous == WbClass.NUMERIC && next == WbClass.NUMERIC) {
            return true;
        }
        if (previous == WbClass.LETTER && next == WbClass.NUMERIC) {
            return true;
        }
        if (previous == WbClass.NUMERIC && next == WbClass.LETTER) {
            return true;
        }
        return previous == WbClass.KATAKANA && next == WbClass.KATAKANA;
    }

    /** WB6, WB7, WB11 et WB12 : séparateurs médians entre deux natures. */
    private static boolean joinsThroughMiddleSeparator(String text, int position,
            WbClass previous, WbClass next, WbClass directPrevious, int directPreviousStart) {
        if (previous == WbClass.LETTER && (next == WbClass.MIDLETTER || next == WbClass.MIDNUMLET)) {
            if (lookAhead(text, position) == WbClass.LETTER) {
                return true;
            }
        }
        if ((directPrevious == WbClass.MIDLETTER || directPrevious == WbClass.MIDNUMLET)
                && next == WbClass.LETTER
                && effectiveClassBefore(text, directPreviousStart) == WbClass.LETTER) {
            return true;
        }
        if (previous == WbClass.NUMERIC && (next == WbClass.MIDNUM || next == WbClass.MIDNUMLET)) {
            if (lookAhead(text, position) == WbClass.NUMERIC) {
                return true;
            }
        }
        return (directPrevious == WbClass.MIDNUM || directPrevious == WbClass.MIDNUMLET)
                && next == WbClass.NUMERIC
                && effectiveClassBefore(text, directPreviousStart) == WbClass.NUMERIC;
    }

    /** Le précédent effectif après traversée des attaches (WB4). */
    private static WbClass effectivePrevious(String text, int directPreviousStart,
            WbClass directPrevious) {
        WbClass previous = directPrevious;
        int baseStart = directPreviousStart;
        while (previous == WbClass.EXTEND || previous == WbClass.FORMAT
                || previous == WbClass.ZWJ) {
            if (baseStart == 0) {
                return WbClass.ANY;
            }
            int codePoint = text.codePointBefore(baseStart);
            previous = WbClassifier.classOf(codePoint);
            baseStart -= Character.charCount(codePoint);
        }
        return previous;
    }

    private static WbClass lookAhead(String text, int position) {
        int i = position + Character.charCount(text.codePointAt(position));
        while (i < text.length()) {
            WbClass candidate = WbClassifier.classOf(text.codePointAt(i));
            if (candidate == WbClass.EXTEND || candidate == WbClass.FORMAT
                    || candidate == WbClass.ZWJ) {
                i += Character.charCount(text.codePointAt(i));
                continue;
            }
            return candidate;
        }
        return WbClass.ANY;
    }

    private static WbClass effectiveClassBefore(String text, int position) {
        int i = position;
        while (i > 0) {
            WbClass candidate = WbClassifier.classOf(text.codePointBefore(i));
            if (candidate == WbClass.EXTEND || candidate == WbClass.FORMAT
                    || candidate == WbClass.ZWJ) {
                i -= Character.charCount(text.codePointBefore(i));
                continue;
            }
            return candidate;
        }
        return WbClass.ANY;
    }
}
