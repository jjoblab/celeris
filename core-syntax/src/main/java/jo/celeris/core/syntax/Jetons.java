package jo.celeris.core.syntax;

import org.jspecify.annotations.Nullable;

/**
 * Utilitaires de tranche de jetons (ADR-004) : texte épuré des blancs
 * soudés, trivia, crochets et bornes — partagés par les analyseurs des
 * langages P0.
 */
public final class Jetons {

    private Jetons() {
    }

    /** Le texte épuré du jeton — les blancs soudés ne font pas partie du mot. */
    public static String mot(TokenSlice slice, int index) {
        return slice.text(index).strip();
    }

    /** Le texte d'un crochet, ou null si le jeton n'en est pas un. */
    public static @Nullable String texteCrochet(TokenSlice slice, int index) {
        if (slice.kind(index) != TokenKind.OPEN_BRACKET
                && slice.kind(index) != TokenKind.CLOSE_BRACKET) {
            return null;
        }
        return mot(slice, index);
    }

    /** L'indice du premier jeton non trivia à partir de {@code depuis}. */
    public static int sauterTrivia(TokenSlice slice, int depuis) {
        int i = depuis;
        while (i < slice.size() && estTrivia(slice, i)) {
            i++;
        }
        return i;
    }

    /** Vrai quand le jeton est un commentaire ou de la documentation. */
    public static boolean estTrivia(TokenSlice slice, int index) {
        TokenKind sorte = slice.kind(index);
        return sorte == TokenKind.COMMENT || sorte == TokenKind.DOC;
    }

    /** La fin du jeton, bornée à la tranche. */
    public static int finDe(TokenSlice slice, int index) {
        return slice.end(Math.max(0, Math.min(index, slice.size() - 1)));
    }

    /**
     * L'indice de l'accolade ouvrante de tête, en sautant les paires
     * intermédiaires ; -1 si une clôture orpheline ou un point-virgule
     * survient d'abord.
     */
    public static int chercherAccolade(TokenSlice slice, int depuis) {
        for (int i = depuis; i < slice.size(); i++) {
            TokenKind sorte = slice.kind(i);
            if (sorte == TokenKind.OPEN_BRACKET) {
                if ("{".equals(texteCrochet(slice, i))) {
                    return i;
                }
                int fermante = slice.matchOf(i);
                if (fermante < 0) {
                    return -1;
                }
                i = fermante;
            } else if (sorte == TokenKind.CLOSE_BRACKET) {
                return -1;
            } else if (sorte == TokenKind.PUNCTUATION
                    && ";".equals(mot(slice, i))) {
                return -1;
            }
        }
        return -1;
    }
}
