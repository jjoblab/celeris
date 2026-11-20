package jo.celeris.android.view;

import jo.celeris.core.syntax.scope.ScopeThemes;

/**
 * Palettes complètes de l'éditeur : clair, sombre, contraste élevé
 * (D-13, ADR-008 §5 — un thème dédié respectant les rapports de
 * contraste recommandés), palette syntaxique comprise (B-01) — et
 * les six thèmes du dépôt de départ (D-01, l'analyse croisée) :
 * dracula, monokai, one-dark, solarized clair, nord, github clair,
 * leurs encres ajustées aux seuils WCAG du banc (l'identité de
 * chaque thème vit dans ses teintes, les encres faibles montent
 * au seuil).
 *
 * <p>Les couleurs sont résolues une fois pour toutes : le chemin de
 * dessin ne dérive jamais de thème (ADR-008). Les rapports de contraste
 * sont vérifiés automatiquement par {@code EditorThemesTest} et
 * {@code SyntaxPaletteTest} — texte ≥ 4,5:1 sur le fond et sur le fond
 * de sélection, gouttière ≥ 4,5:1, curseur ≥ 3:1, chaque encre
 * syntaxique ≥ 4,5:1 sur le fond.</p>
 *
 * <p>Le fond de gouttière est un VOILE TRANSLUCIDE posé sur le fond de
 * la zone de texte (M7.5, redressement — la gouttière du dépôt de
 * départ se teintait ainsi) : ce qui vit derrière transparaît, jamais
 * un plein opaque.</p>
 */
public final class EditorThemes {

    /** Thème clair : fond blanc, encre presque noire, sélection bleu pâle,
     *  gouttière voilée à l'encre douce. */
    public static final EditorTheme CLAIR = new EditorTheme(1,
            0xFFFFFFFF, 0xFF202124, 0xFFD3E3FD, 0xFF1A73E8, 0xFF5F6368,
            0x14202124,
            new SyntaxPalette(
                    0xFF6A1B9A, 0xFF00695C, 0xFF2E7D32, 0xFF1565C0,
                    0xFF5F6368, 0xFF6D4C41, 0xFF8D5000, 0xFF455A64,
                    0xFF202124, 0xFF202124, 0xFFAD1457, 0xFF202124,
                    0xFF202124, 0xFFC62828),
            ScopeThemes.light());

    /** Thème sombre : fond anthracite, encre claire, sélection bleu
     *  profond, gouttière voilée de noir. */
    public static final EditorTheme SOMBRE = new EditorTheme(2,
            0xFF1E1F22, 0xFFE3E3E3, 0xFF2D4A7A, 0xFF8AB4F8, 0xFF9AA0A6,
            0x2E000000,
            new SyntaxPalette(
                    0xFFCC7832, 0xFF7FDBCA, 0xFFC3E88D, 0xFF82AAFF,
                    0xFF9AA0A6, 0xFFB4AFA3, 0xFFFFCB6B, 0xFFB0BEC5,
                    0xFFE3E3E3, 0xFFE3E3E3, 0xFFF06292, 0xFFE3E3E3,
                    0xFFE3E3E3, 0xFFFF8A80),
            ScopeThemes.dark());

    /** Thème contraste élevé : fond noir, encre blanche, sélection bleu
     *  vif, gouttière voilée de blanc. */
    public static final EditorTheme CONTRASTE_ELEVE = new EditorTheme(3,
            0xFF000000, 0xFFFFFFFF, 0xFF004AFF, 0xFFFFFF00, 0xFFFFFFFF,
            0x33FFFFFF,
            new SyntaxPalette(
                    0xFFFFFF00, 0xFF00FFFF, 0xFF66FF66, 0xFFFFA500,
                    0xFFFFFF99, 0xFFE0E0E0, 0xFFFF00FF, 0xFFF0F0F0,
                    0xFFFFFFFF, 0xFFFFFFFF, 0xFFFF69B4, 0xFFFFFFFF,
                    0xFFFFFFFF, 0xFFFF4444),
            ScopeThemes.highContrast());

    /** Dracula (D-01) : fond nuit profonde, encre laiteuse, keyword
     *  rose, type cyan pâle, chaîne jaune — le classique sombre. */
    public static final EditorTheme DRACULA = new EditorTheme(4,
            0xFF282A36, 0xFFF8F8F2, 0xFF44475A, 0xFFFF79C6, 0xFF8B92B8,
            0x2EFFFFFF,
            new SyntaxPalette(
                    0xFFFF79C6, 0xFF8BE9FD, 0xFFF1FA8C, 0xFFBD93F9,
                    0xFF8F92A7, 0xFF9CA0B4, 0xFFFF79C6, 0xFFF8F8F2,
                    0xFFF8F8F2, 0xFFF8F8F2, 0xFFBD93F9, 0xFFF8F8F2,
                    0xFFF8F8F2, 0xFFFF5858),
            ScopeThemes.dark());

