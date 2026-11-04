package jo.celeris.core.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

/**
 * Tests du protocole de mesure des benchmarks (PIEGE-13).
 *
 * <p>Le benchmark d'annulation du dépôt de départ tombait au CI sans aucun
 * changement de code — bruit du runner partagé sur une mesure en un seul
 * tour. Le protocole retenu : trois tours par phase, minimum conservé,
 * assertion sur le minimum (jamais sur un tour unique).</p>
 */
class BenchmarkHarnessTest {

    /**
     * PIEGE-13 — le benchmark d'annulation retient le minimum de trois
     * tours par phase.
     *
     * <p>Horloge scriptée : les deux phases (défaire puis refaire) tournent
     * trois fois ; les durées de la phase A sont 100, 10 puis 500 unités —
     * le tour bruité (500) est écarté, le minimum (10) est retenu. Le bruit
     * ne fait qu'ajouter du temps : le minimum l'écarte.</p>
     */
    @Test
    void undoBenchmarkUsesMinOfThreeRuns() {
        long[] horloge = {
            0L, 100L, 100L, 350L,      // tour 1 : phase A = 100, phase B = 250
            350L, 360L, 360L, 400L,    // tour 2 : phase A = 10, phase B = 40
            400L, 900L, 900L, 910L,    // tour 3 : phase A = 500 (bruit), phase B = 10
        };
        int[] index = {0};
        int[] toursPhaseA = {0};
        int[] toursPhaseB = {0};
        long[] retenus = BenchmarkHarness.minOfTours(
                () -> horloge[index[0]++],
                () -> toursPhaseA[0]++,
                () -> toursPhaseB[0]++);
        assertEquals(3, toursPhaseA[0], "trois tours de la phase A");
        assertEquals(3, toursPhaseB[0], "trois tours de la phase B");
        assertEquals(2, retenus.length);
        assertEquals(10L, retenus[0], "le minimum des trois tours de A est retenu");
        assertEquals(10L, retenus[1], "le minimum des trois tours de B est retenu");
    }

    /**
     * Mesure réelle de l'annulation : document de 2 000 lignes, cent
     * remplacements distincts, undo × 100 puis redo × 100 par tour, chaque
     * phase chronométrée séparément, minimum de trois tours conservé. Le
     * budget généreux (2 s par phase) protège d'une régression structurelle,
     * pas de quelques pourcents de bruit résiduel.
     */
    @Test
    void undoRedoBenchmark_minOfThreeHoldsBudget() {
        StringBuilder contenu = new StringBuilder();
        for (int ligne = 0; ligne < 2_000; ligne++) {
            contenu.append("ligne ").append(ligne).append(" : contenu de la ligne\n");
        }
        TextSession session = TextSession.of(contenu.toString(), () -> 0L);
        for (int i = 0; i < 100; i++) {
            int position = (i * 977) % (session.length() - 4);
            session.replaceRange(position, position + 3, "XYZ");
        }
        assertEquals(100, session.undoDepth());
        String apresEditions = session.text();

        // Chaque tour enchaîne undo puis redo : le redo réarme la pile pour
        // le tour suivant — le réarmement est hors mesure.
        Runnable phaseUndo = () -> {
            for (int i = 0; i < 100; i++) {
                session.undo();
            }
        };
        Runnable phaseRedo = () -> {
            for (int i = 0; i < 100; i++) {
                session.redo();
            }
        };
        long[] retenus = BenchmarkHarness.minOfTours(System::nanoTime, phaseUndo, phaseRedo);
        System.out.printf("[benchmark] undo x100 (2000 lignes, min de 3 tours) : %.2f ms ;"
                        + " redo : %.2f ms%n",
                retenus[0] / 1e6, retenus[1] / 1e6);
        assertTrue(retenus[0] < 2_000_000_000L,
                "undo trop lent : " + (retenus[0] / 1e6) + " ms (min de 3 tours)");
        assertTrue(retenus[1] < 2_000_000_000L,
                "redo trop lent : " + (retenus[1] / 1e6) + " ms (min de 3 tours)");
        assertEquals(apresEditions, session.text());
        assertEquals(100, session.undoDepth());
    }

