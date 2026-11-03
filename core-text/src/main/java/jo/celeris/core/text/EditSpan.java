package jo.celeris.core.text;

/**
 * Plage d'une édition appliquée : offset de début, longueur retirée,
 * longueur insérée.
 *
 * <p>Portée par la voie de mutation unique aux projecteurs de positions :
 * les plages ancrées (diagnostics, plis, composition IME) se projettent à
 * travers l'édition sans connaître son contenu textuel.</p>
 *
 * @param start  offset de début de la plage remplacée
 * @param removed nombre de caractères retirés
 * @param added   nombre de caractères insérés
 */
public record EditSpan(int start, int removed, int added) {

    /** Offset de fin (exclusif) de la plage retirée. */
    public int end() {
        return start + removed;
    }

    /** Variation de longueur du document ({@code added - removed}). */
    public int delta() {
        return added - removed;
    }
}