    /** Monokai (D-01) : fond charbon chaud, keyword rose vif, type
     *  cyan, chaîne jaune d'or — le thème de Sublime. */
    public static final EditorTheme MONOKAI = new EditorTheme(5,
            0xFF272822, 0xFFF8F8F2, 0xFF49483E, 0xFFFD971F, 0xFFA8AB90,
            0x2EFFFFFF,
            new SyntaxPalette(
                    0xFFFF4591, 0xFF66D9EF, 0xFFE6DB74, 0xFFAE81FF,
                    0xFF8F908A, 0xFFA6AB95, 0xFFFF4591, 0xFFF8F8F2,
                    0xFFF8F8F2, 0xFFF8F8F2, 0xFFAE81FF, 0xFFF8F8F2,
                    0xFFF8F8F2, 0xFFFF4591),
            ScopeThemes.dark());

    /** One Dark (D-01) : fond ardoise, encre gris bleuté, keyword
     *  mauve, type ambre — le thème d'Atom. */
    public static final EditorTheme ONE_DARK = new EditorTheme(6,
            0xFF282C34, 0xFFD7DAE0, 0xFF4B5262, 0xFF61AFEF, 0xFF8B949E,
            0x2EFFFFFF,
            new SyntaxPalette(
                    0xFFC678DD, 0xFFE5C07B, 0xFF98C379, 0xFFD19A66,
                    0xFF90959F, 0xFF9CA3AF, 0xFFE5C07B, 0xFFD7DAE0,
                    0xFFD7DAE0, 0xFFD7DAE0, 0xFFD19A66, 0xFFD7DAE0,
                    0xFFD7DAE0, 0xFFE5717A),
            ScopeThemes.dark());

    /** Solarized clair (D-01) : fond crème, encre ardoise bleutée,
     *  keyword vert, type bleu — le thème d'Ethan Schoonover. */
    public static final EditorTheme SOLARIZED_CLAIR = new EditorTheme(7,
            0xFFFDF6E3, 0xFF4A5B68, 0xFFEFE9DA, 0xFF1A6FB5, 0xFF55697A,
            0x14202124,
            new SyntaxPalette(
                    0xFF647800, 0xFF0E73BA, 0xFF936700, 0xFFCB2E7A,
                    0xFF617182, 0xFF586E75, 0xFF0E73BA, 0xFF4A5B68,
                    0xFF4A5B68, 0xFF4A5B68, 0xFFC4440F, 0xFF4A5B68,
                    0xFF4A5B68, 0xFFD52B28),
            ScopeThemes.light());

    /** Nord (D-01) : fond bleu nuit polaire, encre neige, keyword
     *  bleu glacé — la palette du Nord Arctique. */
    public static final EditorTheme NORD = new EditorTheme(8,
            0xFF2E3440, 0xFFD8DEE9, 0xFF434C5E, 0xFF88C0D0, 0xFF9AACB8,
            0x2EFFFFFF,
            new SyntaxPalette(
                    0xFF81A1C1, 0xFF8FBCBB, 0xFFA3BE8C, 0xFFB892B1,
                    0xFF959EAF, 0xFF9DA6B5, 0xFF8FBCBB, 0xFFD8DEE9,
                    0xFFD8DEE9, 0xFFD8DEE9, 0xFFB892B1, 0xFFD8DEE9,
                    0xFFD8DEE9, 0xFFE0828B),
            ScopeThemes.dark());

    /** GitHub clair (D-01) : fond blanc, encre graphite, keyword
     *  rouge brique — le thème de github.com. */
    public static final EditorTheme GITHUB_CLAIR = new EditorTheme(9,
            0xFFFFFFFF, 0xFF1F2328, 0xFFBBE0F5, 0xFF0969DA, 0xFF6E7781,
            0x14202124,
            new SyntaxPalette(
                    0xFFCF222E, 0xFF0550AE, 0xFF0A3069, 0xFF0550AE,
                    0xFF6E7781, 0xFF57606A, 0xFF953800, 0xFF1F2328,
                    0xFF1F2328, 0xFF1F2328, 0xFF0550AE, 0xFF1F2328,
                    0xFF1F2328, 0xFFCF222E),
            ScopeThemes.light());

    private EditorThemes() {
    }
}
