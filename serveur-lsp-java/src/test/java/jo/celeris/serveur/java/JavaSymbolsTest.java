package jo.celeris.serveur.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Le banc des symboles : les types au premier rang, leurs méthodes,
 * constructeurs et champs dedans — noms, sortes et plages.
 */
class JavaSymbolsTest {

    @Test
    void laClasseSeDeclareAvecSesBornes() {
        JavaSource document = JavaSource.of("class A { }\n");
        List<JavaSymbols.Symbol> symboles = JavaSymbols.of(document);
        assertEquals(1, symboles.size());
        JavaSymbols.Symbol classe = symboles.get(0);
        assertEquals("A", classe.name());
        assertEquals(5, classe.kind());
        assertEquals(0, classe.start());
        assertEquals(11, classe.end());
        assertEquals(6, classe.nameStart());
        assertEquals(7, classe.nameEnd());
        assertTrue(classe.children().isEmpty());
    }

    @Test
    void lesSortesSuiventLaDeclaration() {
        JavaSource document = JavaSource.of(
                "interface I { }\nenum E { }\nrecord R(int x) { }\n");
        List<JavaSymbols.Symbol> symboles = JavaSymbols.of(document);
        assertEquals(11, symboles.get(0).kind());
        assertEquals(10, symboles.get(1).kind());
        assertEquals(23, symboles.get(2).kind());
    }

    @Test
    void lesMethodesEtLeursBornes() {
        JavaSource document = JavaSource.of("""
                class A {
                    void faire(int a) {
                        g(a);
                    }
                }
                """);
        JavaSymbols.Symbol classe = JavaSymbols.of(document).get(0);
        List<JavaSymbols.Symbol> enfants = classe.children();
        assertEquals(1, enfants.size());
        JavaSymbols.Symbol methode = enfants.get(0);
        assertEquals("faire", methode.name());
        assertEquals(6, methode.kind());
        assertEquals(14, methode.start());
        assertEquals(19, methode.nameStart());
        assertEquals(24, methode.nameEnd());
        assertEquals('}', document.text().charAt(methode.end() - 1),
                "le membre court jusqu'à sa fermante");
    }

    @Test
    void leConstructeurSeReconnaitASonNomDeClasse() {
        JavaSource document = JavaSource.of("""
                class A {
                    A(int a) { }
                    void autre() { }
                }
                """);
        List<JavaSymbols.Symbol> enfants =
                JavaSymbols.of(document).get(0).children();
        assertEquals(2, enfants.size());
        assertEquals(9, enfants.get(0).kind());
        assertEquals("A", enfants.get(0).name());
        assertEquals(6, enfants.get(1).kind());
    }

    @Test
    void lesChampsExigentUnModificateur() {
        JavaSource document = JavaSource.of("""
                class A {
                    private int compteur;
                    public static final String NOM = "n";
                    void f(int local) {
                        int pasUnChamp = 2;
                    }
                }
                """);
        List<JavaSymbols.Symbol> enfants =
                JavaSymbols.of(document).get(0).children();
        assertEquals(3, enfants.size());
        assertEquals("f", enfants.get(0).name());
        assertEquals(6, enfants.get(0).kind());
        assertEquals("compteur", enfants.get(1).name());
        assertEquals(8, enfants.get(1).kind());
        assertEquals("NOM", enfants.get(2).name());
    }

    @Test
    void lesFauxAmisNeDeclarentRien() {
        JavaSource document = JavaSource.of("""
                class A {
                    void f() {
                        for (int i = 0; i < 3; i++) { }
                        if (x) { }
                        while (y) { }
                        Runnable r = () -> { };
                        new A() { };
                    }
                }
                """);
        List<JavaSymbols.Symbol> enfants =
                JavaSymbols.of(document).get(0).children();
        assertEquals(1, enfants.size());
        assertEquals("f", enfants.get(0).name());
    }

    @Test
    void laClasseImbriqueeResteEnfant() {
        JavaSource document = JavaSource.of("""
                class A {
                    class B {
                        void g() { }
                    }
                    void h() { }
                }
                """);
        JavaSymbols.Symbol classe = JavaSymbols.of(document).get(0);
        assertEquals(2, classe.children().size());
        assertEquals("B", classe.children().get(0).name());
        assertEquals(5, classe.children().get(0).kind());
        assertEquals(1, classe.children().get(0).children().size());
        assertEquals("g", classe.children().get(0).children().get(0).name());
        assertEquals("h", classe.children().get(1).name());
    }

    @Test
    void laMethodeEclateeSurTroisLignesResteEntiere() {
        JavaSource document = JavaSource.of("""
                class A {
                    public void longue(int a,
                            int b)
                            throws Exception {
                    }
                }
                """);
        List<JavaSymbols.Symbol> enfants =
                JavaSymbols.of(document).get(0).children();
        assertEquals(1, enfants.size());
        assertEquals("longue", enfants.get(0).name());
    }

    @Test
    void leCorpsNonFermeVaJusquauBout() {
        JavaSource document = JavaSource.of("class A {\n");
        JavaSymbols.Symbol classe = JavaSymbols.of(document).get(0);
        assertEquals(document.text().length(), classe.end());
    }

    @Test
    void leTexteSansDeclarationRienNeDeclare() {
        JavaSource document = JavaSource.of("int x = 1;\n");
        assertTrue(JavaSymbols.of(document).isEmpty());
    }
}
