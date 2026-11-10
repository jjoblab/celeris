package jo.celeris.core.lsp;

/**
 * La préparation d'un renommage (H-12) : la plage du nom à remplacer
 * et l'amorce que le serveur propose — la boîte de renommage en ligne
 * s'y ouvre, le nom sélectionné prêt à être tapé par-dessus. Un serveur
 * qui ne prépare pas rend null : le renommage se tait, jamais il ne
 * devine la plage.
 *
 * @param plage        la plage du nom à renommer
 * @param texteAmorce l'amorce proposée, ou null
 */
public record PreparationRennomageLsp(PlageLsp plage,
        @org.jspecify.annotations.Nullable String texteAmorce) {
}
