package jo.celeris.serveur.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Le banc des diagnostics : équilibre des paires, littéraux non
 * fermés, pistes de point-virgule — les vrais positifs comme les faux
 * amis expressément neutralisés.
 */
class JavaDiagnosticsTest {

    @Test
    void lAccoladeNonFermeeSeSignaleASaPlace() {
        JavaSource document = JavaSource.of("class A {\n  int x = 1;\n");
        List<JavaDiagnostics.Finding> constats = JavaDiagnostics.of(document);
        assertEquals(1, constats.size());
        assertEquals(JavaDiagnostics.ERROR, constats.get(0).severity());
        assertEquals("accolade ouvrante non fermée",
                constats.get(0).message());
        assertEquals(8, constats.get(0).start());
    }

    @Test
    void laFermanteSansOuvranteSeSignaleSurElleMeme() {
        JavaSource document = JavaSource.of("int x = 1;\n}\n");
        List<JavaDiagnostics.Finding> constats = JavaDiagnostics.of(document);
        assertEquals(1, constats.size());
        assertEquals("accolade fermante sans ouvrante",
                constats.get(0).message());
        assertEquals(11, constats.get(0).start());
    }

    @Test
    void laParentheseEtLeCrochetSeSignalentAussi() {
        JavaSource document = JavaSource.of("f(a[1);\n");
        List<JavaDiagnostics.Finding> constats = JavaDiagnostics.of(document);
        assertEquals(3, constats.size());
        assertEquals("parenthèse ouvrante non fermée",
                constats.get(0).message());
        assertEquals("crochet ouvrant non fermé",
                constats.get(1).message());
        assertEquals("parenthèse fermante sans ouvrante",
                constats.get(2).message());
    }

    @Test
    void leDocumentEquilibreNeSignaleRien() {
        JavaSource document = JavaSource.of("""
                class A {
                    void f(int a) { g(a); }
                }
                """);
        assertTrue(JavaDiagnostics.of(document).isEmpty());
    }

    @Test
    void lesPairesDansLesChainesNeComptentPas() {
        JavaSource document = JavaSource.of(
                "String s = \"({[\";\nint x = 1;\n");
        List<JavaDiagnostics.Finding> constats = JavaDiagnostics.of(document);
        assertTrue(constats.isEmpty(), () -> constats.toString());
    }

    @Test
    void leLitteralNonFermeSePousseCommeAvertissement() {
        JavaSource document = JavaSource.of("String s = \"ouvert\n");
        List<JavaDiagnostics.Finding> constats = JavaDiagnostics.of(document);
        assertEquals(1, constats.size());
        assertEquals(JavaDiagnostics.WARNING, constats.get(0).severity());
        assertEquals("littéral non fermé avant la fin de ligne",
                constats.get(0).message());
        assertEquals(11, constats.get(0).start());
    }

    @Test
    void lePointVirguleManquantSePisteEnFinDeLigne() {
        JavaSource document = JavaSource.of("int x = 1\n");
        List<JavaDiagnostics.Finding> constats = JavaDiagnostics.of(document);
        assertEquals(1, constats.size());
        assertEquals("point-virgule probablement manquant en fin "
                + "d'instruction", constats.get(0).message());
        assertEquals(8, constats.get(0).start());
        assertEquals(9, constats.get(0).end());
    }

    @Test
    void lesEnTetesDeBlocsNeSontPasDesManques() {
        JavaSource document = JavaSource.of(
                "for (int i = 0; i < 3; i++)\nif (x)\nwhile (y)\n{\n}\n");
        assertTrue(JavaDiagnostics.of(document).isEmpty());
    }

    @Test
    void lesAnnotationsEtRetoursNusNeSontPasDesManques() {
        JavaSource document = JavaSource.of(
                "@Override\n@Deprecated\nreturn\n");
        assertTrue(JavaDiagnostics.of(document).isEmpty());
    }

    @Test
    void laConstanteDEnumerationEnFinNelestPasUnManque() {
        JavaSource document = JavaSource.of("""
                enum Couleurs {
                    ROUGE,
                    VERT
                }
                """);
        List<JavaDiagnostics.Finding> constats = JavaDiagnostics.of(document);
        assertTrue(constats.isEmpty(), () -> constats.toString());
    }

    @Test
    void lInstructionPoursuivieNelestPasUnManque() {
        JavaSource document = JavaSource.of(
                "int x = 1\n    + 2;\nString s = \"a\"\n    + \"b\";\n");
        assertTrue(JavaDiagnostics.of(document).isEmpty());
    }

    @Test
    void laDeclarationEclateeNePistePas() {
        JavaSource document = JavaSource.of(
                "public void f()\nthrows Exception {\n}\n");
        assertTrue(JavaDiagnostics.of(document).isEmpty());
    }

    @Test
    void laLigneDeCommentaireNePistePas() {
        JavaSource document = JavaSource.of(
                "// int x = 1\n/* int y = 2 */\nint z = 3;\n");
        assertTrue(JavaDiagnostics.of(document).isEmpty());
    }

    @Test
    void laLigneVideNePistePas() {
        JavaSource document = JavaSource.of("\n\n\n");
        assertTrue(JavaDiagnostics.of(document).isEmpty());
    }
}
