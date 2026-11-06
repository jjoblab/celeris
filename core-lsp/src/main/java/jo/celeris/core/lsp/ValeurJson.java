package jo.celeris.core.lsp;

/**
 * Une valeur JSON (ADR-005) : objet, tableau, chaîne, nombre, booléen ou
 * nul — immuable, figée à la construction. La surface est bornée aux
 * besoins du protocole LSP : pas de nombre bigdecimal, pas de chemin
 * d'accès, pas de mutation.
 */
public sealed interface ValeurJson permits JsonObjet, JsonTableau, JsonChaine,
        JsonNombre, JsonBooleen, JsonNul {
}
