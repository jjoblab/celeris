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
 * L'alignement en ligne (B-21) : la déclaration désignée disparaît —
 * ses usages reçoivent le littéral de son initialisateur, la
 * déclaration se supprime si elle est locale ou privée. Les refus se
 * signalent par un plan nul, jamais par un plan faux : sans littéral
 * seul, sur une réaffectation, une ombre homonyme ou un récepteur
 * étranger.
 */
class InlineTest {

    private final SyntaxService java = SyntaxService.of(JavaSyntax.INSTANCE);

    private Inline.InlinePlan aligne(String source, String designation) {
        Document doc = Document.of(source);
        Analysis analyse = java.analyseFull(doc);
        int debut = source.indexOf(designation);
        assertTrue(debut >= 0, "désignation absente du corpus : " + designation);
        Inline.InlinePlan plan = Inline.declaration(doc, analyse, debut,
                Math.min(debut + designation.length(), source.length()));
        assertNotNull(plan, "le plan refuse une désignation alignable : " + designation);
        return plan;
    }

    private void refuse(String source, String designation) {
        Document doc = Document.of(source);
        Analysis analyse = java.analyseFull(doc);
        int debut = source.indexOf(designation);
        assertTrue(debut >= 0, "désignation absente du corpus : " + designation);
        assertNull(Inline.declaration(doc, analyse, debut, debut + designation.length()),
                "le plan accepte une désignation refusable : " + designation);
    }

    /** B-21 — la locale : usages remplacés, déclaration supprimée, annulable. */
    @Test
    void laLocaleAlignee() {
        String source = """
                class Calcul {
                    int total(int quantite) {
                        int unite = 2;
                        return quantite * unite + unite;
                    }
                }
                """;
        TextSession session = TextSession.of(source, System::nanoTime);
        session.apply(Transaction.of(aligne(source, "unite = 2").edits()));
        assertEquals("""
                class Calcul {
                    int total(int quantite) {
                        return quantite * 2 + 2;
                    }
                }
                """, session.text());
        session.undo();
        assertEquals(source, session.text());
    }

    /** B-21 — le champ privé : usages nus et membres de this remplacés, déclaration supprimée. */
    @Test
    void leChampPriveAligne() {
        String source = """
                class Calcul {
                    private int base = 40;

                    int total(int quantite) {
                        return base + quantite + this.base;
                    }
                }
                """;
        TextSession session = TextSession.of(source, System::nanoTime);
        session.apply(Transaction.of(aligne(source, "base = 40").edits()));
        assertEquals("""
                class Calcul {

                    int total(int quantite) {
                        return 40 + quantite + 40;
                    }
                }
                """, session.text());
        session.undo();
        assertEquals(source, session.text());
    }

    /** B-21 — le champ visible hors de la classe reste déclaré, ses usages remplacés. */
    @Test
    void leChampPublicReste() {
        String source = """
                class Calcul {
                    public int base = 40;

                    int total(int quantite) {
                        return base + quantite;
                    }
                }
                """;
        TextSession session = TextSession.of(source, System::nanoTime);
        session.apply(Transaction.of(aligne(source, "base = 40").edits()));
        assertTrue(session.text().contains("public int base = 40;"),
                "le champ public reste déclaré : " + session.text());
        assertTrue(session.text().contains("return 40 + quantite;"), session.text());
    }

    /** B-21 — l'appel homonyme ne compte pas : le nom d'appel n'est pas une usage. */
    @Test
    void lAppelHomonymeNeComptePas() {
        String source = """
                class Calcul {
                    int limite(int seuil) {
                        int unite = 2;
                        return unite(unite);
                    }

                    int unite(int valeur) {
                        return valeur;
                    }
                }
                """;
        TextSession session = TextSession.of(source, System::nanoTime);
        session.apply(Transaction.of(aligne(source, "unite = 2").edits()));
        assertTrue(session.text().contains("return unite(2);"),
                "l'appel homonyme reste, la locale se remplace : " + session.text());
        assertTrue(!session.text().contains("int unite = 2;"), session.text());
    }

    /** B-21 — la locale réaffectée refuse : la substitution ignorerait la réaffectation. */
    @Test
    void laReaffectationRefuse() {
        refuse("""
                class Calcul {
                    int total() {
                        int unite = 2;
                        unite = 3;
                        return unite;
                    }
                }
                """, "unite = 2");
        refuse("""
                class Calcul {
                    int total() {
                        int unite = 2;
                        unite++;
                        return unite;
                    }
                }
                """, "unite = 2");
    }

    /** B-21 — le champ réaffecté (au constructeur) refuse. */
    @Test
    void leChampReaffecteRefuse() {
        refuse("""
                class Calcul {
                    private int base = 40;

                    Calcul() {
                        base = 41;
                    }
                }
                """, "base = 40");
    }

    /** B-21 — l'initialisateur doit être un littéral seul : « 1 + 2 » refuse (précédence). */
    @Test
    void lInitialisateurComposeRefuse() {
        refuse("""
                class Calcul {
                    int total() {
                        int unite = 1 + 2;
                        return unite;
                    }
                }
                """, "unite = 1 + 2");
    }

    /** B-21 — l'ombre homonyme refuse : un lambda paramétré du même nom changerait le sens. */
    @Test
    void lOmbreDeLambdaRefuse() {
        refuse("""
                class Calcul {
                    int total() {
                        int unite = 2;
                        return java.util.function.IntUnaryOperator.identity()
                                .andThen(unite -> unite + 1).applyAsInt(3);
                    }
                }
                """, "unite = 2");
    }

    /** B-21 — le champ ombré par une locale refuse : la locale capture les usages. */
    @Test
    void leChampOmbreRefuse() {
        refuse("""
                class Calcul {
                    private int base = 40;

                    int total() {
                        int base = 2;
                        return base;
                    }
                }
                """, "base = 40");
    }

    /** B-21 — le récepteur étranger refuse : la cible ne se connaît pas par le nom seul. */
    @Test
    void leRecepteurEtrangerRefuse() {
        refuse("""
                class Calcul {
                    private int base = 40;

                    int total(Calcul autre) {
                        return autre.base;
                    }
                }
                """, "base = 40");
    }

    /** B-21 — hors déclaration (l'usage lui-même), le plan est nul. */
    @Test
    void horsDeclarationRefuse() {
        refuse("""
                class Calcul {
                    int total(int quantite) {
                        int unite = 2;
                        return quantite * unite;
                    }
                }
                """, "quantite * unite");
    }

    /** B-21 — les sélections inversées ou hors bornes se refusent. */
    @Test
    void lesSelectionsInvalidesSeRefusent() {
        String source = """
                class Calcul {
                    int total() {
                        int unite = 2;
                        return unite;
                    }
                }
                """;
        Document doc = Document.of(source);
        Analysis analyse = java.analyseFull(doc);
        int debut = source.indexOf("unite = 2");
        assertThrows(IllegalArgumentException.class,
                () -> Inline.declaration(doc, analyse, debut + 2, debut));
        assertThrows(IllegalArgumentException.class,
                () -> Inline.declaration(doc, analyse, -1, 2));
        assertThrows(IllegalArgumentException.class,
                () -> Inline.declaration(doc, analyse, 0, doc.length() + 1));
    }
}
