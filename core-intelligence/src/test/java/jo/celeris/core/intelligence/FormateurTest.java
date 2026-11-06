package jo.celeris.core.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.JavaSyntax;
import jo.celeris.core.syntax.KotlinSyntax;
import jo.celeris.core.syntax.SyntaxService;
import jo.celeris.core.syntax.XmlSyntax;
import jo.celeris.core.text.Document;

/**
 * Le formatage du code (B-25) : l'indentation se règle sur les niveaux
 * de l'arbre — le même arbre que la coloration (ADR-004) — et le
 * formatage est IDEMPOTENT : reformater le formaté ne change rien.
 */
class FormateurTest {

    private final SyntaxService java = SyntaxService.of(JavaSyntax.INSTANCE);
    private final SyntaxService kotlin = SyntaxService.of(KotlinSyntax.INSTANCE);
    private final SyntaxService xml = SyntaxService.of(XmlSyntax.INSTANCE);

    private String formater(SyntaxService service, String texte, IndentStyle style) {
        Document doc = Document.of(texte);
        Analysis analyse = service.analyseFull(doc);
        return Formateur.formater(doc, analyse, style);
    }

    /** Le formatage Java : les niveaux de l'arbre réindentent chaque ligne. */
    @Test
    void leFormatageJava() {
        String source = """
                class A {
                public void f() {
                int x = 1;
                    if (x > 0) {
                        x = 2;
                        }
                    }
                }
                """;
        String attendu = """
                class A {
                    public void f() {
                        int x = 1;
                        if (x > 0) {
                            x = 2;
                        }
                    }
                }
                """;
        assertEquals(attendu, formater(java, source, IndentStyle.ESPACES_4));
    }

    /** L'idempotence : reformater le formaté ne change RIEN — le critère B-25. */
    @Test
    void lidempotence() {
        String source = """
                class A {
                        public void f() {
                	int x;
                    }
                }
                """;
        String premiere = formater(java, source, IndentStyle.ESPACES_4);
        String seconde = formater(java, premiere, IndentStyle.ESPACES_4);
        assertEquals(premiere, seconde);
        // et sur les autres langages, idem
        String xml = "<a>\n  <b>\n <c/>\n   </b>\n</a>\n";
        String xmlUne = formater(this.xml, xml, IndentStyle.ESPACES_2);
        assertEquals(xmlUne, formater(this.xml, xmlUne, IndentStyle.ESPACES_2));
        String kt = "class K {\nfun f() {\nval x = 1\n}\n}\n";
        String ktUne = formater(kotlin, kt, IndentStyle.ESPACES_4);
        assertEquals(ktUne, formater(kotlin, ktUne, IndentStyle.ESPACES_4));
    }

    /** Les lignes vides ou blanches : le formatage les vide, sans inventer d'indentation. */
    @Test
    void lesLignesVides() {
        String source = "class A {\n   \n\t\n    void f() {}\n}\n";
        assertEquals("class A {\n\n\n    void f() {}\n}\n",
                formater(java, source, IndentStyle.ESPACES_4));
    }

    /** Le style à tabulations : le niveau devient une tabulation par cran. */
    @Test
    void leStyleTabulations() {
        String source = "class A {\n    void f() {}\n}\n";
        assertEquals("class A {\n\tvoid f() {}\n}\n",
                formater(java, source, IndentStyle.tabs(4)));
    }

    /** Le document déjà bien formaté : inchangé, au saut final près. */
    @Test
    void leDocumentDejaBienFormate() {
        String source = "class A {\n    void f() {\n        int x;\n    }\n}\n";
        assertEquals(source, formater(java, source, IndentStyle.ESPACES_4));
        assertTrue(source.length() > 0);
    }

    /** Le XML : l'imbrication des balises porte les niveaux. */
    @Test
    void leFormatageXml() {
        String source = "<racine>\n<enfant>\n<petit/>\n</enfant>\n</racine>\n";
        String attendu = "<racine>\n  <enfant>\n    <petit/>\n  </enfant>\n</racine>\n";
        assertEquals(attendu, formater(xml, source, IndentStyle.ESPACES_2));
    }
}
