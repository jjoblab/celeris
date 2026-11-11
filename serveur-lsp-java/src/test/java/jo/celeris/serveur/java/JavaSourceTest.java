package jo.celeris.serveur.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Le banc du document suivi : l'index des lignes, la transposition
 * décalage ↔ position, les mots autour du curseur et le dépouillement
 * des chaînes et commentaires.
 */
class JavaSourceTest {

    @Test
    void lesLignesSeDecoupentSansLeurSaut() {
        JavaSource document = JavaSource.of("ligne1\nligne2\n");
        assertEquals(3, document.lineCount());
        assertEquals("ligne1", document.line(0));
        assertEquals("ligne2", document.line(1));
        assertEquals("", document.line(2));
    }

    @Test
    void leTexteVideResteUneLigne() {
        JavaSource document = JavaSource.of("");
        assertEquals(1, document.lineCount());
        assertEquals("", document.line(0));
    }

    @Test
    void leRetourChariotResteUnBlancDeFin() {
        JavaSource document = JavaSource.of("a\r\nb");
        assertEquals(2, document.lineCount());
        assertEquals("a", document.line(0),
                "le retour chariot se tient pour un blanc de fin");
    }

    @Test
    void lesPositionsEtDecalagesFontLallerRetour() {
        JavaSource document = JavaSource.of("ab\ncde\n");
        assertEquals(1, document.lineOf(4));
        assertEquals(3, document.offset(1, 0));
        assertEquals(5, document.offset(1, 2));
        assertEquals(6, document.offset(1, 99));
        assertEquals(new JavaSource.Spot(1, 3), document.spot(6));
        assertEquals(new JavaSource.Spot(0, 0), document.spot(0));
        assertEquals(0, document.offset(-1, -1));
    }

    @Test
    void lesUnitesSontCellesDuProtocole() {
        // l'émoji compte DEUX unités UTF-16 : la colonne du protocole
        // suit la même unité
        JavaSource document = JavaSource.of("\uD83D\uDE00x\ny");
        assertEquals(0, document.lineOf(3), "le saut reste sur sa ligne");
        assertEquals(1, document.lineOf(4));
        assertEquals(4, document.offset(1, 0));
        assertEquals(new JavaSource.Spot(0, 3), document.spot(3));
    }

    @Test
    void leMotAutourDuCurseurSePrendAuMilieu() {
        JavaSource document = JavaSource.of("int nombre = 1;");
        assertEquals("nombre", document.wordAround(4));
        assertEquals("nombre", document.wordAround(6));
        assertEquals("nombre", document.wordAround(9));
        assertNull(document.wordAround(11), "le égal ne porte pas de mot");
        assertNull(document.wordAround(12), "le blanc après le égal non plus");
    }

    @Test
    void lePrefixeEnFrappeSeLitAvantLeCurseur() {
        JavaSource document = JavaSource.of("int nombre = 1;");
        assertEquals("nom", document.identifierPrefix(7));
        assertEquals("", document.identifierPrefix(4));
        assertEquals("", document.identifierPrefix(0));
    }

    @Test
    void leCodeDepouilleGardeSesLongueurs() {
        JavaSource document = JavaSource.of("""
                class A { // {(commentaire)
                    String s = "a{b}";
                    char c = '{';
                }""");
        JavaSource.StrippedCode code = document.stripped();
        assertEquals(document.text().length(), code.text().length());
        assertEquals(0, code.unclosedLiterals().size());
        // l'accolade du commentaire et celle de la chaîne ne comptent
        // plus : la paire de la classe demeure équilibrée
        assertEquals(-1, code.firstUnclosedBrace());
        assertEquals(-1, code.firstUnclosedParen());
    }

    @Test
    void leLitteralNonFermeSeConsigne() {
        JavaSource document = JavaSource.of("String s = \"ouvert;\n}");
        List<Integer> nonFermes = document.stripped().unclosedLiterals();
        assertEquals(1, nonFermes.size());
        assertEquals(11, nonFermes.get(0));
    }

    @Test
    void leBlocDeTexteSeTientEnTroisGuillemets() {
        JavaSource document = JavaSource.of(
                "String s = \"\"\"\n  {dedans}\n  \"\"\";\n");
        JavaSource.StrippedCode code = document.stripped();
        assertEquals(0, code.unclosedLiterals().size());
        assertEquals(-1, code.firstUnclosedBrace());
        assertEquals(0, code.comments().size());
    }

    @Test
    void leBlocDeCommentaireSeConsigneAvecSesBornes() {
        JavaSource document = JavaSource.of("/* un\nbloc */ class A { }");
        List<JavaSource.StrippedCode.Comment> blocs =
                document.stripped().comments();
        assertEquals(1, blocs.size());
        assertEquals(0, blocs.get(0).start());
        assertEquals(13, blocs.get(0).end());
    }

    @Test
    void leCommentaireNonFermeVaJusquAuBout() {
        JavaSource document = JavaSource.of("a /* sans fin");
        List<JavaSource.StrippedCode.Comment> blocs =
                document.stripped().comments();
        assertEquals(1, blocs.size());
        assertEquals(document.text().length(), blocs.get(0).end());
    }
}
