package jo.celeris.core.language;

import jo.celeris.core.syntax.scope.ScopeStacks;
import jo.celeris.core.syntax.scope.ScopeStyle;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;




import org.junit.jupiter.api.Test;

/**
 * Les tests de l'importeur de thèmes {@code tokenColors} (M7.6, H-07) :
 * le format de VS Code se lit — nom, couleurs d'éditeur, règles au
 * sélecteur chaîne ou liste, encre aux deux formats, attributs de
 * police — chaque méprise se refuse avec sa position.
 */
final class TokenColorsThemeTest {

    @Test
    void unThemeSeLitEntier() {
        TokenColorsTheme theme = TokenColorsTheme.parse("""
                {
                  "name": "Banc",
                  "colors": {
                    "editor.background": "#101010",
                    "editor.foreground": "#EFEFEF"
                  },
                  "tokenColors": [
                    {"scope": "keyword",
                     "settings": {"foreground": "#AA33CC"}},
                    {"scope": ["string", "string.quoted"],
                     "settings": {"foreground": "#80FF80", "fontStyle": "bold"}}
                  ]
                }
                """);
        assertEquals("Banc", theme.name());
        assertEquals(0xFF101010, theme.background());
        assertEquals(0xFFEFEFEF, theme.foreground());
        assertEquals(2, theme.scopes().rules().size());
        ScopeStyle motCle = theme.scopes().styleFor(
                ScopeStacks.push(ScopeStacks.root("source.banc"),
                        "keyword.other"));
        assertNotNull(motCle);
        assertEquals(0xFFAA33CC, motCle.foreground());
        ScopeStyle chaine = theme.scopes().styleFor(
                ScopeStacks.push(ScopeStacks.root("source.banc"),
                        "string.quoted.double"));
        assertNotNull(chaine);
        assertEquals(0xFF80FF80, chaine.foreground());
        assertTrue(chaine.bold(), "l'attribut bold se lit du fontStyle");
    }

    @Test
    void lesAttributsSeCombinent() {
        TokenColorsTheme theme = TokenColorsTheme.parse("""
                {
                  "tokenColors": [
                    {"scope": "invalid",
                     "settings": {"foreground": "#FF0000",
                                  "fontStyle": "italic underline"}}
                  ]
                }
                """);
        ScopeStyle style = theme.scopes().styleFor(
                ScopeStacks.push(ScopeStacks.root("source.banc"),
                        "invalid.illegal"));
        assertNotNull(style);
        assertTrue(style.italic() && style.underline() && !style.bold());
    }

    @Test
    void unThemeSansCouleursDEditeurResteLisible() {
        TokenColorsTheme theme = TokenColorsTheme.parse("""
                {"tokenColors": []}
                """);
        assertEquals("", theme.name());
        assertEquals(0, theme.background());
        assertEquals(0, theme.foreground());
        assertNull(theme.scopes().styleFor(
                ScopeStacks.root("source.banc")));
    }

    @Test
    void lEncreOpaqueSeLitEtLaTranslucideSeRefuse() {
        assertEquals(0xFF00FF7F, encreLue("#00FF7F"));
        assertEquals(0xFFFF00FF, encreLue("#FFFF00FF"));
        IllegalArgumentException echec = assertThrows(
                IllegalArgumentException.class, () -> encreLue("#8000FF7F"));
        assertTrue(String.valueOf(echec.getMessage()).contains("opaque"),
                "le refus nomme l'opacité : " + echec.getMessage());
    }

    /** L'encre résolue d'une règle isolée sur la portée donnée. */
    private static int encreLue(String encre) {
        jo.celeris.core.syntax.scope.ScopeStyle style = TokenColorsTheme.parse("""
                {"tokenColors": [{"scope": "a",
                 "settings": {"foreground": "%s"}}]}
                """.formatted(encre)).scopes().styleFor(
                        ScopeStacks.root("a"));
        assertNotNull(style, "la règle isolée résout sur sa portée");
        return style.foreground();
    }

    @Test
    void uneRegleSansSelecteurSeRefuse() {
        IllegalArgumentException echec = assertThrows(
                IllegalArgumentException.class, () -> TokenColorsTheme.parse(
                        """
                        {"tokenColors": [{"settings": {"foreground": "#000"}}]}
                        """));
        assertTrue(String.valueOf(echec.getMessage()).contains("scope"));
    }

    @Test
    void uneRegleSansReglagesSeRefuse() {
        IllegalArgumentException echec = assertThrows(
                IllegalArgumentException.class, () -> TokenColorsTheme.parse(
                        """
                        {"tokenColors": [{"scope": "keyword"}]}
                        """));
        assertTrue(String.valueOf(echec.getMessage()).contains("settings"));
    }

    @Test
    void uneEncreMalEcriteSeRefuse() {
        assertThrows(IllegalArgumentException.class,
                () -> TokenColorsTheme.parse("""
                        {"tokenColors": [{"scope": "a",
                         "settings": {"foreground": "00FF7F"}}]}
                        """));
        assertThrows(IllegalArgumentException.class,
                () -> TokenColorsTheme.parse("""
                        {"tokenColors": [{"scope": "a",
                         "settings": {"foreground": "#00FF7"}}]}
                        """));
    }

    @Test
    void unSelecteurMalFormeRefuseLeThemeEntier() {
        assertThrows(IllegalArgumentException.class,
                () -> TokenColorsTheme.parse("""
                        {"tokenColors": [{"scope": "a, , b",
                         "settings": {"foreground": "#000000"}}]}
                        """));
    }
}
