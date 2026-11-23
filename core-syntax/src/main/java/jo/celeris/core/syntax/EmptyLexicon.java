package jo.celeris.core.syntax;

/**
 * La ligne VIDE du lexique partiel (G-01, la suite — le retard
 * résiduel du prompt du 2026-11-23) : la fenêtre d'ouverture ne lex
 * que le visible (marge comprise), les lignes au-delà vivent
 * VIDES — aucun jeton, aucun état de sortie (la lecture d'état y
 * rend l'état initial du document : la complétion relira la ligne
 * depuis zéro).
 *
 * <p>La constante vit SA classe : le lexeur ({@code IncrementalLexer})
 * la pose, le lexique ({@code LexDocument}) la reconnaît à la lecture
 * d'état — les deux partagent l'identité SANS dépendance de l'un en
 * l'autre.</p>
 */
final class EmptyLexicon {

    /** La ligne vide partagée : aucun jeton, aucun état de sortie. */
    static final LexedLine EMPTY_LINE = new LexedLine(new Token[0], null);

    private EmptyLexicon() {
    }
}
