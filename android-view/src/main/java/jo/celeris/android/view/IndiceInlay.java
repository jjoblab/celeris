package jo.celeris.android.view;

/**
 * Un indice en ligne (inlay hint — l'audit Android Studio du
 * 2026-11-14 : le propriétaire — « aussi la fonctionnalité de inlay
 * hint ») : le RENDU qui se lit en fin de ligne UTILE, à l'encre
 * muette italique — le type d'un « var », jamais dans le flux : le
 * texte ne se décale pas, l'indice se lit par-dessus la marge. La vue
 * ne connaît NI le langage, NI le serveur : l'hôte transpose
 * (l'analyse des « var » locaux ou la réponse inlayHint du serveur),
 * le rendu seul vit ici.
 *
 * @param ligne la ligne de l'indice, numérotée à partir de UN (la
 *              convention de la SPI de l'hôte)
 * @param rendu le rendu de l'indice, en écriture libre
 *              (« : int », « : String »)
 */
public record IndiceInlay(int ligne, String rendu) {
}
