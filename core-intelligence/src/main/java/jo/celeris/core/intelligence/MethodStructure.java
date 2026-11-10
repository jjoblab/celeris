package jo.celeris.core.intelligence;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import jo.celeris.core.syntax.TokenKind;

/**
 * La lecture de la méthode porteuse de B-20 : ses bornes (corps,
 * signature) et ses déclarations — paramètres de la signature et locaux
 * du corps, chacun avec son type lu par la course de type
 * ({@link TypeRun}). Un local déclaré « var » ne se lit que si son
 * initialisateur est un littéral. Ce qui ne se lit pas n'est pas déclaré
 * — la lecture ne devine jamais.
 */
final class MethodStructure {

    private MethodStructure() {
    }

    /**
     * L'offset de la première accolade ouvrante du bloc de corps, ou -1
     * quand la méthode n'en porte pas.
     *
     * @param tokens les jetons de la méthode entière
     * @return l'offset du « { » du corps, ou -1
     */
    static int bodyOffset(List<FlowAnalysis.AbsoluteToken> tokens) {
        int body = bodyBraceIndex(tokens);
        return body < 0 ? -1 : tokens.get(body).offset();
    }

    /**
     * Les offsets des parenthèses de la signature : l'ouvrante qui suit
     * le nom déclaré et sa fermante appariée.
     *
     * @param tokens les jetons de la méthode entière
     * @return { ouvrante, fermante } en offsets absolus, ou null si
     *         la signature ne se lit pas
     */
    static int @Nullable [] signatureParens(List<FlowAnalysis.AbsoluteToken> tokens) {
        int body = bodyBraceIndex(tokens);
        if (body < 0) {
            return null;
        }
        int open = nameParenIndex(tokens, body);
        if (open < 0) {
            return null;
        }
        int close = FlowAnalysis.matchingClose(tokens, open);
        if (close <= open) {
            return null;
        }
        return new int[]{tokens.get(open).offset(), tokens.get(close).offset()};
    }

    /**
     * Les déclarations de la méthode porteuse : paramètres de la
     * signature et locaux du corps, chacun avec son type lu.
     *
     * @param tokens les jetons de la méthode entière
     * @return les noms déclarés et leur type
     */
    static Map<String, String> declarations(List<FlowAnalysis.AbsoluteToken> tokens) {
        Map<String, String> declared = new LinkedHashMap<>();
        int body = bodyBraceIndex(tokens);
        if (body < 0) {
            return declared;
        }
        int open = nameParenIndex(tokens, body);
        if (open >= 0) {
            int close = FlowAnalysis.matchingClose(tokens, open);
            if (close > open) {
                declareSegment(tokens, open + 1, close, declared);
            }
        }
        declareBody(tokens, body + 1, declared);
        return declared;
    }

