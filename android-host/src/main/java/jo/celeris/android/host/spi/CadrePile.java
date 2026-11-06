package jo.celeris.android.host.spi;

/**
 * Un cadre de pile d'appels (E-03/E-06) : classe, méthode, fichier et
 * ligne — la matière du saut `fichier:ligne`.
 *
 * @param classe  le nom pleinement qualifié de la classe
 * @param methode le nom de la méthode
 * @param fichier le nom du fichier source
 * @param ligne   la ligne dans le fichier, zéro si inconnue
 */
public record CadrePile(String classe, String methode, String fichier, int ligne) {
}
