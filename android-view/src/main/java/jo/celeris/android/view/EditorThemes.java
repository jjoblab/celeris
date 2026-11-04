package jo.celeris.android.view;

/**
 * Palettes complètes de l'éditeur : clair, sombre, contraste élevé
 * (D-13, ADR-008 §5 — un thème dédié respectant les rapports de
 * contraste recommandés).
 *
 * <p>Les couleurs sont résolues une fois pour toutes : le chemin de
 * dessin ne dérive jamais de thème (ADR-008). Les rapports de contraste
 * sont vérifiés automatiquement par {@code EditorThemesTest} — texte
 * ≥ 4,5:1 sur le fond et sur le fond de sélection, gouttière ≥ 4,5:1,
 * curseur ≥ 3:1.</p>
 */
public final class EditorThemes {

    /** Thème clair : fond blanc, encre presque noire, sélection bleu pâle. */
    public static final EditorTheme CLAIR = new EditorTheme(1,
            0xFFFFFFFF, 0xFF202124, 0xFFD3E3FD, 0xFF1A73E8, 0xFF5F6368);

    /** Thème sombre : fond anthracite, encre claire, sélection bleu profond. */
    public static final EditorTheme SOMBRE = new EditorTheme(2,
            0xFF1E1F22, 0xFFE3E3E3, 0xFF2D4A7A, 0xFF8AB4F8, 0xFF9AA0A6);

    /** Thème contraste élevé : fond noir, encre blanche, sélection bleu vif. */
    public static final EditorTheme CONTRASTE_ELEVE = new EditorTheme(3,
            0xFF000000, 0xFFFFFFFF, 0xFF004AFF, 0xFFFFFF00, 0xFFFFFFFF);

    private EditorThemes() {
    }
}
