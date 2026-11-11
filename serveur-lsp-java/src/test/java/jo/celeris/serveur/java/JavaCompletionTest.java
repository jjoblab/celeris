package jo.celeris.serveur.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.lsp4j.CompletionItem;
import org.junit.jupiter.api.Test;

/**
 * Le banc de la complétion : mots-clés du langage puis symboles du
 * document, filtrés par le préfixe en frappe, ordonnés et documentés.
 */
class JavaCompletionTest {

    private static final String EXEMPLE = """
            class Compte {
                private int solde;
                void deposer(int montant) {
                }
            }
            """;

    @Test
    void sansPrefixeTousLesMotsClesSeProposent() {
        JavaSource document = JavaSource.of(EXEMPLE);
        List<CompletionItem> elements = JavaCompletion.at(document,
                JavaSymbols.of(document), 0);
        assertTrue(elements.size() > 40,
                "les mots-clés du langage se proposent tous");
        assertEquals("abstract", elements.get(0).getLabel());
        assertEquals("Keyword", elements.get(0).getKind().toString());
        assertEquals("mot-clé du langage", elements.get(0).getDetail());
    }

    @Test
    void lePrefixeFiltreLesMotsCles() {
        JavaSource document = JavaSource.of(EXEMPLE);
        int curseur = EXEMPLE.indexOf("cl") + "cl".length();
        List<CompletionItem> elements = JavaCompletion.at(document,
                JavaSymbols.of(document), curseur);
        assertTrue(elements.stream().allMatch(e ->
                e.getLabel().toLowerCase(java.util.Locale.ROOT)
                        .startsWith("cl")));
        assertTrue(elements.stream().anyMatch(e ->
                e.getLabel().equals("class")));
    }

    @Test
    void lesSymbolesDuDocumentSeProposentApresLesMotsCles() {
        JavaSource document = JavaSource.of(EXEMPLE);
        int curseur = EXEMPLE.indexOf("deposer");
        List<CompletionItem> elements = JavaCompletion.at(document,
                JavaSymbols.of(document), curseur);
        List<String> libelles = elements.stream().map(CompletionItem::getLabel)
                .toList();
        assertTrue(libelles.contains("default"));
        assertTrue(libelles.contains("deposer"));
        assertTrue(libelles.contains("do"));
        assertTrue(libelles.indexOf("do") < libelles.indexOf("deposer"),
                "les mots-clés précèdent les symboles du document");
    }

    @Test
    void chaqueElementPorteSaDocumentation() {
        JavaSource document = JavaSource.of(EXEMPLE);
        List<CompletionItem> elements = JavaCompletion.at(document,
                JavaSymbols.of(document), 0);
        CompletionItem classe = elements.stream()
                .filter(e -> e.getLabel().equals("Compte")).findFirst()
                .orElseThrow();
        assertEquals("classe déclarée dans le document",
                classe.getDetail());
        assertEquals("markdown",
                classe.getDocumentation().getRight().getKind());
        assertTrue(classe.getDocumentation().getRight().getValue()
                .contains("Compte"));
    }

    @Test
    void lePrefixeQuiNeCorrespondRienNeProposeRien() {
        JavaSource document = JavaSource.of("int zz = 1;\n");
        int curseur = document.text().indexOf("zz") + "zz".length();
        assertTrue(JavaCompletion.at(document, JavaSymbols.of(document),
                curseur).isEmpty());
    }

    @Test
    void leDocumentVideNeProposeQueLesMotsCles() {
        JavaSource document = JavaSource.of("");
        List<CompletionItem> elements = JavaCompletion.at(document,
                JavaSymbols.of(document), 0);
        assertTrue(elements.stream().noneMatch(e ->
                e.getDetail().contains("document")));
    }
}
