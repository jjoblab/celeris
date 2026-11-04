package jo.celeris.core.text;

/**
 * Plage de sélection unique : une ancre et un curseur, en offsets UTF-16.
 *
 * <p>L'ancre est le point où la sélection a commencé ; le curseur est
 * l'extrémité active, celle que déplacent les flèches. Quand le curseur
 * précède l'ancre, la sélection est dite inversée : la projection et la
 * restauration à l'annulation (A-13) respectent cette direction — jamais
 * un renversement silencieux.</p>
 *
 * @param anchor l'ancre, offset positif ou nul
 * @param caret  le curseur, offset positif ou nul
 */
public record SelRange(int anchor, int caret) {

    /**
     * Valide la plage.
     *
     * @throws IllegalArgumentException si une extrémité est négative
     */
    public SelRange {
        if (anchor < 0 || caret < 0) {
            throw new IllegalArgumentException(
                "negative selection endpoint: anchor=" + anchor + ", caret=" + caret);
        }
    }

    /**
     * Construit une sélection réduite à un curseur.
     *
     * @param position la position du curseur
     * @return la plage vide (ancre et curseur confondus)
     */
    public static SelRange caretAt(int position) {
        return new SelRange(position, position);
    }

    /** Le début de la sélection, quelle que soit sa direction. */
    public int start() {
        return Math.min(anchor, caret);
    }

    /** La fin (exclusive) de la sélection, quelle que soit sa direction. */
    public int end() {
        return Math.max(anchor, caret);
    }

    /** Vrai si l'ancre et le curseur coïncident (plage réduite à un curseur). */
    public boolean isEmpty() {
        return anchor == caret;
    }

    /** Vrai si le curseur précède l'ancre (sélection inversée). */
    public boolean isBackward() {
        return caret < anchor;
    }
}
