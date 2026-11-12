package jo.celeris.core.lsp.types;

/**
 * Un surlignage d'occurrences servi par le serveur (H-12) : la plage
 * et son genre — texte 1, lecture 2, écriture 3 — les occurrences du
 * symbole sous le curseur s'y peignent.
 *
 * @param plage la plage surlignée
 * @param genre le genre au protocole (1 texte, 2 lecture, 3 écriture),
 *              zéro si le serveur n'en dit rien
 */
public record SurlignageLsp(PlageLsp plage, int genre) {
}
