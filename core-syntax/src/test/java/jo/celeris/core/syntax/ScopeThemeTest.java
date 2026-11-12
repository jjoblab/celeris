package jo.celeris.core.syntax;

import jo.celeris.core.syntax.scope.ScopeStacks;
import jo.celeris.core.syntax.scope.ScopeStyle;
import jo.celeris.core.syntax.scope.ScopeTheme;
import jo.celeris.core.syntax.scope.ScopeThemes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Les tests du thème par portées (M7.6, H-07) : la résolution se fait
 * contre la PILE — la meilleure encre gagne, les attributs se
 * cumulent — et le résultat se garde en cache : la même instance
 * ressort, le tracé n'alloue rien par image (PIEGE-15).
 */
final class ScopeThemeTest {

    private static int pile(String... portees) {
        int pile = ScopeStacks.NONE;
        for (String portee : portees) {
            pile = ScopeStacks.push(pile, portee);
        }
        return pile;
    }

    @Test
    void laRegleQuiMatcheRendSonStyle() {
        ScopeTheme theme = ScopeTheme.of(List.of(
                new ScopeTheme.Rule("keyword", ScopeStyle.ink(0xFF123456))));
        ScopeStyle style = theme.styleFor(
                pile("source.demo", "keyword.other"));
        assertNotNull(style);
        assertEquals(0xFF123456, style.foreground());
    }

    @Test
    void laMeilleureEncreGagne() {
        ScopeTheme theme = ScopeTheme.of(List.of(
                new ScopeTheme.Rule("keyword",
                        ScopeStyle.ink(0xFF111111)),
                new ScopeTheme.Rule("keyword.operator",
                        ScopeStyle.ink(0xFF222222))));
        ScopeStyle style = theme.styleFor(
                pile("source.demo", "keyword.operator.assignment"));
        assertNotNull(style);
        assertEquals(0xFF222222, style.foreground(),
                "le sélecteur le plus profond emporte l'encre");
    }

    @Test
    void lesAttributsSeCumulentEntreRegles() {
        ScopeTheme theme = ScopeTheme.of(List.of(
                new ScopeTheme.Rule("entity.name.function",
                        new ScopeStyle(0, true, false, false)),
                new ScopeTheme.Rule("entity.name",
                        new ScopeStyle(0xFF333333, false, true, false))));
        ScopeStyle style = theme.styleFor(
                pile("source.demo", "entity.name.function.def"));
        assertNotNull(style);
        assertTrue(style.bold() && style.italic(),
                "la graisse d'une règle et l'italique de l'autre se cumulent");
        assertEquals(0xFF333333, style.foreground(),
                "l'encre vient de la seule règle qui en fixe une");
    }

    @Test
    void sansRegleQuiMatcheLeStyleEstAbsent() {
        ScopeTheme theme = ScopeTheme.of(List.of(
                new ScopeTheme.Rule("keyword", ScopeStyle.ink(0xFF123456))));
        assertNull(theme.styleFor(pile("source.demo", "string.quoted")));
        assertNull(ScopeTheme.empty().styleFor(
                pile("source.demo", "keyword.other")));
    }

    @Test
    void leResultatSeGardeEnCacheParPile() {
        ScopeTheme theme = ScopeTheme.of(List.of(
                new ScopeTheme.Rule("keyword", ScopeStyle.ink(0xFF123456))));
        int pile = pile("source.demo", "keyword.other");
        ScopeStyle premier = theme.styleFor(pile);
        ScopeStyle second = theme.styleFor(pile);
        assertSame(premier, second,
                "la même instance ressort du cache — aucune allocation par image");
    }

    @Test
    void leSelecteurMalFormeRefuseLeTheme() {
        assertThrows(IllegalArgumentException.class, () -> ScopeTheme.of(
                List.of(new ScopeTheme.Rule(",",
                        ScopeStyle.ink(0xFF123456)))));
        assertThrows(IllegalArgumentException.class, () -> ScopeTheme.of(
                List.of(new ScopeTheme.Rule("- seul",
                        ScopeStyle.ink(0xFF123456)))));
    }

    @Test
    void lesThemesLivresResolventChaqueCategorie() {
        for (ScopeTheme theme : List.of(ScopeThemes.light(),
                ScopeThemes.dark(), ScopeThemes.highContrast())) {
            assertNotNull(theme.styleFor(
                    pile("source.demo", "keyword.other")), "keyword résout");
            assertNotNull(theme.styleFor(
                    pile("source.demo", "string.quoted")), "string résout");
            assertNotNull(theme.styleFor(pile("source.demo", "comment.line")),
                    "comment résout");
            assertNotNull(theme.styleFor(pile("source.demo",
                    "constant.numeric.integer")), "number résout");
            assertNotNull(theme.styleFor(pile("source.demo",
                    "entity.name.type.class")), "type résout");
        }
    }

    @Test
    void lesThemesLivresResolventCommeLaPaletteDeLaVue() {
        // mêmes teintes que la palette CLAIR de la vue (android-view) :
        // le mécanisme change, pas l'encre — le banc WCAG de la vue
        // s'appuie dessus, le sien propre suit
        ScopeStyle keyword = ScopeThemes.light().styleFor(
                pile("source.demo", "keyword.other"));
        assertNotNull(keyword);
        assertEquals(0xFF6A1B9A, keyword.foreground());
        ScopeStyle chaine = ScopeThemes.light().styleFor(
                pile("source.demo", "string.quoted.double"));
        assertNotNull(chaine);
        assertEquals(0xFF2E7D32, chaine.foreground());
    }
}
