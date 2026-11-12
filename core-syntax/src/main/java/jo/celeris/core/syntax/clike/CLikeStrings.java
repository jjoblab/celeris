package jo.celeris.core.syntax.clike;

import java.util.List;
import jo.celeris.core.syntax.LexState;
import jo.celeris.core.syntax.Token;
import jo.celeris.core.syntax.TokenKind;

/**
 * Les chaînes de la famille C (M7.6, H-10) : la part du lexeur
 * partagé qui délimite — chaîne simple (fermeture et gabarits
 * {@code ${…}} selon le dialecte, les guillemets internes aux
 * accolades ne clôturent jamais) et chaîne triple (crochet apparié à
 * l'ouverture comme à la fermeture, la ligne vide émet ou non un
 * jeton vide selon la règle héritée du dialecte). Sans état : le
 * lexeur porte l'état de sortie, ces routines le rendent.
 */
final class CLikeStrings {

    /** L'issue d'une construction multi-lignes : la reprise et l'état de sortie. */
    record Outcome(int resume, LexState exitState) {
    }

    private CLikeStrings() {
    }

    /**
     * Lexe la chaîne délimitée qui s'ouvre à {@code i} : triple
     * ouvrante en crochet apparié, gabarit à accolades sinon — une
     * chaîne non fermée est un jeton ERROR, jamais une exception.
     *
     * @return la reprise et l'état de sortie
     */
    static Outcome delimited(CLikeDialect dialect, CharSequence text,
            List<Token> tokens, int i, int n, char delimiter) {
        boolean triple = (dialect.tripleSingleQuote() || delimiter == '"')
                && i + 2 < n && text.charAt(i + 1) == delimiter
                && text.charAt(i + 2) == delimiter;
        if (triple) {
            return openTriple(dialect, text, tokens, i, n, delimiter);
        }
        int end = i + 1;
        int braces = 0;
        while (end < n) {
            char c = text.charAt(end);
            if (c == '\\') {
                end += 2;
                continue;
            }
            if (braces > 0) {
                if (c == '{') {
                    braces++;
                } else if (c == '}') {
                    braces--;
                }
                end++;
                continue;
            }
            if (dialect.closesString(c, delimiter)) {
                tokens.add(new Token(TokenKind.STRING, i, end + 1));
                return new Outcome(end + 1, CLikeLexer.SimpleState.NORMAL);
            }
            if (dialect.opensTemplate(text, end, n, delimiter)) {
                braces++;
                end += 2;
                continue;
            }
            end++;
        }
        tokens.add(new Token(TokenKind.ERROR, i, n));
        return new Outcome(n, CLikeLexer.SimpleState.NORMAL);
    }

    /**
     * Lexe la chaîne triple qui s'ouvre à {@code i} : crochet ouvrant,
     * contenu jusqu'à la fermeture, crochet fermant — l'état de sortie
     * signale la construction restée ouverte en fin de ligne.
     *
     * @return la reprise et l'état de sortie
     */
    static Outcome openTriple(CLikeDialect dialect, CharSequence text,
            List<Token> tokens, int i, int n, char delimiter) {
        tokens.add(new Token(TokenKind.OPEN_BRACKET, i, i + 3));
        int close = find(text, tripleOf(delimiter), i + 3);
        if (close < 0) {
            if (i + 3 < n) {
                tokens.add(new Token(TokenKind.STRING, i + 3, n));
            }
            return new Outcome(n, new CLikeLexer.InTripleString(delimiter));
        }
        if (close > i + 3) {
            tokens.add(new Token(TokenKind.STRING, i + 3, close));
        }
        tokens.add(new Token(TokenKind.CLOSE_BRACKET, close, close + 3));
        return new Outcome(close + 3, CLikeLexer.SimpleState.NORMAL);
    }

    /**
     * Lexe la suite d'une chaîne triple ouverte avant la ligne : le
     * contenu jusqu'à la fermeture de son délimiteur.
     *
     * @return la reprise et l'état de sortie
     */
    static Outcome resumeTriple(CLikeDialect dialect, CharSequence text,
            List<Token> tokens, char delimiter) {
        int n = text.length();
        int close = find(text, tripleOf(delimiter), 0);
        if (close < 0) {
            if (n > 0 || dialect.keepEmptyTripleLineToken()) {
                tokens.add(new Token(TokenKind.STRING, 0, n));
            }
            return new Outcome(n, new CLikeLexer.InTripleString(delimiter));
        }
        if (close > 0) {
            tokens.add(new Token(TokenKind.STRING, 0, close));
        }
        tokens.add(new Token(TokenKind.CLOSE_BRACKET, close, close + 3));
        return new Outcome(close + 3, CLikeLexer.SimpleState.NORMAL);
    }

    private static String tripleOf(char delimiter) {
        return delimiter == '"' ? "\"\"\"" : "'''";
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
