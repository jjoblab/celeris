package jo.celeris.core.syntax;

import java.util.List;

/**
 * Signes Groovy (ADR-004, dialecte Gradle) : crochets appariés,
 * ponctuation et opérateurs propres — {@code ?., *., .., ..<, <=>, =~,
 * ==~, **} — le plus long d'abord. Un signe inconnu devient un
 * opérateur d'un caractère : jamais perdu, jamais d'exception.
 */
final class GroovySignes {

    private static final List<String> OPERATEURS = List.of(
            ">>>=", "<<=", ">>=", ">>>", "==~", "<=>", "..<", "**=", "?.", "*.",
            "==", "!=", "<=", ">=", "&&", "||", "++", "--", "+=", "-=", "*=",
            "/=", "%=", "&=", "|=", "^=", "<<", ">>", "**", "..", "=~", "->",
            "+", "-", "*", "/", "%", "=", "<", ">", "!", "&", "|", "^", "~",
            "?");

    private GroovySignes() {
    }

    /**
     * Lexe un signe débutant à {@code i}.
     *
     * @return la position après le signe
     */
    static int lex(CharSequence texte, List<Token> jetons, int i, int n) {
        char c = texte.charAt(i);
        if ("(){}[]".indexOf(c) >= 0) {
            TokenKind sorte = c == '(' || c == '{' || c == '['
                    ? TokenKind.OPEN_BRACKET : TokenKind.CLOSE_BRACKET;
            jetons.add(new Token(sorte, i, i + 1));
            return i + 1;
        }
        if (c == ';' || c == ',' || c == '.') {
            jetons.add(new Token(TokenKind.PUNCTUATION, i, i + 1));
            return i + 1;
        }
        if (c == ':' && i + 1 < n && texte.charAt(i + 1) == ':') {
            jetons.add(new Token(TokenKind.PUNCTUATION, i, i + 2));
            return i + 2;
        }
        return operateur(texte, jetons, i);
    }

    private static int operateur(CharSequence texte, List<Token> jetons, int i) {
        for (String signe : OPERATEURS) {
            if (texte.length() >= i + signe.length()
                    && texte.subSequence(i, i + signe.length()).toString()
                    .equals(signe)) {
                jetons.add(new Token(TokenKind.OPERATOR, i, i + signe.length()));
                return i + signe.length();
            }
        }
        jetons.add(new Token(TokenKind.OPERATOR, i, i + 1));
        return i + 1;
    }
}
