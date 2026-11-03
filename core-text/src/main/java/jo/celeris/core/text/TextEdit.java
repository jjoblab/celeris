package jo.celeris.core.text;

/**
 * Édition unitaire réversible : la plage {@code [start, start +
 * removed.length())} a été remplacée par {@code inserted}.
 *
 * <p>Le texte retiré est porté par l'édition (et non relu au rejeu) : la
 * {@link Transaction} et l'étape d'annulation qui la contiennent restent
 * auto-inversibles, exactement comme l'opération d'édition du dépôt de
 * départ dont elle est le portage. Une édition sans effet (rien retiré,
 * rien inséré) est refusée : elle n'a rien à faire sur la voie de
 * mutation.</p>
 *
 * @param start    offset de début de la plage remplacée
 * @param removed  le texte qui occupait la plage
 * @param inserted le texte qui la remplace
 */
public record TextEdit(int start, String removed, String inserted) {

    /**
     * Valide l'édition : départ positif ou nul, édition non vide.
     *
     * @throws IllegalArgumentException si {@code start < 0} ou si l'édition
     *         ne retire et n'insère rien
     */
    public TextEdit {
        if (start < 0) {
            throw new IllegalArgumentException("start must be >= 0: " + start);
        }
        if (removed.isEmpty() && inserted.isEmpty()) {
            throw new IllegalArgumentException("edit removes and inserts nothing");
        }
    }

    /** Offset de fin (exclusif) de la plage remplacée. */
    public int end() {
        return start + removed.length();
    }

    /** L'édition inverse : échange le texte retiré et le texte inséré. */
    public TextEdit inverse() {
        return new TextEdit(start, inserted, removed);
    }
}
