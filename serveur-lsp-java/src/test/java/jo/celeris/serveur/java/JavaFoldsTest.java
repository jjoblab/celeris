package jo.celeris.serveur.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Le banc des plis : les corps d'accolades et les blocs de
 * commentaires qui courent sur plusieurs lignes.
 */
class JavaFoldsTest {

    @Test
    void leCorpsDAccoladesSePlieSansSaFermante() {
        JavaSource document = JavaSource.of("class A {\n  int x;\n}\n");
        List<JavaFolds.Fold> plis = JavaFolds.of(document);
        assertEquals(1, plis.size());
        assertEquals(0, plis.get(0).startLine());
        assertEquals(1, plis.get(0).endLine());
    }

    @Test
    void leCorpsDuneLigneNeSePliePas() {
        JavaSource document = JavaSource.of("class A { int x; }\n");
        assertTrue(JavaFolds.of(document).isEmpty());
    }

    @Test
    void lesCorpsSembriquentChacunSonPlis() {
        JavaSource document = JavaSource.of("""
                class A {
                    void f() {
                        g();
                    }
                }
                """);
        List<JavaFolds.Fold> plis = JavaFolds.of(document);
        assertEquals(2, plis.size());
        assertEquals(0, plis.get(0).startLine());
        assertEquals(3, plis.get(0).endLine());
        assertEquals(1, plis.get(1).startLine());
        assertEquals(2, plis.get(1).endLine());
    }

    @Test
    void leBlocDeCommentaireSePlieEntier() {
        JavaSource document = JavaSource.of("/* un\nbloc */\nclass A { }\n");
        List<JavaFolds.Fold> plis = JavaFolds.of(document);
        assertEquals(1, plis.size());
        assertEquals(0, plis.get(0).startLine());
        assertEquals(1, plis.get(0).endLine());
    }

    @Test
    void leCommentaireDuneLigneNeSePliePas() {
        JavaSource document = JavaSource.of("// un\nclass A { }\n");
        assertTrue(JavaFolds.of(document).isEmpty());
    }

    @Test
    void lesAccoladesDesChainesNeSePlientPas() {
        JavaSource document = JavaSource.of("String s = \"{\"\n+ \"}\";\n");
        assertTrue(JavaFolds.of(document).isEmpty());
    }
}
