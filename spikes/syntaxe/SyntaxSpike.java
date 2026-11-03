// SPDX-License-Identifier: Apache-2.0
package spike.syntax;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Spike d'analyse lexicale incrémentale (M0, alimente l'ADR-004).
 *
 * <p>Question mesurée : après l'édition d'un caractère dans un fichier de
 * 50 000 lignes, quel est le coût d'une re-lexicographie limitée aux lignes
 * dont l'état de sortie a changé, comparée à une re-lexicographie complète ?
 * Le budget visé pour la ré-analyse est de 10 ms au plus hors fil UI.</p>
 *
 * <p>Le lexeur est volontairement simple (mots-clés, identifiants, chaînes,
 * commentaires ligne et bloc, accolades) : il sert de mesure de plancher,
 * pas de conception. Il porte un état par ligne (commentaire de bloc ouvert
 * ou non), ce qui délimite la plage à re-lexicographier après une édition.</p>
 */
public final class SyntaxSpike {

    private static final int LINES = 50_000;
    private static final int EDITS = 1_000;

    /** État de lexicographie transporté d'une ligne à la suivante. */
    record LexState(boolean inBlockComment) {
    }

    /** Résultat de la lexicographie d'une ligne. */
    record LexLine(LexState endState, int tokenCount, int braceDelta) {
    }

    private SyntaxSpike() {
    }

    public static void main(String[] args) {
        List<String> lines = generate(LINES);
        String[] text = lines.toArray(new String[0]);

        // Re-lexicographie complète (à froid), répétée trois fois : on retient le minimum.
        double fullMs = Double.MAX_VALUE;
        for (int round = 0; round < 3; round++) {
            long t0 = System.nanoTime();
            LexState state = new LexState(false);
            for (String line : text) {
                state = lexLine(line, state).endState();
            }
            long dt = System.nanoTime() - t0;
            fullMs = Math.min(fullMs, dt / 1e6);
        }
        System.out.printf("lexicographie complete (50 000 lignes) : %.2f ms (min de 3 tours)%n", fullMs);

        // État de fin de ligne mémorisé pour chaque ligne : c'est la clé de l'incrémental.
        LexState[] endStates = new LexState[LINES];
        LexState st = new LexState(false);
        for (int i = 0; i < LINES; i++) {
            st = lexLine(text[i], st).endState();
            endStates[i] = st;
        }

        // Éditions aléatoires d'un caractère, re-lexicographie depuis la ligne touchée
        // jusqu'à retrouver l'état de sortie déjà mémorisé.
        Random rng = new Random(42);
        long[] times = new long[EDITS];
        long rescannedTotal = 0;
        for (int e = 0; e < EDITS; e++) {
            // Les lignes vides ou trop courtes ne sont pas mutables : on en choisit une autre.
            int lineIdx = rng.nextInt(LINES);
            while (text[lineIdx].length() < 2) {
                lineIdx = rng.nextInt(LINES);
            }
            int col = rng.nextInt(text[lineIdx].length() - 1);
            StringBuilder mut = new StringBuilder(text[lineIdx]);
            if (e % 2 == 0) {
                mut.insert(col, 'x');
            } else {
                mut.deleteCharAt(col);
            }
            String edited = mut.toString();
            long t0 = System.nanoTime();
            LexState state = (lineIdx == 0) ? new LexState(false) : endStates[lineIdx - 1];
            int i = lineIdx;
            int rescanned = 0;
            while (i < LINES) {
                rescanned++;
                LexLine result = lexLine(edited, state);
                state = result.endState();
                if (state.equals(endStates[i]) || i - lineIdx > 200) {
                    break;
                }
                edited = text[i + 1];
                i++;
            }
            long dt = System.nanoTime() - t0;
            times[e] = dt;
            rescannedTotal += rescanned;
        }
        java.util.Arrays.sort(times);
        System.out.printf("re-lexicographie incrementale (1 caractere) : p50 %.1f us ; "
                + "p95 %.1f us ; max %.1f us ; %.1f lignes relues en moyenne%n",
            times[EDITS / 2] / 1e3, times[(int) (EDITS * 0.95)] / 1e3,
            times[EDITS - 1] / 1e3, rescannedTotal / (double) EDITS);

        // Index de lignes : coût d'une insertion de ligne dans un tableau de débuts
        // maintenu par intervalle (simule le pire cas d'un index naïf pour comparaison).
        int[] starts = new int[LINES];
        for (int i = 1; i < LINES; i++) {
            starts[i] = starts[i - 1] + text[i - 1].length() + 1;
        }
        long t0 = System.nanoTime();
        for (int e = 0; e < 100; e++) {
            int at = rng.nextInt(LINES);
            int[] shifted = new int[LINES + 1];
            System.arraycopy(starts, 0, shifted, 0, at);
            System.arraycopy(starts, at, shifted, at + 1, LINES - at);
            starts = shifted;
        }
        System.out.printf("insertion de ligne (index naif par recopie) : %.1f us%n",
            (System.nanoTime() - t0) / 100.0 / 1e3);
    }

