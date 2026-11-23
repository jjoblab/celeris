package jo.celeris.core.syntax;

import jo.celeris.core.syntax.clike.JavaSyntax;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextSession;

/**
 * Charge et limites à 480 000 lignes (G-01 étendu, chantier C10) : la
 * voie de la vie réelle du moteur — l'ouverture paie la fenêtre
 * d'abord (lexicographie complète plus les seuls chunks visibles, hors
 * fil au moteur, ADR-007), la complétion suit par lots, l'édition au
 * milieu reprend par l'incrément — la lexicographie bornée par la
 * reconvergence des états (PIEGE-19) — et le défilement ne coûte
 * rien : la fenêtre se note, le travail en vol ne s'annule pas.
 *
 * <p>Calibrage (2026-11-10, bac à sable de développement, tas dédié
 * de 1 Go) : 480 029 lignes, 12,1 M caractères — fenêtre d'abord
 * 1214–1441 ms (budget 2500 ms, marge 1,7–2,1×), incrément après
 * édition au milieu 2–36 ms (budget 100 ms, marge 2,8–50×), tas de
 * l'ouverture +90 Mo (budget 192 Mo, marge 2,1×). Le protocole
 * retient le minimum de trois tours (PIEGE-13) et l'échauffement ne
 * compte pas.</p>
 */
class Charge480kLignesTest {

    /** Les lignes du document du banc — la cible du chantier C10. */
    private static final int EMPTY_LINES = 480_000;

    /** La demi-fenêtre visible — soixante-quatre lignes. */
    private static final int FENETRE = 64;

    /** Le budget d'ouverture : la fenêtre d'abord, en nanosecondes. */
    private static final long BUDGET_OUVERTURE_NS = 2_500_000_000L;

    /** Le budget d'incrément après une édition au milieu, en nanosecondes. */
    private static final long BUDGET_INCREMENT_NS = 100_000_000L;

    /** Le budget de tas de l'ouverture — états et caches, en octets. */
    private static final long BUDGET_TAS_OCTETS = 192L * 1024 * 1024;

    /** Les tours de mesure (PIEGE-13). */
    private static final int TOURS = 3;

    /**
     * L'ouverture à 480 000 lignes : la fenêtre d'abord sous le
     * budget, partielle hors fenêtre, le tas de l'ouverture borné —
     * la complétion par lots suivra, l'éditeur n'attend jamais
     * l'arbre complet.
     */
    @Test
    void ouvertureFenetreDabordBornee() {
        String source = CorpusChargeur.replique(CorpusChargeur.exempleJava(),
                EMPTY_LINES);
        TextSession session = TextSession.of(source, System::nanoTime);
        Document doc = session.document();
        assertTrue(doc.lineCount() >= EMPTY_LINES, "le banc porte ses lignes");

        SyntaxService service = SyntaxService.of(JavaSyntax.INSTANCE);
        int milieu = doc.lineCount() / 2;
        // Échauffement non mesuré : le JIT ne doit pas compter.
        service.analyzeWindow(doc, milieu - FENETRE, milieu + FENETRE,
                new TokenCancellation());

        System.gc();
        try {
            Thread.sleep(100);
        } catch (InterruptedException attente) {
            Thread.currentThread().interrupt();
        }
        System.gc();
        Runtime tas = Runtime.getRuntime();
        long avant = tas.totalMemory() - tas.freeMemory();

        Analysis window = null;
        long meilleur = Long.MAX_VALUE;
        for (int tour = 0; tour < TOURS; tour++) {
            long start = System.nanoTime();
            window = service.analyzeWindow(doc, milieu - FENETRE,
                    milieu + FENETRE, new TokenCancellation());
            meilleur = Math.min(meilleur, System.nanoTime() - start);
        }

        System.gc();
        long apres = tas.totalMemory() - tas.freeMemory();

        assertTrue(window != null,
                "la fenêtre seule publie");
        assertTrue(window.lex().porteLignesVides(),
                "la fenêtre seule laisse le reste du LEXIQUE en attente"
                        + " (la couleur visible vit déjà)");
        assertTrue(meilleur <= BUDGET_OUVERTURE_NS,
                "fenêtre d'abord sur " + doc.lineCount() + " lignes en "
                        + meilleur / 1_000_000.0 + " ms (budget 2500 ms)");
        assertTrue(apres - avant <= BUDGET_TAS_OCTETS,
                "tas de l'ouverture +" + (apres - avant) / 1_000_000
                        + " Mo (budget 192 Mo)");
    }

    /**
     * L'édition au milieu d'un document de 480 000 lignes reprend par
     * l'incrément : la lexicographie bornée par la reconvergence, les
     * seuls chunks propriétaires des lignes touchées rejoués — la
     * frappe reste bornée, où que le curseur vive.
     */
    @Test
    void editionAuMilieuReprendParIncrementBornee() {
        String source = CorpusChargeur.replique(CorpusChargeur.exempleJava(),
                EMPTY_LINES);
        TextSession session = TextSession.of(source, System::nanoTime);
        Document doc = session.document();
        SyntaxService service = SyntaxService.of(JavaSyntax.INSTANCE);
        int milieu = doc.lineCount() / 2;
        // La base de l'incrément : l'arbre complet, calculé hors mesure.
        Analysis base = service.analyseFull(doc);
        Document docBase = doc;
        assertTrue(base.isComplete(), "la base de l'incrément est complète");

        long meilleur = Long.MAX_VALUE;
        for (int tour = 0; tour < TOURS; tour++) {
            int offset = session.document().lineStart(milieu) + 10;
            session.replaceRange(offset, offset, "xyz");
            Document apresEdit = session.document();
            long start = System.nanoTime();
            Analysis incrementee = service.analyseIncremental(apresEdit, base,
                    EditSite.of(docBase, apresEdit, offset, offset, "xyz"));
            meilleur = Math.min(meilleur, System.nanoTime() - start);
            assertEquals(apresEdit.revision(), incrementee.revision(),
                    "l'incrément publie l'instantané édité");
            // le retrait rend le document au banc, la base suit
            session.replaceRange(offset, offset + 3, "");
            Document apresRetrait = session.document();
            base = service.analyseIncremental(apresRetrait, base,
                    EditSite.of(apresEdit, apresRetrait, offset, offset + 3, ""));
            docBase = apresRetrait;
        }

        assertTrue(meilleur <= BUDGET_INCREMENT_NS,
                "incrément au milieu de " + doc.lineCount() + " lignes en "
                        + meilleur / 1_000_000.0 + " ms (budget 100 ms)");
    }
}
