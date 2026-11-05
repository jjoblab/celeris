package jo.celeris.android.view;

import jo.celeris.core.syntax.TokenKind;

/**
 * Palette syntaxique résolue : une encre par sorte de jeton du socle
 * (B-01, ADR-008 — le chemin de dessin ne dérive jamais de thème, les
 * couleurs sont résolues une fois pour toutes ici).
 *
 * <p>Les palettes complètes vivent dans {@link EditorThemes} et sont
 * vérifiées par contraste WCAG (D-13) : chaque encre contraste
 * ≥ 4,5:1 sur le fond de son thème. L'identificateur, la ponctuation et
 * les crochets restent d'ordinaire à l'encre de base — le langage
 * courant ne crie pas. Immuable : la consultation par sorte ne fait
 * qu'une lecture de tableau, sans allocation dans le chemin de dessin
 * (PIEGE-15).</p>
 */
public final class SyntaxPalette {

    private final int[] couleurs;

    /**
     * Construit la palette, une encre par sorte dans l'ordre de
     * {@link TokenKind} (le tableau s'indexe par ordinal).
     *
     * @param keyword      l'encre des mots réservés
     * @param type         l'encre des noms de types
     * @param string       l'encre des chaînes et gabarits
     * @param number       l'encre des littéraux numériques
     * @param comment      l'encre des commentaires
     * @param doc          l'encre de la documentation
     * @param annotation   l'encre des annotations
     * @param operator     l'encre des opérateurs
     * @param punctuation  l'encre de la ponctuation non appariable
     * @param identifier   l'encre des identificateurs simples
     * @param constant     l'encre des constantes nommées
     * @param openBracket  l'encre des crochets ouvrants
     * @param closeBracket l'encre des crochets fermants
     * @param error        l'encre des jetons malformés
     */
    public SyntaxPalette(int keyword, int type, int string, int number,
            int comment, int doc, int annotation, int operator, int punctuation,
            int identifier, int constant, int openBracket, int closeBracket,
            int error) {
        couleurs = new int[] {keyword, type, string, number, comment, doc,
                annotation, operator, punctuation, identifier, constant,
                openBracket, closeBracket, error};
        for (int couleur : couleurs) {
            if ((couleur >>> 24) != 0xFF) {
                throw new IllegalArgumentException(
                        "la couleur doit être opaque : " + couleur);
            }
        }
    }

    /**
     * La palette monochrome : toutes les sortes à la même encre — le
     * thème hérité sans palette propre reste un thème valide, sans
     * coloration.
     *
     * @param encre l'encre unique de toutes les sortes
     * @return la palette monochrome
     */
    public static SyntaxPalette monochrome(int encre) {
        return new SyntaxPalette(encre, encre, encre, encre, encre, encre,
                encre, encre, encre, encre, encre, encre, encre, encre);
    }

    /**
     * L'encre d'une sorte de jeton.
     *
     * @param sorte la sorte de jeton du socle
     * @return la couleur résolue, en ARGB
     */
    public int couleur(TokenKind sorte) {
        return couleurs[sorte.ordinal()];
    }
}
