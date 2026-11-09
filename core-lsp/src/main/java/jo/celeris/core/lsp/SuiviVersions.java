package jo.celeris.core.lsp;

/**
 * Le suivi des versions de documents (ADR-005) : qui porte l'état des
 * synchronisations — ouverture à la version 1, chaque changement à la
 * version donnée par l'hôte, fermeture qui oublie. L'ordonnanceur
 * interroge ce suivi pour péimer les réponses qui traversent un
 * changement de version : la réponse doit être celle du document
 * qu'on a quitté.
 *
 * <p>La frontière protocolaire (ADR-015) l'implémente pour sa
 * session ; le contrat reste au domaine, la mécanique n'y connaît
 * aucun fil ni aucune trame.</p>
 */
public interface SuiviVersions {

    /**
     * La dernière version envoyée pour un document.
     *
     * @param uri l'uri du document
     * @return la version, ou -1 si le document n'est pas suivi
     */
    int versionDe(String uri);
}
