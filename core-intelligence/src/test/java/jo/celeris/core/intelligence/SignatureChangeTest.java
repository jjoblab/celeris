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
 * Le changement de signature (B-22) : les paramètres de la méthode
 * désignée se réordonnent — la signature et chaque appel du document
 * suivent le nouvel ordre. Les refus se signalent par un plan nul,
 * jamais par un plan faux : surcharge homonyme, référence de méthode,
 * appel d'arité différente, varargs, chevrons non appariés.
 */
class SignatureChangeTest {

    private final SyntaxService java = SyntaxService.of(JavaSyntax.INSTANCE);

    private static final String SOURCE = """
            class Achat {
                double total(int quantite, double prix) {
                    return quantite * prix;
                }

                double facture() {
                    return total(3, 9.5) + total(4, 2.5);
                }
            }
            """;

    private SignatureChange.SignaturePlan planDe(String source, String designation, int[] order) {
        Document doc = Document.of(source);
        Analysis analyse = java.analyseFull(doc);
        int debut = source.indexOf(designation);
        assertTrue(debut >= 0, "désignation absente du corpus : " + designation);
        SignatureChange.SignaturePlan plan = SignatureChange.reorder(doc, analyse,
                debut, debut + designation.length(), order);
        assertNotNull(plan);
        return plan;
    }

    private void refuse(String source, String designation, int[] order) {
        Document doc = Document.of(source);
        Analysis analyse = java.analyseFull(doc);
        int debut = source.indexOf(designation);
        assertTrue(debut >= 0, "désignation absente du corpus : " + designation);
        assertNull(SignatureChange.reorder(doc, analyse,
                debut, debut + designation.length(), order),
                "le plan accepte une désignation refusable : " + designation);
    }

    /** B-22 — le réordonnancement : la signature et chaque appel suivent le nouvel ordre. */
    @Test
    void leReordonnancement() {
        TextSession session = TextSession.of(SOURCE, System::nanoTime);
        session.apply(Transaction.of(planDe(SOURCE, "int quantite, double prix",
                new int[]{1, 0}).edits()));
        assertEquals("""
                class Achat {
                    double total(double prix, int quantite) {
                        return quantite * prix;
                    }

                    double facture() {
                        return total(9.5, 3) + total(2.5, 4);
                    }
                }
                """, session.text());
        session.undo();
        assertEquals(SOURCE, session.text());
    }

    /** B-22 — trois paramètres tournent d'un cran : signature et appels suivent. */
    @Test
    void laRotationDeTrois() {
        String source = """
                class Achat {
                    String etiquette(String titre, int quantite, double prix) {
                        return titre + quantite + prix;
                    }

                    String facture() {
                        return etiquette("Achat", 3, 9.5);
                    }
                }
                """;
        TextSession session = TextSession.of(source, System::nanoTime);
        session.apply(Transaction.of(planDe(source, "String titre, int quantite, double prix",
                new int[]{2, 0, 1}).edits()));
        assertTrue(session.text().contains(
                "String etiquette(double prix, String titre, int quantite) {"),
                "la signature tourne : " + session.text());
        assertTrue(session.text().contains("return etiquette(9.5, \"Achat\", 3);"),
                "l'appel tourne : " + session.text());
    }

    /** B-22 — le constructeur suit : les constructions se réordonnent aussi. */
    @Test
    void leConstructeurSuit() {
        String source = """
                class Prix {
                    private final int brut;
                    private final double taux;

                    Prix(int brut, double taux) {
                        this.brut = brut;
                        this.taux = taux;
                    }
                }

                class Marche {
                    Prix achat() {
                        return new Prix(100, 9.5);
                    }
                }
                """;
        TextSession session = TextSession.of(source, System::nanoTime);
        session.apply(Transaction.of(planDe(source, "int brut, double taux",
                new int[]{1, 0}).edits()));
        assertTrue(session.text().contains("Prix(double taux, int brut) {"),
                session.text());
        assertTrue(session.text().contains("new Prix(9.5, 100);"), session.text());
    }

