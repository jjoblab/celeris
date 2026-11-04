package jo.celeris.core.syntax;

/**
 * Document lexicographié : un panier de jetons et un état de sortie par
 * ligne, plus la profondeur de crochets au début de chaque ligne. Les
 * offsets des jetons sont relatifs à leur ligne — les paniers d'une ligne
 * au contenu inchangé se partagent entre relectures successives, c'est le
 * réemploi incrémental (ADR-004).
 *
 * <p>Immuable : chaque relecture produit une nouvelle instance, les lignes
 * non concernées y désignent les mêmes objets {@link LexedLine}.</p>
 */
public final class LexDocument {

    private final LexedLine[] lignes;
    private final int[] profondeurs;
    private final LexState initial;

    LexDocument(LexedLine[] lignes, int[] profondeurs, LexState initial) {
        this.lignes = lignes;
        this.profondeurs = profondeurs;
        this.initial = initial;
    }

    /** Nombre de lignes lexicographiées (une ligne vide finale comprise). */
    public int lineCount() {
        return lignes.length;
    }

    /**
     * Le panier et l'état de sortie d'une ligne.
     *
     * @param line numéro de ligne, borné au document
     * @return la ligne lexicographiée (jetons vides pour une ligne blanche)
     */
    public LexedLine line(int line) {
        return lignes[Math.min(Math.max(line, 0), lignes.length - 1)];
    }

    /**
     * L'état d'entrée d'une ligne — sortie de la précédente, état initial
     * pour la première.
     *
     * @param line numéro de ligne
     * @return l'état avant la lecture de la ligne
     */
    public LexState stateBefore(int line) {
        if (line <= 0) {
            return initial;
        }
        return lignes[Math.min(line, lignes.length) - 1].exitState();
    }

    /**
     * La profondeur de crochets au début de la ligne — indentation et plis
     * s'y servent ; un document sans crochets reste à zéro partout.
     *
     * @param line numéro de ligne
     * @return la profondeur (nombre d'ouvrants non fermés avant la ligne)
     */
    public int bracketDepthAtLineStart(int line) {
        return profondeurs[Math.min(Math.max(line, 0), profondeurs.length - 1)];
    }

    LexedLine[] lignes() {
        return lignes;
    }

    int[] profondeurs() {
        return profondeurs;
    }

    LexState initial() {
        return initial;
    }
}
