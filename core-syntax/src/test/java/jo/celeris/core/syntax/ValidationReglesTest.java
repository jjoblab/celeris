package jo.celeris.core.syntax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;

/**
 * Le garde-fou de syntaxe des règles R8/ProGuard (C-09) : les règles
 * inconnues, les accolades non appariées et les membres sans
 * point-virgule se signalent à la plage exacte — le fichier sain ne
 * produit rien.
 */
class ValidationReglesTest {

    @Test
    void leFichierSainNeProduitRien() {
        Document doc = Document.of("""
                # les règles de la démonstration
                -verbose

                -keep public class com.celeris.demo.MainActivity {
                    public static *** main(java.lang.String[]);
                }

                -keepclassmembers class * extends android.app.Activity {
                    <init>();
                    void onCreate(...);
                }

                -keepnames class com.celeris.core.text.**
                -dontwarn org.slf4j.**
                """);
        assertTrue(ValidationRegles.valider(doc).isEmpty(),
                "le corpus sain ne se signale pas");
    }

    @Test
    void laRegleInconnueSeSignaleASaPlageExacte() {
        Document doc = Document.of("-keep class Foo {\n}\n-kepp class Bar\n");
        List<SyntaxIssue> problemes = ValidationRegles.valider(doc);
        assertEquals(1, problemes.size());
        SyntaxIssue probleme = problemes.get(0);
        assertEquals("-keep class Foo {\n}\n".length(), probleme.startOffset());
        assertEquals("-kepp".length(), probleme.length());
        assertTrue(probleme.message().contains("-kepp"),
                "le message nomme la règle fautive");
    }

    @Test
    void lAccoladeOrphelineSeSignale() {
        Document doc = Document.of("-keep class Foo {\n    void cree();\n}\n}\n");
        List<SyntaxIssue> problemes = ValidationRegles.valider(doc);
        assertEquals(1, problemes.size());
        int position = "-keep class Foo {\n    void cree();\n}\n".length();
        assertEquals(position, problemes.get(0).startOffset());
        assertEquals(1, problemes.get(0).length());
    }

    @Test
    void leBlocJamaisFermeSeTaitMaisLaProfondeurCompte() {
        // Le bloc ouvert à la fin ne se signale pas : la fin de fichier
        // peut couper un fichier en cours d'édition — le garde-fou
        // reste délibérément basique, sans faux positif.
        Document doc = Document.of("-keep class Foo {\n    void cree();\n");
        List<SyntaxIssue> problemes = ValidationRegles.valider(doc);
        assertTrue(problemes.isEmpty());
    }

    @Test
    void leMembreSansPointVirguleSeSignaleDansLeBloc() {
        Document doc = Document.of("""
                -keep class Foo {
                    void cree()
                }
                """);
        List<SyntaxIssue> problemes = ValidationRegles.valider(doc);
        assertEquals(1, problemes.size());
        SyntaxIssue probleme = problemes.get(0);
        assertEquals("-keep class Foo {\n    ".length(), probleme.startOffset());
        assertTrue(probleme.message().contains("cree()"),
                "le message cite le membre fautif");
    }

    @Test
    void laRegleAvecTiretInterneNeSeSignalePas() {
        Document doc = Document.of("-keepclassmembers class *\n");
        assertTrue(ValidationRegles.valider(doc).isEmpty());
    }

    @Test
    void leCommentaireNeSeVerifiePas() {
        Document doc = Document.of("# -kepp dans un commentaire\n-verbose\n");
        assertTrue(ValidationRegles.valider(doc).isEmpty());
    }
}
