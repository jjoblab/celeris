package jo.celeris.core.syntax;

/**
 * Analyse d'un instantané : le lexique incrémental (paniers, états,
 * profondeurs) et l'arbre des chunks, tous deux versionnés par la
 * révision du document (ADR-004 — l'analyse est rejouable sur tout
 * instantané, y compris au retour de l'annulation).
 *
 * @param revision la révision du document analysé
 * @param lex      le document lexicographié
 * @param tree     l'arbre syntaxique tolérant
 */
public record Analysis(int revision, LexDocument lex, SyntaxTree tree) {

    /** Vrai quand l'arbre est complet — aucun chunk en attente (G-01). */
    public boolean estComplet() {
        return tree.estComplet();
    }
}
