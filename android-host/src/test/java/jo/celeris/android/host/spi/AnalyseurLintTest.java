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
        String output = """
                app/src/main/java/com/exemple/App.kt:42: Error: Missing permission check [MissingPermission]
                res/values/strings.xml:7: Warning: Replace "..." with ellipsis character [TypographyEllipsis]
                """;
        List<HostDiagnostic> diagnostiques = AnalyseurLint.parser(output);
        assertEquals(2, diagnostiques.size());
        assertEquals(CheminDoc.of("/app/src/main/java/com/exemple/App.kt"),
                diagnostiques.get(0).chemin());
        assertEquals(42, diagnostiques.get(0).line());
        assertEquals(GraviteDiagnostic.ERREUR, diagnostiques.get(0).severity());
        assertEquals(GraviteDiagnostic.AVERTISSEMENT, diagnostiques.get(1).severity());
        assertEquals(7, diagnostiques.get(1).line());
    }

    @Test
    void laColonneFacultativeSeLit() {
        HostDiagnostic avec = AnalyseurLint
                .parser("res/layout/activity.xml:3:12: Error: Unexpected text [XmlValue]")
                .get(0);
        assertEquals(12, avec.column());
        HostDiagnostic sans = AnalyseurLint
                .parser("res/layout/activity.xml:3: Error: Unexpected text [XmlValue]")
                .get(0);
        assertEquals(0, sans.column());
    }

    @Test
    void lInformationSeConvertitEtLeBruitNeProduitRien() {
        String output = """
                Checking build.gradle.kts: /chemin/du/projet
                gradle/libs.versions.toml:20: Information: Unknown dependency alias [UnknownAlias]
                1 errors, 1 warnings, 1 informations
                """;
        List<HostDiagnostic> diagnostiques = AnalyseurLint.parser(output);
        assertEquals(1, diagnostiques.size());
        assertEquals(GraviteDiagnostic.INFO, diagnostiques.get(0).severity());
    }

    @Test
    void lIdentifiantDeRegleSePorteParLeMessage() {
        HostDiagnostic diagnostic = AnalyseurLint
                .parser(ligneLintEllipse(7))
                .get(0);
        assertEquals("TypographyEllipsis", AnalyseurLint.regle(diagnostic.message()));
        assertEquals("Replace \"...\" with ellipsis character",
                AnalyseurLint.text(diagnostic.message()));
        assertEquals("", AnalyseurLint.regle("sans identifiant"));
    }

    @Test
    void leCorrectifDeLigneSeCalculeALOccurrenceExacte() {
        HostDiagnostic diagnostic = AnalyseurLint
                .parser(ligneLintEllipse(7))
                .get(0);
        List<String> lines = List.of(
                "<resources>",
                "  <string name=\"titre\">Attendez...",
                "</resources>");
        // la ligne porteuse est la septième : les lignes manquantes
        // ne changent rien, le correctif lit la sienne.
        lines = new java.util.ArrayList<>(lines);
        while (lines.size() < 7) {
            lines.add("");
        }
        lines.set(6, "  <string name=\"titre\">Attendez...</string>");
        Optional<CorrectifLint> correctif = CorrectifsLint.pour(diagnostic, lines);
        assertTrue(correctif.isPresent());
        assertEquals(7, correctif.get().line());
        assertEquals(32, correctif.get().column());
        assertEquals("...", correctif.get().avant());
        assertEquals("…", correctif.get().apres());
        assertEquals("  <string name=\"titre\">Attendez…</string>",
                correctif.get().appliquer(lines.get(6)));
    }

    @Test
    void leCorrectifDeCadratinRemplaceLeDoubleTiret() {
        HostDiagnostic diagnostic = AnalyseurLint
                .parser("res/values/strings.xml:2: Warning: Use an em-dash instead of two hyphens [TypographyDashes]")
                .get(0);
        List<String> lines = List.of("<resources>", "attente -- puis suite");
        Optional<CorrectifLint> correctif = CorrectifsLint.pour(diagnostic, lines);
        assertTrue(correctif.isPresent());
        assertEquals("attente — puis suite", correctif.get().appliquer(lines.get(1)));
    }

    @Test
    void laRegleSansCorrectifMecaniqueNeProposeRien() {
        HostDiagnostic diagnostic = AnalyseurLint
                .parser("app/src/main/java/App.kt:42: Error: Missing permission check [MissingPermission]")
                .get(0);
        assertTrue(CorrectifsLint.pour(diagnostic, List.of("ligne")).isEmpty());
    }

    @Test
    void laLigneSansLeTexteFautifNeProposeRien() {
        HostDiagnostic diagnostic = AnalyseurLint
                .parser(ligneLintEllipse(1))
                .get(0);
        assertTrue(CorrectifsLint.pour(diagnostic, List.of("rien a corriger")).isEmpty());
    }

    @Test
    void lesDecalagesDuCorrectifSeCalculentDepuisLesDebutsDeLignes() {
        HostDiagnostic diagnostic = AnalyseurLint
                .parser(ligneLintEllipse(2))
                .get(0);
        List<String> lines = List.of("<resources>", "attente ...");
        CorrectifLint correctif = CorrectifsLint.pour(diagnostic, lines).orElseThrow();
        int[] debuts = {0, 13};
        assertEquals(13 + 8, correctif.start(debuts));
        assertEquals(13 + 8 + 3, correctif.end(debuts));
    }
    /** La ligne de sortie Lint de l'ellipse, à la ligne près. */
    private static String ligneLintEllipse(int line) {
        return "res/values/strings.xml:" + line
                + ": Warning: Replace \"...\" with ellipsis character"
                + " [TypographyEllipsis]";
    }
}
