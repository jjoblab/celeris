package jo.celeris.android.host.spi;

/**
 * Une entrée d'un arbre de documents (E-01) : le nom affichable,
 * l'identifiant de document opaque du fournisseur, et la nature —
 * fichier ou dossier.
 *
 * @param nom        le nom de l'entrée, jamais vide
 * @param idDocument l'identifiant opaque du fournisseur
 * @param dossier    vrai pour un dossier, faux pour un fichier
 */
public record EntreeDocument(String nom, String idDocument, boolean dossier) {
}
