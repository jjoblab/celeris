package jo.celeris.core.lsp.types;

/**
 * Un jeton sémantique décodé (B-02) : position absolue — ligne et
 * colonne de début, longueur en caractères — type et modificateurs,
 * les index convenus entre client et serveur à l'initialisation.
 *
 * @param line         la ligne du début, à partir de zéro
 * @param column       la colonne de début en unités UTF-16, à partir de zéro
 * @param length      la longueur en unités UTF-16
 * @param type          l'index du type de jeton dans la légende convenue
 * @param modificateurs le champ de bits des modificateurs
 */
public record SemanticTokenLsp(int line, int column, int length, int type,
                              int modificateurs) {
}
