package jo.celeris.android.view;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.SelRange;

/**
 * Fenêtres et suppressions d'entourage IME : la partie pure de
 * {@code getSurroundingText}, {@code getTextBeforeCursor},
 * {@code getTextAfterCursor}, {@code deleteSurroundingText} et sa
 * variante en points de code (ADR-006 — la colle applique, elle ne
 * calcule pas).
 *
 * <p>Discipline Unicode du registre : une suppression dont la borne
 * tomberait au milieu d'un cluster de graphèmes l'absorbe entier —
 * jamais une demi-paire de substitution, jamais un emoji lié par
 * sélecteur amputé. Les frontières se calculent par la même machine
 * de clusters que la composition ({@link CompositionState},
 * méthodes de paquet).</p>
 */
public final class SurroundingTexts {

    private SurroundingTexts() {
    }

    /**
     * Une fenêtre de texte autour de la sélection : le texte, la
     * sélection relative à la fenêtre et la position de la fenêtre
     * dans le document.
     *
     * @param text           le texte de la fenêtre
     * @param selectionStart le début de sélection, relatif à la fenêtre
     * @param selectionEnd   la fin de sélection, relative à la fenêtre
     * @param docOffset      la position de la fenêtre dans le document
     */
    public record Window(String text, int selectionStart, int selectionEnd,
            int docOffset) {
    }

    /**
     * Un plan de suppression : la plage à remplacer par le vide.
     *
     * @param start le début de la plage, en offsets de document
     * @param end   la fin de la plage, en offsets de document
     */
    public record Deletion(int start, int end) {
    }

    /**
     * La fenêtre autour de la sélection primaire : les longueurs
     * demandées sont honorées, bornées au document, nulles si
     * négatives.
     *
     * @param document l'instantané de document courant
     * @param primary  la plage de sélection primaire
     * @param before   la longueur demandée avant la sélection
     * @param after    la longueur demandée après la sélection
     * @return la fenêtre, sélection relative et position de document
     */
    public static Window window(Document document, SelRange primary, int before,
            int after) {
        int avant = Math.max(0, before);
        int apres = Math.max(0, after);
        int start = Math.max(0, primary.start() - avant);
        int end = Math.min(document.length(), primary.end() + apres);
        String text = document.subText(start, end);
        return new Window(text, primary.start() - start,
                primary.end() - start, start);
    }

    /**
     * Le plan de suppression d'entourage : autant d'unités (ou de
     * points de code) que demandé avant et après la sélection, borné
     * au document, aligné aux clusters — un cluster entamé par une
     * borne part en entier.
     *
     * @param document   l'instantané de document courant
     * @param primary    la plage de sélection primaire
     * @param before     les unités à supprimer avant la sélection
     * @param after      les unités à supprimer après la sélection
     * @param codePoints vrai pour compter en points de code
     * @return la plage à remplacer par le vide
     */
    public static Deletion deleteAround(Document document, SelRange primary,
            int before, int after, boolean codePoints) {
        int start = recule(document, primary.start(), Math.max(0, before),
                codePoints);
        int end = avance(document, primary.end(), Math.max(0, after),
                codePoints);
        start = CompositionState.debutDeCluster(document, start);
        end = CompositionState.finDeCluster(document, end);
        return new Deletion(start, end);
    }

    /** Recule de n unités ou n points de code, borné au début. */
    private static int recule(Document document, int from, int n,
            boolean codePoints) {
        int position = from;
        for (int i = 0; i < n && position > 0; i++) {
            if (codePoints && position >= 2
                    && Character.isLowSurrogate(document.charAt(position - 1))
                    && Character.isHighSurrogate(document.charAt(position - 2))) {
                position -= 2;
            } else {
                position -= 1;
            }
        }
        return Math.max(0, position);
    }

    /** Avance de n unités ou n points de code, borné à la fin. */
    private static int avance(Document document, int from, int n,
            boolean codePoints) {
        int position = from;
        int length = document.length();
        for (int i = 0; i < n && position < length; i++) {
            if (codePoints && position + 1 < length
                    && Character.isHighSurrogate(document.charAt(position))
                    && Character.isLowSurrogate(document.charAt(position + 1))) {
                position += 2;
            } else {
                position += 1;
            }
        }
        return Math.min(length, position);
    }
}
