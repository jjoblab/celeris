package jo.celeris.core.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.JavaSyntax;
import jo.celeris.core.syntax.SyntaxService;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextSession;
import jo.celeris.core.text.Transaction;

/**
 * L'extraction (B-20) : l'expression sélectionnée devient une
 * déclaration — variable locale, constante ou champ de classe — et la
 * sélection se remplace par son nom. Les plans s'appliquent par la
 * voie de transaction, annulables ; les refus se signalent par un
 * plan nul, jamais par un plan faux.
 */
class ExtractionTest {

    private final SyntaxService java = SyntaxService.of(JavaSyntax.INSTANCE);

    private static final String SOURCE = """
            package demo;

            public class Calcul {
                private int base = 40;

                public int total(int ajout) {
                    int partiel = base + ajout;
                    return partiel * 2;
                }
            }
            """;

    private int debutDe(String expression) {
        int index = SOURCE.indexOf(expression);
        assertTrue(index >= 0, "expression absente du corpus : " + expression);
        return index;
    }

    private Extraction.ExtractionPlan planVariable(String expression, String nom) {
        Document doc = Document.of(SOURCE);
        Analysis analyse = java.analyseFull(doc);
        Extraction.ExtractionPlan plan = Extraction.variable(doc, analyse,
                debutDe(expression), debutDe(expression) + expression.length(), nom);
        assertNotNull(plan);
        return plan;
    }

    /** B-20 — la variable locale : déclaration avant la ligne, remplacement par le nom. */
    @Test
    void laVariableLocale() {
        Extraction.ExtractionPlan plan = planVariable("base + ajout", "somme");
        TextSession session = TextSession.of(SOURCE, System::nanoTime);
        session.apply(Transaction.of(plan.edits()));
        assertTrue(session.text().contains("        var somme = base + ajout;\n"
                + "        int partiel = somme;"),
                "la déclaration se pose avant la ligne de la sélection, à son retrait,"
                        + " et la sélection se remplace par le nom : " + session.text());
        assertTrue(!session.text().contains("int partiel = base + ajout;"),
                "la sélection remplacée ne reste pas");
        // annulable : l'original revient d'un geste
        session.undo();
        assertEquals(SOURCE, session.text());
    }

    /** B-20 — la variable exige une méthode porteuse : hors méthode, plan nul. */
    @Test
    void laVariableHorsMethodeRefuse() {
        Document doc = Document.of(SOURCE);
        Analysis analyse = java.analyseFull(doc);
        int debut = debutDe("40");
        assertNull(Extraction.variable(doc, analyse, debut, debut + 2, "dix"));
    }

    /** B-20 — la sélection au début de sa ligne : le nom remplacé reste après la déclaration. */
    @Test
    void laVariableEnDebutDeLigne() {
        String source = """
                class F {
                    int f() {
                a + b;
                        return 0;
                    }
                }
                """;
        Document doc = Document.of(source);
        Analysis analyse = java.analyseFull(doc);
        int debut = source.indexOf("a + b");
        Extraction.ExtractionPlan plan = Extraction.variable(doc, analyse,
                debut, debut + 5, "somme");
        assertNotNull(plan);
        TextSession session = TextSession.of(source, System::nanoTime);
        session.apply(Transaction.of(plan.edits()));
        assertEquals("""
                class F {
                    int f() {
                var somme = a + b;
                somme;
                        return 0;
                    }
                }
                """, session.text());
    }

    /** B-20 — la constante : type lu sur le littéral, posée avant l'accolade fermante. */
    @Test
    void laConstante() {
        Document doc = Document.of(SOURCE);
        Analysis analyse = java.analyseFull(doc);
        int debut = debutDe("40");
        Extraction.ExtractionPlan plan = Extraction.constant(doc, analyse,
                debut, debut + 2, "baseInitiale");
        assertNotNull(plan);
        TextSession session = TextSession.of(SOURCE, System::nanoTime);
        session.apply(Transaction.of(plan.edits()));
        assertTrue(session.text().contains("    private static final int BASE_INITIALE = 40;\n}"),
                "la constante se pose avant l'accolade fermante, criée aux joints : "
                        + session.text());
        assertTrue(session.text().contains("private int base = BASE_INITIALE;"),
                "la sélection se remplace par le nom crié");
        session.undo();
        assertEquals(SOURCE, session.text());
    }

