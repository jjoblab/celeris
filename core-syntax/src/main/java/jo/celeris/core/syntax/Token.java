package jo.celeris.core.syntax;

import jo.celeris.core.syntax.scope.ScopeStacks;
/**
 * Jeton d'une ligne : sorte, bornes RELATIVES au début de la ligne
 * (fin exclusive) et pile de portées internée. Le relatif est la clé
 * du réemploi incrémental — une ligne dont le contenu ne change pas
 * garde ses jetons tels quels quand le document bouge ailleurs
 * (ADR-004).
 *
 * <p>La pile de portées (M7.6, H-07) ne vit que dans la voie des
 * grammaires : l'identifiant interné désigne la pile de la racine au
 * jeton, la COULEUR s'y résout par le thème — la sorte reste la
 * catégorie grossière des consommateurs hérités (crochets, plis).
 * L'absence de pile (zéro) dit « analyseur écrit » : la palette par
 * sorte fait foi.</p>
 *
 * @param kind  la sorte du jeton
 * @param start début relatif du jeton dans sa ligne
 * @param end   fin exclusive du jeton dans sa ligne
 * @param scope l'identifiant de la pile de portées, ou NONE
 */
public record Token(TokenKind kind, int start, int end, int scope) {

    /**
     * Le jeton sans pile de portées — celui des analyseurs écrits à
     * la main et des paniers de la voie commune.
     *
     * @param kind  la sorte du jeton
     * @param start début relatif du jeton dans sa ligne
     * @param end   fin exclusive du jeton dans sa ligne
     */
    public Token(TokenKind kind, int start, int end) {
        this(kind, start, end, ScopeStacks.NONE);
    }

    /** Longueur du jeton en caractères. */
    public int length() {
        return end - start;
    }
}
