package jo.celeris.android.view;

/**
 * Thème d'éditeur : les couleurs résolues du chemin de dessin (ADR-008 :
 * un seul chemin de dessin, aucun dérivé de thème dans les couches).
 *
 * <p>L'identifiant entre dans la clé du cache de rangées (PIEGE-02) :
 * changer de thème change l'identifiant, jamais les couleurs sous la
 * main.</p>
 *
 * @param id               l'identifiant du thème, unique par thème
 * @param baseTextColor    la couleur de base du texte
 * @param selectionColor   la couleur des fonds de sélection
 * @param cursorColor      la couleur du curseur
 * @param gutterTextColor  la couleur du texte de la gouttière
 */
public record EditorTheme(int id, int baseTextColor, int selectionColor,
        int cursorColor, int gutterTextColor) {
}
