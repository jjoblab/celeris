// SPDX-License-Identifier: Apache-2.0
package spike.rope;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

/**
 * Spike de re-mesure de la rope du dépôt de départ (M0).
 *
 * <p>Objectif : vérifier sur JVM pure le coût d'une édition d'un caractère
 * sur un document de 50 000 lignes, condition nécessaire au portage du
 * moteur (voir ADR-003). Le protocole reprend la règle héritée du dépôt de
 * départ : minimum de trois tours par phase, le bruit du runner ne devant
 * jamais faire échouer une mesure (PIEGE-13).</p>
 *
 * <p>La classe {@code Rope} n'est pas copiée dans ce dépôt : elle est compilée
 * directement depuis le clone en lecture seule du dépôt de départ, passé en
 * argument. Aucun code n'est porté avant la validation du propriétaire.</p>
 */
public final class RopeSpike {

    private static final int LINES = 50_000;
    private static final int LINE_WIDTH = 40;
    private static final int EDITS_PER_ROUND = 1_000;
    private static final int ROUNDS = 3;

    private RopeSpike() {
    }

    /** Point d'entrée : {@code java spike.rope.RopeSpike <chemin Rope.java>}. */
    public static void main(String[] args) throws Exception {
        Path ropeSource = Path.of(args[0]);
        run(ropeSource);
    }

    private static void run(Path ropeSource) throws Exception {
        // Compilation de la rope du dépôt de départ dans un répertoire temporaire.
        Path outDir = Path.of(System.getProperty("java.io.tmpdir"), "spike-rope-classes");
        Files.createDirectories(outDir);
        var compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
        int code = compiler.run(null, null, null, "-d", outDir.toString(), ropeSource.toString());
        if (code != 0) {
            throw new IllegalStateException("rope compilation failed");
        }
        var loader = new java.net.URLClassLoader(
            new java.net.URL[] { outDir.toUri().toURL() }, RopeSpike.class.getClassLoader());
        Class<?> ropeClass = Class.forName("jo.codeeditor.rope.Rope", true, loader);

        var fromString = ropeClass.getMethod("fromString", String.class);
        var replace = ropeClass.getMethod("replace", int.class, int.class, String.class);
        var length = ropeClass.getMethod("length");

        String document = buildDocument(LINES, LINE_WIDTH);
        Object rope = fromString.invoke(null, document);
        long ropeLength = (Integer) length.invoke(rope);
        System.out.printf("document : %d lignes x %d caracteres = %d caracteres%n",
            LINES, LINE_WIDTH, ropeLength);

        Random rng = new Random(42);
        double[] bestRoundAvg = { Double.MAX_VALUE };
        long[] allTimes = new long[ROUNDS * EDITS_PER_ROUND];
        int idx = 0;
        for (int round = 0; round < ROUNDS; round++) {
            Object work = fromString.invoke(null, document);
            long roundNanos = 0L;
            for (int i = 0; i < EDITS_PER_ROUND; i++) {
                int offset = rng.nextInt((Integer) length.invoke(work) - 1);
                boolean insert = (i % 2 == 0);
                long t0 = System.nanoTime();
                if (insert) {
                    work = replace.invoke(work, offset, offset, "x");
                } else {
                    work = replace.invoke(work, offset, offset + 1, "");
                }
                long dt = System.nanoTime() - t0;
                allTimes[idx++] = dt;
                roundNanos += dt;
            }
            double avgMs = roundNanos / (double) EDITS_PER_ROUND / 1e6;
            bestRoundAvg[0] = Math.min(bestRoundAvg[0], avgMs);
            System.out.printf("tour %d : moyenne %.4f ms/edition%n", round + 1, avgMs);
        }
        java.util.Arrays.sort(allTimes, 0, idx);
        System.out.printf("retenu (min des tours) : %.4f ms/edition%n", bestRoundAvg[0]);
        System.out.printf("p50 %.4f ms ; p95 %.4f ms ; max %.4f ms%n",
            allTimes[idx / 2] / 1e6, allTimes[(int) (idx * 0.95)] / 1e6, allTimes[idx - 1] / 1e6);

        // Empreinte mémoire approximative : différence de tas avant/après construction.
        Runtime rt = Runtime.getRuntime();
        for (int i = 0; i < 3; i++) {
            System.gc();
        }
        long baseline = rt.totalMemory() - rt.freeMemory();
        Object big = fromString.invoke(null, document);
        for (int i = 0; i < 3; i++) {
            System.gc();
        }
        long withRope = rt.totalMemory() - rt.freeMemory();
        if (big == null) {
            throw new IllegalStateException("unreachable");
        }
        System.out.printf("memoire approximative du modele : %d Ko pour %d Ko de texte (ratio %.2f)%n",
            Math.round((withRope - baseline) / 1024.0),
            document.getBytes(StandardCharsets.UTF_8).length / 1024,
            (withRope - baseline) / (double) document.length());
    }

    private static String buildDocument(int lines, int width) {
        StringBuilder doc = new StringBuilder(lines * (width + 1));
        for (int i = 0; i < lines; i++) {
            StringBuilder line = new StringBuilder(width);
            line.append("ligne ").append(i).append(" : ");
            while (line.length() < width - 1) {
                line.append((char) ('a' + (i % 26)));
            }
            doc.append(line).append('\n');
        }
        return doc.toString();
    }
}
