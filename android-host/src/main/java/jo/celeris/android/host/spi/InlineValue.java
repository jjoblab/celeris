package jo.celeris.android.host.spi;

/**
 * Une valeur à rendre en marge de ligne pendant la pause (E-03) :
 * l'expression et sa valeur, en écriture libre.
 *
 * @param line      la ligne de la valeur, numérotée à partir de un
 * @param expression l'expression évaluée
 * @param value     la valeur rendue
 */
public record InlineValue(int line, String expression, String value) {
}
