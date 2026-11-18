package jo.celeris.android.host.spi;

/**
 * Un point d'arrêt du débogueur (E-03, D-04) : son fichier, sa ligne
 * et son état — posé puis désactivé, l'arrêt reste en place.
 *
 * @param fichier le chemin du fichier
 * @param ligne   la ligne, numérotée à partir de un
 * @param actif   vrai si l'arrêt interrompt l'exécution
 */
public record Breakpoint(CheminDoc fichier, int ligne, boolean actif) {

    /** L'arrêt jumeau à l'état inverse. */
    public Breakpoint bascule() {
        return new Breakpoint(fichier, ligne, !actif);
    }
}
