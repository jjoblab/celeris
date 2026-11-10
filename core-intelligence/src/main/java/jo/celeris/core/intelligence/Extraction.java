package jo.celeris.core.intelligence;

import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.NodeCategory;
import jo.celeris.core.syntax.OutlineEntry;
import jo.celeris.core.syntax.TreeOutline;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextEdit;

/**
 * L'extraction (B-20) : l'expression sélectionnée devient une
 * déclaration et la sélection se remplace par son nom. Trois variantes
 * — la variable locale avant la ligne de la sélection
 * ({@code var nom = expression ;}), la constante et le champ de classe
 * avant l'accolade fermante de la classe porteuse. Le plan se construit
 * sur l'arbre (méthode porteuse, classe porteuse) et s'applique par la
 * voie de transaction, annulable ; les éditions descendent — les offsets
 * originaux restent valides au fil de l'application séquentielle.
 *
 * <p>Limites documentées : les variantes méthode (flux d'exécution à
 * extraire, paramètres à inférer) et paramètre (signature porteuse à
 * réécrire) attendent l'analyse des flux — elles restent au chantier.
 * Le type de la constante et du champ se lit sur la FORME du littéral
 * par {@link #literalType(String)} — sans littéral lisible, les deux
 * variantes refusent (plan nul, jamais un plan faux) ; la variable
 * locale tombe sur {@code var}, qui n'exige rien. La déclaration locale
 * se pose avant la LIGNE de la sélection : l'instruction multi-lignes
 * qui porterait la sélection au-delà de sa première ligne reste la
 * limite documentée. La validité du nom reste la charge de l'appelant
 * — la constante se crie en majuscules aux joints de casse, le champ
 * garde la casse donnée.</p>
 */
public final class Extraction {

    private Extraction() {
    }

    /**
     * Le plan d'extraction : les éditions du geste, en ordre descendant
     * d'application.
     *
     * @param edits les éditions : remplacement de la sélection puis
     *              insertion de la déclaration
     */
    public record ExtractionPlan(List<TextEdit> edits) {

        /** Constructeur canonique : liste figée, jamais vide. */
        public ExtractionPlan {
            Objects.requireNonNull(edits, "edits");
            if (edits.isEmpty()) {
                throw new IllegalArgumentException("edits must not be empty");
            }
            edits = List.copyOf(edits);
        }
    }

    /**
     * Extrait l'expression sélectionnée en variable locale : la
     * déclaration se pose avant la ligne de la sélection, à son retrait.
     * La variable exige une méthode porteuse — un plan nul refuse hors
     * méthode, jamais un plan faux.
     *
     * @param doc     le document, jamais muté
     * @param analyse l'analyse courante du document
     * @param start   début de la sélection
     * @param end     fin de la sélection (exclusive)
     * @param name    le nom de la variable, casse gardée
     * @return le plan, ou null hors d'une méthode porteuse
     * @throws IllegalArgumentException si la sélection est vide, inversée
     *         ou hors bornes, ou si le nom est blanc
     */
    public static @Nullable ExtractionPlan variable(Document doc, Analysis analyse,
            int start, int end, String name) {
        requireSelection(doc, start, end);
        requireName(name);
        if (enclosing(doc, analyse, start, end, NodeCategory.FUNCTION) == null) {
            return null;
        }
        String expression = doc.subText(start, end).strip();
        int lineStart = doc.lineStart(doc.lineForOffset(start));
        String declaration = indentationOf(doc, lineStart)
                + "var " + name + " = " + expression + ";\n";
        return plan(doc, start, end, name, lineStart, declaration);
    }

    /**
     * Extrait l'expression sélectionnée en constante de classe : la
     * déclaration se pose avant l'accolade fermante de la classe
     * porteuse, un niveau en dessous de son retrait, et le nom se crie
     * en majuscules aux joints de casse (« maLimite » devient
     * « MA_LIMITE »). Le type se lit sur la forme du littéral — un plan
     * nul refuse, jamais un type faux.
     *
     * @param doc     le document, jamais muté
     * @param analyse l'analyse courante du document
     * @param start   début de la sélection
     * @param end     fin de la sélection (exclusive)
     * @param name    le nom de la constante, crié aux joints de casse
     * @return le plan, ou null hors d'une classe porteuse ou sans
     *         littéral lisible
     * @throws IllegalArgumentException si la sélection est vide, inversée
     *         ou hors bornes, ou si le nom est blanc
     */
    public static @Nullable ExtractionPlan constant(Document doc, Analysis analyse,
            int start, int end, String name) {
        return member(doc, analyse, start, end, name, "private static final ", true);
    }

