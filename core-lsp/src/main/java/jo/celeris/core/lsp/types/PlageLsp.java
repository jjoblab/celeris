package jo.celeris.core.lsp.types;

/**
 * Une plage LSP : début inclus, fin exclusive — deux positions en
 * unités UTF-16. La lecture depuis les types de la bibliothèque vit
 * derrière la frontière protocolaire (ADR-015).
 *
 * @param debut la position de début
 * @param fin   la position de fin
 */
public record PlageLsp(PositionLsp debut, PositionLsp fin) {
}
