package jo.celeris.core.syntax;

/**
 * Jeton d'une ligne : sorte et bornes RELATIVES au début de la ligne
 * (fin exclusive). Le relatif est la clé du réemploi incrémental — une
 * ligne dont le contenu ne change pas garde ses jetons tels quels quand
 * le document bouge ailleurs (ADR-004).
 *
 * @param kind  la sorte du jeton
 * @param start début relatif du jeton dans sa ligne
 * @param end   fin exclusive du jeton dans sa ligne
 */
public record Token(TokenKind kind, int start, int end) {

    /** Longueur du jeton en caractères. */
    public int length() {
        return end - start;
    }
}
