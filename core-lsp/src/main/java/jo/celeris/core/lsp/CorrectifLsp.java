package jo.celeris.core.lsp;

/**
 * Un correctif rapide du serveur (B-17) : un titre à montrer et une
 * édition d'espace de travail à appliquer — par la voie de transaction,
 * diffable et annulable. Le correctif sans édition (le serveur n'en
 * donne pas toujours) reste un correctif : l'hôte montre le titre,
 * l'édition vide ne fait rien.
 *
 * @param titre   le titre du correctif, montré à l'utilisateur
 * @param edition l'édition d'espace à appliquer, vide si absente
 */
public record CorrectifLsp(String titre, EditionEspaceLsp edition) {
}
