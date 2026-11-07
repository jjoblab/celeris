package jo.celeris.android.view;

/**
 * Une pastille de couleur de la gouttière (C-05) : le littéral de
 * couleur résolu en ARGB — « #RRGGBB » prend l'alpha plein, la forme
 * courte « #RGB » s'étend, « #AARRGGBB » se lit telle quelle. Une
 * pastille par ligne, la première.
 *
 * @param ligne   la ligne porteuse, numérotée à partir de zéro
 * @param colonne la colonne du dièse, à partir de un
 * @param argb    la couleur résolue, au format ARGB
 * @param longueur la longueur du littéral, dièse compris
 */
public record PastilleCouleur(int ligne, int colonne, int argb, int longueur) {
}
