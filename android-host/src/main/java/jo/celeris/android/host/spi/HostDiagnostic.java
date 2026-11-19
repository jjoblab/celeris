package jo.celeris.android.host.spi;

/**
 * Un diagnostic d'hôte (E-05) : fichier, ligne, colonne, gravité et
 * message — la sortie de build convertie, prête pour la gouttière et
 * la bande d'erreurs.
 *
 * @param chemin  le chemin du fichier, raciné dans le système virtuel
 * @param line   la ligne, numérotée à partir de un
 * @param column la colonne, à partir de un, zéro si inconnue
 * @param severity la gravité
 * @param message le message, sans le préfixe de gravité
 */
public record HostDiagnostic(CheminDoc chemin, int line, int column,
                             GraviteDiagnostic severity, String message) {
}
