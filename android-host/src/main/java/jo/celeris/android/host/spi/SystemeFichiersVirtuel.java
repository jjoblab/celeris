package jo.celeris.android.host.spi;

import java.util.List;

/**
 * Le système de fichiers virtuel (E-01) : l'abstraction au-dessus des
 * accès cloisonnés du stockage — arbres de documents, périmètre
 * accordé. Les chemins sont validés par construction
 * ({@link CheminDoc}) : la traversée ne passe pas.
 *
 * <p>L'implémentation de référence, en mémoire, sert les tests et les
 * faux de démonstration ; l'adaptateur de production s'appuie sur le
 * framework d'accès au stockage de l'hôte.</p>
 */
public interface SystemeFichiersVirtuel {

    /**
     * Ouvre le fichier donné.
     *
     * @param chemin le chemin du fichier
     * @return le fichier, chemin et contenu
     * @throws FichierAbsentException si le chemin n'est pas un fichier
     */
    FichierVirtuel ouvrir(CheminDoc chemin);

    /**
     * Écrit le fichier donné : créé s'il est absent, remplacé sinon —
     * les parents implicites se créent au passage.
     *
     * @param chemin   le chemin du fichier
     * @param contenu  le contenu intégral à écrire
     */
    void ecrire(CheminDoc chemin, String contenu);

    /**
     * Les fichiers directement contenus dans le dossier donné.
     *
     * @param dossier le chemin du dossier
     * @return les chemins des fichiers enfants, l'ordre est stable
     */
    List<CheminDoc> lister(CheminDoc dossier);

    /**
     * Vrai si le chemin désigne un fichier (pas un dossier, pas absent).
     *
     * @param chemin le chemin à sonder
     * @return vrai si un fichier y répond
     */
    boolean estFichier(CheminDoc chemin);
}
