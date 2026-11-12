package jo.celeris.core.syntax.clike;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import jo.celeris.core.syntax.LanguageLexer;
import jo.celeris.core.syntax.LexState;
import jo.celeris.core.syntax.LexedLine;
import jo.celeris.core.syntax.Token;
import jo.celeris.core.syntax.TokenKind;

/**
 * Le lexeur de la famille C (M7.6, H-10) : le socle partagé des
 * analyseurs écrits — les mots, les littéraux, les chaînes délimitées
 * et triples, les gabarits, les commentaires de bloc plats ou nichés,
 * les annotations, la convention de casse (majuscule initiale non
 * suivie d'un point = type, tout en majuscules = constante). Tout ce
 * qui distingue une langue de l'autre vit dans son
 * {@link CLikeDialect} ; une chaîne non fermée est un jeton ERROR,
 * jamais une exception.
 *
 * <p>Usage à fil unique (ADR-007) : le moteur appelle {@link #lex}
 * ligne après ligne, séquentiellement — l'état de sortie de la
 * dernière ligne vit dans le champ privé, réarmé à chaque appel.</p>
 */
final class CLikeLexer implements LanguageLexer {

    /** Hors de toute construction multi-lignes. */
    enum SimpleState implements LexState {
        /** Hors de tout construction multi-lignes. */
        NORMAL
    }

    /**
     * Dans un commentaire de bloc, à la profondeur donnée — la
     * profondeur ne compte que pour les dialectes qui nichent ; le
     * plat la garde à un.
     */
    record InBlockComment(int depth, boolean documentation) implements LexState {
    }

    /** Dans une chaîne triple du délimiteur donné. */
    record InTripleString(char delimiter) implements LexState {
    }

    private final CLikeDialect dialect;

    private LexState exitState = SimpleState.NORMAL;

    CLikeLexer(CLikeDialect dialect) {
        this.dialect = dialect;
    }

    @Override
    public LexState initialState() {
        return SimpleState.NORMAL;
    }

    @Override
    public LexedLine lex(CharSequence text, LexState entry) {
        exitState = SimpleState.NORMAL;
        List<Token> tokens = new ArrayList<>();
        int resume = 0;
        if (entry instanceof InBlockComment comment) {
            resume = dialect.nestedBlockComments()
                    ? resumeNestedComment(text, tokens, comment.depth(),
                            comment.documentation())
                    : resumeFlatComment(text, tokens, comment.documentation());
        } else if (entry instanceof InTripleString triple) {
            CLikeStrings.Outcome outcome = CLikeStrings.resumeTriple(dialect, text,
                    tokens, triple.delimiter());
            exitState = outcome.exitState();
            resume = outcome.resume();
        }
        int n = text.length();
        int i = resume;
        while (i < n) {
            i = lexOne(text, tokens, i, n);
        }
        Token[] line = tokens.toArray(new Token[0]);
        if (dialect.weldTokens()) {
            line = weld(line);
        }
        return new LexedLine(line, exitState);
    }

    /** Le commentaire plat continue : la première fermeture clôt tout. */
    private int resumeFlatComment(CharSequence text, List<Token> tokens,
            boolean documentation) {
        int close = find(text, "*/", 0);
        int end = close < 0 ? text.length() : close + 2;
        tokens.add(new Token(documentation ? TokenKind.DOC : TokenKind.COMMENT,
                0, end));
        exitState = close < 0
                ? new InBlockComment(1, documentation) : SimpleState.NORMAL;
        return end;
    }

    /** Le commentaire niché continue ; la reprise suit la fermeture complète. */
    private int resumeNestedComment(CharSequence text, List<Token> tokens,
            int depth, boolean documentation) {
        int n = text.length();
        int current = depth;
        int start = 0;
        TokenKind sort = documentation ? TokenKind.DOC : TokenKind.COMMENT;
        int i = 0;
        while (i < n) {
            if (startsWith(text, i, "/*")) {
                current++;
                i += 2;
                continue;
            }
            if (startsWith(text, i, "*/")) {
                current--;
                i += 2;
                if (current <= 0) {
                    tokens.add(new Token(sort, start, i));
                    exitState = SimpleState.NORMAL;
                    return i;
                }
                continue;
            }
            i++;
        }
        tokens.add(new Token(sort, start, n));
        exitState = new InBlockComment(current, documentation);
        return n;
    }

    private int lexOne(CharSequence text, List<Token> tokens, int i, int n) {
        char c = text.charAt(i);
        if (Character.isWhitespace(c)) {
            return i + 1;
        }
        if (c == '/') {
            int after = lexComment(text, tokens, i, n);
            if (after >= 0) {
                return after;
            }
        }
        if (c == '"' || c == '\'') {
            CLikeStrings.Outcome outcome = CLikeStrings.delimited(dialect, text,
                    tokens, i, n, c);
            exitState = outcome.exitState();
            return outcome.resume();
        }
        if (c == '@') {
            return lexAnnotation(text, tokens, i, n);
        }
        if (JavaNombres.ouvre(c, text, i, n)) {
            return JavaNombres.lex(text, tokens, i, n);
        }
        if (Character.isJavaIdentifierStart(c)) {
            return lexWord(text, tokens, i, n);
        }
        return dialect.lexSign(text, tokens, i, n);
    }

