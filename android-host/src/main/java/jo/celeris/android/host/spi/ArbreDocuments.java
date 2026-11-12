package jo.celeris.android.host.spi;

import java.util.List;

/**
 * La porte étroite vers un arbre de documents du framework d'accès au
 * stockage (E-01) : tout ce que l'adaptateur de production doit savoir
 * demander à l'hôte — les enfants d'un dossier, la lecture, l'écriture
 * et la création. L'implémentation {@link ArbreDocumentsSaf} traduit
 * ces appels en requêtes {@code DocumentsContract} ; les tests et la
 * démonstration parlent à un arbre en mémoire — le contrat est le
 * même.
 *
 * <p>Les identifiants de document sont opaques : ils appartiennent au
 * fournisseur, l'adaptateur ne les interprète jamais.</p>
 */
public interface ArbreDocuments {

    /**
     * Les entrées directement contenues dans le dossier donné.
     *
     * @param idDossier l'identifiant du dossier parent
     * @return les entrées, dossiers et fichiers confondus
     */
    List<EntreeDocument> enfants(String idDossier);

    /**
     * Lit le contenu textuel du fichier donné.
     *
     * @param idFichier l'identifiant du fichier
     * @return le contenu intégral
     */
    String lire(String idFichier);

    /**
     * Lit les octets BRUTS du fichier donné (A-26) : la détection
     * binaire s'y lit, l'aperçu hexadécimal s'y sert — jamais après
     * l'aller-retour du décodage (il détruirait ce qu'il lit : le
     * binaire ne se juge pas décodé).
     *
     * @param idFichier l'identifiant du fichier
     * @return les octets intégraux, bruts
     */
    byte[] lireOctets(String idFichier);

    /**
     * Remplace le contenu du fichier donné.
     *
     * @param idFichier l'identifiant du fichier existant
     * @param contenu   le contenu intégral à écrire
     */
    void remplacer(String idFichier, String contenu);

    /**
     * Crée un fichier ou un dossier nommé dans le parent donné.
     *
     * @param idParent l'identifiant du dossier parent
     * @param nom       le nom de l'entrée à créer
     * @param dossier   vrai pour un dossier, faux pour un fichier
     * @return l'identifiant de l'entrée créée
     */
    String creer(String idParent, String nom, boolean dossier);

    /**
     * Vrai si l'identifiant donné désigne un fichier.
     *
     * @param idDocument l'identifiant à sonder
     * @return vrai si un fichier y répond
     */
    boolean estFichier(String idDocument);
}
