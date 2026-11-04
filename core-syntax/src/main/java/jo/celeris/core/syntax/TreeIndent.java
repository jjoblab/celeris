package jo.celeris.core.syntax;

import jo.celeris.core.text.Document;

/**
 * Niveaux d'indentation dérivés de l'arbre (ADR-004) : une ligne porte le
 * nombre de nœuds structurants qui l'englobent strictement — ouverts sur
 * une ligne antérieure, fermés sur la sienne au plus tôt — et la ligne qui
 * commence par un crochet fermant porte un cran de plus : le groupe qu'elle
 * ferme était encore ouvert à son début. Le calcul relit l'analyse de la
 * révision courante, maintenue incrémentalement — jamais le source seul.
 */
public final class TreeIndent {

    private TreeIndent() {
    }

    /**
     * Le niveau d'indentation d'une ligne.
     *
     * @param doc      le document porteur
     * @param analysis l'analyse courante
     * @param line     la ligne interrogée
     * @return son niveau, 0 hors de tout bloc
     */
    public static int levelAt(Document doc, Analysis analysis, int line) {
        int niveau = 0;
        for (Chunk chunk : analysis.tree().chunks()) {
            int base = analysis.tree().chunkStartOffset(doc, chunk);
            niveau += englobants(chunk.root(), doc, base, line);
        }
        if (commenceParFermant(analysis, line)) {
            niveau++;
        }
        return niveau;
    }

    private static int englobants(SynNode noeud, Document doc, int base, int line) {
        int total = 0;
        if (noeud.category() != NodeCategory.OTHER) {
            int debut = doc.lineForOffset(base + noeud.start());
            int fin = doc.lineForOffset(base + Math.max(noeud.start(), noeud.end() - 1));
            if (debut < line && line <= fin) {
                total = 1;
            }
        }
        for (SynNode enfant : noeud.children()) {
            total += englobants(enfant, doc, base, line);
        }
        return total;
    }

    private static boolean commenceParFermant(Analysis analysis, int line) {
        if (line < 0 || line >= analysis.lex().lineCount()) {
            return false;
        }
        for (Token jeton : analysis.lex().line(line).tokens()) {
            return jeton.kind() == TokenKind.CLOSE_BRACKET;
        }
        return false;
    }
}
