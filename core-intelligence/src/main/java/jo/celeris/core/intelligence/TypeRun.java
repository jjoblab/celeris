package jo.celeris.core.intelligence;

import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import jo.celeris.core.syntax.TokenKind;

/**
 * La course de type de B-20 : le type déclaré avant un nom, lu en une
 * course arrière bornée — mots de type, crochets et arguments génériques
 * équilibrés, jamais au travers d'une virgule de déclarateur, modifieurs
 * ôtés. Ce qui ne se referme pas rend null, jamais un type faux.
 */
final class TypeRun {

    /** Les mots qui portent un type ou un modifieur de déclaration. */
    private static final Set<String> TYPE_WORDS = Set.of(
            "var", "int", "long", "short", "byte", "char", "float", "double",
            "boolean", "final");

    /** Les modifieurs ôtés du type déclaré. */
    private static final Set<String> MODIFIERS = Set.of(
            "final", "private", "protected", "public", "static", "transient",
            "volatile", "abstract", "synchronized", "native", "strictfp",
            "default", "sealed");

    /** Les textes qui prolongent une course de type en arrière. */
    private static final Set<String> TYPE_LINKS = Set.of(
            ".", ",", "<", ">", "[", "]", "?", "&");

    private TypeRun() {
    }

    /**
     * Le type déclaré avant un nom : la course arrière des mots de type,
     * crochets et génériques équilibrés — bornée à gauche de la plage,
     * modifieurs ôtés. La course s'arrête à la virgule de déclarateur
     * hors génériques.
     *
     * @param tokens les jetons de la déclaration
     * @param i      l'index du nom déclaré
     * @return le type lu, ou null quand la course ne se referme pas
     */
    static @Nullable String beforeName(List<FlowAnalysis.AbsoluteToken> tokens, int i) {
        int angles = 0;
        int end = i;
        int j = i - 1;
        while (j >= 0) {
            FlowAnalysis.AbsoluteToken token = tokens.get(j);
            boolean word = isTypeWord(token);
            boolean link = isTypeLink(token);
            if (token.text().equals(">")) {
                angles++;
            } else if (token.text().equals("<") && angles > 0) {
                angles--;
            } else if (token.text().equals(",") && angles == 0) {
                break;
            } else if (!word && !link) {
                break;
            }
            end = j;
            j--;
        }
        if (end >= i || angles != 0 || !startsType(tokens.get(end))) {
            return null;
        }
        String type = joinTokens(tokens, end, i);
        while (startsWithModifier(type)) {
            type = type.substring(type.indexOf(' ') + 1);
        }
        return type.isBlank() ? null : type;
    }

    /** Vrai quand le nom est suivi d'un déclarateur : = ; : , ou ). */
    static boolean declaresAfter(List<FlowAnalysis.AbsoluteToken> tokens, int i) {
        if (i + 1 >= tokens.size()) {
            return false;
        }
        String after = tokens.get(i + 1).text();
        return after.equals("=") || after.equals(";") || after.equals(":")
                || after.equals(",") || after.equals(")");
    }

    /** Vrai quand le jeton précédent peut clore un type : nom, mot de type, chevron, crochet. */
    static boolean followsType(List<FlowAnalysis.AbsoluteToken> tokens, int i) {
        if (i <= 0) {
            return false;
        }
        FlowAnalysis.AbsoluteToken previous = tokens.get(i - 1);
        if (FlowAnalysis.isName(previous.kind())) {
            return true;
        }
        if (previous.kind() == TokenKind.KEYWORD && TYPE_WORDS.contains(previous.text())) {
            return true;
        }
        return previous.text().equals(">") || previous.text().equals("]");
    }

    /** Vrai quand le jeton porte un mot de type ou un nom. */
    private static boolean isTypeWord(FlowAnalysis.AbsoluteToken token) {
        if (FlowAnalysis.isName(token.kind())) {
            return true;
        }
        if (token.kind() != TokenKind.KEYWORD) {
            return false;
        }
        return TYPE_WORDS.contains(token.text())
                || token.text().equals("extends") || token.text().equals("super");
    }

    /** Vrai quand le jeton lie une course de type : point, virgule, chevrons, crochets. */
    private static boolean isTypeLink(FlowAnalysis.AbsoluteToken token) {
        if (!TYPE_LINKS.contains(token.text())) {
            return false;
        }
        TokenKind kind = token.kind();
        return kind == TokenKind.OPERATOR
                || kind == TokenKind.PUNCTUATION
                || ((token.text().equals("[") || token.text().equals("]"))
                        && (kind == TokenKind.OPEN_BRACKET
                                || kind == TokenKind.CLOSE_BRACKET));
    }

    /** Vrai quand la course commence par un mot qui porte un type. */
    private static boolean startsType(FlowAnalysis.AbsoluteToken first) {
        if (FlowAnalysis.isName(first.kind())) {
            return true;
        }
        return first.kind() == TokenKind.KEYWORD && TYPE_WORDS.contains(first.text());
    }

    /** Les textes de jetons, joints d'un espace. */
    private static String joinTokens(List<FlowAnalysis.AbsoluteToken> tokens, int from, int to) {
        StringBuilder text = new StringBuilder();
        for (int i = from; i < to; i++) {
            if (!text.isEmpty()) {
                text.append(' ');
            }
            text.append(tokens.get(i).text());
        }
        return text.toString();
    }

    private static boolean startsWithModifier(String type) {
        int space = type.indexOf(' ');
        return space > 0 && MODIFIERS.contains(type.substring(0, space));
    }
}
