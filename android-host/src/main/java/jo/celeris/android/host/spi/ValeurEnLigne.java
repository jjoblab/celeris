package jo.celeris.android.host.spi;

/**
 * Une valeur à rendre en marge de ligne pendant la pause (E-03) :
 * l'expression et sa valeur, en écriture libre.
 *
 * @param ligne      la ligne de la valeur, numérotée à partir de un
 * @param expression l'expression évaluée
 * @param valeur     la valeur rendue
 */
public record ValeurEnLigne(int ligne, String expression, String valeur) {
}
