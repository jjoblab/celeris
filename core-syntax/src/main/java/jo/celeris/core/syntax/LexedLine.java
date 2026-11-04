package jo.celeris.core.syntax;

/**
 * Une ligne lexicographiée : ses jetons (offsets relatifs à la ligne) et
 * l'état de sortie qui sert d'état d'entrée à la ligne suivante.
 *
 * <p>Classe finale à tableau nu — le moteur partage le tableau entre
 * relectures successives et l'identité de l'objet est la preuve du
 * réemploi incrémental. Le tableau est immuable par convention : construit
 * une fois par le lexeur, jamais muté, jamais exposé au mutation. Ne pas
 * cloner à l'accès : le partage est le contrat, pas une fuite.</p>
 */
public final class LexedLine {

    private final Token[] tokens;
    private final LexState exitState;

    /**
     * Une ligne lexicographiée.
     *
     * @param tokens    les jetons de la ligne, triés par début croissant
     * @param exitState l'état après la fin de la ligne
     */
    public LexedLine(Token[] tokens, LexState exitState) {
        this.tokens = tokens;
        this.exitState = exitState;
    }

    /** Les jetons de la ligne — immuables par convention, ne jamais muter. */
    public Token[] tokens() {
        return tokens;
    }

    /** L'état de sortie de la ligne. */
    public LexState exitState() {
        return exitState;
    }
}
