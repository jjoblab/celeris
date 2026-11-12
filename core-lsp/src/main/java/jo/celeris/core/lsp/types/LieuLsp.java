package jo.celeris.core.lsp.types;

import org.jspecify.annotations.Nullable;

/**
 * Un lieu de navigation (B-10, B-11) : l'uri et la plage d'une cible —
 * la définition donnée par le serveur, une référence raffinée. Le lieu
 * est ce que l'historique de navigation M5-t4 consomme : la cible
 * visitée s'y empile, le retour au point de départ s'y lit. La règle
 * des rôles (ADR-005) tient : l'index hors ligne répond vite, le
 * serveur répond juste, jamais de chaînage. La lecture depuis les
 * types de la bibliothèque vit derrière la frontière protocolaire
 * (ADR-015).
 *
 * @param uri   l'uri du fichier cible, vide si le serveur ne la donne pas
 * @param plage la plage de la cible dans son fichier, nulle si le
 *              serveur ne la donne pas — jamais devinée
 */
public record LieuLsp(String uri, @Nullable PlageLsp plage) {
}
