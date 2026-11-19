package jo.celeris.core.syntax.json;

import java.util.ArrayList;
import java.util.List;
import jo.celeris.core.syntax.LanguageLexer;
import jo.celeris.core.syntax.LexState;
import jo.celeris.core.syntax.LexedLine;
import jo.celeris.core.syntax.Token;
import jo.celeris.core.syntax.TokenKind;

/**
 * Lexeur JSON (ADR-004) : chaînes à échappements, nombres signés à
 * virgule et exposant, mots réservés true/false/null, accolades et
 * crochets appariés, deux-points et virgules en ponctuation. Aucun état
 * transporté — une chaîne ne traverse jamais les lignes ; une chaîne non
 * fermée est un jeton ERROR, jamais une exception. Le reste toléré en
 * identificateur : la douceur de l'éditeur prime sur la rigueur du RFC.
 */
final class JsonLexeur implements LanguageLexer {

    /** L'unique état — JSON n'en transporte aucun. */
    enum State implements LexState {
        /** Hors de toute construction multi-lignes. */
        NORMAL
    }

    @Override
    public LexState initialState() {
        return State.NORMAL;
    }

    @Override
    public LexedLine lex(CharSequence text, LexState input) {
        List<Token> tokens = new ArrayList<>();
        int n = text.length();
        int i = 0;
        while (i < n) {
            i = lexUn(text, tokens, i, n);
        }
        return new LexedLine(tokens.toArray(new Token[0]), State.NORMAL);
    }

    private int lexUn(CharSequence text, List<Token> tokens, int i, int n) {
        char c = text.charAt(i);
        if (Character.isWhitespace(c)) {
            return i + 1;
        }
        if (c == '"') {
            return lexChaine(text, tokens, i, n);
        }
        if (c == '{' || c == '[') {
            tokens.add(new Token(TokenKind.OPEN_BRACKET, i, i + 1));
            return i + 1;
        }
        if (c == '}' || c == ']') {
            tokens.add(new Token(TokenKind.CLOSE_BRACKET, i, i + 1));
            return i + 1;
        }
        if (c == ':' || c == ',') {
            tokens.add(new Token(TokenKind.PUNCTUATION, i, i + 1));
            return i + 1;
        }
        if (c == '-' || isDigit(c)) {
            return lexNombre(text, tokens, i, n);
        }
        return lexMot(text, tokens, i, n);
    }

    private int lexChaine(CharSequence text, List<Token> tokens, int i, int n) {
        int end = i + 1;
        while (end < n) {
            char c = text.charAt(end);
            if (c == '\\') {
                end += 2;
                continue;
            }
            if (c == '"') {
                tokens.add(new Token(TokenKind.STRING, i, end + 1));
                return end + 1;
            }
            end++;
        }
        tokens.add(new Token(TokenKind.ERROR, i, n));
        return n;
    }

    private int lexNombre(CharSequence text, List<Token> tokens, int i, int n) {
        int end = i + 1;
        if (text.charAt(i) == '-' && end < n && isDigit(text.charAt(end))) {
            end++;
        }
        end = digits(text, end, n);
        end = fraction(text, end, n);
        end = exposant(text, end, n);
        tokens.add(new Token(TokenKind.NUMBER, i, end));
        return end;
    }

    private static int digits(CharSequence text, int depuis, int n) {
        int end = depuis;
        while (end < n && isDigit(text.charAt(end))) {
            end++;
        }
        return end;
    }

    private static int fraction(CharSequence text, int depuis, int n) {
        if (depuis + 1 < n && text.charAt(depuis) == '.'
                && isDigit(text.charAt(depuis + 1))) {
            return digits(text, depuis + 1, n);
        }
        return depuis;
    }

    private static int exposant(CharSequence text, int depuis, int n) {
        if (depuis >= n || "eE".indexOf(text.charAt(depuis)) < 0) {
            return depuis;
        }
        int apres = depuis + 1;
        if (apres < n && "+-".indexOf(text.charAt(apres)) >= 0) {
            apres++;
        }
        if (apres < n && isDigit(text.charAt(apres))) {
            return digits(text, apres, n);
        }
        return depuis;
    }

    private int lexMot(CharSequence text, List<Token> tokens, int i, int n) {
        int end = i + 1;
        while (end < n && Character.isLetterOrDigit(text.charAt(end))) {
            end++;
        }
        String mot = text.toString().substring(i, end);
        tokens.add(new Token("true".equals(mot) || "false".equals(mot)
                || "null".equals(mot) ? TokenKind.KEYWORD : TokenKind.IDENTIFIER,
                i, end));
        return end;
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }
}
