package jo.celeris.android.host.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * L'analyseur de la sortie d'Android Lint (C-12) et ses correctifs
 * mécaniques : les trois gravités se convertissent, la colonne
 * facultative se lit, le bruit ne produit rien — et le correctif de
 * l'ellipse s'applique au décalage exact.
 */
class AnalyseurLintTest {

    @Test
    void laFormeCompleteSeConvertitAuxLignesExactes() {
        String sortie = """
                app/src/main/java/com/exemple/App.kt:42: Error: Missing permission check [MissingPermission]
                res/values/strings.xml:7: Warning: Replace "..." with ellipsis character [TypographyEllipsis]
                """;
        List<DiagnosticHote> diagnostiques = AnalyseurLint.parser(sortie);
        assertEquals(2, diagnostiques.size());
        assertEquals(CheminDoc.de("/app/src/main/java/com/exemple/App.kt"),
                diagnostiques.get(0).chemin());
        assertEquals(42, diagnostiques.get(0).ligne());
        assertEquals(GraviteDiagnostic.ERREUR, diagnostiques.get(0).gravite());
        assertEquals(GraviteDiagnostic.AVERTISSEMENT, diagnostiques.get(1).gravite());
        assertEquals(7, diagnostiques.get(1).ligne());
    }

    @Test
    void laColonneFacultativeSeLit() {
        DiagnosticHote avec = AnalyseurLint
                .parser("res/layout/activity.xml:3:12: Error: Unexpected text [XmlValue]")
                .get(0);
        assertEquals(12, avec.colonne());
        DiagnosticHote sans = AnalyseurLint
                .parser("res/layout/activity.xml:3: Error: Unexpected text [XmlValue]")
                .get(0);
        assertEquals(0, sans.colonne());
    }

    @Test
    void lInformationSeConvertitEtLeBruitNeProduitRien() {
        String sortie = """
                Checking build.gradle.kts: /chemin/du/projet
                gradle/libs.versions.toml:20: Information: Unknown dependency alias [UnknownAlias]
                1 errors, 1 warnings, 1 informations
                """;
        List<DiagnosticHote> diagnostiques = AnalyseurLint.parser(sortie);
        assertEquals(1, diagnostiques.size());
        assertEquals(GraviteDiagnostic.INFO, diagnostiques.get(0).gravite());
    }

    @Test
    void lIdentifiantDeRegleSePorteParLeMessage() {
        DiagnosticHote diagnostic = AnalyseurLint
                .parser(ligneLintEllipse(7))
                .get(0);
        assertEquals("TypographyEllipsis", AnalyseurLint.regle(diagnostic.message()));
        assertEquals("Replace \"...\" with ellipsis character",
                AnalyseurLint.texte(diagnostic.message()));
        assertEquals("", AnalyseurLint.regle("sans identifiant"));
    }

    @Test
    void leCorrectifDeLigneSeCalculeALOccurrenceExacte() {
        DiagnosticHote diagnostic = AnalyseurLint
                .parser(ligneLintEllipse(7))
                .get(0);
        List<String> lignes = List.of(
                "<resources>",
                "  <string name=\"titre\">Attendez...",
                "</resources>");
        // la ligne porteuse est la septième : les lignes manquantes
        // ne changent rien, le correctif lit la sienne.
        lignes = new java.util.ArrayList<>(lignes);
        while (lignes.size() < 7) {
            lignes.add("");
        }
        lignes.set(6, "  <string name=\"titre\">Attendez...</string>");
        Optional<CorrectifLint> correctif = CorrectifsLint.pour(diagnostic, lignes);
        assertTrue(correctif.isPresent());
        assertEquals(7, correctif.get().ligne());
        assertEquals(32, correctif.get().colonne());
        assertEquals("...", correctif.get().avant());
        assertEquals("…", correctif.get().apres());
        assertEquals("  <string name=\"titre\">Attendez…</string>",
                correctif.get().appliquer(lignes.get(6)));
    }

    @Test
    void leCorrectifDeCadratinRemplaceLeDoubleTiret() {
        DiagnosticHote diagnostic = AnalyseurLint
                .parser("res/values/strings.xml:2: Warning: Use an em-dash instead of two hyphens [TypographyDashes]")
                .get(0);
        List<String> lignes = List.of("<resources>", "attente -- puis suite");
        Optional<CorrectifLint> correctif = CorrectifsLint.pour(diagnostic, lignes);
        assertTrue(correctif.isPresent());
        assertEquals("attente — puis suite", correctif.get().appliquer(lignes.get(1)));
    }

    @Test
    void laRegleSansCorrectifMecaniqueNeProposeRien() {
        DiagnosticHote diagnostic = AnalyseurLint
                .parser("app/src/main/java/App.kt:42: Error: Missing permission check [MissingPermission]")
                .get(0);
        assertTrue(CorrectifsLint.pour(diagnostic, List.of("ligne")).isEmpty());
    }

    @Test
    void laLigneSansLeTexteFautifNeProposeRien() {
        DiagnosticHote diagnostic = AnalyseurLint
                .parser(ligneLintEllipse(1))
                .get(0);
        assertTrue(CorrectifsLint.pour(diagnostic, List.of("rien a corriger")).isEmpty());
    }

    @Test
    void lesDecalagesDuCorrectifSeCalculentDepuisLesDebutsDeLignes() {
        DiagnosticHote diagnostic = AnalyseurLint
                .parser(ligneLintEllipse(2))
                .get(0);
        List<String> lignes = List.of("<resources>", "attente ...");
        CorrectifLint correctif = CorrectifsLint.pour(diagnostic, lignes).orElseThrow();
        int[] debuts = {0, 13};
        assertEquals(13 + 8, correctif.debut(debuts));
        assertEquals(13 + 8 + 3, correctif.fin(debuts));
    }
    /** La ligne de sortie Lint de l'ellipse, à la ligne près. */
    private static String ligneLintEllipse(int ligne) {
        return "res/values/strings.xml:" + ligne
                + ": Warning: Replace \"...\" with ellipsis character"
                + " [TypographyEllipsis]";
    }
}
