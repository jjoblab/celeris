package jo.celeris.core.syntax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.SelRange;

/**
 * La sélection syntaxique (A-03, M5) : la chaîne des étendues
 * englobantes — le mot courant, puis les nœuds du plus petit au plus
 * grand, jusqu'au document. Étendre monte d'un cran, réduire descend.
 * Les extensions successives couvrent mot → instruction → bloc → type
 * sur Java, l'imbrication des éléments sur XML (l'arbre tolérant du
 * socle n'expose pas de nœuds d'expression — ADR-004 : l'étape
 * « expression » se réduit au plus petit nœud englobant).
 */
class TreeSelectionTest {

    private final SyntaxService service = SyntaxService.of(JavaSyntax.INSTANCE);

    /** La chaîne depuis un mot : mot, instruction, méthode, type, document. */
    @Test
    void chaineDepuisUnMot() {
        String texte = "class A {\n    int f() {\n        return x + 1;\n    }\n}\n";
        Document doc = Document.of(texte);
        Analysis analyse = service.analyseFull(doc);
        // le curseur sur « x » (offset 39)
        List<SelRange> chaine = TreeSelection.chaine(doc, analyse, 39);
        assertEquals(5, chaine.size());
        // le mot « x »
        assertEquals(38, chaine.get(0).start());
        assertEquals(40, chaine.get(0).end());
        // l'instruction « return x + 1; »
        assertEquals(32, chaine.get(1).start());
        assertEquals(45, chaine.get(1).end());
        // la méthode « int f() { ... } »
        assertEquals(14, chaine.get(2).start());
        assertEquals(51, chaine.get(2).end());
        // le type « class A { ... } »
        assertEquals(0, chaine.get(3).start());
        assertEquals(53, chaine.get(3).end());
        // le document
        assertEquals(0, chaine.get(4).start());
        assertEquals(doc.length(), chaine.get(4).end());
    }

    /** Les extensions successives montent la chaîne d'un cran (A-03). */
    @Test
    void extensionsSuccessives() {
        String texte = "class A {\n    int f() {\n        return x + 1;\n    }\n}\n";
        Document doc = Document.of(texte);
        Analysis analyse = service.analyseFull(doc);
        SelRange etendue = new SelRange(38, 40);
        etendue = suivanteOuEchec(doc, analyse, etendue);
        assertEquals(new SelRange(32, 45), etendue);
        etendue = suivanteOuEchec(doc, analyse, etendue);
        assertEquals(new SelRange(14, 51), etendue);
        etendue = suivanteOuEchec(doc, analyse, etendue);
        assertEquals(new SelRange(0, 53), etendue);
        etendue = suivanteOuEchec(doc, analyse, etendue);
        assertEquals(new SelRange(0, doc.length()), etendue);
        assertNull(TreeSelection.suivante(doc, analyse, etendue));
    }

    /** La réduction redescend la chaîne d'un cran. */
    @Test
    void reductionRedescend() {
        String texte = "class A {\n    int f() {\n        return x + 1;\n    }\n}\n";
        Document doc = Document.of(texte);
        Analysis analyse = service.analyseFull(doc);
        SelRange etendue = new SelRange(0, doc.length());
        etendue = precedenteOuEchec(doc, analyse, etendue);
        assertEquals(new SelRange(0, 53), etendue);
        etendue = precedenteOuEchec(doc, analyse, etendue);
        assertEquals(new SelRange(14, 51), etendue);
        etendue = precedenteOuEchec(doc, analyse, etendue);
        assertEquals(new SelRange(32, 45), etendue);
        assertNull(TreeSelection.precedente(doc, analyse, new SelRange(38, 40)));
    }

    /** XML : le mot est le jeton de texte, l'imbrication des éléments est la chaîne. */
    @Test
    void xmlImbricationDesElements() {
        String texte = "<racine>\n    <a>\n        <b>texte</b>\n    </a>\n</racine>\n";
        Document doc = Document.of(texte);
        Analysis analyse = SyntaxService.of(XmlSyntax.INSTANCE).analyseFull(doc);
        // le curseur dans « texte » (offset 29) : le mot d'abord
        SelRange etendue = suivanteOuEchec(doc, analyse, new SelRange(29, 29));
        assertEquals(new SelRange(28, 33), etendue);
        etendue = suivanteOuEchec(doc, analyse, etendue);
        assertEquals(new SelRange(25, 37), etendue);
        etendue = suivanteOuEchec(doc, analyse, etendue);
        assertEquals(new SelRange(13, 46), etendue);
        etendue = suivanteOuEchec(doc, analyse, etendue);
        assertEquals(new SelRange(0, 56), etendue);
    }

    /** Kotlin : la chaîne traverse mot, instruction, fonction, classe, document. */
    @Test
    void kotlinFunEtClasse() {
        String texte = "class A {\n    fun f(): Int {\n        return x\n    }\n}\n";
        Document doc = Document.of(texte);
        Analysis analyse = SyntaxService.of(KotlinSyntax.INSTANCE).analyseFull(doc);
        // le curseur sur « x » (offset 44)
        List<SelRange> chaine = TreeSelection.chaine(doc, analyse, 44);
        assertEquals(5, chaine.size());
        // le mot « x », l'instruction « return x », la fonction, la classe, le document
        assertEquals(new SelRange(44, 45), chaine.get(0));
        assertEquals("return x", texte.substring(chaine.get(1).start(), chaine.get(1).end()));
        assertEquals("fun", texte.substring(chaine.get(2).start(), chaine.get(2).start() + 3));
        assertEquals(0, chaine.get(3).start());
        assertEquals(new SelRange(0, doc.length()), chaine.get(4));
    }

    /** La chaîne ne répète pas les étendues identiques. */
    @Test
    void pasDeRepetition() {
        String texte = "int x = 1;\n";
        Document doc = Document.of(texte);
        Analysis analyse = service.analyseFull(doc);
        List<SelRange> chaine = TreeSelection.chaine(doc, analyse, 4);
        for (int i = 1; i < chaine.size(); i++) {
            org.junit.jupiter.api.Assertions.assertTrue(
                    chaine.get(i - 1).end() - chaine.get(i - 1).start()
                            < chaine.get(i).end() - chaine.get(i).start(),
                    "chaîne strictement croissante");
        }
    }

    /** L'étendue suivante, ou l'échec explicite du test. */
    private static SelRange suivanteOuEchec(Document doc, Analysis analyse, SelRange plage) {
        SelRange suivante = TreeSelection.suivante(doc, analyse, plage);
        org.junit.jupiter.api.Assertions.assertNotNull(suivante, "étendue suivante");
        return java.util.Objects.requireNonNull(suivante);
    }

    /** L'étendue précédente, ou l'échec explicite du test. */
    private static SelRange precedenteOuEchec(Document doc, Analysis analyse, SelRange plage) {
        SelRange precedente = TreeSelection.precedente(doc, analyse, plage);
        org.junit.jupiter.api.Assertions.assertNotNull(precedente, "étendue précédente");
        return java.util.Objects.requireNonNull(precedente);
    }
}
