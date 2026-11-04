package jo.celeris.core.text;

/**
 * Règles faibles (W1-W7) et neutres (N1-N2) de l'algorithme
 * bidirectionnel UAX #9 — visible du paquet seulement, la surface
 * publique étant {@link Bidi}.
 *
 * <p>Chaque run de même niveau se résout indépendamment, du type fort
 * de départ ({@code sos}) au type fort d'arrivée.</p>
 */
final class BidiRuns {

    private BidiRuns() {
    }

    /** Résout chaque run de même niveau, borne à borne. */
    static void resolve(BidiCategory[] classes, int[] levels) {
        int length = classes.length;
        int start = 0;
        while (start < length) {
            int end = start;
            while (end < length && levels[end] == levels[start]) {
                end++;
            }
            resolveRun(classes, start, end, levels[start]);
            start = end;
        }
    }

    private static void resolveRun(BidiCategory[] classes, int start, int end, int runLevel) {
        BidiCategory sos = runLevel % 2 == 0 ? BidiCategory.L : BidiCategory.R;
        resolveMarks(classes, start, end, sos);
        // W3 : les lettres arabes se comportent comme du droite à
        // gauche.
        for (int i = start; i < end; i++) {
            if (classes[i] == BidiCategory.AL) {
                classes[i] = BidiCategory.R;
            }
        }
        resolveSeparators(classes, start, end);
        resolveEuropeanNumbers(classes, start, end, sos);
        // N1 et N2 : les neutres prennent la direction environnante,
        // sinon la direction d'enchâssement.
        resolveNeutrals(classes, start, end, runLevel, sos);
    }

    /** W1 : les marques suivent le caractère précédent ; W2 : contexte arabe. */
    private static void resolveMarks(BidiCategory[] classes, int start, int end,
            BidiCategory sos) {
        BidiCategory previous = sos;
        for (int i = start; i < end; i++) {
            if (classes[i] == BidiCategory.NSM) {
                classes[i] = previous;
            } else {
                previous = classes[i];
            }
        }
        BidiCategory lastStrong = sos;
        for (int i = start; i < end; i++) {
            if (classes[i] == BidiCategory.EN && lastStrong == BidiCategory.AL) {
                classes[i] = BidiCategory.AN;
            }
            if (isStrong(classes[i])) {
                lastStrong = classes[i];
            }
        }
    }

    /** W7 : chiffres européens dans un contexte fort gauche à droite. */
    private static void resolveEuropeanNumbers(BidiCategory[] classes, int start, int end,
            BidiCategory sos) {
        BidiCategory lastStrong = sos;
        for (int i = start; i < end; i++) {
            if (classes[i] == BidiCategory.EN && lastStrong == BidiCategory.L) {
                classes[i] = BidiCategory.L;
            }
            if (classes[i] == BidiCategory.L || classes[i] == BidiCategory.R) {
                lastStrong = classes[i];
            }
        }
    }

    /** W4, W5 et W6 : séparateurs et symboles numériques. */
    private static void resolveSeparators(BidiCategory[] classes, int start, int end) {
        // W4 : séparateur unique entre deux nombres de même nature.
        for (int i = start + 1; i < end - 1; i++) {
            if (classes[i] == BidiCategory.ES || classes[i] == BidiCategory.CS) {
                BidiCategory before = classes[i - 1];
                if ((before == BidiCategory.EN || before == BidiCategory.AN)
                        && classes[i + 1] == before) {
                    classes[i] = before;
                }
            }
        }
        // W5 : les symboles de nombre collés aux chiffres européens.
        for (int i = start; i < end; i++) {
            if (classes[i] == BidiCategory.ET) {
                i = resolveEuropeanTermini(classes, start, end, i);
            }
        }
        // W6 : le reste des séparateurs devient neutre.
        for (int i = start; i < end; i++) {
            if (classes[i] == BidiCategory.ES || classes[i] == BidiCategory.ET
                    || classes[i] == BidiCategory.CS) {
                classes[i] = BidiCategory.ON;
            }
        }
    }

    /** La plage de symboles qui touche un chiffre européen devient EN. */
    private static int resolveEuropeanTermini(BidiCategory[] classes, int start, int end,
            int from) {
        int j = from;
        while (j < end && classes[j] == BidiCategory.ET) {
            j++;
        }
        boolean nearEn = (from > start && classes[from - 1] == BidiCategory.EN)
                || (j < end && classes[j] == BidiCategory.EN);
        if (nearEn) {
            for (int k = from; k < j; k++) {
                classes[k] = BidiCategory.EN;
            }
        }
        return j - 1;
    }

    private static void resolveNeutrals(BidiCategory[] classes, int start, int end,
            int runLevel, BidiCategory sos) {
        int i = start;
        while (i < end) {
            if (!BidiClassifier.isNeutral(classes[i])) {
                i++;
                continue;
            }
            int j = i;
            while (j < end && BidiClassifier.isNeutral(classes[j])) {
                j++;
            }
            BidiCategory resolved = neutralDirection(classes, start, end, i, j, runLevel, sos);
            for (int k = i; k < j; k++) {
                classes[k] = resolved;
            }
            i = j;
        }
    }

    /** N1 et N2 : la direction d'une plage neutre. */
    private static BidiCategory neutralDirection(BidiCategory[] classes, int start, int end,
            int from, int to, int runLevel, BidiCategory sos) {
        BidiCategory before = from > start ? classes[from - 1] : sos;
        BidiCategory after = to < end ? classes[to] : sos;
        if (before == BidiCategory.L && after == BidiCategory.L) {
            return BidiCategory.L;
        }
        if (BidiClassifier.actsAsRtl(before) && BidiClassifier.actsAsRtl(after)) {
            return BidiCategory.R;
        }
        return runLevel % 2 == 0 ? BidiCategory.L : BidiCategory.R;
    }

    private static boolean isStrong(BidiCategory cls) {
        return cls == BidiCategory.L || cls == BidiCategory.R || cls == BidiCategory.AL;
    }
}
