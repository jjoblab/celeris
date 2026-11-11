package jo.celeris.core.intelligence;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import jo.celeris.core.syntax.TokenKind;

/**
 * La lecture des déclarations d'une méthode (B-20 extraction, B-21
 * alignement) : paramètres de la signature et locaux du corps, chacun
 * avec son type lu par la course de type ({@link TypeRun}) et les
 * bornes de son instruction et de son initialisateur. Un paramètre
 * n'est pas une instruction — ses bornes restent à -1. Un local
 * déclaré « var » ne se lit que si son initialisateur est un littéral.
 * Ce qui ne se lit pas n'est pas déclaré — la lecture ne devine
 * jamais.
 */
final class MethodDeclarations {

    /**
     * Une déclaration locale : le nom, le type lu, les index de jetons
     * de l'instruction entière (du premier jeton du type au
     * point-virgule) et de l'initialisateur seul (-1 s'il n'y en a pas,
     * toujours -1 pour un paramètre).
     */
    record Declaration(String name, String type, int statementStart, int statementEnd,
            int initializerStart, int initializerEnd) {
    }

    private MethodDeclarations() {
    }

    /**
     * Les paramètres d'une signature : segments séparés par des virgules
     * de fond — celles des génériques ne comptent pas.
     *
     * @param tokens les jetons de la signature
     * @param from   l'index après la parenthèse ouvrante
     * @param close  l'index de la parenthèse fermante
     * @param declared les déclarations lues, enrichies
     */
    static void declareSegment(List<FlowAnalysis.AbsoluteToken> tokens, int from, int close,
            Map<String, Declaration> declared) {
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
    static void declareBody(List<FlowAnalysis.AbsoluteToken> tokens, int from,
            Map<String, Declaration> declared) {
        int i = from;
        while (i < tokens.size()) {
            Declaration declaration = declarableAt(tokens, i);
            if (declaration != null) {
                declared.putIfAbsent(tokens.get(i).text(), declaration);
                i = declareSiblings(tokens, i, declaration, declared);
                continue;
            }
            i++;
        }
    }

    /** Le nom déclaré d'un segment : sa course de type le précède. */
    private static void declareName(List<FlowAnalysis.AbsoluteToken> tokens, int from, int to,
            Map<String, Declaration> declared) {
        for (int i = from; i < to; i++) {
            Declaration declaration = parameterAt(tokens, i);
            if (declaration != null) {
                declared.putIfAbsent(tokens.get(i).text(), declaration);
                return;
            }
        }
    }

    /**
     * La déclaration d'un paramètre de signature : le type seul se lit,
     * les bornes n'existent pas (un paramètre n'est pas une instruction).
     */
    private static @Nullable Declaration parameterAt(List<FlowAnalysis.AbsoluteToken> tokens,
            int i) {
        if (!FlowAnalysis.isName(tokens.get(i).kind())
                || !TypeRun.followsType(tokens, i)
                || !TypeRun.declaresAfter(tokens, i)) {
            return null;
        }
        String type = refineVar(tokens, i, TypeRun.beforeName(tokens, i));
        if (type == null || type.equals("var")) {
            return null;
        }
        return new Declaration(tokens.get(i).text(), type, -1, -1, -1, -1);
    }

    /** La déclaration du nom à l'index donné, ou null s'il ne se déclare pas. */
    private static @Nullable Declaration declarableAt(List<FlowAnalysis.AbsoluteToken> tokens,
            int i) {
        if (!FlowAnalysis.isName(tokens.get(i).kind())
                || !TypeRun.followsType(tokens, i)
                || !TypeRun.declaresAfter(tokens, i)) {
            return null;
        }
        String type = refineVar(tokens, i, TypeRun.beforeName(tokens, i));
        if (type == null || type.equals("var")) {
            return null;
        }
        int statementEnd = statementEnd(tokens, i);
        int run = TypeRun.runStart(tokens, i);
        if (statementEnd < 0 || run < 0) {
            return null;
        }
        int initializerStart = tokens.get(i + 1).text().equals("=") ? i + 2 : -1;
        int initializerEnd = initializerStart < 0 ? -1
                : initializerEnd(tokens, initializerStart, statementEnd);
        return new Declaration(tokens.get(i).text(), type, run, statementEnd,
                initializerStart, initializerEnd);
    }

    /**
     * Les déclarateurs d'une même instruction : « int a = 1, b = 2 ; ».
     * Rend l'index du point-virgule qui clôt l'instruction.
     */
    private static int declareSiblings(List<FlowAnalysis.AbsoluteToken> tokens, int i,
            Declaration first, Map<String, Declaration> declared) {
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
                declared.putIfAbsent(tokens.get(j + 1).text(),
                        sibling(tokens, j + 1, first));
            }
        }
        return tokens.size();
    }

    /** La déclaration d'un déclarateur frère : même type, mêmes bornes d'instruction. */
    private static Declaration sibling(List<FlowAnalysis.AbsoluteToken> tokens, int i,
            Declaration first) {
        int initializerStart = tokens.get(i + 1).text().equals("=") ? i + 2 : -1;
        int initializerEnd = initializerStart < 0 ? -1
                : initializerEnd(tokens, initializerStart, first.statementEnd());
        return new Declaration(tokens.get(i).text(), first.type(),
                first.statementStart(), first.statementEnd(),
                initializerStart, initializerEnd);
    }

    /** L'index du point-virgule qui clôt l'instruction du nom, ou -1. */
    private static int statementEnd(List<FlowAnalysis.AbsoluteToken> tokens, int i) {
        int depth = 0;
        for (int j = i; j < tokens.size(); j++) {
            TokenKind kind = tokens.get(j).kind();
            if (kind == TokenKind.OPEN_BRACKET) {
                depth++;
            } else if (kind == TokenKind.CLOSE_BRACKET) {
                depth--;
            } else if (depth == 0 && tokens.get(j).text().equals(";")) {
                return j;
            }
        }
        return -1;
    }

    /** L'index du dernier jeton de l'initialisateur, ou un de moins s'il est vide. */
    private static int initializerEnd(List<FlowAnalysis.AbsoluteToken> tokens, int from,
            int statementEnd) {
        int depth = 0;
        for (int j = from; j < statementEnd; j++) {
            TokenKind kind = tokens.get(j).kind();
            if (kind == TokenKind.OPEN_BRACKET) {
                depth++;
            } else if (kind == TokenKind.CLOSE_BRACKET) {
                depth--;
            } else if (depth == 0
                    && (tokens.get(j).text().equals(";") || tokens.get(j).text().equals(","))) {
                return j - 1;
            }
        }
        return statementEnd - 1;
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
