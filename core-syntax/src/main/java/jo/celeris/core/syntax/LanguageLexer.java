package jo.celeris.core.syntax;

/**
 * Lexeur d'un langage (ADR-004) : lexicographie d'UNE ligne à la fois,
 * pilotée par l'état d'entrée, productrice de l'état de sortie. Le
 * moteur incrémental appelle {@link #lex} ligne par ligne — jamais le
 * document entier — et compare les états pour borner la cascade.
 *
 * <p>Contrat : déterminisme (même texte, même état d'entrée → mêmes
 * jetons, même état de sortie), aucun jeton au-delà de la ligne, jetons
 * triés et disjoints. La ligne ne contient jamais le saut de ligne
 * final.</p>
 */
public interface LanguageLexer {

    /** L'état avant la première ligne. */
    LexState initialState();

    /**
     * Lexicographie une ligne.
     *
     * @param lineText le contenu de la ligne, sans le saut de ligne
     * @param entry    l'état d'entrée (sortie de la ligne précédente)
     * @return les jetons de la ligne et l'état de sortie
     */
    LexedLine lex(CharSequence lineText, LexState entry);
}
