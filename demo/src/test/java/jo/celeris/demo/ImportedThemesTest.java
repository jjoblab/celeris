package jo.celeris.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jo.celeris.android.view.EditorTheme;
import jo.celeris.core.language.TokenColorsTheme;
import jo.celeris.core.syntax.ScopeStacks;
import jo.celeris.core.syntax.ScopeStyle;
import jo.celeris.core.syntax.TokenKind;

import org.junit.jupiter.api.Test;

/**
 * Le thème importé de la démonstration (M7.6, H-07) : le fichier
 * {@code tokenColors} au format de VS Code — le format des thèmes du
 * commerce, le contenu écrit ici — se lit par l'importeur du
 * core-language, fonde le chrome de la vue et DÉRIVE la palette par
 * sorte de ses résolutions : une seule table de couleurs vit dans le
 * fichier. Chaque encre contraste ≥ 4,5:1 sur son fond (D-13) — le
 * thème importé ne déroge pas à la règle des thèmes livrés.
 */
class ImportedThemesTest {

    @Test
    void leFichierSeLitAvecSesCouleursDEditeur() {
        TokenColorsTheme importe = ImportedThemes.lire();
        assertEquals("Atelier", importe.name());
        assertEquals(0xFFFBF6EE, importe.background());
        assertEquals(0xFF3A3630, importe.foreground());
    }

    @Test
    void lesReglesDuFichierPortentLeursAttributs() {
        TokenColorsTheme importe = ImportedThemes.lire();
        int pileFonction = ScopeStacks.push(
                ScopeStacks.root("source.demo"), "entity.name.function");
        ScopeStyle fonction = importe.scopes().styleFor(pileFonction);
        assertNotNull(fonction);
        assertTrue(fonction.bold(),
                "le nom de fonction se trace en graisse (fontStyle du fichier)");
        assertEquals(0xFF8A4B08, fonction.foreground());
        int pileCommentaire = ScopeStacks.push(
                ScopeStacks.root("source.demo"), "comment.line");
        ScopeStyle commentaire = importe.scopes().styleFor(pileCommentaire);
        assertNotNull(commentaire);
        assertTrue(commentaire.italic(),
                "le commentaire se trace en italique (fontStyle du fichier)");
    }

    @Test
    void leThemeCompletDeriveSaPaletteDuFichierSeul() {
        EditorTheme theme = ImportedThemes.atelier();
        assertEquals(0xFFFBF6EE, theme.backgroundColor());
        assertEquals(0xFF3A3630, theme.baseTextColor());
        assertEquals(0xFF5B1E8E, theme.syntaxe().couleur(TokenKind.KEYWORD),
                "l'encre des mots-clés vient de la règle keyword du fichier");
        assertEquals(0xFF3A3630, theme.syntaxe().couleur(TokenKind.IDENTIFIER),
                "la sorte sans règle retombe à l'encre de base du fichier");
    }

    @Test
    void chaqueEncreImporteeContrasteSurSonFond() {
        EditorTheme theme = ImportedThemes.atelier();
        TokenKind[] sortes = TokenKind.values();
        for (TokenKind sorte : sortes) {
            double rapport = Wcag.ratio(theme.syntaxe().couleur(sorte),
                    theme.backgroundColor());
            assertTrue(rapport >= 4.5,
                    sorte + " contraste à " + String.format("%.2f", rapport)
                            + ":1 sur le fond importé");
        }
        double curseur = Wcag.ratio(theme.cursorColor(),
                theme.backgroundColor());
        assertTrue(curseur >= 3.0,
                "le curseur contraste à " + String.format("%.2f", curseur)
                        + ":1 sur le fond importé");
        double selection = Wcag.ratio(theme.baseTextColor(),
                theme.selectionColor());
        assertTrue(selection >= 4.5,
                "l'encre de base contraste sur la sélection importée : "
                        + String.format("%.2f", selection));
    }

    /** Le rapport de contraste WCAG — le même calcul que la vue. */
    private static final class Wcag {
        private static double luminance(int argb) {
            double rouge = lineaire(((argb >> 16) & 0xFF) / 255.0);
            double vert = lineaire(((argb >> 8) & 0xFF) / 255.0);
            double bleu = lineaire((argb & 0xFF) / 255.0);
            return 0.2126 * rouge + 0.7152 * vert + 0.0722 * bleu;
        }

        private static double lineaire(double composante) {
            return composante <= 0.03928
                    ? composante / 12.92
                    : Math.pow((composante + 0.055) / 1.055, 2.4);
        }

        static double ratio(int premier, int second) {
            double un = luminance(premier);
            double deux = luminance(second);
            double clair = Math.max(un, deux);
            double sombre = Math.min(un, deux);
            return (clair + 0.05) / (sombre + 0.05);
        }
    }
}
