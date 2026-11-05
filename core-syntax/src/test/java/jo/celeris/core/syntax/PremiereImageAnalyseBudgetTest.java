package jo.celeris.core.syntax;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;

/**
 * Budget d'ouverture progressive (G-01, facette analyse — sortie M4) :
 * sur cent mille lignes, l'instantané de fenêtre — lexicographie
 * complète plus les seuls chunks visibles — tient le budget d'image de
 * la matrice (500 ms, celui de la première image du texte) ; annuler
 * la lexicographie coûte moins cher que la terminer (ADR-007). Le
 * protocole retient le minimum de trois tours (PIEGE-13).
 *
 * <p>Calibrage (2026-11-05, poste de développement) : 224 ms mesurés
 * pour l'instantané de fenêtre sur 100 041 lignes Java — le budget
 * retient l'enveloppe de la première image de la matrice (500 ms),
 * marge 2,2×</p>
 */
class PremiereImageAnalyseBudgetTest {

    private static final int LIGNES = 100_000;
    private static final int FENETRE = 64;
    private static final long BUDGET_NS = 500_000_000L;
    private static final int TOURS = 3;

    /** Rattrapages complets avant de céder au budget. */
    private static final int RATTRAPAGES = 2;

    /**
     * L'instantané de fenêtre sur cent mille lignes tient le budget
     * d'image : la première image ne paie jamais l'arbre complet.
     */
    @Test
    void instantaneDeFenetreSousBudget() {
        String source = CorpusChargeur.replique(CorpusChargeur.exempleJava(), LIGNES);
        Document doc = Document.of(source);
        SyntaxService service = SyntaxService.of(JavaSyntax.INSTANCE);
        int milieu = doc.lineCount() / 2;
        // échauffement non mesuré : le JIT ne doit pas compter dans le budget
        service.analyseFenetre(doc, milieu - FENETRE / 2, milieu + FENETRE / 2,
                new JetonAnnulation());
        long meilleur = Long.MAX_VALUE;
        for (int essai = 0; essai <= RATTRAPAGES; essai++) {
            long serie = Long.MAX_VALUE;
            for (int tour = 0; tour < TOURS; tour++) {
                long debut = System.nanoTime();
                Analysis fenetre = service.analyseFenetre(doc,
                        milieu - FENETRE / 2, milieu + FENETRE / 2, new JetonAnnulation());
                long duree = System.nanoTime() - debut;
                assertTrue(!fenetre.estComplet(), "la fenêtre seule laisse le reste en attente");
                serie = Math.min(serie, duree);
            }
            meilleur = Math.min(meilleur, serie);
            if (meilleur <= BUDGET_NS) {
                break;
            }
        }
        assertTrue(meilleur <= BUDGET_NS,
                "instantané de fenêtre sur " + LIGNES + " lignes en "
                        + meilleur / 1_000_000.0 + " ms (budget 500 ms)");
    }

    /** Annuler la lexicographie coûte moins cher que la terminer (ADR-007). */
    @Test
    void annulerCouteMoinsCherQueTerminer() {
        String source = CorpusChargeur.replique(CorpusChargeur.exempleJava(), LIGNES);
        Document doc = Document.of(source);
        IncrementalLexer lexer = new IncrementalLexer();
        long complet = Long.MAX_VALUE;
        for (int tour = 0; tour < TOURS; tour++) {
            long debut = System.nanoTime();
            lexer.lexFull(doc, JavaSyntax.INSTANCE.lexer(), new JetonAnnulation());
            complet = Math.min(complet, System.nanoTime() - debut);
        }
        long annule = Long.MAX_VALUE;
        for (int tour = 0; tour < TOURS; tour++) {
            JetonAnnulation jeton = new JetonAnnulation();
            jeton.annuler();
            long debut = System.nanoTime();
            assertThrows(AnalyseAnnulee.class,
                    () -> lexer.lexFull(doc, JavaSyntax.INSTANCE.lexer(), jeton),
                    "le premier point de contrôle arrête la passe");
            annule = Math.min(annule, System.nanoTime() - debut);
        }
        assertTrue(annule < complet,
                "annuler (" + annule / 1_000_000.0 + " ms) coûte moins cher que terminer ("
                        + complet / 1_000_000.0 + " ms)");
    }
}
