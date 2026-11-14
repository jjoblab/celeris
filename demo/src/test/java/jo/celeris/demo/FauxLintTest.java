package jo.celeris.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.android.host.spi.AnalyseurLint;
import jo.celeris.android.host.spi.CorrectifsLint;

/**
 * Le faux build de lint de la démonstration (C-12) : la sortie rend le
 * format exact d'Android Lint, l'analyseur la relit, et les correctifs
 * mécaniques se posent sur les lignes porteuses — la boucle complète
 * du scénario sans appareil.
 */
class FauxLintTest {

    @Test
    void laSortiePorteLeFormatExactDeLint() {
        String sortie = FauxLint.sortie("README.md", List.of(
                "# Titre",
                "Attendez... la suite",
                "rien ici",
                "tiret -- cadratin"));
        assertEquals("""
                README.md:2: Warning: Replace "..." with ellipsis character [TypographyEllipsis]
                README.md:4: Warning: Use an em-dash instead of two hyphens [TypographyDashes]
                """, sortie);
    }

    @Test
    void leTexteSansFauteNeProduitRien() {
        assertTrue(FauxLint.sortie("App.kt", List.of("class App", "propre"))
                .isEmpty());
    }

    @Test
    void laBoucleCompleteAnalysePuisCorrige() {
        List<String> lignes = FauxLint.lignes("Attendez... puis -- repartez");
        var diagnostics = AnalyseurLint.parser(FauxLint.sortie("l.txt", lignes));
        assertEquals(2, diagnostics.size());
        assertEquals(1, diagnostics.get(0).ligne());
        assertTrue(CorrectifsLint.pour(diagnostics.get(0), lignes).isPresent());
        assertTrue(CorrectifsLint.pour(diagnostics.get(1), lignes).isPresent());
        assertEquals(List.of("Attendez... puis -- repartez"), lignes);
    }

    @Test
    void lesLignesSeDecoupentExactement() {
        List<String> lignes = FauxLint.lignes("un\ndeux\n\nquatre");
        assertEquals(List.of("un", "deux", "", "quatre"), lignes);
    }

    /** La marque « noinspection » (SuppressionInspection, catégorie B)
     *  masque SA règle en la ligne — jamais les autres. */
    @Test
    void laMarqueNoinspectionMasqueSaRegleSeule() {
        String sortie = FauxLint.sortie("App.kt", List.of(
                "Attendez... // noinspection TypographyEllipsis",
                "tiret -- cadratin"));
        assertEquals("""
                App.kt:2: Warning: Use an em-dash instead of two hyphens [TypographyDashes]
                """, sortie, "la règle masquée ne se rapporte plus, l'autre demeure");
    }
}
