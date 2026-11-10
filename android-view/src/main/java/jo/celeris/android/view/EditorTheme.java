package jo.celeris.android.view;

import jo.celeris.core.syntax.ScopeTheme;

/**
 * Thème d'éditeur : les couleurs résolues du chemin de dessin (ADR-008 :
 * un seul chemin de dessin, aucun dérivé de thème dans les couches).
 *
 * <p>L'identifiant entre dans la clé du cache de rangées (PIEGE-02) :
 * changer de thème change l'identifiant, jamais les couleurs sous la
 * main. Les palettes complètes (clair, sombre, contraste élevé) vivent
 * dans {@link EditorThemes}, vérifiées par contraste WCAG (D-13) — la
 * palette syntaxique comprise (B-01).</p>
 *
 * <p>Le thème par portées (M7.6, H-07) : les jetons qui portent une
 * pile de portées (la voie des grammaires) y résolvent leur couleur
 * par sélecteurs TextMate — la palette par sorte reste le repli des
 * analyseurs écrits et des piles sans règle.</p>
 *
 * @param id              l'identifiant du thème, unique par thème
 * @param backgroundColor le fond de la zone de texte
 * @param baseTextColor   la couleur de base du texte
 * @param selectionColor  le fond des sélections
 * @param cursorColor     la couleur du curseur
 * @param gutterTextColor la couleur du texte de gouttière
 * @param gutterBackground le fond TRANSLUCIDE de la gouttière — un voile
 *                        posé sur le fond de la zone de texte, jamais
 *                        un plein opaque (M7.5, redressement : le
 *                        dépôt de départ teintait sa gouttière ainsi)
 * @param syntaxe         la palette syntaxique résolue, une encre par
 *                        sorte de jeton
 * @param portees         le thème par portées des jetons grammaticaux
 */
public record EditorTheme(int id, int backgroundColor, int baseTextColor,
        int selectionColor, int cursorColor, int gutterTextColor,
        int gutterBackground, SyntaxPalette syntaxe, ScopeTheme portees) {

    /**
     * Le thème hérité à six champs : la palette syntaxique est
     * monochrome à l'encre de base — un thème sans coloration déclarée
     * reste un thème valide. Le fond de gouttière reste SANS voile
     * (alpha nul) : la gouttière vit sur le fond de la zone de texte.
     * Le thème par portées reste vide : tout retombe à la palette.
     *
     * @param id              l'identifiant du thème, unique par thème
     * @param backgroundColor le fond de la zone de texte
     * @param baseTextColor   la couleur de base du texte
     * @param selectionColor  le fond des sélections
     * @param cursorColor     la couleur du curseur
     * @param gutterTextColor la couleur du texte de gouttière
     */
    public EditorTheme(int id, int backgroundColor, int baseTextColor,
            int selectionColor, int cursorColor, int gutterTextColor) {
        this(id, backgroundColor, baseTextColor, selectionColor, cursorColor,
                gutterTextColor, 0x00000000,
                SyntaxPalette.monochrome(baseTextColor), ScopeTheme.empty());
    }

    /**
     * Le thème à palette seule : les portées restent vides, la
     * couleur vient de la palette par sorte.
     *
     * @param id              l'identifiant du thème, unique par thème
     * @param backgroundColor le fond de la zone de texte
     * @param baseTextColor   la couleur de base du texte
     * @param selectionColor  le fond des sélections
     * @param cursorColor     la couleur du curseur
     * @param gutterTextColor la couleur du texte de gouttière
     * @param gutterBackground le fond translucide de la gouttière
     * @param syntaxe         la palette syntaxique résolue
     */
    public EditorTheme(int id, int backgroundColor, int baseTextColor,
            int selectionColor, int cursorColor, int gutterTextColor,
            int gutterBackground, SyntaxPalette syntaxe) {
        this(id, backgroundColor, baseTextColor, selectionColor, cursorColor,
                gutterTextColor, gutterBackground, syntaxe,
                ScopeTheme.empty());
    }
}
