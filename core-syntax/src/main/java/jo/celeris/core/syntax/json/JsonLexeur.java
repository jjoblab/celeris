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
    enum Etat implements LexState {
        /** Hors de toute construction multi-lignes. */
        NORMAL
    }

    @Override
    public LexState initialState() {
        return Etat.NORMAL;
    }

    @Override
    public LexedLine lex(CharSequence texte, LexState entree) {
        List<Token> jetons = new ArrayList<>();
        int n = texte.length();
        int i = 0;
        while (i < n) {
            i = lexUn(texte, jetons, i, n);
        }
        return new LexedLine(jetons.toArray(new Token[0]), Etat.NORMAL);
    }

    private int lexUn(CharSequence texte, List<Token> jetons, int i, int n) {
        char c = texte.charAt(i);
        if (Character.isWhitespace(c)) {
            return i + 1;
        }
        if (c == '"') {
            return lexChaine(texte, jetons, i, n);
        }
        if (c == '{' || c == '[') {
            jetons.add(new Token(TokenKind.OPEN_BRACKET, i, i + 1));
            return i + 1;
        }
        if (c == '}' || c == ']') {
            jetons.add(new Token(TokenKind.CLOSE_BRACKET, i, i + 1));
            return i + 1;
        }
        if (c == ':' || c == ',') {
            jetons.add(new Token(TokenKind.PUNCTUATION, i, i + 1));
            return i + 1;
        }
        if (c == '-' || estChiffre(c)) {
            return lexNombre(texte, jetons, i, n);
        }
        return lexMot(texte, jetons, i, n);
    }

    private int lexChaine(CharSequence texte, List<Token> jetons, int i, int n) {
        int fin = i + 1;
        while (fin < n) {
            char c = texte.charAt(fin);
            if (c == '\\') {
                fin += 2;
                continue;
            }
            if (c == '"') {
                jetons.add(new Token(TokenKind.STRING, i, fin + 1));
                return fin + 1;
            }
            fin++;
        }
        jetons.add(new Token(TokenKind.ERROR, i, n));
        return n;
    }

    private int lexNombre(CharSequence texte, List<Token> jetons, int i, int n) {
        int fin = i + 1;
        if (texte.charAt(i) == '-' && fin < n && estChiffre(texte.charAt(fin))) {
            fin++;
        }
        fin = chiffres(texte, fin, n);
        fin = fraction(texte, fin, n);
        fin = exposant(texte, fin, n);
        jetons.add(new Token(TokenKind.NUMBER, i, fin));
        return fin;
    }

    private static int chiffres(CharSequence texte, int depuis, int n) {
        int fin = depuis;
        while (fin < n && estChiffre(texte.charAt(fin))) {
            fin++;
        }
        return fin;
    }

    private static int fraction(CharSequence texte, int depuis, int n) {
        if (depuis + 1 < n && texte.charAt(depuis) == '.'
                && estChiffre(texte.charAt(depuis + 1))) {
            return chiffres(texte, depuis + 1, n);
        }
        return depuis;
    }

    private static int exposant(CharSequence texte, int depuis, int n) {
        if (depuis >= n || "eE".indexOf(texte.charAt(depuis)) < 0) {
            return depuis;
        }
        int apres = depuis + 1;
        if (apres < n && "+-".indexOf(texte.charAt(apres)) >= 0) {
            apres++;
        }
        if (apres < n && estChiffre(texte.charAt(apres))) {
            return chiffres(texte, apres, n);
        }
        return depuis;
    }

    private int lexMot(CharSequence texte, List<Token> jetons, int i, int n) {
        int fin = i + 1;
        while (fin < n && Character.isLetterOrDigit(texte.charAt(fin))) {
            fin++;
        }
        String mot = texte.toString().substring(i, fin);
        jetons.add(new Token("true".equals(mot) || "false".equals(mot)
                || "null".equals(mot) ? TokenKind.KEYWORD : TokenKind.IDENTIFIER,
                i, fin));
        return fin;
    }

    private static boolean estChiffre(char c) {
        return c >= '0' && c <= '9';
    }
}
