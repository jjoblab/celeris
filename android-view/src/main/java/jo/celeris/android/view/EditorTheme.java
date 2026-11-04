package jo.celeris.android.view;

/**
 * Thème d'éditeur : les couleurs résolues du chemin de dessin (ADR-008 :
 * un seul chemin de dessin, aucun dérivé de thème dans les couches).
 *
 * <p>L'identifiant entre dans la clé du cache de rangées (PIEGE-02) :
 * changer de thème change l'identifiant, jamais les couleurs sous la
 * main. Les palettes complètes (clair, sombre, contraste élevé) vivent
 * dans {@link EditorThemes}, vérifiées par contraste WCAG (D-13).</p>
 *
 * @param id              l'identifiant du thème, unique par thème
 * @param backgroundColor le fond de la zone de texte
 * @param baseTextColor   la couleur de base du texte
 * @param selectionColor  le fond des sélections
 * @param cursorColor     la couleur du curseur
 * @param gutterTextColor la couleur du texte de la gouttière
 */
public record EditorTheme(int id, int backgroundColor, int baseTextColor,
        int selectionColor, int cursorColor, int gutterTextColor) {
}
