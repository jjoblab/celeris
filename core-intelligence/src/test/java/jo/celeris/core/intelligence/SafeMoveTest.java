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
 * Le déplacement sûr (B-23) : la méthode désignée déménage dans un autre
 * type du même document si, et seulement si, rien ne pend — usages,
 * champs d'origine, méthodes sœurs, conflit homonyme. Le refus se
 * signale par un plan nul, jamais par un plan faux.
 */
class SafeMoveTest {

    private final SyntaxService java = SyntaxService.of(JavaSyntax.INSTANCE);

    /** B-23 — la méthode autonome déménage, l'annulation restitue. */
    @Test
    void laMethodeDemenage() {
        String source = """
                class Achat {
                    double tva(double montant) {
                        return montant * 0.2;
                    }

                    double facture() {
                        return 100.0;
                    }
                }

                class Etiquette {
                    String titre() {
                        return "a";
                    }
                }
                """;
        TextSession session = TextSession.of(source, System::nanoTime);
        session.apply(Transaction.of(plan(source, "double tva", "Etiquette").edits()));
        assertEquals("""
                class Achat {
                    double facture() {
                        return 100.0;
                    }
                }

                class Etiquette {
                    String titre() {
                        return "a";
                    }
                    double tva(double montant) {
                        return montant * 0.2;
                    }
                }
                """, session.text());
        session.undo();
        assertEquals(source, session.text());
    }

    /** B-23 — le type imbriqué impose son indentation : le corps suit le niveau. */
    @Test
    void leTypeImbriqueIndente() {
        String source = """
                class Achat {
                    class Etiquette {
                        String titre() {
                            return "a";
                        }
                    }

                    double tva(double montant) {
                        return montant * 0.2;
                    }
                }
                """;
        TextSession session = TextSession.of(source, System::nanoTime);
        session.apply(Transaction.of(plan(source, "double tva", "Etiquette").edits()));
        assertEquals("""
                class Achat {
                    class Etiquette {
                        String titre() {
                            return "a";
                        }
                        double tva(double montant) {
                            return montant * 0.2;
                        }
                    }
                }
                """, session.text());
    }

    /** B-23 — les annotations contiguës déménagent avec la méthode. */
    @Test
    void lesAnnotationsDemenagent() {
        String source = """
                class Achat {
                    @Override
                    public String toString() {
                        return "achat";
                    }
                }

                class Etiquette {
                    String titre() {
                        return "a";
                    }
                }
                """;
        TextSession session = TextSession.of(source, System::nanoTime);
        session.apply(Transaction.of(plan(source, "public String toString", "Etiquette").edits()));
        assertEquals("""
                class Achat {
                }

                class Etiquette {
                    String titre() {
                        return "a";
                    }
                    @Override
                    public String toString() {
                        return "achat";
                    }
                }
                """, session.text());
    }

    /** B-23 — la méthode utilisée refuse : l'usage perdrait sa cible. */
    @Test
    void laMethodeUtiliseeRefuse() {
        String source = """
                class Achat {
                    double tva(double montant) {
                        return montant * 0.2;
                    }

                    double facture() {
                        return tva(100.0);
                    }
                }

                class Etiquette {
                }
                """;
        refuse(source, "double tva", "Etiquette");
    }

    /** B-23 — la lecture d'un champ d'origine refuse : le corps ne peut pas suivre. */
    @Test
    void leChampDOrigineRefuse() {
        String source = """
                class Achat {
                    double taux = 0.2;

                    double tva(double montant) {
                        return montant * taux;
                    }
                }

                class Etiquette {
                }
                """;
        refuse(source, "double tva", "Etiquette");
    }

    /** B-23 — l'appel d'une méthode sœur refuse. */
    @Test
    void lAppelSoeurRefuse() {
        String source = """
                class Achat {
                    double tva(double montant) {
                        return montant * 0.2;
                    }

                    double facture() {
                        return 100.0;
                    }

                    double total(double montant) {
                        return tva(montant) + facture();
                    }
                }

                class Etiquette {
                }
                """;
        refuse(source, "double total", "Etiquette");
    }

    /** B-23 — la méthode homonyme de la cible refuse : le conflit précède. */
    @Test
    void leConflitHomonymeRefuse() {
        String source = """
                class Achat {
                    String titre() {
                        return "achat";
                    }
                }

                class Etiquette {
                    String titre() {
                        return "etiquette";
                    }
                }
                """;
        refuse(source, "String titre", "Etiquette");
    }

    /** B-23 — le constructeur, la surcharge, la cible inconnue ou identique refusent. */
    @Test
    void lesRefusStructures() {
        refuse("""
                class Achat {
                    Achat() {
                    }

                    int lecture() {
                        return 3;
                    }
                }

                class Etiquette {
                }
                """, "Achat()", "Etiquette");
        refuse("""
                class Achat {
                    double tva(double montant) {
                        return montant * 0.2;
                    }

                    double tva(double montant, double seuil) {
                        return montant * seuil;
                    }
                }

                class Etiquette {
                }
                """, "double tva(double montant) {", "Etiquette");
        refuse("""
                class Achat {
                    double tva(double montant) {
                        return montant * 0.2;
                    }
                }

                class Etiquette {
                }
                """, "double tva", "Inconnue");
        refuse("""
                class Achat {
                    double tva(double montant) {
                        return montant * 0.2;
                    }
                }
                """, "double tva", "Achat");
    }

    /** B-23 — le type cible d'une ligne, ou sans accolade fermante lisible, refuse. */
    @Test
    void laCibleIllisibleRefuse() {
        refuse("""
                class Achat {
                    double tva(double montant) {
                        return montant * 0.2;
                    }
                }

                class Etiquette { }
                """, "double tva", "Etiquette");
    }

    /** B-23 — hors méthode, le plan est nul ; la sélection invalide refuse d'exception. */
    @Test
    void horsMethodeEtSelectionInvalide() {
        String source = """
                class Achat {
                    int quantite;

                    int taille() {
                        return quantite;
                    }
                }

                class Etiquette {
                }
                """;
        Document doc = Document.of(source);
        Analysis analyse = java.analyseFull(doc);
        int champ = source.indexOf("quantite;");
        assertNull(SafeMove.move(doc, analyse, champ, champ + 4, "Etiquette"),
                "le champ ne déménage pas");
        assertThrows(IllegalArgumentException.class,
                () -> SafeMove.move(doc, analyse, 6, 2, "Etiquette"));
        assertThrows(IllegalArgumentException.class,
                () -> SafeMove.move(doc, analyse, 0, 4, " "));
    }

    /** Le plan de déménagement d'une désignation permise, jamais nul. */
    private SafeMove.MovePlan plan(String source, String designation, String targetName) {
        SafeMove.MovePlan plan = move(source, designation, targetName);
        assertNotNull(plan, "le déménagement refuse une désignation permise : " + designation);
        return plan;
    }

    /** Le refus attendu : le plan est nul. */
    private void refuse(String source, String designation, String targetName) {
        assertNull(move(source, designation, targetName),
                "le déménagement accepte une désignation refusable : " + designation);
    }

    /** Le plan du déplacement de la désignation vers la cible. */
    private SafeMove.@org.jspecify.annotations.Nullable MovePlan move(String source,
            String designation, String targetName) {
        Document doc = Document.of(source);
        Analysis analyse = java.analyseFull(doc);
        int debut = source.indexOf(designation);
        assertTrue(debut >= 0, "désignation absente du corpus : " + designation);
        return SafeMove.move(doc, analyse, debut, debut + designation.length(), targetName);
    }
}
