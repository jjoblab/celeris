package jo.celeris.core.syntax;

/**
 * Une paire de crochets appariés, en offsets absolus.
 *
 * @param start      début du crochet visé
 * @param end        fin exclusive du crochet visé
 * @param otherStart début du partenaire
 * @param otherEnd   fin exclusive du partenaire
 */
public record TokenMatch(int start, int end, int otherStart, int otherEnd) {
}