    /**
     * Extrait l'expression sélectionnée en champ de classe : la
     * déclaration se pose avant l'accolade fermante de la classe
     * porteuse, un niveau en dessous de son retrait, casse du nom gardée.
     * Le type se lit sur la forme du littéral — un plan nul refuse,
     * jamais un type faux.
     *
     * @param doc     le document, jamais muté
     * @param analyse l'analyse courante du document
     * @param start   début de la sélection
     * @param end     fin de la sélection (exclusive)
     * @param name    le nom du champ, casse gardée
     * @return le plan, ou null hors d'une classe porteuse ou sans
     *         littéral lisible
     * @throws IllegalArgumentException si la sélection est vide, inversée
     *         ou hors bornes, ou si le nom est blanc
     */
    public static @Nullable ExtractionPlan field(Document doc, Analysis analyse,
            int start, int end, String name) {
        return member(doc, analyse, start, end, name, "private ", false);
    }

    /**
     * Le type lexical d'une expression littérale : la forme du texte
     * décide, jamais le contexte — {@code int} pour l'entier décimal,
     * hexadécimal ou binaire ({@code long} avec le suffixe), {@code
     * double} et {@code float} pour les décimaux à point, {@code
     * boolean}, {@code char} et {@code String} pour les trois autres
     * littéraux. La notation scientifique ne se lit pas.
     *
     * @param expression l'expression extraite
     * @return le type lu, ou null quand la forme ne se lit pas
     */
    public static @Nullable String literalType(String expression) {
        Objects.requireNonNull(expression, "expression");
        String e = expression.strip();
        if (e.equals("true") || e.equals("false")) {
            return "boolean";
        }
        if (quoted(e, '\'')) {
            return "char";
        }
        if (quoted(e, '"')) {
            return "String";
        }
        return numericType(e);
    }

    /** Le plan commun : remplacement de la sélection, puis insertion. */
    private static ExtractionPlan plan(Document doc, int start, int end, String name,
            int insertion, String declaration) {
        TextEdit replacement = new TextEdit(start, doc.subText(start, end), name);
        TextEdit insert = new TextEdit(insertion, "", declaration);
        // Ordre descendant d'application : les offsets originaux restent
        // valides au fil de la transaction séquentielle ; à offset égal,
        // le remplacement précède l'insertion — le nom remplacé reste
        // après la déclaration insérée au même point.
        if (insertion > start) {
            return new ExtractionPlan(List.of(insert, replacement));
        }
        return new ExtractionPlan(List.of(replacement, insert));
    }

    /** Le plan de la constante et du champ : même pose, modificateurs et casse mis à part. */
    private static @Nullable ExtractionPlan member(Document doc, Analysis analyse,
            int start, int end, String name, String modifiers, boolean upperCase) {
        requireSelection(doc, start, end);
        requireName(name);
        OutlineEntry classe = enclosing(doc, analyse, start, end, NodeCategory.TYPE);
        if (classe == null) {
            return null;
        }
        String expression = doc.subText(start, end).strip();
        String type = literalType(expression);
        if (type == null) {
            return null;
        }
        String declaredName = upperCase ? constantName(name) : name;
        int lineStart = doc.lineStart(doc.lineForOffset(classe.endOffset() - 1));
        String declaration = indentationOf(doc, lineStart) + "    " + modifiers
                + type + " " + declaredName + " = " + expression + ";\n";
        return plan(doc, start, end, declaredName, lineStart, declaration);
    }

    /** Vrai quand l'expression est encadrée par le caractère donné. */
    private static boolean quoted(String e, char quote) {
        return e.length() >= 2 && e.charAt(0) == quote && e.charAt(e.length() - 1) == quote;
    }

