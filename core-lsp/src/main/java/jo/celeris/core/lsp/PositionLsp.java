package jo.celeris.core.lsp;

/**
 * Une position LSP : ligne et colonne — la colonne compte les unités
 * UTF-16, le repère du protocole. La transposition vers les offsets du
 * document vit dans {@link PositionsLsp}, frontière explicite (ADR-005) ;
 * la lecture depuis les types de la bibliothèque vit derrière la
 * frontière protocolaire (ADR-015).
 *
 * @param ligne    la ligne, à partir de zéro
 * @param colonne  la colonne en unités UTF-16, à partir de zéro
 */
public record PositionLsp(int ligne, int colonne) {
}
