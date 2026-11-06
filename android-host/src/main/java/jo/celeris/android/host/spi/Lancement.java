package jo.celeris.android.host.spi;

/**
 * Un lancement d'exécution ou de test (E-04) : son identifiant, son
 * fichier et sa nature.
 *
 * @param id      l'identifiant du lancement, unique par exécution
 * @param fichier le chemin du fichier lancé
 * @param test    vrai pour une exécution de test, faux pour un run
 */
public record Lancement(String id, CheminDoc fichier, boolean test) {
}
