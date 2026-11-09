/**
 * La frontière protocolaire du LSP (ADR-015) : le protocole et le
 * JSON-RPC viennent de la bibliothèque, l'ordonnancement, les priorités
 * et les types du domaine restent dans core-lsp — aucun type de la
 * bibliothèque ne paraît ici.
 */
@NullMarked
package jo.celeris.lsp.protocole;

import org.jspecify.annotations.NullMarked;
