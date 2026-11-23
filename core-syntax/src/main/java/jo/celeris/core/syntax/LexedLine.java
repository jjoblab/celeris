package jo.celeris.core.syntax;

import org.jspecify.annotations.Nullable;

/**
 * Une ligne lexicographiée : ses jetons (offsets relatifs à la ligne) et
 * l'état de sortie qui sert d'état d'entrée à la ligne suivante.
 *
 * <p>Classe finale à tableau nu — le moteur partage le tableau entre
 * relectures successives et l'identité de l'objet est la preuve du
 * réemploi incrémental. Le tableau est immuable par convention : construit
 * une fois par le lexeur, jamais muté, jamais exposé au mutation. Ne pas
 * cloner à l'accès : le partage est le contrat, pas une fuite.</p>
 *
 * <p>L'état de sortie devient nullable à la ligne VIDE du lexique
 * partiel (fenêtre d'ouverture, G-01 la suite) : aucune ligne NEUTRE
 * ne la suit au lexique — la lecture d'état la reconnaît et rend
 * l'état initial du document ({@code LexDocument.stateBefore}).</p>
 */
public final class LexedLine {

    private final Token[] tokens;
    private final @Nullable LexState exitState;

    /**
     * Une ligne lexicographiée.
     *
     * @param tokens    les jetons de la ligne, triés par début croissant
     * @param exitState l'état après la fin de la ligne (null pour la
     *                  ligne vide du lexique partiel)
     */
    public LexedLine(Token[] tokens, @Nullable LexState exitState) {
        this.tokens = tokens;
        this.exitState = exitState;
    }

    /** Les jetons de la ligne — immuables par convention, ne jamais muter. */
    public Token[] tokens() {
        return tokens;
    }

    /** L'état de sortie de la ligne — null pour la ligne vide du
     *  lexique partiel (la lecture d'état y rend l'initial). */
    public @Nullable LexState exitState() {
        return exitState;
    }
}
