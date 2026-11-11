package jo.celeris.core.intelligence;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

/**
 * L'appariement unifié des candidats de complétion (C6) — UNE seule
 * implémentation pour le filtre, le classement et la surbrillance :
 * les paliers décident qui correspond, les groupes départagent à
 * palier égal, le tri reste stable (à l'intérieur d'un couple palier
 * et groupe, l'ordre du fournisseur est conservé). La signature ne
 * participe jamais au flou : l'appariement travaille sur
 * l'identifiant de tête du libellé, jamais de parenthèse en gras.
 *
 * <p>Les six paliers, du meilleur au pire : exact (sensible à la
 * casse), exact à la casse près, préfixe, préfixe à la casse près,
 * bosses de chameau (initiales de mots et runs contigus), sous-séquence
 * quelconque. Les groupes départagent : le symbole résolu précède le
 * mot-clé, qui précède le gabarit, qui précède le mot du tampon.</p>
 *
 * <p>L'algorithme descend de l'apparieur unifié du dépôt de départ
 * (registre de portage, C6 — Spécifier : l'ancien sert d'oracle) ;
 * les contrats celeris s'appliquent : aucun argument nul.</p>
 */
public final class CompletionMatcher {

    /** Le texte de référence est le préfixe (sensible à la casse). */
    public static final int TIER_EXACT = 0;

    /** Le texte de référence est le préfixe, à la casse près. */
    public static final int TIER_EXACT_CI = 1;

    /** Le texte de référence commence par le préfixe (sensible à la casse). */
    public static final int TIER_PREFIX = 2;

    /** Le texte de référence commence par le préfixe, à la casse près. */
    public static final int TIER_PREFIX_CI = 3;

    /** Bosses de chameau : initiales de mots et runs contigus. */
    public static final int TIER_CAMEL_HUMP = 4;

    /** Sous-séquence quelconque, insensible à la casse. */
    public static final int TIER_SUBSEQUENCE = 5;

    /** Aucune correspondance. */
    public static final int TIER_NONE = 6;

    /** Symbole résolu (méthode, champ, type, constante…). */
    public static final int GROUP_RESOLVED = 0;

    /** Mot-clé du langage. */
    public static final int GROUP_KEYWORD = 1;

    /** Gabarit d'expansion. */
    public static final int GROUP_SNIPPET = 2;

    /** Mot du tampon — pas un symbole résolu. */
    public static final int GROUP_BUFFER_WORD = 3;

    private CompletionMatcher() {
    }

    /**
     * L'identifiant de tête d'un libellé : la suite de caractères
     * d'identifiant au début, étendue aux séparateurs de noms qualifiés
     * ({@code : . / @ #}) quand ils sont suivis d'un caractère
     * d'identifiant — « {@code Text(text: String)} » s'arrête à
     * « {@code Text} », « {@code android:layout_width} » reste entier.
     *
     * @param label le libellé du candidat
     * @return l'identifiant de tête, éventuellement vide
     */
    public static String leadingIdentifier(String label) {
        int end = 0;
        while (end < label.length()) {
            char c = label.charAt(end);
            if (isIdentifierChar(c)) {
                end++;
                continue;
            }
            if (isNameSeparator(c) && end + 1 < label.length()
                    && isIdentifierChar(label.charAt(end + 1))) {
                end++;
                continue;
            }
            break;
        }
        return label.substring(0, end);
    }

    /**
     * Le palier du libellé : l'identifiant de tête d'abord, le
     * libellé entier ensuite pour les paliers exact et préfixe
     * seulement (la signature ne participe jamais au flou), le texte
     * de filtrage en dernier recours — le meilleur des trois.
     *
     * @param label      le libellé du candidat
     * @param filterText le texte de filtrage du fournisseur, vide
     *                   s'il n'en pose pas
     * @param prefix     le préfixe tapé (vide = tout correspond)
     * @return une constante {@code TIER_*}
     */
    public static int tier(String label, String filterText, String prefix) {
        if (prefix.isEmpty()) {
            return TIER_EXACT;
        }
        String ident = leadingIdentifier(label);
        int best = tierOnReference(ident, prefix);
        if (!label.equals(ident)) {
            int labelTier = tierOnReference(label, prefix);
            if (labelTier <= TIER_PREFIX_CI) {
                best = Math.min(best, labelTier);
            }
        }
        if (!filterText.isEmpty()) {
            best = Math.min(best, tierOnReference(filterText, prefix));
        }
        return best;
    }

    /**
     * Le candidat correspond-il au préfixe (palier quelconque) ? Un
     * préfixe vide correspond à tout.
     *
     * @param label      le libellé du candidat
     * @param filterText le texte de filtrage, vide s'il n'y en a pas
     * @param prefix     le préfixe tapé
     * @return vrai sur un palier quelconque
     */
    public static boolean matches(String label, String filterText, String prefix) {
        return tier(label, filterText, prefix) != TIER_NONE;
    }

    /**
     * Le groupe du candidat : le symbole résolu précède le mot-clé,
     * qui précède le gabarit, qui précède le mot du tampon.
     *
     * @param resolved   vrai pour un symbole résolu
     * @param keyword    vrai pour un mot-clé du langage
     * @param snippet    vrai pour un gabarit d'expansion
     * @return une constante {@code GROUP_*}
     */
    public static int group(boolean resolved, boolean keyword, boolean snippet) {
        if (resolved) {
            return GROUP_RESOLVED;
        }
        if (keyword) {
            return GROUP_KEYWORD;
        }
        return snippet ? GROUP_SNIPPET : GROUP_BUFFER_WORD;
    }

