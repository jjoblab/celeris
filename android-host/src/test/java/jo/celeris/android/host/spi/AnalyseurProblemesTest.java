package jo.celeris.android.host.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests de l'analyseur de problèmes de build (E-05) : la sortie du
 * compilateur se convertit en diagnostics — fichier, ligne et colonne
 * exacts, gravité lue.
 */
final class AnalyseurProblemesTest {

    /** La ligne javac/kotlinc se convertit : chemin, ligne, colonne, gravité, message. */
    @Test
    void ligneDeCompilateurConvertie() {
        List<DiagnosticHote> diagnostiques = AnalyseurProblemes.parser("""
                > Task :app:compileDebugJavaWithJavac
                app/src/main/java/com/exemple/Main.java:18:5: error: cannot find symbol
                app/src/main/java/com/exemple/Main.java:24:1: warning: unused import
                2 problems""");

        assertEquals(2, diagnostiques.size());
        assertEquals("/app/src/main/java/com/exemple/Main.java",
                diagnostiques.get(0).chemin().ecriture());
        assertEquals(18, diagnostiques.get(0).ligne());
        assertEquals(5, diagnostiques.get(0).colonne());
        assertEquals(GraviteDiagnostic.ERREUR, diagnostiques.get(0).gravite());
        assertEquals("cannot find symbol", diagnostiques.get(0).message());

        assertEquals(GraviteDiagnostic.AVERTISSEMENT, diagnostiques.get(1).gravite());
        assertEquals("unused import", diagnostiques.get(1).message());
    }

    /** La forme sans colonne reste datée : colonne inconnue, ligne exacte. */
    @Test
    void formeSansColonne() {
        List<DiagnosticHote> diagnostiques =
                AnalyseurProblemes.parser("src/Main.kt:3: error: unresolved reference");
        assertEquals(1, diagnostiques.size());
        assertEquals("/src/Main.kt", diagnostiques.get(0).chemin().ecriture());
        assertEquals(3, diagnostiques.get(0).ligne());
        assertEquals(0, diagnostiques.get(0).colonne(), "colonne inconnue : zéro");
    }

    /** Les lignes de bruit ne produisent rien : jamais de diagnostic fantôme. */
    @Test
    void bruitIgnore() {
        List<DiagnosticHote> diagnostiques = AnalyseurProblemes.parser("""
                > Task :app:build
                BUILD SUCCESSFUL in 3s
                note: rebuild with -Xlint
                18 actionable tasks""");
        assertEquals(0, diagnostiques.size());
    }

    /** L'entrée vide rend vide : rien à diagnostiquer. */
    @Test
    void entreeVide() {
        assertEquals(0, AnalyseurProblemes.parser("").size());
    }

    /** Plusieurs diagnostics d'un coup gardent l'ordre de la sortie. */
    @Test
    void ordrePreserve() {
        List<DiagnosticHote> diagnostiques = AnalyseurProblemes.parser("""
                a/A.java:1:1: error: un
                a/B.java:2:2: error: deux
                a/C.java:3:3: warning: trois""");
        assertEquals(3, diagnostiques.size());
        assertEquals("un", diagnostiques.get(0).message());
        assertEquals("deux", diagnostiques.get(1).message());
        assertEquals("trois", diagnostiques.get(2).message());
    }
}
