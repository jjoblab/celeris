package jo.celeris.serveur.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.lsp4j.Hover;
import org.junit.jupiter.api.Test;

/**
 * Le banc du survol : le mot-clé rend sa documentation, le symbole
 * déclaré rend sa déclaration en exergue, le reste rend null.
 */
class JavaHoverTest {

    private static final String EXEMPLE = """
            class Compte {
                private int solde;
                void deposer(int montant) {
                }
            }
            """;

    @Test
    void leMotCleSeDocumente() {
        JavaSource document = JavaSource.of(EXEMPLE);
        Hover survol = JavaHover.at(document, JavaSymbols.of(document), 0);
        assertNotNull(survol);
        assertEquals("markdown", survol.getContents().getRight().getKind());
        assertTrue(survol.getContents().getRight().getValue()
                .startsWith("**class**"));
        assertEquals(0, survol.getRange().getStart().getLine());
        assertEquals(0, survol.getRange().getStart().getCharacter());
        assertEquals(5, survol.getRange().getEnd().getCharacter());
    }

    @Test
    void leSymboleDeclareSeSurvoleAvecSaLigne() {
        JavaSource document = JavaSource.of(EXEMPLE);
        int curseur = document.text().indexOf("solde") + 2;
        Hover survol = JavaHover.at(document, JavaSymbols.of(document),
                curseur);
        assertNotNull(survol);
        String contenu = survol.getContents().getRight().getValue();
        assertTrue(contenu.contains("**solde**"));
        assertTrue(contenu.contains("un champ"));
        assertTrue(contenu.contains("private int solde;"));
    }

    @Test
    void leSymboleLePlusIntimeGagne() {
        JavaSource document = JavaSource.of(EXEMPLE);
        int curseur = document.text().indexOf("deposer") + 2;
        Hover survol = JavaHover.at(document, JavaSymbols.of(document),
                curseur);
        assertNotNull(survol);
        assertTrue(survol.getContents().getRight().getValue()
                .contains("**deposer**"));
        assertTrue(survol.getContents().getRight().getValue()
                .contains("une méthode"));
    }

    @Test
    void leMotInconnuNeSurvoleRien() {
        JavaSource document = JavaSource.of("int zzz = 1;\n");
        assertNull(JavaHover.at(document, JavaSymbols.of(document), 4));
    }

    @Test
    void laPonctuationNeSurvoleRien() {
        JavaSource document = JavaSource.of(EXEMPLE);
        assertNull(JavaHover.at(document, JavaSymbols.of(document), 15),
                "le saut après l’accolade ne porte pas de mot");
    }
}
