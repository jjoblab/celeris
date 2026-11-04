package jo.celeris.core.text;

import java.util.Arrays;
import java.util.function.LongSupplier;

/**
 * Harnais de mesure des benchmarks de core-text (outillage de test, ADR-007).
 *
 * <p>PIEGE-13 : un benchmark mesuré en un seul tour échoue au CI par simple
 * bruit du runner partagé (JIT, GC, contention) — le dépôt de départ l'a
 * mesuré sur un commit sans aucun changement de code. La mesure se fait en
 * {@value #ROUNDS} tours et retient le MINIMUM par phase : le bruit ne fait
 * qu'ajouter du temps, le minimum l'écarte.</p>
 *
 * <p>Les phases d'un même tour se suivent dans l'ordre donné — chacune
 * chronométrée séparément, le réarmement d'une phase par la suivante
 * (défaire puis refaire) est ainsi naturellement hors mesure. L'horloge est
 * injectée (ADR-007) : les tests du protocole obtiennent un temps
 * déterministe, aucune réflexion, aucun sommeil.</p>
 */
public final class BenchmarkHarness {

    /** Nombre de tours par phase (PIEGE-13 : minimum de trois tours). */
    public static final int ROUNDS = 3;

    private BenchmarkHarness() {
    }

    /**
     * Mesure des phases en {@value #ROUNDS} tours : chaque tour exécute les
     * phases dans l'ordre, chacune chronométrée séparément, et la valeur
     * minimale observée est retenue par phase.
     *
     * @param clock  l'horloge injectée (temps en unité croissante)
     * @param phases les phases à mesurer, exécutées dans l'ordre à chaque
     *               tour
     * @return la durée minimale de chaque phase, dans l'ordre (en unités de
     *         l'horloge injectée)
     */
    public static long[] minOfTours(LongSupplier clock, Runnable... phases) {
        long[] meilleurs = new long[phases.length];
        Arrays.fill(meilleurs, Long.MAX_VALUE);
        for (int tour = 0; tour < ROUNDS; tour++) {
            for (int phase = 0; phase < phases.length; phase++) {
                long debut = clock.getAsLong();
                phases[phase].run();
                meilleurs[phase] = Math.min(meilleurs[phase], clock.getAsLong() - debut);
            }
        }
        return meilleurs;
    }
}
