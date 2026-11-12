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
    static int lex(CharSequence texte, List<Token> jetons, int i, int n) {
        int fin = partieNumerique(texte, i, n);
        if (fin < n && "lLfFdD".indexOf(texte.charAt(fin)) >= 0) {
            fin++;
        }
        jetons.add(new Token(TokenKind.NUMBER, i, fin));
        return fin;
    }

    /** Vrai quand le caractère peut ouvrir un littéral numérique. */
    static boolean ouvre(char c, CharSequence texte, int i, int n) {
        return estChiffre(c) || (c == '.' && i + 1 < n && estChiffre(texte.charAt(i + 1)));
    }

    private static int partieNumerique(CharSequence texte, int i, int n) {
        if (texte.charAt(i) == '0' && i + 1 < n
                && "xXbB".indexOf(texte.charAt(i + 1)) >= 0) {
            return base(texte, i + 2, n);
        }
        int fin = ciffrable(texte, i, n);
        if (fin < n && texte.charAt(fin) == '.' && fin + 1 < n
                && estChiffre(texte.charAt(fin + 1))) {
            fin = ciffrable(texte, fin + 1, n);
        }
        if (fin < n && "eE".indexOf(texte.charAt(fin)) >= 0) {
            fin = exposant(texte, fin, n);
        }
        return fin;
    }

    private static int base(CharSequence texte, int depuis, int n) {
        int fin = depuis;
        while (fin < n && (estChiffre(texte.charAt(fin))
                || "abcdefABCDEF_".indexOf(texte.charAt(fin)) >= 0)) {
            fin++;
        }
        return fin;
    }

    private static int ciffrable(CharSequence texte, int depuis, int n) {
        int fin = depuis;
        while (fin < n && (estChiffre(texte.charAt(fin)) || texte.charAt(fin) == '_')) {
            fin++;
        }
        return fin;
    }

    private static int exposant(CharSequence texte, int marqueur, int n) {
        int apres = marqueur + 1;
        if (apres < n && "+-".indexOf(texte.charAt(apres)) >= 0) {
            apres++;
        }
        if (apres < n && estChiffre(texte.charAt(apres))) {
            return ciffrable(texte, apres, n);
        }
        return marqueur;
    }

    private static boolean estChiffre(char c) {
        return c >= '0' && c <= '9';
    }
}