    /** Renvoie -1 quand ce n'est PAS un commentaire (le «&#x2f;» retombera sur l'opérateur). */
    private int lexComment(CharSequence text, List<Token> tokens, int i, int n) {
        if (i + 1 >= n) {
            return -1;
        }
        char next = text.charAt(i + 1);
        if (next == '/') {
            tokens.add(new Token(TokenKind.COMMENT, i, n));
            return n;
        }
        if (next != '*') {
            return -1;
        }
        return dialect.nestedBlockComments()
                ? openNestedComment(text, tokens, i, n)
                : openFlatComment(text, tokens, i, n);
    }

    /** Le commentaire plat s'ouvre : la première fermeture clôt tout. */
    private int openFlatComment(CharSequence text, List<Token> tokens, int i, int n) {
        boolean doc = isDocOpening(text, i, n);
        int close = find(text, "*/", i + 2);
        int end = close < 0 ? n : close + 2;
        tokens.add(new Token(doc ? TokenKind.DOC : TokenKind.COMMENT, i, end));
        exitState = close < 0
                ? new InBlockComment(1, doc) : SimpleState.NORMAL;
        return end;
    }

    /** Le commentaire niché s'ouvre : la profondeur traverse les lignes par état. */
    private int openNestedComment(CharSequence text, List<Token> tokens, int i, int n) {
        boolean doc = isDocOpening(text, i, n);
        int depth = 1;
        int j = i + 2;
        while (j < n && depth > 0) {
            if (startsWith(text, j, "/*")) {
                depth++;
                j += 2;
                continue;
            }
            if (startsWith(text, j, "*/")) {
                depth--;
                j += 2;
                continue;
            }
            j++;
        }
        if (depth == 0) {
            tokens.add(new Token(doc ? TokenKind.DOC : TokenKind.COMMENT, i, j));
            exitState = SimpleState.NORMAL;
            return j;
        }
        tokens.add(new Token(doc ? TokenKind.DOC : TokenKind.COMMENT, i, n));
        exitState = new InBlockComment(depth, doc);
        return n;
    }

    /** La documentation s'ouvre au double astérisque qui n'est pas une fermeture. */
    private static boolean isDocOpening(CharSequence text, int i, int n) {
        return i + 2 < n && text.charAt(i + 2) == '*'
                && !(i + 3 < n && text.charAt(i + 3) == '/');
    }

    private static int lexAnnotation(CharSequence text, List<Token> tokens,
            int i, int n) {
        int end = i + 1;
        while (end < n && Character.isJavaIdentifierPart(text.charAt(end))) {
            end++;
        }
        if (end == i + 1) {
            tokens.add(new Token(TokenKind.OPERATOR, i, end));
            return end;
        }
        tokens.add(new Token(TokenKind.ANNOTATION, i, end));
        return end;
    }

    private int lexWord(CharSequence text, List<Token> tokens, int i, int n) {
        int end = i + 1;
        while (end < n && Character.isJavaIdentifierPart(text.charAt(end))) {
            end++;
        }
        String word = text.toString().substring(i, end);
        boolean qualifiedSegment = end < n && text.charAt(end) == '.';
        tokens.add(new Token(classify(word, qualifiedSegment), i, end));
        return end;
    }

    private TokenKind classify(String word, boolean qualifiedSegment) {
        if (dialect.reservedWords().contains(word)) {
            return TokenKind.KEYWORD;
        }
        boolean initialUpper = !word.isEmpty()
                && Character.isUpperCase(word.charAt(0));
        if (initialUpper && word.length() > 1
                && word.equals(word.toUpperCase(Locale.ROOT))) {
            return TokenKind.CONSTANT;
        }
        if (initialUpper && !qualifiedSegment) {
            return TokenKind.TYPE;
        }
        return TokenKind.IDENTIFIER;
    }

    /** Soude les jetons : le jeton suivant commence à la fin du précédent. */
    private static Token[] weld(Token[] tokens) {
        for (int j = 1; j < tokens.length; j++) {
            Token previous = tokens[j - 1];
            Token current = tokens[j];
            if (current.start() > previous.end()) {
                tokens[j] = new Token(current.kind(), previous.end(),
                        current.end());
            }
        }
        return tokens;
    }

    private static boolean startsWith(CharSequence text, int i, String motif) {
        if (i + motif.length() > text.length()) {
            return false;
        }
        for (int j = 0; j < motif.length(); j++) {
            if (text.charAt(i + j) != motif.charAt(j)) {
                return false;
            }
        }
        return true;
    }

    private static int find(CharSequence text, String motif, int from) {
        for (int i = from; i + motif.length() <= text.length(); i++) {
            boolean equal = true;
            for (int j = 0; j < motif.length(); j++) {
                if (text.charAt(i + j) != motif.charAt(j)) {
                    equal = false;
                    break;
                }
            }
            if (equal) {
                return i;
            }
        }
        return -1;
    }
}