    /** La première accolade ouvrante du bloc de corps. */
    private static int bodyBraceIndex(List<FlowAnalysis.AbsoluteToken> tokens) {
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).kind() == TokenKind.OPEN_BRACKET
                    && tokens.get(i).text().equals("{")) {
                return i;
            }
        }
        return -1;
    }

    /** La parenthèse des paramètres : celle qui suit le nom déclaré. */
    private static int nameParenIndex(List<FlowAnalysis.AbsoluteToken> tokens, int body) {
        for (int i = body - 1; i >= 0; i--) {
            if (FlowAnalysis.isName(tokens.get(i).kind()) && i + 1 < tokens.size()
                    && tokens.get(i + 1).text().equals("(")) {
                return i + 1;
            }
        }
        return -1;
    }

    /**
     * Les paramètres d'une signature : segments séparés par des virgules
     * de fond — celles des génériques ne comptent pas.
     */
    private static void declareSegment(List<FlowAnalysis.AbsoluteToken> tokens, int from,
            int close, Map<String, String> declared) {
        int segmentStart = from;
        int depth = 0;
        int angles = 0;
        for (int i = from; i <= close; i++) {
            if (i == close) {
                declareName(tokens, segmentStart, i, declared);
                break;
            }
            TokenKind kind = tokens.get(i).kind();
            String text = tokens.get(i).text();
            if (kind == TokenKind.OPEN_BRACKET) {
                depth++;
            } else if (kind == TokenKind.CLOSE_BRACKET) {
                depth--;
            } else if (text.equals("<")) {
                angles++;
            } else if (text.equals(">")) {
                angles--;
            } else if (text.equals(",") && depth == 0 && angles == 0) {
                declareName(tokens, segmentStart, i, declared);
                segmentStart = i + 1;
            }
        }
    }

    /** Le corps : chaque nom déclaré se lit, ses déclarateurs frères suivent. */
    private static void declareBody(List<FlowAnalysis.AbsoluteToken> tokens, int from,
            Map<String, String> declared) {
        int i = from;
        while (i < tokens.size()) {
            String type = declarableAt(tokens, i);
            if (type != null) {
                declared.putIfAbsent(tokens.get(i).text(), type);
                i = siblingDeclarators(tokens, i, type, declared);
                continue;
            }
            i++;
        }
    }

    /** Le type du nom déclaré à l'index donné, ou null s'il ne se déclare pas. */
    private static @Nullable String declarableAt(List<FlowAnalysis.AbsoluteToken> tokens, int i) {
        if (!FlowAnalysis.isName(tokens.get(i).kind())
                || !TypeRun.followsType(tokens, i)
                || !TypeRun.declaresAfter(tokens, i)) {
            return null;
        }
        return refineVar(tokens, i, TypeRun.beforeName(tokens, i));
    }

    /**
     * Les déclarateurs d'une même instruction : « int a = 1, b = 2 ; ».
     * Rend l'index du point-virgule qui clôt l'instruction.
     */
    private static int siblingDeclarators(List<FlowAnalysis.AbsoluteToken> tokens, int i,
            String type, Map<String, String> declared) {
        int depth = 0;
        for (int j = i + 1; j < tokens.size(); j++) {
            TokenKind kind = tokens.get(j).kind();
            if (kind == TokenKind.OPEN_BRACKET) {
                depth++;
            } else if (kind == TokenKind.CLOSE_BRACKET) {
                depth--;
            } else if (depth == 0 && tokens.get(j).text().equals(";")) {
                return j;
            } else if (depth == 0 && tokens.get(j).text().equals(",")
                    && j + 1 < tokens.size() && FlowAnalysis.isName(tokens.get(j + 1).kind())
                    && TypeRun.declaresAfter(tokens, j + 1)) {
                declared.putIfAbsent(tokens.get(j + 1).text(), type);
            }
        }
        return tokens.size();
    }

    /** Le nom déclaré d'un segment : sa course de type le précède. */
    private static void declareName(List<FlowAnalysis.AbsoluteToken> tokens, int from, int to,
            Map<String, String> declared) {
        for (int i = from; i < to; i++) {
            String type = declarableAt(tokens, i);
            if (type != null) {
                declared.putIfAbsent(tokens.get(i).text(), type);
                return;
            }
        }
    }

    /**
     * Le local déclaré « var » se lit sur son initialisateur littéral :
     * « var x = 40 ; » lit int, « var x = calcul() ; » reste illisible.
     */
    private static @Nullable String refineVar(List<FlowAnalysis.AbsoluteToken> tokens, int i,
            @Nullable String type) {
        if (type == null || !type.equals("var")
                || i + 1 >= tokens.size() || !tokens.get(i + 1).text().equals("=")) {
            return type;
        }
        int depth = 0;
        for (int j = i + 2; j < tokens.size(); j++) {
            TokenKind kind = tokens.get(j).kind();
            if (kind == TokenKind.OPEN_BRACKET) {
                depth++;
            } else if (kind == TokenKind.CLOSE_BRACKET) {
                depth--;
            } else if (depth == 0
                    && (tokens.get(j).text().equals(";") || tokens.get(j).text().equals(","))) {
                String read = Extraction.literalType(joinTokens(tokens, i + 2, j));
                return read == null ? type : read;
            }
        }
        return type;
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
}
