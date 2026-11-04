package jo.celeris.core.syntax;

/**
 * Signes Kotlin (ADR-004) : opérateurs propres (appel sûr, elvis,
 * références, flèches, intervalles) et ponctuation — le plus long
 * d'abord, un signe inconnu devient un opérateur d'un caractère.
 */
final class KotlinSignes {

    private static final java.util.List<String> OPERATEURS = java.util.List.of(
            "===", "!==", ">>>", "<<=", ">>=", "..<", "?:", "?.", "::", "->",
            "==", "!=", "<=", ">=", "&&", "||", "++", "--", "+=", "-=", "*=",
            "/=", "%=", "..", "<<", ">>", "+", "-", "*", "/", "%", "=", "<",
            ">", "!", "&", "|", "^", "~", "?");

    private KotlinSignes() {
    }

    /**
     * Lexe un signe débutant à {@code i}.
     *
     * @return la position après le signe
     */
    static int lex(CharSequence texte, java.util.List<Token> jetons, int i, int n) {
        char c = texte.charAt(i);
        if ("(){}[]".indexOf(c) >= 0) {
            TokenKind sorte = c == '(' || c == '{' || c == '['
                    ? TokenKind.OPEN_BRACKET : TokenKind.CLOSE_BRACKET;
            jetons.add(new Token(sorte, i, i + 1));
            return i + 1;
        }
        int compose = signeCompose(texte, jetons, i, n);
        if (compose >= 0) {
            return compose;
        }
        if (c == ';' || c == ',' || c == '.' || c == ':') {
            jetons.add(new Token(TokenKind.PUNCTUATION, i, i + 1));
            return i + 1;
        }
        return operateur(texte, jetons, i);
    }

    /** Les signes composés qui précèdent leur forme simple : :: et .. */
    private static int signeCompose(CharSequence texte, java.util.List<Token> jetons,
                                    int i, int n) {
        char c = texte.charAt(i);
        if (c == ':' && i + 1 < n && texte.charAt(i + 1) == ':') {
            jetons.add(new Token(TokenKind.OPERATOR, i, i + 2));
            return i + 2;
        }
        if (c == '.' && i + 1 < n && texte.charAt(i + 1) == '.') {
            int fin = i + 2;
            if (fin < n && texte.charAt(fin) == '<') {
                fin++;
            }
            jetons.add(new Token(TokenKind.OPERATOR, i, fin));
            return fin;
        }
        return -1;
    }

    private static int operateur(CharSequence texte, java.util.List<Token> jetons,
                                 int i) {
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
