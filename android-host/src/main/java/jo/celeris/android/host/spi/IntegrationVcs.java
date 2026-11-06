package jo.celeris.android.host.spi;

import java.util.List;

/**
 * L'intégration VCS de l'hôte (E-02) : l'état des lignes pour les
 * marques de gouttière (D-03), le blame ligne à ligne et l'historique.
 *
 * <p>Le contrat est synchrone et sans état côté éditeur : l'hôte
 * rafraîchit quand il le décide (révision de référence changée,
 * enregistrement, focus de fichier).</p>
 */
public interface IntegrationVcs {

    /**
     * L'état des lignes du fichier face à la révision de référence.
     *
     * @param fichier le chemin du fichier
     * @return l'état des lignes, vide si le fichier est inconnu
     */
    EtatLignesVcs etatLignes(CheminDoc fichier);

    /**
     * Le blame du fichier : une annotation par ligne connue.
     *
     * @param fichier le chemin du fichier
     * @return les annotations, dans l'ordre des lignes
     */
    List<AnnotationBlame> blame(CheminDoc fichier);

    /**
     * L'historique du fichier, la révision la plus récente en tête.
     *
     * @param fichier le chemin du fichier
     * @return les révisions, la plus récente d'abord
     */
    List<RevisionVcs> historique(CheminDoc fichier);
}
