package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import jo.celeris.core.syntax.TokenKind;

import org.junit.jupiter.api.Test;

/**
 * Tests des palettes syntaxiques des thèmes (B-01 : coloration lexicale
 * et syntaxique ; D-13 : contraste vérifié automatiquement).
 *
 * <p>Chaque thème complet porte une palette résolue — une couleur par
 * sorte de jeton du socle (ADR-004/ADR-008 : le chemin de dessin ne
 * dérive jamais de thème). Les quatorze sortes sont couvertes et chaque
 * encre contraste ≥ 4,5:1 sur le fond du thème.</p>
 */
class SyntaxPaletteTest {

    /** Les trois thèmes complets portent une palette complète et contrastée. */
    @Test
    void lesTroisThemesPortentUnePaletteCompleteContrastee() {
        Map<String, EditorTheme> themes = new LinkedHashMap<>();
        themes.put("clair", EditorThemes.CLAIR);
        themes.put("sombre", EditorThemes.SOMBRE);
        themes.put("contraste élevé", EditorThemes.CONTRASTE_ELEVE);

        for (Map.Entry<String, EditorTheme> entree : themes.entrySet()) {
            EditorTheme theme = entree.getValue();
            String nom = entree.getKey();
            SyntaxPalette palette = theme.syntaxe();
            for (TokenKind sorte : TokenKind.values()) {
                double rapport = WcagContrast.ratio(palette.couleur(sorte),
                        theme.backgroundColor());
                assertTrue(rapport >= 4.5,
                        nom + " : la sorte " + sorte + " doit contraster "
                                + "≥ 4,5:1 sur le fond (mesuré " + rapport + ")");
            }
        }
    }

    /** La coloration est signifiante : les sortes structurantes se distinguent de l'encre de base. */
    @Test
    void lesSortesStructurantesSeDistinguentDeLaBase() {
        for (EditorTheme theme : new EditorTheme[] {EditorThemes.CLAIR,
                EditorThemes.SOMBRE, EditorThemes.CONTRASTE_ELEVE}) {
            SyntaxPalette palette = theme.syntaxe();
            for (TokenKind sorte : new TokenKind[] {TokenKind.KEYWORD,
                    TokenKind.TYPE, TokenKind.STRING, TokenKind.COMMENT,
                    TokenKind.ERROR}) {
                assertNotEquals(theme.baseTextColor(), palette.couleur(sorte),
                        "la sorte " + sorte + " doit porter sa propre encre");
            }
            // L'identificateur et les crochets restent à l'encre de base :
            // le langage courant ne crie pas.
            assertEquals(theme.baseTextColor(), palette.couleur(TokenKind.IDENTIFIER));
            assertEquals(theme.baseTextColor(), palette.couleur(TokenKind.PUNCTUATION));
            assertEquals(theme.baseTextColor(),
                    palette.couleur(TokenKind.OPEN_BRACKET));
            assertEquals(theme.baseTextColor(),
                    palette.couleur(TokenKind.CLOSE_BRACKET));
        }
    }

    /** Une plage colorée valide ses bornes : croissantes, fin exclusive. */
    @Test
    void plageColoreeValideSesBornes() {
        assertThrows(IllegalArgumentException.class,
                () -> new ColoredSpan(TokenKind.KEYWORD, -1, 3));
        assertThrows(IllegalArgumentException.class,
                () -> new ColoredSpan(TokenKind.KEYWORD, 4, 4));
        assertThrows(IllegalArgumentException.class,
                () -> new ColoredSpan(TokenKind.KEYWORD, 5, 2));
        ColoredSpan saine = new ColoredSpan(TokenKind.STRING, 2, 7);
        assertEquals(TokenKind.STRING, saine.kind());
        assertEquals(2, saine.start());
        assertEquals(7, saine.end());
    }

    /** Le thème hérité (six champs) reçoit une palette monochrome à l'encre de base. */
    @Test
    void themeHeriteRecoitPaletteMonochrome() {
        EditorTheme theme = new EditorTheme(9, 0xFFFFFFFF, 0xFF202124,
                0xFFD3E3FD, 0xFF1A73E8, 0xFF5F6368);
        for (TokenKind sorte : TokenKind.values()) {
            assertEquals(theme.baseTextColor(), theme.syntaxe().couleur(sorte),
                    "sans palette propre, la sorte " + sorte + " reste à l'encre de base");
        }
    }
}
