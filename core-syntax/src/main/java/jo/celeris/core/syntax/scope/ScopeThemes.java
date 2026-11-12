package jo.celeris.core.syntax.scope;

import java.util.List;

/**
 * Les thèmes par portées livrés avec l'éditeur (M7.6, H-07) : clair,
 * sombre et contraste élevé, écrits ici. Chaque encre reprend celle
 * de la catégorie correspondante des palettes par sorte de la vue
 * ({@code EditorThemes}) — la résolution change de MÉCANISME (la
 * pile de portées, ses sélecteurs, leur spécificité), pas de teinte :
 * les encres vérifiées WCAG (D-13) restent les mêmes, la vue s'en
 * assure par son banc.
 */
public final class ScopeThemes {

    private ScopeThemes() {
    }

    /**
     * Le thème clair : les encres claires de la vue.
     *
     * @return le thème par portées du thème clair
     */
    public static ScopeTheme light() {
        return ScopeTheme.of(List.of(
                new ScopeTheme.Rule("comment.block.documentation",
                        ScopeStyle.ink(0xFF6D4C41)),
                new ScopeTheme.Rule("comment",
                        ScopeStyle.ink(0xFF5F6368)),
                new ScopeTheme.Rule("string",
                        ScopeStyle.ink(0xFF2E7D32)),
                new ScopeTheme.Rule("constant.numeric",
                        ScopeStyle.ink(0xFF1565C0)),
                new ScopeTheme.Rule("keyword.operator",
                        ScopeStyle.ink(0xFF455A64)),
                new ScopeTheme.Rule("keyword, storage",
                        ScopeStyle.ink(0xFF6A1B9A)),
                new ScopeTheme.Rule("entity.name.type, entity.name.section,"
                        + " entity.name.tag, support.type",
                        ScopeStyle.ink(0xFF00695C)),
                new ScopeTheme.Rule("constant, support.constant",
                        ScopeStyle.ink(0xFFAD1457)),
                new ScopeTheme.Rule("meta.annotation",
                        ScopeStyle.ink(0xFF8D5000)),
                new ScopeTheme.Rule("invalid.illegal",
                        ScopeStyle.ink(0xFFC62828))));
    }

    /**
     * Le thème sombre : les encres sombres de la vue.
     *
     * @return le thème par portées du thème sombre
     */
    public static ScopeTheme dark() {
        return ScopeTheme.of(List.of(
                new ScopeTheme.Rule("comment.block.documentation",
                        ScopeStyle.ink(0xFFB4AFA3)),
                new ScopeTheme.Rule("comment",
                        ScopeStyle.ink(0xFF9AA0A6)),
                new ScopeTheme.Rule("string",
                        ScopeStyle.ink(0xFFC3E88D)),
                new ScopeTheme.Rule("constant.numeric",
                        ScopeStyle.ink(0xFF82AAFF)),
                new ScopeTheme.Rule("keyword.operator",
                        ScopeStyle.ink(0xFFB0BEC5)),
                new ScopeTheme.Rule("keyword, storage",
                        ScopeStyle.ink(0xFFCC7832)),
                new ScopeTheme.Rule("entity.name.type, entity.name.section,"
                        + " entity.name.tag, support.type",
                        ScopeStyle.ink(0xFF7FDBCA)),
                new ScopeTheme.Rule("constant, support.constant",
                        ScopeStyle.ink(0xFFF06292)),
                new ScopeTheme.Rule("meta.annotation",
                        ScopeStyle.ink(0xFFFFCB6B)),
                new ScopeTheme.Rule("invalid.illegal",
                        ScopeStyle.ink(0xFFFF8A80))));
    }

    /**
     * Le thème contraste élevé : les encres vives de la vue.
     *
     * @return le thème par portées du contraste élevé
     */
    public static ScopeTheme highContrast() {
        return ScopeTheme.of(List.of(
                new ScopeTheme.Rule("comment.block.documentation",
                        ScopeStyle.ink(0xFFE0E0E0)),
                new ScopeTheme.Rule("comment",
                        ScopeStyle.ink(0xFFFFFF99)),
                new ScopeTheme.Rule("string",
                        ScopeStyle.ink(0xFF66FF66)),
                new ScopeTheme.Rule("constant.numeric",
                        ScopeStyle.ink(0xFFFFA500)),
                new ScopeTheme.Rule("keyword.operator",
                        ScopeStyle.ink(0xFFF0F0F0)),
                new ScopeTheme.Rule("keyword, storage",
                        ScopeStyle.ink(0xFFFFFF00)),
                new ScopeTheme.Rule("entity.name.type, entity.name.section,"
                        + " entity.name.tag, support.type",
                        ScopeStyle.ink(0xFF00FFFF)),
                new ScopeTheme.Rule("constant, support.constant",
                        ScopeStyle.ink(0xFFFF69B4)),
                new ScopeTheme.Rule("meta.annotation",
                        ScopeStyle.ink(0xFFFF00FF)),
                new ScopeTheme.Rule("invalid.illegal",
                        ScopeStyle.ink(0xFFFF4444))));
    }
}
