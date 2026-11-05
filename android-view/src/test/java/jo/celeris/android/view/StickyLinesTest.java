package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.JavaSyntax;
import jo.celeris.core.syntax.SyntaxService;
import jo.celeris.core.text.Document;

/**
 * Les lignes collantes (A-16, M5) : les en-têtes des structures qui
 * englobent la première ligne visible restent collés en haut — lus
 * dans le même arbre que la coloration. Le budget d'image est tenu :
 * au plus « max » lignes collées, les plus profondes l'emportent.
 */
class StickyLinesTest {

    private final SyntaxService service = SyntaxService.of(JavaSyntax.INSTANCE);

    /** Les en-têtes englobants collent, de la plus externe à la plus profonde. */
    @Test
    void enTetesEnglobantsCollent() {
        String texte = "class A {\n"
                + "    void m() {\n"
                + "        int x;\n"
                + "        int y;\n"
                + "    }\n"
                + "}\n";
        Document doc = Document.of(texte);
        Analysis analyse = service.analyseFull(doc);
        // la première ligne visible est la ligne 3 (« int y ; »)
        List<Integer> entetes = StickyLines.entetes(doc, analyse, 3, 5);
        assertEquals(List.of(0, 1), entetes);
    }

    /** Le budget d'image borne : les plus profondes l'emportent (A-16). */
    @Test
    void budgetDBorneAuxPlusProfondes() {
        String texte = "class A {\n"
                + "    void m() {\n"
                + "        int x;\n"
                + "        int y;\n"
                + "    }\n"
                + "}\n";
        Document doc = Document.of(texte);
        Analysis analyse = service.analyseFull(doc);
        // un seul cran de budget : la méthode seule colle
        List<Integer> entetes = StickyLines.entetes(doc, analyse, 3, 1);
        assertEquals(List.of(1), entetes);
    }

    /** La première ligne visible dans aucune structure ne colle rien. */
    @Test
    void horsStructureRienNeColle() {
        String texte = "int a;\nint b;\n";
        Document doc = Document.of(texte);
        Analysis analyse = service.analyseFull(doc);
        assertEquals(List.of(), StickyLines.entetes(doc, analyse, 1, 5));
    }

    /** La ligne d'en-tête elle-même visible ne colle pas (déjà à l'écran). */
    @Test
    void enTeteDejaVisibleNeCollePas() {
        String texte = "class A {\n    int x;\n}\n";
        Document doc = Document.of(texte);
        Analysis analyse = service.analyseFull(doc);
        // la ligne visible est la ligne 0 : rien au-dessus à coller
        assertEquals(List.of(), StickyLines.entetes(doc, analyse, 0, 5));
        // la ligne visible est la ligne 1 : la classe colle
        assertEquals(List.of(0), StickyLines.entetes(doc, analyse, 1, 5));
    }
}
