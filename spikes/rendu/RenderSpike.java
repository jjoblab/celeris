// SPDX-License-Identifier: Apache-2.0
package spike.render;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.util.Random;

/**
 * Spike de rendu par proxy JVM (M0, alimente l'ADR-006).
 *
 * <p><strong>Limite assumée :</strong> ce spike mesure le coût de façonnage
 * de lignes sur JVM de bureau, pas sur appareil Android. Il dimensionne
 * l'ordre de grandeur du cache de mise en page (créer une mise en page coûte
 * cent fois la consulter) et fixe le protocole de calibration qui sera rejoué
 * sur l'appareil de référence dès M1 avec l'instrumentation native.</p>
 *
 * <p>Mesures : coût de création d'une mise en page de ligne de code (80
 * caractères), coût de consultation d'un cache (lecture de tableau), et
 * extrapolation sur une fenêtre de 50 rangées visibles par image à 60 et
 * 120 Hz.</p>
 */
public final class RenderSpike {

    private static final int SAMPLES = 2_000;
    private static final int LINE_WIDTH = 80;
    private static final int VISIBLE_ROWS = 50;
    private static final int ROUNDS = 3;

    private RenderSpike() {
    }

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("environnement graphique : headless (attendu)");
        }
        var font = new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 14);
        FontRenderContext frc = new FontRenderContext(null, false, false);
        String[] lines = generate(SAMPLES, LINE_WIDTH);

        // Échauffement du JIT avant toute mesure.
        for (int i = 0; i < 500; i++) {
            new TextLayout(lines[i % SAMPLES], font, frc);
        }

        // Création de mise en page : min de trois tours, puis distribution.
        double bestMs = Double.MAX_VALUE;
        long[] times = new long[SAMPLES];
        for (int round = 0; round < ROUNDS; round++) {
            long t0 = System.nanoTime();
            for (int i = 0; i < SAMPLES; i++) {
                long a = System.nanoTime();
                TextLayout layout = new TextLayout(lines[i], font, frc);
                layout.getAdvance();
                times[i] = System.nanoTime() - a;
            }
            bestMs = Math.min(bestMs, (System.nanoTime() - t0) / 1e6);
        }
        java.util.Arrays.sort(times);
        System.out.printf("creation de mise en page par ligne (80 car.) : %.1f us en moyenne "
                + "(p50 %.1f us ; p95 %.1f us)%n",
            bestMs * 1e3 / SAMPLES, times[SAMPLES / 2] / 1e3, times[(int) (SAMPLES * 0.95)] / 1e3);

        // Consultation de cache : lecture de l'avancement précalculé dans un tableau.
        float[] cached = new float[SAMPLES];
        for (int i = 0; i < SAMPLES; i++) {
            cached[i] = new TextLayout(lines[i], font, frc).getAdvance();
        }
        long t0 = System.nanoTime();
        float sink = 0f;
        for (int r = 0; r < 100; r++) {
            for (int i = 0; i < VISIBLE_ROWS; i++) {
                sink += cached[(i * 37) % SAMPLES];
            }
        }
        double cachedPerFrameNs = (System.nanoTime() - t0) / 100.0;
        System.out.printf("consultation de cache pour %d rangees : %.2f us par image (sink %.0f)%n",
            VISIBLE_ROWS, cachedPerFrameNs / 1e3, sink);

        // Extrapolation : fenêtre non cachée contre fenêtre cachée.
        double uncachedPerFrameMs = bestMs * 1e3 / SAMPLES * VISIBLE_ROWS / 1e3;
        System.out.printf("extrapolation : %d rangees sans cache = %.2f ms/image ; "
                + "avec cache = %.4f ms/image (budgets image : 16 ms a 60 Hz, 8 ms a 120 Hz)%n",
            VISIBLE_ROWS, uncachedPerFrameMs, cachedPerFrameNs / 1e6);

        // Métrique de police (proxy de ce qu'expose l'appareil) : avance par caractère.
        Dimension advance = new TextLayout("0".repeat(80), font, frc).getPixelBounds(
            frc, 0, 0).getSize();
        System.out.printf("metrique : 80 caracteres occupent %.1f px de large "
                + "(%d px de hauteur de ligne estimee a 1,35 x corps)%n",
            advance.getWidth() / 1.0, Math.round(14 * 1.35));
    }

    /** Génère des lignes de code représentatives (mots, espaces, ponctuation). */
    private static String[] generate(int count, int width) {
        String[] words = {"return", "int", "value", "compute", "final", "static", "name",
            "total", "if", "else", "while", "public", "private", "String", "null"};
        Random rng = new Random(7);
        String[] out = new String[count];
        for (int i = 0; i < count; i++) {
            StringBuilder sb = new StringBuilder(width);
            int indent = rng.nextInt(4) * 4;
            sb.append(" ".repeat(indent));
            while (sb.length() < width - 8) {
                sb.append(words[rng.nextInt(words.length)]).append(' ');
                if (rng.nextInt(10) == 0) {
                    sb.append("= ").append(rng.nextInt(1000)).append("; ");
                }
            }
            out[i] = sb.toString();
        }
        return out;
    }
}