    /** Lexicographie une ligne depuis {@code state} ; compte les jetons et les accolades. */
    static LexLine lexLine(String line, LexState state) {
        boolean block = state.inBlockComment();
        int tokens = 0;
        int braces = 0;
        boolean word = false;
        int i = 0;
        int n = line.length();
        while (i < n) {
            char c = line.charAt(i);
            if (block) {
                if (c == '*' && i + 1 < n && line.charAt(i + 1) == '/') {
                    block = false;
                    i += 2;
                    tokens++;
                } else {
                    i++;
                }
                continue;
            }
            if (c == '/' && i + 1 < n && line.charAt(i + 1) == '/') {
                break;
            }
            if (c == '/' && i + 1 < n && line.charAt(i + 1) == '*') {
                block = true;
                i += 2;
                continue;
            }
            if (c == '"') {
                i++;
                while (i < n && line.charAt(i) != '"') {
                    if (line.charAt(i) == '\\') {
                        i++;
                    }
                    i++;
                }
                i++;
                tokens++;
                continue;
            }
            if (c == '{') {
                braces++;
                tokens++;
                i++;
                continue;
            }
            if (c == '}') {
                braces--;
                tokens++;
                i++;
                continue;
            }
            boolean isWordChar = Character.isJavaIdentifierPart(c);
            if (isWordChar && !word) {
                tokens++;
                word = true;
            } else if (!isWordChar) {
                word = false;
            }
            i++;
        }
        return new LexLine(new LexState(block), tokens, braces);
    }

    /** Génère un fichier Java synthétique réaliste (classes, méthodes, chaînes, commentaires). */
    private static List<String> generate(int lines) {
        List<String> out = new ArrayList<>(lines);
        out.add("package exemple.synthetique;");
        out.add("");
        out.add("import java.util.List;");
        out.add("import java.util.Map;");
        out.add("/* bloc de commentaire multi-lignes ouvert ici");
        out.add("   et referme plus bas ; le lexeur doit transporter l'etat */");
        out.add("*/");
        out.add("");
        out.add("public final class Exemple" + 0 + " {");
        int i = out.size();
        int cls = 1;
        while (out.size() < lines - 2) {
            out.add("");
            out.add("    /** Javadoc de la methode membre numero " + i + ". */");
            out.add("    public int calculer" + i + "(int valeur, String libelle) {");
            out.add("        int total = valeur * " + (i % 97) + " + " + (i % 31) + ";");
            out.add("        String cle = libelle + \"-\" + total;");
            out.add("        if (total > " + (i % 1000) + ") {");
            out.add("            total -= valeur / " + (1 + i % 7) + ";");
            out.add("        } else if (cle.isEmpty()) {");
            out.add("            // commentaire de fin de ligne sur la branche vide");
            out.add("            return 0;");
            out.add("        }");
            out.add("        return total;");
            out.add("    }");
            out.add("");
            if (i % 20 == 0) {
                out.add("    private static final class Interne" + cls + " {");
                out.add("        final int constante = " + cls + ";");
                out.add("    }");
                cls++;
            }
            i++;
        }
        out.add("}");
        return out;
    }
}
