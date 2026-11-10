package jo.celeris.core.text;

import java.util.ArrayList;
import java.util.List;

/**
 * Segmentation en bosses de chameau (A-04) : les frontières de
 * {@link Word} (UAX #29) raffinées par les transitions de casse et de
 * nature. Trois règles coupent l'intérieur d'un mot UAX #29 :
 * la minuscule vers la majuscule (« monCompteur » coupe avant le C),
 * la fin de course de majuscules (« HTTPServer » coupe avant le S
 * final — la course « HTTP » se termine sur sa dernière lettre), et
 * le passage lettre vers chiffre ou chiffre vers lettre
 * (« abc123def » se coupe en trois mots — seuls les chiffres du
 * script commun bossent, les chiffres des autres scripts restent
 * des chiffres de mot).
 *
 * <p>La base UAX #29 reste entière : les espaces, la ponctuation et
 * les fins de ligne coupent comme avant, les séparateurs médians et
 * les tirets bas prolongent le mot (« mot_de_passe » et « 3.14 »
 * restent entiers). Les écritures sans casse — CJK, arabe, hébreu,
 * kana — ne changent d'un cran : la bosse ne vit que dans les
 * écritures casées, et la sélection comme le déplacement par mots
 * héritent du même raffinement. Une bosse ne coupe JAMAIS deux mots
 * UAX #29 : ses règles n'unissent que des caractères de même nature
 * que la norme unit déjà, la frontière ne peut donc naître qu'à
 * l'intérieur d'un mot.</p>
 *
 * <p>La fin de course de majuscules se lit sur le caractère qui suit
 * la frontière candidate : dans une fenêtre tronquée (le regard du
 * déplacement ne matérialise jamais le texte entier), le caractère
 * absent ne confirme rien — la frontière ne se prononce pas, la
 * fenêtre croissante de l'appelant reprendra avec plus de contexte.
 * Fil d'exécution : utilitaire pur.</p>
 */
public final class CamelWord {

    private CamelWord() {
    }

    /**
     * Renvoie la première frontière de bossage strictement après la
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
            throw new IndexOutOfBoundsException(
                    "from " + from + " out of [0, " + length + "]");
        }
        if (from == length) {
            return length;
        }
        int wordEnd = Word.nextBoundary(text, from);
        int i = from + Character.charCount(text.codePointAt(from));
        while (i < wordEnd) {
            if (isHumpBoundary(text, i)) {
                return i;
            }
            i += Character.charCount(text.codePointAt(i));
        }
        return wordEnd;
    }

    /**
     * Renvoie la première frontière — UAX #29 ou bossage — strictement
     * avant la position donnée.
     *
     * <p>Le mot UAX #29 qui porte la position précédente borne le recul
     * : une bosse ne coupe jamais deux mots, le recul s'arrête à la
     * naissance du mot.</p>
     *
     * @param text texte segmenté
     * @param from position de départ (frontière cherchée avant elle)
     * @return décalage de la frontière précédente, ou 0
     * @throws IndexOutOfBoundsException si la position est hors bornes
     */
    public static int previousBoundary(String text, int from) {
        int length = text.length();
        if (from < 0 || from > length) {
            throw new IndexOutOfBoundsException(
                    "from " + from + " out of [0, " + length + "]");
        }
        if (from == 0) {
            return 0;
        }
        int wordStart = 0;
        for (int boundary : Word.boundaries(text)) {
            if (boundary >= from) {
                break;
            }
            wordStart = boundary;
        }
        int i = from;
        while (i > wordStart + 1) {
            int start = i - Character.charCount(text.codePointBefore(i));
            if (isHumpBoundary(text, start)) {
                return start;
            }
            i = start;
        }
        return wordStart;
    }

    /**
     * Renvoie toutes les frontières — UAX #29 et bossages compris — du
     * texte donné.
     *
     * @param text texte segmenté
     * @return liste croissante commençant à 0 et finissant à la longueur
     */
    public static List<Integer> boundaries(String text) {
        List<Integer> wordBoundaries = Word.boundaries(text);
        List<Integer> result = new ArrayList<>(wordBoundaries.size() + 8);
        result.add(0);
        for (int k = 0; k < wordBoundaries.size() - 1; k++) {
            int start = wordBoundaries.get(k);
            int end = wordBoundaries.get(k + 1);
            int i = start + Character.charCount(text.codePointAt(start));
            while (i < end) {
                if (isHumpBoundary(text, i)) {
                    result.add(i);
                }
                i += Character.charCount(text.codePointAt(i));
            }
            result.add(end);
        }
        return result;
    }

    /**
     * La frontière de bossage à la position donnée : les trois règles
     * ne regardent que les caractères adjacents, jamais au-delà — la
     * fin de course de majuscules exige que le caractère qui suit soit
     * présent dans la fenêtre.
     */
    private static boolean isHumpBoundary(String text, int position) {
        char previous = text.charAt(position - 1);
        char current = text.charAt(position);
        // Minuscule vers majuscule : « mon | Compteur ».
        if (Character.isLowerCase(previous) && Character.isUpperCase(current)) {
            return true;
        }
        // Chiffre du script commun vers lettre et lettre vers chiffre
        // du script commun : « abc | 123 | def » — les chiffres des
        // autres scripts ne bossent pas, ils restent des chiffres de mot.
        if (isAsciiDigit(previous) != isAsciiDigit(current)
                && (Character.isLetter(previous) || Character.isLetter(current))) {
            return true;
        }
        // Fin de course de majuscules : « HTTP | Server » — la course
        // se termine sur sa dernière majuscule, la preuve vit sur le
        // caractère qui suit.
        return Character.isUpperCase(previous) && Character.isUpperCase(current)
                && position + 1 < text.length()
                && Character.isLowerCase(text.charAt(position + 1));
    }

    /** Le chiffre du script commun : les autres scripts numériques ne bossent pas. */
    private static boolean isAsciiDigit(char c) {
        return c >= '0' && c <= '9';
    }
}