    /** B-22 — l'appel récursif suit le réordonnancement. */
    @Test
    void lAppelRecursifSuit() {
        String source = """
                class Compte {
                    int rebours(int restant, int pas) {
                        if (restant <= 0) {
                            return 0;
                        }
                        return rebours(restant - pas, pas) + 1;
                    }
                }
                """;
        TextSession session = TextSession.of(source, System::nanoTime);
        session.apply(Transaction.of(planDe(source, "int restant, int pas",
                new int[]{1, 0}).edits()));
        assertTrue(session.text().contains("int rebours(int pas, int restant) {"),
                session.text());
        assertTrue(session.text().contains("return rebours(pas, restant - pas) + 1;"),
                session.text());
    }

    /** B-22 — la surcharge homonyme refuse : l'appel ne se distingue pas par le nom seul. */
    @Test
    void laSurchargeRefuse() {
        String source = """
            class Achat {
                double total(int quantite, double prix) {
                    return quantite * prix;
                }

                double total(int quantite) {
                    return quantite;
                }
            }
            """;
        refuse(source, "int quantite, double prix", new int[]{1, 0});
    }

    /** B-22 — l'appel d'arité différente refuse : le réordonnancement n'aurait pas de sens. */
    @Test
    void lAriteDifferenteRefuse() {
        String source = """
            class Achat {
                double total(int quantite, double prix) {
                    if (prix < 0) {
                        return total(quantite, 1.0);
                    }
                    return quantite * prix + total(quantite);
                }
            }
            """;
        refuse(source, "int quantite, double prix", new int[]{1, 0});
    }

    /** B-22 — les varargs refusent : l'arité varie. */
    @Test
    void lesVarargsRefusent() {
        String source = """
            class Achat {
                double somme(double seuil, int... nombres) {
                    return seuil;
                }
            }
            """;
        refuse(source, "double seuil, int... nombres", new int[]{1, 0});
    }

    /** B-22 — la référence de méthode refuse. */
    @Test
    void laReferenceDeMethodeRefuse() {
        String source = """
            class Achat {
                double total(int quantite, double prix) {
                    return quantite * prix;
                }

                java.util.function.IntToDoubleFunction tarif() {
                    return this::total;
                }
            }
            """;
        refuse(source, "int quantite, double prix", new int[]{1, 0});
    }

    /** B-22 — l'ordre qui n'est pas une permutation se refuse, inversion comprise. */
    @Test
    void laPermutationInvalideSeRefuse() {
        Document doc = Document.of(SOURCE);
        Analysis analyse = java.analyseFull(doc);
        int debut = SOURCE.indexOf("int quantite, double prix");
        assertThrows(IllegalArgumentException.class,
                () -> SignatureChange.reorder(doc, analyse, debut, debut + 5, new int[]{0, 0}));
        assertThrows(IllegalArgumentException.class,
                () -> SignatureChange.reorder(doc, analyse, debut, debut + 5, new int[]{0, 1, 2}));
        assertThrows(IllegalArgumentException.class,
                () -> SignatureChange.reorder(doc, analyse, debut, debut + 5, new int[]{1}));
    }

    /** B-22 — les sélections inversées ou hors bornes se refusent. */
    @Test
    void lesSelectionsInvalidesSeRefusent() {
        Document doc = Document.of(SOURCE);
        Analysis analyse = java.analyseFull(doc);
        int debut = SOURCE.indexOf("int quantite");
        assertThrows(IllegalArgumentException.class,
                () -> SignatureChange.reorder(doc, analyse, debut + 2, debut,
                        new int[]{1, 0}));
        assertThrows(IllegalArgumentException.class,
                () -> SignatureChange.reorder(doc, analyse, -1, 2, new int[]{1, 0}));
    }
}
