package jo.celeris.core.lsp.types;

import jo.celeris.core.lsp.EditsEspaceLsp;
/**
 * Une édition de texte du protocole : la plage remplacée et le texte
 * nouveau — en positions LSP (unités UTF-16), la transposition en
 * offsets vivant dans {@link EditsEspaceLsp}. La lecture depuis les
 * types de la bibliothèque vit derrière la frontière protocolaire
 * (ADR-015).
 *
 * @param plage        la plage remplacée
 * @param nouveauTexte le texte qui la remplace
 */
public record EditionTexteLsp(PlageLsp plage, String nouveauTexte) {
}
