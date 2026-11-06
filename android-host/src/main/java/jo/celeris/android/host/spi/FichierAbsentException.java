package jo.celeris.android.host.spi;

/**
 * Le fichier demandé n'existe pas dans le système de fichiers virtuel
 * (E-01) : l'ouverture d'un chemin absent lève, jamais null.
 */
public final class FichierAbsentException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Construit l'exception pour le chemin demandé.
     *
     * @param chemin le chemin absent
     */
    public FichierAbsentException(CheminDoc chemin) {
        super("file not found: " + chemin.ecriture());
    }
}
