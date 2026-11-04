package jo.celeris.core.text;

/**
 * Résolution bidirectionnelle selon UAX #9 (sous-ensemble documenté).
 *
 * <p>Le cœur doit connaître la directionnalité pour poser les curseurs,
 * déplacer les sélections et découper les lignes en runs d'affichage.
 * Cette implémentation suit l'algorithme standard : niveau de
 * paragraphe par premier caractère fort hors isolats (P2-P3),
 * enchâssements et isolats explicites (X1-X8), types faibles (W1-W7),
 * types neutres (N1-N2), niveaux implicites (I1-I2), blancs de fin de
 * ligne ramenés au niveau de paragraphe (L1) et réordonnancement
 * visuel (L2).</p>
 *
 * <p>Limites documentées : la règle des paires de crochets (N0) n'est
 * pas implémentée ; les caractères de contrôle explicites restent
 * présents comme bornes neutres invisibles au lieu d'être retirés
 * (X9) ; les séparateurs de section et le repli des niveaux
 * supérieurs à la profondeur maximale suivent l'approche classique.
 * L'entrée est une ligne : les séparateurs de paragraphe doivent être
 * retirés par l'appelant. La classification vit dans
 * {@link BidiClassifier}, les enchâssements dans {@link BidiEmbeddings}
 * et les règles de run dans {@link BidiRuns}, visibles du paquet.
 * Fil d'exécution : utilitaire pur.</p>
 */
public final class Bidi {

    private Bidi() {
    }

    /**
     * Résultat de la résolution bidirectionnelle d'une ligne : niveaux
     * par unité de code et ordre visuel. Les tableaux exposés ne doivent
     * jamais être mutés par les lecteurs.
     */
    public static final class BidiInfo {

        private final int paragraphLevel;
        private final int[] levels;
        private final int[] visualToLogical;
        private final int[] logicalToVisual;

        private BidiInfo(int paragraphLevel, int[] levels, int[] visualToLogical,
                int[] logicalToVisual) {
            this.paragraphLevel = paragraphLevel;
            this.levels = levels;
            this.visualToLogical = visualToLogical;
            this.logicalToVisual = logicalToVisual;
        }

        /** Le niveau de paragraphe : 0 (gauche à droite) ou 1 (droite à gauche). */
        public int paragraphLevel() {
            return paragraphLevel;
        }

        /** Les niveaux par unité de code UTF-16 ; pair = gauche à droite. */
        public int[] levels() {
            return levels;
        }

        /** Les indices logiques dans l'ordre visuel. */
        public int[] visualToLogical() {
            return visualToLogical;
        }

        /** Le rang visuel de chaque unité logique. */
        public int[] logicalToVisual() {
            return logicalToVisual;
        }

        /**
         * Renvoie le niveau directionnel d'une unité de code.
         *
         * @param unitIndex index d'unité UTF-16
         * @return niveau, pair pour gauche à droite
         */
        public int levelAt(int unitIndex) {
            return levels[unitIndex];
        }

        /**
         * Indique si une unité se lit de droite à gauche.
         *
         * @param unitIndex index d'unité UTF-16
         * @return {@code true} si le niveau est impair
         */
        public boolean isRtl(int unitIndex) {
            return (levels[unitIndex] & 1) == 1;
        }
    }

    /**
     * Analyse une ligne et renvoie niveaux et ordre visuel.
     *
     * @param line texte de la ligne, sans séparateur de paragraphe
     * @return informations bidirectionnelles
     */
    public static BidiInfo analyze(String line) {
        int length = line.length();
        BidiCategory[] classes = new BidiCategory[length];
        int[] levels = new int[length];
        int paragraphLevel = BidiEmbeddings.paragraphLevel(line);
        BidiEmbeddings.apply(line, classes, levels, paragraphLevel);
        BidiCategory[] originalClasses = classes.clone();
        BidiRuns.resolve(classes, levels);
        applyImplicitLevels(classes, levels);
        resetEdgeWhitespace(originalClasses, levels, paragraphLevel);
        int[] visualToLogical = reorder(levels);
        return new BidiInfo(paragraphLevel, levels, visualToLogical,
                inverse(visualToLogical));
    }

    /** I1 et I2 : les niveaux implicites suivent les types résolus. */
    private static void applyImplicitLevels(BidiCategory[] classes, int[] levels) {
        for (int i = 0; i < levels.length; i++) {
            int level = levels[i];
            if (classes[i] == BidiCategory.BN) {
                continue;
            }
            if (level % 2 == 0) {
                if (classes[i] == BidiCategory.R) {
                    levels[i] = level + 1;
                } else if (classes[i] == BidiCategory.AN || classes[i] == BidiCategory.EN) {
                    levels[i] = level + 2;
                }
            } else {
                if (classes[i] == BidiCategory.L || classes[i] == BidiCategory.AN
                        || classes[i] == BidiCategory.EN) {
                    levels[i] = level + 1;
                }
            }
        }
    }

    /** L1 : les blancs de bord de ligne suivent le niveau de paragraphe. */
    private static void resetEdgeWhitespace(BidiCategory[] originalClasses, int[] levels,
            int paragraphLevel) {
        int start = 0;
        while (start < levels.length
                && (originalClasses[start] == BidiCategory.WS
                        || originalClasses[start] == BidiCategory.BN)) {
            levels[start++] = paragraphLevel;
        }
        int end = levels.length - 1;
        while (end >= 0 && (originalClasses[end] == BidiCategory.WS
                || originalClasses[end] == BidiCategory.BN)) {
            levels[end--] = paragraphLevel;
        }
    }

    /** L2 : renversement des plages contiguës, du niveau le plus haut au plus bas impair. */
    private static int[] reorder(int[] levels) {
        int length = levels.length;
        int[] visualToLogical = new int[length];
        for (int i = 0; i < length; i++) {
            visualToLogical[i] = i;
        }
        int maxLevel = 0;
        int lowestOdd = -1;
        for (int level : levels) {
            maxLevel = Math.max(maxLevel, level);
            if (level % 2 == 1) {
                lowestOdd = lowestOdd == -1 ? level : Math.min(lowestOdd, level);
            }
        }
        if (lowestOdd <= 0) {
            // Aucun niveau impair : l'ordre logique est déjà l'ordre
            // visuel, L2 ne renverse rien.
            return visualToLogical;
        }
        for (int level = maxLevel; level >= lowestOdd; level--) {
            reverseLevelRanges(levels, visualToLogical, level);
        }
        return visualToLogical;
    }

    /** Le renversement d'une plage contiguë du niveau donné. */
    private static void reverseLevelRanges(int[] levels, int[] visualToLogical, int level) {
        int length = levels.length;
        int i = 0;
        while (i < length) {
            if (levels[visualToLogical[i]] < level) {
                i++;
                continue;
            }
            int j = i;
            while (j < length && levels[visualToLogical[j]] >= level) {
                j++;
            }
            for (int a = i, b = j - 1; a < b; a++, b--) {
                int swap = visualToLogical[a];
                visualToLogical[a] = visualToLogical[b];
                visualToLogical[b] = swap;
            }
            i = j;
        }
    }

    private static int[] inverse(int[] permutation) {
        int[] result = new int[permutation.length];
        for (int i = 0; i < permutation.length; i++) {
            result[permutation[i]] = i;
        }
        return result;
    }
}
