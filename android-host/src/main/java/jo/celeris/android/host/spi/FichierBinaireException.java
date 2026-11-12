package jo.celeris.android.host.spi;

/**
 * Le fichier demandé est binaire (A-26) : l'écriture par la voie
 * texte se REFUSE — jamais l'aller-retour destructeur (le décodage
 * permissif qui se réencoderait, l'octet perdu au passage). La
 * détection s'est lue sur les octets bruts, la décision se prend
 * d'elle.
 */
public final class FichierBinaireException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Construit l'exception pour le chemin demandé.
     *
     * @param chemin le chemin du fichier binaire
     */
    public FichierBinaireException(CheminDoc chemin) {
        super("binary file refuses text writes: " + chemin.ecriture());
    }
}