    /** Le type d'un numérique : préfixé (hexa, binaire) ou décimal, signe ôté. */
    private static @Nullable String numericType(String e) {
        String digits = e.startsWith("-") ? e.substring(1) : e;
        if (digits.isEmpty()) {
            return null;
        }
        if (digits.startsWith("0x") || digits.startsWith("0X")) {
            return radixType(digits, "0123456789abcdefABCDEF_");
        }
        if (digits.startsWith("0b") || digits.startsWith("0B")) {
            return radixType(digits, "01_");
        }
        return decimalType(digits);
    }

    /** Le type d'un entier préfixé : suffixe L mis à part, chiffres du radix seuls. */
    private static @Nullable String radixType(String digits, String alphabet) {
        String body = suffixOf(digits) == '\0' ? digits : digits.substring(0, digits.length() - 1);
        return allOf(body.substring(2), alphabet)
                ? (suffixOf(digits) == 'L' ? "long" : "int") : null;
    }

    /** Le type d'un décimal : point ou pas, suffixe L, f ou d. */
    private static @Nullable String decimalType(String digits) {
        char suffix = suffixOf(digits);
        String body = suffix == '\0' ? digits : digits.substring(0, digits.length() - 1);
        int point = body.indexOf('.');
        if (point < 0) {
            if (!allOf(body, "0123456789_")) {
                return null;
            }
            return switch (suffix) {
                case 'L' -> "long";
                case 'f' -> "float";
                case 'd' -> "double";
                default -> "int";
            };
        }
        boolean leftOk = point == 0 || allOf(body.substring(0, point), "0123456789_");
        boolean rightOk = point == body.length() - 1
                || allOf(body.substring(point + 1), "0123456789_");
        if (!leftOk || !rightOk) {
            return null;
        }
        return switch (suffix) {
            case 'f' -> "float";
            case 'L' -> null;
            default -> "double";
        };
    }

    /**
     * L'entrée du plan qui enclot la sélection : la déclaration la plus
     * intime de la catégorie donnée.
     */
    private static @Nullable OutlineEntry enclosing(Document doc, Analysis analyse,
            int start, int end, NodeCategory category) {
        OutlineEntry best = null;
        for (OutlineEntry entry : TreeOutline.entries(doc, analyse)) {
            if (entry.category() != category) {
                continue;
            }
            if (entry.startOffset() <= start && end <= entry.endOffset()
                    && (best == null || entry.startOffset() >= best.startOffset())) {
                best = entry;
            }
        }
        return best;
    }

    /** Le retrait d'une ligne : espaces et tabulations de tête. */
    private static String indentationOf(Document doc, int lineStart) {
        int end = lineStart;
        while (end < doc.length() && (doc.charAt(end) == ' ' || doc.charAt(end) == '\t')) {
            end++;
        }
        return doc.subText(lineStart, end);
    }

    /** Le nom de constante : majuscules, tirets bas aux joints de casse. */
    private static String constantName(String name) {
        StringBuilder out = new StringBuilder(name.length() + 4);
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c) && i > 0 && out.charAt(out.length() - 1) != '_'
                    && !Character.isUpperCase(name.charAt(i - 1))) {
                out.append('_');
            }
            out.append(Character.toUpperCase(c));
        }
        return out.toString();
    }

    private static void requireSelection(Document doc, int start, int end) {
        if (start < 0 || end > doc.length() || start >= end) {
            throw new IllegalArgumentException(
                    "selection [" + start + ", " + end + ") out of (0, " + doc.length() + ")");
        }
    }

    private static void requireName(String name) {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }

    /** Le suffixe de littéral : L, f ou d — 0 s'il n'y en a pas. */
    private static char suffixOf(String digits) {
        if (digits.length() < 2) {
            return '\0';
        }
        char last = Character.toLowerCase(digits.charAt(digits.length() - 1));
        if (last != 'l' && last != 'f' && last != 'd') {
            return '\0';
        }
        return last == 'l' ? 'L' : last;
    }

    /** Vrai quand la chaîne n'est pas vide et ne porte que les caractères donnés. */
    private static boolean allOf(String s, String alphabet) {
        if (s.isEmpty()) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            if (alphabet.indexOf(s.charAt(i)) < 0) {
                return false;
            }
        }
        return true;
    }
}
