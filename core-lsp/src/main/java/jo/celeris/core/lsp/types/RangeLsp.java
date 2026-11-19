package jo.celeris.core.lsp.types;

/**
 * Une plage LSP : début inclus, fin exclusive — deux positions en
 * unités UTF-16. La lecture depuis les types de la bibliothèque vit
 * derrière la frontière protocolaire (ADR-015).
 *
 * @param start la position de début
 * @param end   la position de fin
 */
public record RangeLsp(LspPosition start, LspPosition end) {
}