    /**
     * Les positions des caractères du texte de référence qui ont fait
     * correspondre le préfixe — exactement ceux-là : le run initial
     * pour un préfixe, les débuts de mots et runs contigus pour les
     * bosses, la marche gloutonne pour la sous-séquence.
     *
     * @param reference le texte de référence (identifiant de tête)
     * @param prefix    le préfixe tapé
     * @return les indices en gras, vide si aucune correspondance
     */
    public static List<Integer> matchPositions(String reference, String prefix) {
        if (prefix.isEmpty()) {
            List<Integer> all = new ArrayList<>();
            for (int i = 0; i < reference.length(); i++) {
                all.add(i);
            }
            return List.copyOf(all);
        }
        if (reference.isEmpty()) {
            return List.of();
        }
        List<Integer> positions = new ArrayList<>();
        if (reference.equals(prefix) || reference.startsWith(prefix)) {
            for (int i = 0; i < prefix.length(); i++) {
                positions.add(i);
            }
            return List.copyOf(positions);
        }
        if (camelHumpWalk(reference, prefix, positions)) {
            return List.copyOf(positions);
        }
        if (isSubsequence(reference, prefix)) {
            int prefixIndex = 0;
            for (int i = 0; i < reference.length() && prefixIndex < prefix.length(); i++) {
                if (Character.toLowerCase(reference.charAt(i))
                        == Character.toLowerCase(prefix.charAt(prefixIndex))) {
                    positions.add(i);
                    prefixIndex++;
                }
            }
        }
        return List.copyOf(positions);
    }

    /** Palier d'un texte de référence unique. */
    private static int tierOnReference(String reference, String prefix) {
        if (reference.isEmpty()) {
            return TIER_NONE;
        }
        if (reference.equals(prefix)) {
            return TIER_EXACT;
        }
        if (reference.equalsIgnoreCase(prefix)) {
            return TIER_EXACT_CI;
        }
        if (reference.startsWith(prefix)) {
            return TIER_PREFIX;
        }
        if (reference.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))) {
            return TIER_PREFIX_CI;
        }
        if (camelHumpSubsequence(reference, prefix)) {
            return TIER_CAMEL_HUMP;
        }
        return isSubsequence(reference, prefix) ? TIER_SUBSEQUENCE : TIER_NONE;
    }

    /**
     * Sous-séquence à bosses de chameau : chaque caractère du préfixe
     * correspond, dans l'ordre, au premier caractère (à toute
     * position), à un début de mot, ou immédiatement après le
     * précédent (run contigu).
     */
    public static boolean camelHumpSubsequence(String text, String prefix) {
        return camelHumpWalk(text, prefix, null);
    }

    /**
     * La marche à bosses, en collectant les positions : le premier
     * caractère est essayé à chacune de ses occurrences (retour
     * arrière limité au premier caractère).
     */
    private static boolean camelHumpWalk(String text, String prefix,
            @Nullable List<Integer> positions) {
        if (prefix.isEmpty()) {
            return true;
        }
        char first = Character.toLowerCase(prefix.charAt(0));
        for (int start = 0; start < text.length(); start++) {
            if (Character.toLowerCase(text.charAt(start)) != first) {
                continue;
            }
            List<Integer> walk = positions != null ? new ArrayList<>() : null;
            if (camelHumpFrom(text, prefix, start, walk)) {
                if (positions != null) {
                    positions.clear();
                    positions.addAll(walk);
                }
                return true;
            }
        }
        return false;
    }

    /** La marche gloutonne depuis la position du premier caractère. */
    private static boolean camelHumpFrom(String text, String prefix, int start,
            @Nullable List<Integer> positions) {
        if (positions != null) {
            positions.add(start);
        }
        int prefixIndex = 1;
        int lastPos = start;
        for (int i = start + 1; i < text.length() && prefixIndex < prefix.length(); i++) {
            char tc = text.charAt(i);
            char pc = prefix.charAt(prefixIndex);
            if (Character.toLowerCase(tc) == Character.toLowerCase(pc)
                    && (isWordStart(text, i) || i == lastPos + 1)) {
                if (positions != null) {
                    positions.add(i);
                }
                prefixIndex++;
                lastPos = i;
            }
        }
        return prefixIndex == prefix.length();
    }

    /** Début de mot : majuscule, début du texte, ou après un séparateur ou une espace. */
    private static boolean isWordStart(String text, int index) {
        char c = text.charAt(index);
        if (Character.isUpperCase(c) || index == 0) {
            return true;
        }
        char prev = text.charAt(index - 1);
        return prev == '_' || prev == '.' || prev == ':' || prev == '/'
                || prev == '@' || prev == '#' || Character.isWhitespace(prev);
    }

    /** Sous-séquence quelconque, insensible à la casse. */
    private static boolean isSubsequence(String text, String prefix) {
        int prefixIndex = 0;
        for (int i = 0; i < text.length() && prefixIndex < prefix.length(); i++) {
            if (Character.toLowerCase(text.charAt(i))
                    == Character.toLowerCase(prefix.charAt(prefixIndex))) {
                prefixIndex++;
            }
        }
        return prefixIndex == prefix.length();
    }

    /** Vrai pour un caractère d'identifiant : lettre, chiffre, trait bas ou dollar. */
    private static boolean isIdentifierChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }

    /** Vrai pour un séparateur de noms qualifiés : deux-points, point, barre, arobase, dièse. */
    private static boolean isNameSeparator(char c) {
        return c == ':' || c == '.' || c == '/' || c == '@' || c == '#';
    }
}
