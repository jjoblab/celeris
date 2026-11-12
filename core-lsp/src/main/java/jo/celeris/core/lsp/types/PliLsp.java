package jo.celeris.core.lsp.types;

/**
 * Un pli servi par le serveur (H-12) : la plage de lignes qu'il
 * couvre — début inclus, fin incluse au protocole, ramenée ici en
 * fin EXCLUSIVE pour rejoindre la convention de l'arbre commun ; le
 * consommateur fusionne ces plis avec les siens, le serveur n'est
 * qu'une source parmi d'autres.
 *
 * @param ligneDebut la ligne de début, à partir de zéro, incluse
 * @param ligneFin   la ligne de fin, à partir de zéro, EXCLUSIVE
 */
public record PliLsp(int ligneDebut, int ligneFin) {
}
