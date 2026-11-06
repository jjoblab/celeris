package jo.celeris.android.host.spi;

/**
 * Un fichier du système de fichiers virtuel : son chemin validé et son
 * contenu intégral (E-01).
 *
 * @param chemin  le chemin normalisé du fichier
 * @param contenu le contenu textuel intégral
 */
public record FichierVirtuel(CheminDoc chemin, String contenu) {
}
