package jo.celeris.android.view;

import jo.celeris.core.syntax.ScopeThemes;

/**
 * Palettes complètes de l'éditeur : clair, sombre, contraste élevé
 * (D-13, ADR-008 §5 — un thème dédié respectant les rapports de
 * contraste recommandés), palette syntaxique comprise (B-01).
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

    private EditorThemes() {
    }
}
