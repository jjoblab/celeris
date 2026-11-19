package jo.celeris.core.lsp.types;

import jo.celeris.core.lsp.WorkspaceEditsLsp;
/**
 * Une édition de texte du protocole : la plage remplacée et le texte
 * nouveau — en positions LSP (unités UTF-16), la transposition en
 * offsets vivant dans {@link WorkspaceEditsLsp}. La lecture depuis les
 * types de la bibliothèque vit derrière la frontière protocolaire
 * (ADR-015).
 *
 * @param range        la plage remplacée
 * @param newText le texte qui la remplace
 */
public record TextEditLsp(RangeLsp range, String newText) {
}
