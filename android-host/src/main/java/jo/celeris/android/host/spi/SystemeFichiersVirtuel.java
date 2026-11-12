package jo.celeris.android.host.spi;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Le système de fichiers virtuel (E-01) : l'abstraction au-dessus des
 * accès cloisonnés du stockage — arbres de documents, périmètre
 * accordé. Les chemins sont validés par construction
 * ({@link CheminDoc}) : la traversée ne passe pas.
 *
 * <p>L'implémentation de référence, en mémoire, sert les tests et les
 * faux de démonstration ; l'adaptateur de production s'appuie sur le
 * framework d'accès au stockage de l'hôte.</p>
 *
 * <p>Les fichiers BINAIRES (A-26) : la détection se demande
 * ({@link #estBinaire}) AVANT l'ouverture texte — l'aperçu
 * hexadécimal borné ({@link #apercuBinaire}) sert la lecture sûre,
 * la session qui le montre se pose en lecture seule ; l'écriture
 * par la voie texte se refuse ({@link FichierBinaireException}) —
 * jamais l'aller-retour destructeur. Les implémentations qui ne
 * portent que du texte n'en font rien : leurs défauts disent non.</p>
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

    /**
     * Vrai quand le fichier donné est binaire (A-26) : la décision se
     * lit sur les octets bruts, dans le préfixe — l'ouverture texte
     * se refuse alors, l'aperçu hexadécimal sert la lecture seule.
     * Les systèmes qui ne portent que du texte répondent toujours
     * non.
     *
     * @param chemin le chemin du fichier à sonder
     * @return vrai si le fichier se refuse au texte
     */
    default boolean estBinaire(CheminDoc chemin) {
        return false;
    }

    /**
     * L'aperçu hexadécimal du fichier binaire (A-26) : borné (256
     * lignes, 4 Kio), lecture sûre — null quand le fichier n'est pas
     * binaire ou n'existe pas. La session qui le montre se pose en
     * lecture seule.
     *
     * @param chemin le chemin du fichier binaire
     * @return le vidage hexadécimal rendu, ligne à ligne, ou null
     */
    default @Nullable String apercuBinaire(CheminDoc chemin) {
        return null;
    }
}
