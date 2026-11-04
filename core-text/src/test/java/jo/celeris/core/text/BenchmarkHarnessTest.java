package jo.celeris.core.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
