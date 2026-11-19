package jo.celeris.core.lsp.types;

import jo.celeris.core.lsp.LspPositions;
/**
 * Une position LSP : ligne et colonne — la colonne compte les unités
 * UTF-16, le repère du protocole. La transposition vers les offsets du
 * document vit dans {@link LspPositions}, frontière explicite (ADR-005) ;
 * la lecture depuis les types de la bibliothèque vit derrière la
 * frontière protocolaire (ADR-015).
 *
 * @param line    la ligne, à partir de zéro
 * @param column  la colonne en unités UTF-16, à partir de zéro
 */
public record LspPosition(int line, int column) {
}
