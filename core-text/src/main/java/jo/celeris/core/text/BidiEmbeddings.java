package jo.celeris.core.text;

import org.jspecify.annotations.Nullable;

/**
 * Niveaux d'enchâssement explicites (X1-X8) et niveau de paragraphe
 * (P2-P3) de l'algorithme bidirectionnel UAX #9 — visible du paquet
 * seulement, la surface publique étant {@link Bidi}.
 *
 * <p>Les caractères de contrôle explicites restent dans la ligne comme
 * bornes neutres invisibles (simplification de X9) : leur niveau
 * participe aux runs, leur affichage est nul.</p>
 */
final class BidiEmbeddings {

    /** Profondeur maximale d'enchâssement explicite (X1). */
    private static final int MAX_DEPTH = 125;

    private BidiEmbeddings() {
    }

    /** Le niveau de paragraphe par le premier caractère fort hors isolats. */
    static int paragraphLevel(String line) {
        int depth = 0;
        for (int i = 0; i < line.length(); ) {
            int codePoint = line.codePointAt(i);
            BidiCategory cls = BidiClassifier.classOf(codePoint);
            if (depth == 0 && isStrong(cls)) {
                return cls == BidiCategory.L ? 0 : 1;
            }
            depth = nextIsolateDepth(cls, depth);
            i += Character.charCount(codePoint);
        }
        return 0;
    }

    /** Applique les enchâssements, survols et isolats explicites. */
    static void apply(String line, BidiCategory[] classes, int[] levels, int paragraphLevel) {
        Stack stack = new Stack(paragraphLevel);
        for (int i = 0; i < line.length(); ) {
            int codePoint = line.codePointAt(i);
            int count = Character.charCount(codePoint);
            BidiCategory cls = BidiClassifier.classOf(codePoint);
            int currentLevel = stack.currentLevel();
            switch (cls) {
                case LRE, RLE, LRO, RLO -> pushEmbedding(stack, classes, levels, i, count, cls,
                        currentLevel);
                case PDF -> {
                    fill(classes, levels, i, count, BidiCategory.BN, currentLevel);
                    stack.popEmbedding();
                }
                case LRI, RLI, FSI -> pushIsolate(line, stack, classes, levels, i, count, cls,
                        currentLevel);
                case PDI -> {
                    stack.popToIsolate();
                    fill(classes, levels, i, count, BidiCategory.BN, stack.currentLevel());
                }
                default -> {
                    BidiCategory effective =
                            stack.currentOverride() != null ? stack.currentOverride() : cls;
                    fill(classes, levels, i, count, effective, currentLevel);
                }
            }
            i += count;
        }
    }

    /** X2/X3 : ouverture d'un enchâssement (ou survol silencieux). */
    private static void pushEmbedding(Stack stack, BidiCategory[] classes, int[] levels,
            int i, int count, BidiCategory cls, int currentLevel) {
        fill(classes, levels, i, count, BidiCategory.BN, currentLevel);
        int newLevel = cls == BidiCategory.LRE || cls == BidiCategory.LRO
                ? nextEven(currentLevel) : nextOdd(currentLevel);
        BidiCategory override = cls == BidiCategory.LRO ? BidiCategory.L
                : cls == BidiCategory.RLO ? BidiCategory.R : null;
        stack.push(newLevel, override, false);
    }

    /** X5a/X5b/X5c : ouverture d'un isolat, FSI résolu par premier fort. */
    private static void pushIsolate(String line, Stack stack, BidiCategory[] classes,
            int[] levels, int i, int count, BidiCategory cls, int currentLevel) {
        fill(classes, levels, i, count, BidiCategory.BN, currentLevel);
        boolean rtl = cls == BidiCategory.RLI
                || (cls == BidiCategory.FSI && firstStrongIsolated(line, i) == 1);
        int newLevel = rtl ? nextOdd(currentLevel) : nextEven(currentLevel);
        stack.push(newLevel, null, true);
    }

    /** Le premier caractère fort à l'intérieur d'un isolat, hors sous-isolats. */
    private static int firstStrongIsolated(String line, int from) {
        int depth = 0;
        for (int i = from + Character.charCount(line.codePointAt(from)); i < line.length(); ) {
            int codePoint = line.codePointAt(i);
            BidiCategory cls = BidiClassifier.classOf(codePoint);
            if (depth == 0 && isStrong(cls)) {
                return cls == BidiCategory.L ? 0 : 1;
            }
            depth = nextIsolateDepth(cls, depth);
            i += Character.charCount(codePoint);
        }
        return 0;
    }

    private static boolean isStrong(BidiCategory cls) {
        return cls == BidiCategory.L || cls == BidiCategory.R || cls == BidiCategory.AL;
    }

    /** La profondeur d'isolat après un caractère donné. */
    private static int nextIsolateDepth(BidiCategory cls, int depth) {
        if (cls == BidiCategory.LRI || cls == BidiCategory.RLI || cls == BidiCategory.FSI) {
            return depth + 1;
        }
        if (cls == BidiCategory.PDI && depth > 0) {
            return depth - 1;
        }
        return depth;
    }

    private static int nextEven(int level) {
        return level % 2 == 0 ? level + 2 : level + 1;
    }

    private static int nextOdd(int level) {
        return level % 2 == 0 ? level + 1 : level + 2;
    }

    private static void fill(BidiCategory[] classes, int[] levels, int from, int count,
            BidiCategory cls, int level) {
        for (int k = 0; k < count; k++) {
            classes[from + k] = cls;
            levels[from + k] = level;
        }
    }

    /** États de la pile d'enchâssement. */
    private static final class Stack {
        private final int[] levels = new int[MAX_DEPTH + 2];
        private final BidiCategory[] overrides = new BidiCategory[MAX_DEPTH + 2];
        private final boolean[] isolates = new boolean[MAX_DEPTH + 2];
        private int top;

        private Stack(int paragraphLevel) {
            levels[0] = paragraphLevel;
        }

        private int currentLevel() {
            return levels[top];
        }

        private @Nullable BidiCategory currentOverride() {
            return overrides[top];
        }

        private void push(int level, @Nullable BidiCategory override, boolean isolate) {
            if (level <= MAX_DEPTH && top < MAX_DEPTH) {
                top++;
                levels[top] = level;
                overrides[top] = override;
                isolates[top] = isolate;
            }
        }

        private void popEmbedding() {
            if (!isolates[top] && top > 0) {
                top--;
            }
        }

        private void popToIsolate() {
            int scanned = top;
            while (scanned > 0 && !isolates[scanned]) {
                scanned--;
            }
            if (scanned > 0) {
                top = scanned - 1;
            }
        }
    }
}
