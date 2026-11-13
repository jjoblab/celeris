package jo.celeris.android.view;

/**
 * Une valeur de débogage en ligne (E-03) : le RENDU qui s'affiche en
 * fin de ligne pendant la pause — l'expression et sa valeur, en
 * écriture libre. La vue ne connaît pas le débogueur : l'hôte
 * transpose, le rendu seul vit ici.
 *
 * @param ligne la ligne de la valeur, numérotée à partir de UN (la
 *              convention de la SPI de l'hôte)
 * @param rendu le rendu de la valeur, en écriture libre
 */
public record ValeurLigne(int ligne, String rendu) {
}
