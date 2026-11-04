package jo.celeris.core.syntax;

/**
 * Une entrée du plan du fichier (B-16) : catégorie, nom, bornes absolues
 * et lignes — tout ce qu'il faut pour naviguer et afficher.
 *
 * @param category    la catégorie générique (icône, tri)
 * @param name        le nom déclaré
 * @param startOffset début absolu de la déclaration
 * @param endOffset   fin exclusive de la déclaration
 * @param startLine   ligne de la déclaration
 * @param endLine     ligne de fin de la déclaration
 */
public record OutlineEntry(NodeCategory category, String name, int startOffset,
                           int endOffset, int startLine, int endLine) {
}
