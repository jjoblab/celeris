package jo.celeris.core.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.clike.JavaSyntax;
import jo.celeris.core.syntax.SyntaxService;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextSession;
import jo.celeris.core.text.Transaction;

/**
 * La suppression sûre (B-23) : la déclaration désignée — méthode, type ou
 * champ — ne disparaît que si le document ne l'utilise nulle part
 * ailleurs. Le verdict rend les usages barants ou le plan d'effacement ;
 * l'absence de désignation se signale par un verdict nul, jamais par un
 * plan faux.
 */
class SafeDeleteTest {

    private final SyntaxService java = SyntaxService.of(JavaSyntax.INSTANCE);

    /** B-23 — la méthode inutilisée disparaît, lignes et ligne blanche absorbées. */
    @Test
    void laMethodeInutiliseeDisparait() {
        String source = """
                class Achat {
                    double total(int quantite) {
                        return quantite;
                    }

                    double facture() {
                        return 2.5;
                    }
                }
                """;
        TextSession session = TextSession.of(source, System::nanoTime);
        session.apply(Transaction.of(plan(source, "total").edits()));
        assertEquals("""
                class Achat {
                    double facture() {
                        return 2.5;
                    }
                }
                """, session.text());
        session.undo();
        assertEquals(source, session.text());
    }

    /** B-23 — la méthode utilisée refuse : le verdict rend l'usage qui barre. */
    @Test
    void laMethodeUtiliseeRefuse() {
        String source = """
                class Achat {
                    double total(int quantite) {
                        return quantite;
                    }

                    double facture() {
                        return total(3);
                    }
                }
                """;
        SafeDelete.Verdict verdict = verdict(source, "total");
        assertTrue(verdict.edits().isEmpty(), "la voie n'est pas libre");
        assertEquals(1, verdict.usages().size(), "un usage attendu");
        assertEquals(source.indexOf("total(3)"), verdict.usages().get(0).offset());
        assertEquals("total", verdict.usages().get(0).text());
    }

    /** B-23 — la récursion seule n'est pas un usage : la méthode disparaît. */
    @Test
    void laRecursionSeuleNeBarrePas() {
        String source = """
                class Achat {
                    int compte(int n) {
                        return n <= 0 ? 0 : compte(n - 1);
                    }
                }
                """;
        TextSession session = TextSession.of(source, System::nanoTime);
        session.apply(Transaction.of(plan(source, "compte").edits()));
        assertEquals("""
                class Achat {
                }
                """, session.text());
    }

    /** B-23 — la référence de méthode barre : le verdict la rend. */
    @Test
    void laReferenceDeMethodeBarre() {
        String source = """
                import java.util.function.IntSupplier;

                class Achat {
                    int taille() {
                        return 3;
                    }

                    IntSupplier fournisseur() {
                        return this::taille;
                    }
                }
                """;
        SafeDelete.Verdict verdict = verdict(source, "int taille()");
        assertTrue(verdict.edits().isEmpty(), "la voie n'est pas libre");
        assertEquals(1, verdict.usages().size(), "le suspect attendu");
        assertEquals(source.indexOf("::taille") + 2, verdict.usages().get(0).offset());
    }

    /** B-23 — le constructeur construit ailleurs barre ; inutilisé, il disparaît. */
    @Test
    void leConstructeur() {
        String utilise = """
                class Achat {
                    Achat() {
                    }

                    Achat cloneAchat() {
                        return new Achat();
                    }
                }
                """;
        SafeDelete.Verdict verdict = verdict(utilise, "Achat()");
        assertTrue(verdict.edits().isEmpty(), "la construction barre");
        assertEquals(1, verdict.usages().size());

        String libre = """
                class Achat {
                    Achat() {
                    }

                    double total() {
                        return 2.5;
                    }
                }
                """;
        TextSession session = TextSession.of(libre, System::nanoTime);
        session.apply(Transaction.of(plan(libre, "Achat()").edits()));
        assertEquals("""
                class Achat {
                    double total() {
                        return 2.5;
                    }
                }
                """, session.text());
    }

