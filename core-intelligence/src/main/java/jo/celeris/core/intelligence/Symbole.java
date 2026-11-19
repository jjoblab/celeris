package jo.celeris.core.intelligence;

import jo.celeris.core.syntax.NodeCategory;

/**
 * Un symbole déclaré de l'index hors ligne (ADR-005) : ce que le plan
 * du même arbre que la coloration a nommé — catégorie, uri, bornes
 * absolues et ligne de déclaration. La navigation « aller à un
 * symbole » saute à ces coordonnées sans serveur.
 *
 * @param nom       le nom déclaré
 * @param categorie la catégorie générique du plan
 * @param uri       l'uri du fichier porteur
 * @param start     l'offset absolu du début de la déclaration
 * @param end       l'offset absolu de fin (exclusif)
 * @param line     la ligne de la déclaration
 */
public record Symbole(String nom, NodeCategory categorie, String uri,
                      int start, int end, int line) {
}
