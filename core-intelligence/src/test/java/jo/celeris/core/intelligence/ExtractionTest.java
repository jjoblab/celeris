package jo.celeris.core.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.clike.JavaSyntax;
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

    private Extraction.ExtractionPlan planVariable(String expression, String name) {
        Document doc = Document.of(SOURCE);
        Analysis analyse = java.analyseFull(doc);
        Extraction.ExtractionPlan plan = Extraction.variable(doc, analyse,
                debutDe(expression), debutDe(expression) + expression.length(), name);
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
                "la déclaration se pose avant la ligne de la sélection, à son indent,"
                        + " et la sélection se remplace par le nom : " + session.text());
        assertTrue(!session.text().contains("int partiel = base + ajout;"),
                "la sélection remplacée ne reste pas");
        // annulable : l'original revient d'un geste
        session.undo();
        assertEquals(SOURCE, session.text());
    }

    /** B-20 — la variable exige une méthode enclosing : hors méthode, plan nul. */
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

    /** B-20 — la constante : type lu sur le littéral, posée avant l'accolade closeParen. */
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
                "la constante se pose avant l'accolade closeParen, criée aux joints : "
                        + session.text());
        assertTrue(session.text().contains("private int base = BASE_INITIALE;"),
                "la sélection se remplace par le nom crié");
        session.undo();
        assertEquals(SOURCE, session.text());
    }

    /** B-20 — le champ : même pose que la constante, casse du name gardée. */
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

    /** B-20 — la méthode : l'appel remplace la sélection, la méthode naît
     * après la porteuse, le paramètre déclaré devient le sien, le champ passe. */
    @Test
    void laMethode() {
        Document doc = Document.of(SOURCE);
        Analysis analyse = java.analyseFull(doc);
        int debut = debutDe("base + ajout");
        Extraction.ExtractionPlan plan = Extraction.method(doc, analyse,
                debut, debut + "base + ajout".length(), "somme");
        assertNotNull(plan);
        TextSession session = TextSession.of(SOURCE, System::nanoTime);
        session.apply(Transaction.of(plan.edits()));
        assertTrue(session.text().contains("        int partiel = somme(ajout);"),
                "la sélection se remplace par l'appel : " + session.text());
        assertTrue(session.text().contains("""
                    }

                    private int somme(int ajout) {
                        return base + ajout;
                    }
                }"""), "la méthode naît après la porteuse, le champ passe sans paramètre : "
                        + session.text());
        session.undo();
        assertEquals(SOURCE, session.text());
    }

    /** B-20 — la méthode sans référence déclarée : l'appel ne prend pas d'argument. */
    @Test
    void laMethodeSansParametre() {
        Document doc = Document.of(SOURCE);
        Analysis analyse = java.analyseFull(doc);
        int debut = debutDe("partiel * 2");
        Extraction.ExtractionPlan plan = Extraction.method(doc, analyse,
                debut, debut + 11, "foisDeux");
        assertNotNull(plan);
        TextSession session = TextSession.of(SOURCE, System::nanoTime);
        session.apply(Transaction.of(plan.edits()));
        assertTrue(session.text().contains("        return foisDeux(partiel);"),
                "le local devient le paramètre de la nouvelle méthode : " + session.text());
        assertTrue(session.text().contains(
                "    private int foisDeux(int partiel) {\n"
                        + "        return partiel * 2;\n    }"),
                session.text());
    }

    /** B-20 — la méthode : la promotion numérique du returnType (long ici) et
     * les paramètres dans l'ordre de leur première lecture. */
    @Test
    void laMethodePromue() {
        String source = """
                class Compteur {
                    long totaliser(int compteur) {
                        long borne = 10_000L;
                        return borne - compteur;
                    }
                }
                """;
        Document doc = Document.of(source);
        Analysis analyse = java.analyseFull(doc);
        int debut = source.indexOf("borne - compteur");
        Extraction.ExtractionPlan plan = Extraction.method(doc, analyse,
                debut, debut + 16, "restant");
        assertNotNull(plan);
        TextSession session = TextSession.of(source, System::nanoTime);
        session.apply(Transaction.of(plan.edits()));
        assertTrue(session.text().contains("private long restant(long borne, int compteur) {"),
                "le returnType se promeut au plus large des opérandes : " + session.text());
        assertTrue(session.text().contains("return restant(borne, compteur);"), session.text());
    }

    /** B-20 — la méthode : la concaténation se lit quand une chaîne se
     * joint à des « + » seuls. */
    @Test
    void laMethodeConcatenee() {
        String source = """
                class Libelle {
                    String composer(String prefixe) {
                        return prefixe + " !";
                    }
                }
                """;
        Document doc = Document.of(source);
        Analysis analyse = java.analyseFull(doc);
        int debut = source.indexOf("prefixe + \" !\"");
        Extraction.ExtractionPlan plan = Extraction.method(doc, analyse,
                debut, debut + 14, "suffixer");
        assertNotNull(plan);
        TextSession session = TextSession.of(source, System::nanoTime);
        session.apply(Transaction.of(plan.edits()));
        assertTrue(session.text().contains("private String suffixer(String prefixe) {"),
                session.text());
        assertTrue(session.text().contains("return suffixer(prefixe);"), session.text());
    }

    /** B-20 — la méthode refuse hors d'une méthode enclosing (initialisateur de champ). */
    @Test
    void laMethodeHorsMethodeRefuse() {
        Document doc = Document.of(SOURCE);
        Analysis analyse = java.analyseFull(doc);
        int debut = debutDe("40");
        assertNull(Extraction.method(doc, analyse, debut, debut + 2, "quarante"));
    }

    /** B-20 — la méthode refuse une référence nue non déclarée, jamais un plan faux. */
    @Test
    void laMethodeRefuseLeNomInconnu() {
        String source = """
                class Compte {
                    int lire() {
                        return mystere * 2;
                    }
                }
                """;
        Document doc = Document.of(source);
        Analysis analyse = java.analyseFull(doc);
        int debut = source.indexOf("mystere * 2");
        assertNull(Extraction.method(doc, analyse, debut, debut + 11, "double"));
    }

    /** B-20 — la méthode refuse un local « var » dont l'initialisateur ne se lit pas. */
    @Test
    void laMethodeRefuseLeVarIllisible() {
        String source = """
                class T {
                    int lire() {
                        var tampon = charger();
                        return tampon;
                    }
                }
                """;
        Document doc = Document.of(source);
        Analysis analyse = java.analyseFull(doc);
        int debut = source.indexOf("tampon;");
        assertNull(Extraction.method(doc, analyse, debut, debut + 6, "valeur"));
    }

    /** B-20 — le paramètre : la signature gagne le paramètre, la sélection
     * se remplace par son nom, chaque call du document passe l'expression. */
    @Test
    void leParametre() {
        String source = """
                class Achat {
                    double total(int quantite) {
                        return quantite * 2;
                    }

                    double facture() {
                        return total(3) + total(4);
                    }
                }
                """;
        Document doc = Document.of(source);
        Analysis analyse = java.analyseFull(doc);
        int debut = source.indexOf("quantite * 2");
        int debutLitteral = source.indexOf("2", debut);
        Extraction.ExtractionPlan plan = Extraction.parameter(doc, analyse,
                debutLitteral, debutLitteral + 1, "unite");
        assertNotNull(plan);
        TextSession session = TextSession.of(source, System::nanoTime);
        session.apply(Transaction.of(plan.edits()));
        assertTrue(session.text().contains("double total(int quantite, int unite) {"),
                "la signature gagne le paramètre typé : " + session.text());
        assertTrue(session.text().contains("return quantite * unite;"), session.text());
        assertTrue(session.text().contains("return total(3, 2) + total(4, 2);"),
                "chaque call du document passe l'expression : " + session.text());
        session.undo();
        assertEquals(source, session.text());
    }

    /** B-20 — le paramètre : un call sans argument reçoit l'expression
     * sans virgule, un champ passe en argument. */
    @Test
    void leParametreChamp() {
        String source = """
                class Achat {
                    private double tarif = 9.5;

                    double remise() {
                        return tarif / 2;
                    }

                    double net() {
                        return remise();
                    }
                }
                """;
        Document doc = Document.of(source);
        Analysis analyse = java.analyseFull(doc);
        int debut = source.indexOf("tarif / 2");
        Extraction.ExtractionPlan plan = Extraction.parameter(doc, analyse,
                debut, debut + 5, "seuil");
        assertNotNull(plan);
        TextSession session = TextSession.of(source, System::nanoTime);
        session.apply(Transaction.of(plan.edits()));
        assertTrue(session.text().contains("double remise(double seuil) {"),
                session.text());
        assertTrue(session.text().contains("return seuil / 2;"), session.text());
        assertTrue(session.text().contains("return remise(tarif);"),
                "l'appel vide reçoit le champ sans virgule : " + session.text());
    }

    /** B-20 — le paramètre du constructeur : les constructions suivent. */
    @Test
    void leParametreDuConstructeur() {
        String source = """
                class Prix {
                    private final int brut;

                    Prix() {
                        brut = 100;
                    }
                }

                class Marche {
                    Prix achat() {
                        return new Prix();
                    }
                }
                """;
        Document doc = Document.of(source);
        Analysis analyse = java.analyseFull(doc);
        int debut = source.indexOf("100");
        Extraction.ExtractionPlan plan = Extraction.parameter(doc, analyse,
                debut, debut + 3, "arrondi");
        assertNotNull(plan);
        TextSession session = TextSession.of(source, System::nanoTime);
        session.apply(Transaction.of(plan.edits()));
        assertTrue(session.text().contains("Prix(int arrondi) {"), session.text());
        assertTrue(session.text().contains("brut = arrondi;"), session.text());
        assertTrue(session.text().contains("new Prix(100)"), session.text());
    }

    /** B-20 — le paramètre refuse une expression qui lit un paramètre de
     * la porteuse : invivable aux appels. */
    @Test
    void leParametreRefuseLeLocal() {
        String source = """
                class Achat {
                    double total(int quantite) {
                        return total(quantite);
                    }
                }
                """;
        Document doc = Document.of(source);
        Analysis analyse = java.analyseFull(doc);
        int debut = source.indexOf("quantite);");
        assertNull(Extraction.parameter(doc, analyse, debut, debut + 8, "unite"));
    }

    /** B-20 — le paramètre refuse la surcharge homonyme : l'appel ne se
     * distingue pas par le nom seul. */
    @Test
    void leParametreRefuseLaSurcharge() {
        String source = """
                class Achat {
                    double total(int quantite) {
                        return quantite * 2;
                    }

                    double total() {
                        return 0;
                    }
                }
                """;
        Document doc = Document.of(source);
        Analysis analyse = java.analyseFull(doc);
        int debut = source.indexOf("2;");
        assertNull(Extraction.parameter(doc, analyse, debut, debut + 1, "unite"));
    }

    /** B-20 — le paramètre refuse l'appel sur un récepteur qui n'est pas
     * this : la cible ne se connaît pas par le nom seul. */
    @Test
    void leParametreRefuseLAppelAmbigu() {
        String source = """
                class Achat {
                    int total() {
                        return 2;
                    }

                    int appelant(Achat autre) {
                        return autre.total();
                    }
                }
                """;
        Document doc = Document.of(source);
        Analysis analyse = java.analyseFull(doc);
        int debut = source.indexOf("2;");
        assertNull(Extraction.parameter(doc, analyse, debut, debut + 1, "unite"));
    }
}