    /**
     * Mesure réelle de l'édition, benchmark de référence de M2 : document
     * de 50 000 lignes (≈ 2 M de caractères), mille remplacements d'un
     * caractère par tour — par la voie de mutation unique de la session
     * (lecture du texte retiré, splice de la rope, maintien de l'index de
     * lignes, enregistrement d'étape) — minimum de trois tours conservé
     * (PIEGE-13). Budget du mandat (ADR-006) : 1 ms par édition ; le seuil
     * asserté est exactement ce budget, c'est-à-dire une protection
     * structurelle (un facteur ~50 au-dessus de la mesure courante), pas
     * une chasse aux pourcents de bruit résiduel.
     *
     * <p>Positions déterministes à graine fixe, calculées une fois contre
     * le document initial : chaque remplacement conserve la longueur, les
     * positions restent valides pour les trois tours et chaque tour
     * exécute le même travail — les tours restent comparables.</p>
     */
    @Test
    void editBenchmark_minOfThreeHoldsBudget() {
        StringBuilder contenu = new StringBuilder();
        for (int ligne = 0; ligne < 50_000; ligne++) {
            contenu.append("ligne ").append(ligne).append(" : contenu de la ligne\n");
        }
        TextSession session = TextSession.of(contenu.toString(), () -> 0L);
        Random positions = new Random(20261_104L);
        int[] cibles = new int[1_000];
        for (int i = 0; i < cibles.length; i++) {
            cibles[i] = positions.nextInt(session.length() - 1);
        }

        Runnable phaseEditions = () -> {
            for (int cible : cibles) {
                session.replaceRange(cible, cible + 1, "X");
            }
        };
        long[] retenus = BenchmarkHarness.minOfTours(System::nanoTime, phaseEditions);
        double parEdition = retenus[0] / 1e6 / cibles.length;
        System.out.printf("[benchmark] edition x1000 (50000 lignes, min de 3 tours) :"
                        + " %.2f ms ; par edition : %.4f ms%n",
                retenus[0] / 1e6, parEdition);
        assertTrue(retenus[0] < 1_000_000_000L,
                "edition trop lente : " + parEdition + " ms par edition (min de 3 tours)");
    }

    /**
     * Mesure réelle du remplacement global : document de 50 000 lignes
     * (≈ 2 M de caractères), un marqueur toutes les cinquante lignes (1 000
     * occurrences), remplacement global PAR LA VOIE COMPLÈTE de la session
     * (passe du moteur, éditions ajustées en deltas cumulés, application,
     * une étape d'annulation) — minimum de trois tours conservé
     * (PIEGE-13). Chaque tour enchaîne remplacement puis annulation :
     * l'annulation réarme le document pour le tour suivant, chaque phase
     * est chronométrée séparément. Protection structurelle (pas une
     * chasse aux pourcents de bruit résiduel).
     */
    @Test
    void replaceAllBenchmark_minOfThreeHoldsBudget() {
        StringBuilder contenu = new StringBuilder();
        for (int ligne = 0; ligne < 50_000; ligne++) {
            contenu.append("ligne ").append(ligne);
            if (ligne % 50 == 0) {
                contenu.append(" MARQUEUR");
            }
            contenu.append('\n');
        }
        String initial = contenu.toString();
        TextSession session = TextSession.of(initial, () -> 0L);

        Runnable phaseRemplacement = () -> assertEquals(1_000,
                session.replaceAll("MARQUEUR", "CIBLE", new FindOptions()));
        Runnable phaseAnnulation = () -> assertTrue(session.undo());
        long[] retenus = BenchmarkHarness.minOfTours(System::nanoTime,
                phaseRemplacement, phaseAnnulation);
        double parOccurrence = retenus[0] / 1e6 / 1_000;
        System.out.printf("[benchmark] replace-all 1000 occurrences (50000 lignes,"
                        + " min de 3 tours) : %.2f ms (%.4f ms/occurrence) ;"
                        + " annulation : %.2f ms%n",
                retenus[0] / 1e6, parOccurrence, retenus[1] / 1e6);
        assertTrue(retenus[0] < 1_000_000_000L,
                "replace-all trop lent : " + parOccurrence
                        + " ms par occurrence (min de 3 tours)");
        assertTrue(retenus[1] < 1_000_000_000L,
                "annulation du replace-all trop lente : "
                        + retenus[1] / 1e6 + " ms (min de 3 tours)");
        assertEquals(initial, session.text(), "l'annulation finale restaure l'original");
        assertEquals(0, session.undoDepth());
    }
}