    /** B-20 — le champ : même pose que la constante, casse du nom gardée. */
    @Test
    void leChamp() {
        Document doc = Document.of(SOURCE);
        Analysis analyse = java.analyseFull(doc);
        int debut = debutDe("40");
        Extraction.ExtractionPlan plan = Extraction.field(doc, analyse,
                debut, debut + 2, "valeurBase");
        assertNotNull(plan);
        TextSession session = TextSession.of(SOURCE, System::nanoTime);
        session.apply(Transaction.of(plan.edits()));
        assertTrue(session.text().contains("    private int valeurBase = 40;\n}"),
                session.text());
        assertTrue(session.text().contains("private int base = valeurBase;"));
        session.undo();
        assertEquals(SOURCE, session.text());
    }

    /** B-20 — sans littéral lisible, la constante et le champ refusent. */
    @Test
    void sansLitteralLesMembresRefusent() {
        Document doc = Document.of(SOURCE);
        Analysis analyse = java.analyseFull(doc);
        int debut = debutDe("base + ajout");
        assertNull(Extraction.constant(doc, analyse, debut, debut + 11, "somme"));
        assertNull(Extraction.field(doc, analyse, debut, debut + 11, "somme"));
        // La variable, elle, tombe sur « var ».
        assertNotNull(Extraction.variable(doc, analyse, debut, debut + 11, "somme"));
    }

    /** B-20 — les types lexicaux : la forme du littéral décide. */
    @Test
    void lesTypesLitteraux() {
        assertEquals("int", Extraction.literalType("42"));
        assertEquals("int", Extraction.literalType("10_000"));
        assertEquals("long", Extraction.literalType("42L"));
        assertEquals("int", Extraction.literalType("0x1F"));
        assertEquals("long", Extraction.literalType("0xAL"));
        assertEquals("int", Extraction.literalType("0b1010"));
        assertEquals("double", Extraction.literalType("3.14"));
        assertEquals("float", Extraction.literalType("3.14f"));
        assertEquals("double", Extraction.literalType("3."));
        assertEquals("boolean", Extraction.literalType("true"));
        assertEquals("char", Extraction.literalType("'x'"));
        assertEquals("String", Extraction.literalType("\"abc\""));
        assertNull(Extraction.literalType("compte * 2"));
        assertNull(Extraction.literalType("1e5"));
        assertNull(Extraction.literalType("-"));
        assertNull(Extraction.literalType("0x"));
        assertNull(Extraction.literalType("3.14L"));
    }

    /** B-20 — les sélections vides, inversées ou hors bornes se refusent. */
    @Test
    void lesSelectionsInvalidesSeRefusent() {
        Document doc = Document.of(SOURCE);
        Analysis analyse = java.analyseFull(doc);
        int debut = debutDe("40");
        assertThrows(IllegalArgumentException.class,
                () -> Extraction.variable(doc, analyse, debut, debut, "x"));
        assertThrows(IllegalArgumentException.class,
                () -> Extraction.variable(doc, analyse, debut + 2, debut, "x"));
        assertThrows(IllegalArgumentException.class,
                () -> Extraction.variable(doc, analyse, -1, 2, "x"));
        assertThrows(IllegalArgumentException.class,
                () -> Extraction.variable(doc, analyse, debut, doc.length() + 1, "x"));
        assertThrows(IllegalArgumentException.class,
                () -> Extraction.variable(doc, analyse, debut, debut + 2, " "));
    }
}
