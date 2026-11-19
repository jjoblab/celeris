package jo.celeris.android.host.spi;

/**
 * Le lieu de la pause (E-03, D-05) : le fichier et la ligne où
 * l'exécution s'est arrêtée.
 *
 * @param fichier le chemin du fichier en cours d'exécution
 * @param line   la ligne courante, numérotée à partir de un
 */
public record BreakpointLine(CheminDoc fichier, int line) {
}
