package jo.celeris.core.syntax.structure;

/**
 * Une région de pli : la ligne d'en-tête reste visible, les lignes
 * {@code startLine + 1} à {@code endLine} se replient (A-15).
 *
 * @param startLine la ligne d'en-tête
 * @param endLine   la dernière ligne repliée
 */
public record FoldSpan(int startLine, int endLine) {
}
