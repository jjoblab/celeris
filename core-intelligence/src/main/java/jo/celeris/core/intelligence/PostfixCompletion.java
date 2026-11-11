package jo.celeris.core.intelligence;

import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextEdit;

/**
 * La complétion postfixe (A-23) : l'expression devant le point se
 * consomme dans son gabarit — {@code expr.if} devient
 * {@code if (expr) {}} en une seule édition, l'expression ne se
 * matérialise jamais en préfixe (le piège hérité du lot L0 : le
 * dépôt de départ dupliquait l'expression, l'originale restait et la
 * copie naissait dans le gabarit). Les gabarits suivent la syntaxe
 * Java ; l'expression se lit en remontant les identifiants, les
 * points et les appels appariés — le mot incomplet refuse (le geste
 * arrive sur invocation, jamais sur frappe partielle).
 */
public final class PostfixCompletion {

    private PostfixCompletion() {
    }

    /**
     * Le geste postfixe : l'édition qui remplace
     * {@code expr.motif} par le gabarit, et la position du curseur
     * après l'application.
     *
     * @param edit  l'édition : l'expression, le point et le motif
     *              remplacés par le gabarit
     * @param caret la position du curseur après l'application, dans
     *              le texte inséré
     */
    public record PostfixEdit(TextEdit edit, int caret) {

        /** Constructeur canonique : le curseur vit dans le texte inséré. */
        public PostfixEdit {
            Objects.requireNonNull(edit, "edit");
            int fin = edit.start() + edit.inserted().length();
            if (caret < edit.start() || caret > fin) {
                throw new IllegalArgumentException(
                        "caret " + caret + " out of inserted [" + edit.start() + ", " + fin + ")");
            }
        }
    }

    /** Les motifs reconnus, dans l'ordre du dictionnaire. */
    public static List<String> motifs() {
        return List.of("for", "if", "not", "notnull", "null", "return", "while");
    }

    /**
     * Étend le geste postfixe au curseur : l'expression devant le
     * point se consomme dans son gabarit.
     *
     * @param doc   le document, jamais muté
     * @param caret la position du curseur, juste après le motif
     * @return le geste, ou null sur un motif inconnu, un mot sans
     *         point, une expression vide, littérale ou mal terminée
     * @throws IllegalArgumentException si le curseur est hors bornes
     */
    public static @Nullable PostfixEdit expand(Document doc, int caret) {
        if (caret < 0 || caret > doc.length()) {
            throw new IllegalArgumentException(
                    "caret " + caret + " out of (0, " + doc.length() + ")");
        }
        int[] bornes = wordBefore(doc, caret);
        if (bornes == null || !motifs().contains(doc.subText(bornes[0], bornes[1]))) {
            return null;
        }
        int dot = bornes[0] - 1;
        if (dot < 0 || doc.charAt(dot) != '.') {
            return null;
        }
        int start = expressionStart(doc, dot);
        if (start < 0 || !estExpression(doc.subText(start, dot))) {
            return null;
        }
        String expr = doc.subText(start, dot);
        String gabarit = gabarit(doc.subText(bornes[0], caret), expr);
        TextEdit edit = new TextEdit(start, doc.subText(start, caret), gabarit);
        boolean auBout = gabarit.equals("!" + expr) || gabarit.equals("return " + expr + ";");
        return new PostfixEdit(edit,
                auBout ? edit.start() + gabarit.length() : edit.start() + gabarit.length() - 1);
    }

    /** Le gabarit du motif : l'expression s'y consomme entière. */
    private static String gabarit(String motif, String expr) {
        return switch (motif) {
            case "if" -> "if (" + expr + ") {}";
            case "while" -> "while (" + expr + ") {}";
            case "for" -> "for (var element : " + expr + ") {}";
            case "not" -> "!" + expr;
            case "null" -> "if (" + expr + " == null) {}";
            case "notnull" -> "if (" + expr + " != null) {}";
            default -> "return " + expr + ";";
        };
    }

    /** Le mot complet devant le curseur, ou null si vide. */
    private static int @Nullable [] wordBefore(Document doc, int caret) {
        int start = caret;
        while (start > 0 && estDeMot(doc.charAt(start - 1))) {
            start--;
        }
        return start == caret ? null : new int[]{start, caret};
    }

    /**
     * Le début de l'expression devant le point : la remontée des
     * identifiants, des points et des appels appariés — le crochet
     * non apparié ou le délimiteur arrête la course.
     */
    private static int expressionStart(Document doc, int dot) {
        int position = dot;
        int depth = 0;
        while (position > 0) {
            char c = doc.charAt(position - 1);
            if (Character.isLetterOrDigit(c) || c == '_' || c == '.') {
                position--;
            } else if (c == ')' || c == ']') {
                depth++;
                position--;
            } else if ((c == '(' || c == '[') && depth > 0) {
                depth--;
                position--;
            } else {
                break;
            }
        }
        return depth == 0 ? position : -1;
    }

    /** Vrai quand le texte commence par une lettre et ne finit pas par un point. */
    private static boolean estExpression(String texte) {
        if (texte.isEmpty()) {
            return false;
        }
        char premiere = texte.charAt(0);
        return (Character.isLetter(premiere) || premiere == '_')
                && texte.charAt(texte.length() - 1) != '.';
    }

    /** Vrai quand le caractère compose un mot : lettre, chiffre ou trait bas. */
    private static boolean estDeMot(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}