    /** B-23 — le type utilisé refuse (la construction compte), inutilisé il disparaît. */
    @Test
    void leType() {
        String utilise = """
                class Achat {
                }

                class Etiquette {
                    Achat achat() {
                        return new Achat();
                    }
                }
                """;
        SafeDelete.Verdict verdict = verdict(utilise, "class Achat");
        assertTrue(verdict.edits().isEmpty(), "le type utilisé barre");
        assertEquals(2, verdict.usages().size(), "la déclaration du retour et la construction");

        String libre = """
                class Achat {
                }

                class Etiquette {
                    String titre() {
                        return "a";
                    }
                }
                """;
        TextSession session = TextSession.of(libre, System::nanoTime);
        session.apply(Transaction.of(plan(libre, "class Achat").edits()));
        assertEquals("""
                class Etiquette {
                    String titre() {
                        return "a";
                    }
                }
                """, session.text());
    }

    /** B-23 — le champ lu ailleurs barre ; l'appel homonyme d'une méthode ne lit pas. */
    @Test
    void leChamp() {
        String utilise = """
                class Achat {
                    int quantite;

                    int taille() {
                        return quantite;
                    }
                }
                """;
        assertTrue(verdict(utilise, "quantite").edits().isEmpty(), "la lecture barre");
        assertEquals(1, verdict(utilise, "quantite").usages().size());

        String appele = """
                class Achat {
                    int quantite() {
                        return 3;
                    }

                    int taille;

                    int lecture() {
                        return quantite();
                    }
                }
                """;
        TextSession session = TextSession.of(appele, System::nanoTime);
        session.apply(Transaction.of(plan(appele, "int taille;").edits()));
        assertEquals("""
                class Achat {
                    int quantite() {
                        return 3;
                    }

                    int lecture() {
                        return quantite();
                    }
                }
                """, session.text());
    }

    /** B-23 — le champ qui partage sa ligne ou sa déclaration refuse. */
    @Test
    void leChampPartageRefuse() {
        refuse("""
                class Achat {
                    int a, b;
                }
                """, "int a", "la déclaration partagée refuse");
        refuse("""
                class Achat {
                    int a; int b;
                }
                """, "int a;", "la ligne partagée refuse");
    }

    /** Le refus attendu : le verdict est nul. */
    private void refuse(String source, String designation, String message) {
        Document doc = Document.of(source);
        Analysis analyse = java.analyseFull(doc);
        int debut = source.indexOf(designation);
        assertTrue(debut >= 0, "désignation absente du corpus : " + designation);
        assertNull(SafeDelete.delete(doc, analyse, debut, debut + designation.length()), message);
    }

    /** B-23 — hors déclaration, le verdict est nul ; la sélection invalide refuse d'exception. */
    @Test
    void horsDeclarationEtSelectionInvalide() {
        String source = """
                import java.util.List;

                class Achat {
                    double total() {
                        return 2.5;
                    }
                }
                """;
        Document doc = Document.of(source);
        Analysis analyse = java.analyseFull(doc);
        assertNull(SafeDelete.delete(doc, analyse, 0, 6), "l'import n'est pas une désignation");
        assertThrows(IllegalArgumentException.class,
                () -> SafeDelete.delete(doc, analyse, 6, 2));
        assertThrows(IllegalArgumentException.class,
                () -> SafeDelete.delete(doc, analyse, -1, 2));
    }

    /** Le plan d'effacement d'une désignation présente — la voie est libre. */
    private SafeDelete.Verdict plan(String source, String designation) {
        SafeDelete.Verdict verdict = verdict(source, designation);
        assertTrue(verdict.usages().isEmpty(),
                "la désignation est utilisée : " + verdict.usages());
        return verdict;
    }

    /** Le verdict d'une désignation présente, jamais nul. */
    private SafeDelete.Verdict verdict(String source, String designation) {
        Document doc = Document.of(source);
        Analysis analyse = java.analyseFull(doc);
        int debut = source.indexOf(designation);
        assertTrue(debut >= 0, "désignation absente du corpus : " + designation);
        SafeDelete.Verdict verdict = SafeDelete.delete(doc, analyse,
                debut, debut + designation.length());
        assertNotNull(verdict, "la désignation n'est pas reconnue : " + designation);
        return verdict;
    }

    /** Le constructeur canonique refuse les verdicts vides ou doubles. */
    @Test
    void leVerdictIncompleteRefuse() {
        assertThrows(IllegalArgumentException.class,
                () -> new SafeDelete.Verdict(List.of(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new SafeDelete.Verdict(
                List.of(new SafeDelete.Usage(0, "a")),
                List.of(new jo.celeris.core.text.TextEdit(0, "a", "b"))));
    }
}
