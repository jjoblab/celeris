package jo.celeris.core.ai;

import jo.celeris.core.syntax.JetonAnnulation;
import jo.celeris.core.text.Document;

/**
 * La requête envoyée à un modèle (ADR-012) : l'instantané immuable du
 * document, la position du curseur et le jeton d'annulation.
 *
 * <p>Le cœur ne connaît aucun fournisseur : la requête ne porte que des
 * données — jamais de transport, jamais de réseau. L'hôte fournit le
 * modèle et décide du contexte à en tirer. Le jeton (ADR-007) suit la
 * requête de bout en bout : annuler coûte moins cher que terminer, le
 * modèle l'interroge avant de répondre et se tait s'il est mort.</p>
 *
 * @param document l'instantané immuable contre lequel la requête se calcule
 * @param offset   la position du curseur dans le document
 * @param jeton    le jeton d'annulation du vol
 */
public record RequeteModele(Document document, int offset, JetonAnnulation jeton) {

    /**
     * Valide la requête : le curseur vit dans le document.
     *
     * @throws IllegalArgumentException si {@code offset} sort du document
     */
    public RequeteModele {
        if (offset < 0 || offset > document.length()) {
            throw new IllegalArgumentException(
                    "offset out of document bounds: " + offset);
        }
    }
}
