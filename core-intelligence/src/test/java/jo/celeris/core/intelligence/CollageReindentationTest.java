package jo.celeris.core.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.JavaSyntax;
import jo.celeris.core.syntax.SyntaxService;
import jo.celeris.core.text.Document;

/**
 * Le collage avec réindentation (A-09, M5) : un collage multiligne
 * s'aligne sur le contexte d'accueil — la profondeur lue dans l'arbre au
 * point de collage — en conservant la structure relative du collage. Un
 * collage monoligne ne bouge pas. Sans arbre, la base est l'indentation
 * de la ligne d'accueil.
 */
class CollageReindentationTest {

    private final SyntaxService service = SyntaxService.of(JavaSyntax.INSTANCE);

    /** Collage multiligne dans un corps de méthode : aligné à l'accueil. */
    @Test
    void collageMultiligneAligneALAccueil() {
        String texte = "class A {\n    void m() {\n    }\n}";
        Document doc = Document.of(texte);
        Analysis analyse = service.analyseFull(doc);
        // collage en fin de « void m() { » (offset 24)
        String colle = PasteReindenter.reindent(doc, analyse, 24,
                "int a;\nint b;", IndentStyle.spaces(4));
        assertEquals("int a;\n        int b;", colle);
    }

    /** La structure relative du collage se conserve. */
    @Test
    void structureRelativeConservee() {
        String texte = "class A {\n    void m() {\n    }\n}";
        Document doc = Document.of(texte);
        Analysis analyse = service.analyseFull(doc);
        String colle = PasteReindenter.reindent(doc, analyse, 24,
                "if (x) {\n    y();\n}", IndentStyle.spaces(4));
        // la ligne du « if » reste au niveau d'accueil (8), le corps un cran plus bas
        assertEquals("if (x) {\n            y();\n        }", colle);
    }

    /** Collage monoligne : inchangé. */
    @Test
    void collageMonoligneInchange() {
        String texte = "class A {\n    void m() {\n    }\n}";
        Document doc = Document.of(texte);
        Analysis analyse = service.analyseFull(doc);
        String colle = PasteReindenter.reindent(doc, analyse, 24,
                "int a;", IndentStyle.spaces(4));
        assertEquals("int a;", colle);
    }

    /** Sans arbre : la base est l'indentation de la ligne d'accueil. */
    @Test
    void sansArbreBaseIndentationAccueil() {
        Document doc = Document.of("        x();");
        String colle = PasteReindenter.reindent(doc, null, 12,
                "a();\nb();", IndentStyle.spaces(4));
        assertEquals("a();\n        b();", colle);
    }

    /** Le premier niveau du collage au niveau de la classe : un cran. */
    @Test
    void collageAuNiveauClasse() {
        String texte = "class A {\n}";
        Document doc = Document.of(texte);
        Analysis analyse = service.analyseFull(doc);
        // collage en fin de ligne ouvrante (offset 9)
        String colle = PasteReindenter.reindent(doc, analyse, 9,
                "int a;\nint b;", IndentStyle.spaces(4));
        assertEquals("int a;\n    int b;", colle);
    }
}
