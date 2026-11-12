package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import jo.celeris.core.syntax.scope.ScopeStacks;
import jo.celeris.core.syntax.scope.ScopeStyle;
import jo.celeris.core.syntax.TokenKind;

import org.junit.jupiter.api.Test;

/**
 * Les thèmes par portées de la vue (M7.6, H-07) : chaque encre des
 * thèmes livrés contraste ≥ 4,5:1 sur le fond de SON thème (D-13) —
 * les teintes reprennent celles de la palette par sorte, vérifiées de
 * longue date ; le banc s'assure que le MÉCANISME des portées ne les
 * a pas perdue en route. Le tracé par portées se vérifie au peintre
 * ({@code TextPainterScopesTest}).
 */
final class ScopeThemeContrastTest {

    /** Les sondes par catégorie : une portée de tête représentative. */
    private static final List<String> SONDES = List.of(
            "comment.line.probe", "comment.block.documentation.probe",
            "string.quoted.probe", "constant.numeric.probe",
            "keyword.other.probe", "keyword.operator.probe",
            "entity.name.type.probe", "support.constant.probe",
            "meta.annotation.probe", "invalid.illegal.probe");

    @Test
    void chaqueEncreDesThemesLivresContrasteSurSonFond() {
        for (EditorTheme theme : List.of(EditorThemes.CLAIR,
                EditorThemes.SOMBRE, EditorThemes.CONTRASTE_ELEVE)) {
            for (String sonde : SONDES) {
                int pile = ScopeStacks.push(
                        ScopeStacks.root("source.probe"), sonde);
                ScopeStyle style = theme.portees().styleFor(pile);
                assertNotNull(style, sonde + " résout dans " + theme.id());
                assertTrue(style.hasInk(),
                        sonde + " fixe une encre dans " + theme.id());
                double rapport = WcagContrast.ratio(style.foreground(),
                        theme.backgroundColor());
                assertTrue(rapport >= 4.5,
                        sonde + " contraste à " + String.format("%.2f", rapport)
                                + ":1 sur le fond du thème " + theme.id());
            }
        }
    }

    @Test
    void lesSondesSansPileRestentALaPalette() {
        // les analyseurs écrits ne portent pas de pile : la palette par
        // sorte les sert, le banc WCAG de la palette (SyntaxPaletteTest)
        // la vérifie — ici, la pile ABSENTE ne résout jamais par portées
        ScopeStyle style = EditorThemes.CLAIR.portees()
                .styleFor(ScopeStacks.NONE);
        assertTrue(style == null,
                "l'absence de pile ne résout pas par portées");
        assertTrue(EditorThemes.CLAIR.syntaxe()
                .couleur(TokenKind.KEYWORD) != 0);
    }
}
