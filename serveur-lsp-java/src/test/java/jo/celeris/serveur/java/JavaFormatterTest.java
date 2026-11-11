package jo.celeris.serveur.java;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Le banc du formatage : la réindentation par profondeur
 * d'accolades, idempotente, en espaces comme en tabulations.
 */
class JavaFormatterTest {

    @Test
    void lIndentationSuitLaProfondeur() {
        JavaSource document = JavaSource.of(
                "class A {\nvoid f() {\ng();\n}\n}\n");
        String attendu = "class A {\n    void f() {\n        g();\n    }\n}\n";
        assertEquals(attendu, JavaFormatter.format(document, 4, true));
    }

    @Test
    void laFermanteRemonteDunCran() {
        JavaSource document = JavaSource.of("class A {\n    int x;\n    }\n");
        String attendu = "class A {\n    int x;\n}\n";
        assertEquals(attendu, JavaFormatter.format(document, 4, true));
    }

    @Test
    void lesTabulationsServentAUneUnite() {
        JavaSource document = JavaSource.of("class A {\nvoid f() { }\n}\n");
        String attendu = "class A {\n\tvoid f() { }\n}\n";
        assertEquals(attendu, JavaFormatter.format(document, 4, false));
    }

    @Test
    void laLigneVideResteVide() {
        JavaSource document = JavaSource.of("class A {\n\nint x;\n}\n");
        String attendu = "class A {\n\n    int x;\n}\n";
        assertEquals(attendu, JavaFormatter.format(document, 4, true));
    }

    @Test
    void leFormatageEstIdempotent() {
        JavaSource document = JavaSource.of(
                "class A {\nvoid f() {\ng( \"}\" );\n}\n}\n");
        String premier = JavaFormatter.format(document, 4, true);
        assertEquals(premier, JavaFormatter.format(
                JavaSource.of(premier), 4, true));
    }

    @Test
    void lesAccoladesDesChainesNeComptentPas() {
        JavaSource document = JavaSource.of(
                "class A {\nString s = \"\\u007b\\u007d\";\n}\n");
        String attendu = "class A {\n    String s = \"\\u007b\\u007d\";\n}\n";
        assertEquals(attendu, JavaFormatter.format(document, 4, true));
    }

    @Test
    void leDocumentDejaPropreResteTelQuel() {
        String propre = "class A {\n    int x;\n}\n";
        assertEquals(propre, JavaFormatter.format(
                JavaSource.of(propre), 4, true));
    }
}
