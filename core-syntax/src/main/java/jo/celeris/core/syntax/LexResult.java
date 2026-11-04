package jo.celeris.core.syntax;

/**
 * Résultat d'une lexicographie : le document lexé et la plage de lignes
 * relues. La cascade s'arrête à la première ligne au-delà de l'édition
 * dont l'état de sortie re-converge avec l'ancien — la suite est reprise
 * telle quelle (PIEGE-19).
 *
 * @param lex       le document lexicographié après la passe
 * @param firstLine première ligne relue (0 pour une passe complète)
 * @param lastLine  dernière ligne relue
 */
public record LexResult(LexDocument lex, int firstLine, int lastLine) {

    /** Nombre de lignes relues par la passe. */
    public int reLexedLines() {
        return lastLine - firstLine + 1;
    }
}
