package jo.celeris.core.ai;

/**
 * Une suggestion de texte fantôme (F-01, ADR-012) : une complétion en
 * ligne, possiblement multiligne, affichée comme suggestion éphémère à
 * l'ancre donnée.
 *
 * <p>La suggestion est datée par l'instantané contre lequel elle a été
 * calculée : {@code revision} doit correspondre à la révision courante du
 * document au moment de l'affichage. Une suggestion périmée se jette —
 * jamais elle ne se ré-ancre ni ne se fusionne à la hâte (ADR-007 : les
 * résultats datés d'un instantané périmé sont jetés). L'acceptation
 * partielle, elle, est un geste de l'utilisateur sur une suggestion
 * visible : le reste survit au fragment accepté.</p>
 *
 * @param texte    le texte suggéré, jamais vide, possiblement multiligne
 * @param offset   l'ancre — où le texte s'insérerait
 * @param revision la révision du document contre laquelle la suggestion
 *                 a été calculée
 */
public record SuggestionFantome(String texte, int offset, int revision) {

    /**
     * Valide la suggestion : du texte, une ancre, une révision.
     *
     * @throws IllegalArgumentException si le texte est vide, si l'ancre est
     *         négative ou si la révision est négative
     */
    public SuggestionFantome {
        if (texte.isEmpty()) {
            throw new IllegalArgumentException("suggestion text is empty");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset must be >= 0: " + offset);
        }
        if (revision < 0) {
            throw new IllegalArgumentException("revision must be >= 0: " + revision);
        }
    }
}
