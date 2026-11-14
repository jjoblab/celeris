package jo.celeris.android.view;

/**
 * Un indice en ligne (inlay hint — l'audit Android Studio du
 * 2026-11-14, complété le même jour : « je vois que le inlay hint
 * fonctionne avec seulement var et val, assure-toi qu'il soit
 * complet comme Android Studio ») : le RENDU qui se lit à l'offset
 * donné, à l'encre muette italique — le type d'un « var », le nom
 * d'un paramètre muet, jamais dans le flux : le texte ne se décale
 * pas, l'indice se lit par-dessus la marge. La vue ne connaît NI le
 * langage, NI le serveur : l'hôte transpose (l'analyse locale ou la
 * réponse inlayHint du serveur), le rendu seul vit ici.
 *
 * @param offset l'offset de l'indice DANS LA LIGNE (la colonne du
 *               texte où l'indice se lit — l'IDE de référence ancre
 *               les noms de paramètres à leur argument)
 * @param ligne  la ligne de l'indice, numérotée à partir de UN (la
 *               convention de la SPI de l'hôte)
 * @param rendu  le rendu de l'indice, en écriture libre
 *              (« : int », « : String », « solde »)
 */
public record IndiceInlay(int ligne, int offset, String rendu) {
}
