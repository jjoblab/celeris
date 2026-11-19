package jo.celeris.core.syntax.clike;

import java.util.List;
import jo.celeris.core.syntax.Token;
import jo.celeris.core.syntax.TokenKind;

/**
 * Littéraux numériques Java (ADR-004) : toutes bases — décimal, hexadécimal,
 * binaire — soulignés de regroupement, virgule flottante, exposant signé et
 * suffixe de type. La numération est un tout cohérent : le lexeur n'en
 * garde que l'entrée.
 */
final class JavaNombres {

    private JavaNombres() {
    }

    /**
     * Lexe un littéral numérique débutant à {@code i}.
     *
     * @return la position après le littéral
     */
    static int lex(CharSequence text, List<Token> tokens, int i, int n) {
        int end = partieNumerique(text, i, n);
        if (end < n && "lLfFdD".indexOf(text.charAt(end)) >= 0) {
            end++;
        }
        tokens.add(new Token(TokenKind.NUMBER, i, end));
        return end;
    }

    /** Vrai quand le caractère peut ouvrir un littéral numérique. */
    static boolean ouvre(char c, CharSequence text, int i, int n) {
        return isDigit(c) || (c == '.' && i + 1 < n && isDigit(text.charAt(i + 1)));
    }

    private static int partieNumerique(CharSequence text, int i, int n) {
        if (text.charAt(i) == '0' && i + 1 < n
                && "xXbB".indexOf(text.charAt(i + 1)) >= 0) {
            return base(text, i + 2, n);
        }
        int end = ciffrable(text, i, n);
        if (end < n && text.charAt(end) == '.' && end + 1 < n
                && isDigit(text.charAt(end + 1))) {
            end = ciffrable(text, end + 1, n);
        }
        if (end < n && "eE".indexOf(text.charAt(end)) >= 0) {
            end = exposant(text, end, n);
        }
        return end;
    }

    private static int base(CharSequence text, int depuis, int n) {
        int end = depuis;
        while (end < n && (isDigit(text.charAt(end))
                || "abcdefABCDEF_".indexOf(text.charAt(end)) >= 0)) {
            end++;
        }
        return end;
    }

    private static int ciffrable(CharSequence text, int depuis, int n) {
        int end = depuis;
        while (end < n && (isDigit(text.charAt(end)) || text.charAt(end) == '_')) {
            end++;
        }
        return end;
    }

    private static int exposant(CharSequence text, int marqueur, int n) {
        int apres = marqueur + 1;
        if (apres < n && "+-".indexOf(text.charAt(apres)) >= 0) {
            apres++;
        }
        if (apres < n && isDigit(text.charAt(apres))) {
            return ciffrable(text, apres, n);
        }
        return marqueur;
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }
}
