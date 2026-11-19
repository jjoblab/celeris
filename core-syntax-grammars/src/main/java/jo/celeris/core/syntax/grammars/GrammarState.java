package jo.celeris.core.syntax.grammars;

import jo.celeris.core.syntax.LexState;

import org.jspecify.annotations.Nullable;

/**
 * État transporté de la voie de grammaires (ADR-004, PIEGE-21) : la
 * pile des blocs ouverts, immuable et à égalité de valeur — deux états
 * portant les mêmes blocs ouverts (même règle, même fermeture
 * substituée) sont égaux, la cascade de re-lexicographie s'arrête
 * exactement à la reconvergence, jamais elle ne relit la suite du
 * document. Une pile réifiée par identité ne reconvergerait jamais :
 * c'est le piège, il est couvert par test.
 *
 * @param bloc   le bloc ouvert au sommet, null hors de tout bloc
 * @param parent la pile en dessous, null au fond
 */
record GrammarState(@Nullable OpenBlock bloc, @Nullable GrammarState parent)
        implements LexState {

    /** Hors de tout bloc — l'état avant la première ligne. */
    static final GrammarState EMPTY = new GrammarState(null, null);

    /** Empile un bloc ouvert. */
    GrammarState empiler(OpenBlock ouvert) {
        return new GrammarState(ouvert, this);
    }

    /** Dépile le sommet — le parent, ou VIDE au fond. */
    GrammarState depiler() {
        GrammarState parent = this.parent;
        return parent == null ? EMPTY : parent;
    }

    /** Vrai quand un bloc est ouvert — les écarts portent sa portée de contenu. */
    boolean dansBloc() {
        return bloc != null;
    }
}
