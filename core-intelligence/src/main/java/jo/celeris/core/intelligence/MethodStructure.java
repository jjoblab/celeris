package jo.celeris.core.intelligence;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import jo.celeris.core.syntax.TokenKind;

/**
 * La lecture de la méthode porteuse de B-20 (extraction) et B-21
 * (alignement) : ses bornes — l'offset du corps, les parenthèses de la
 * signature — et le câblage de ses déclarations (les paramètres de la
 * signature et les locaux du corps vivent dans
 * {@link MethodDeclarations}).
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
     * signature et locaux du corps, chacun avec son type lu et ses
     * bornes.
     *
     * @param tokens les jetons de la méthode entière
     * @return les noms déclarés et leur déclaration
     */
    static Map<String, MethodDeclarations.Declaration> declarations(
            List<FlowAnalysis.AbsoluteToken> tokens) {
        Map<String, MethodDeclarations.Declaration> declared = new LinkedHashMap<>();
        int body = bodyBraceIndex(tokens);
        if (body < 0) {
            return declared;
        }
        int open = nameParenIndex(tokens, body);
        if (open >= 0) {
            int close = FlowAnalysis.matchingClose(tokens, open);
            if (close > open) {
                MethodDeclarations.declareSegment(tokens, open + 1, close, declared);
            }
        }
        MethodDeclarations.declareBody(tokens, body + 1, declared);
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
}
