package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Tests des palettes complètes de l'éditeur (D-13 : thèmes clair, sombre
 * et contraste élevé, contraste vérifié automatiquement).
 *
 * <p>Seuils WCAG : texte ≥ 4,5:1 (fond courant et fond de sélection —
 * l'encre passe sur les deux), gouttière ≥ 4,5:1, curseur ≥ 3:1
 * (élément non textuel).</p>
 */
class EditorThemesTest {

    /**
     * D-13 — les trois thèmes complets passent les contrastes WCAG.
     */
    @Test
    void lesTroisThemesPassentLesContrastesWcag() {
        Map<String, EditorTheme> themes = new LinkedHashMap<>();
        themes.put("clair", EditorThemes.CLAIR);
        themes.put("sombre", EditorThemes.SOMBRE);
        themes.put("contraste élevé", EditorThemes.CONTRASTE_ELEVE);

        for (Map.Entry<String, EditorTheme> entree : themes.entrySet()) {
            EditorTheme theme = entree.getValue();
            String nom = entree.getKey();

            assertTrue(
                    WcagContrast.ratio(theme.baseTextColor(), theme.backgroundColor()) >= 4.5,
                    nom + " : le texte doit contraster ≥ 4,5:1 sur le fond");
            assertTrue(
                    WcagContrast.ratio(theme.baseTextColor(), theme.selectionColor()) >= 4.5,
                    nom + " : le texte sélectionné doit contraster ≥ 4,5:1 sur le fond de sélection");
            assertTrue(
                    WcagContrast.ratio(theme.gutterTextColor(), theme.backgroundColor()) >= 4.5,
                    nom + " : la gouttière doit contraster ≥ 4,5:1 sur le fond");
            assertTrue(
                    WcagContrast.ratio(theme.cursorColor(), theme.backgroundColor()) >= 3.0,
                    nom + " : le curseur doit contraster ≥ 3:1 sur le fond");
        }
    }

    /** Les trois thèmes sont distincts et identifiables par leur identifiant. */
    @Test
    void themesDistinctsParIdentifiant() {
        assertEquals(1, EditorThemes.CLAIR.id());
        assertEquals(2, EditorThemes.SOMBRE.id());
        assertEquals(3, EditorThemes.CONTRASTE_ELEVE.id());
    }
}
