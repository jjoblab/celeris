package jo.celeris.core.syntax;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.core.text.Document;

/**
 * Diagnostics de syntaxe (D-02, facette moteur) : chaque nœud d'erreur de
 * l'arbre devient une plage source localisée, triée par position — prête
 * pour les marques de gouttière et les soulignements. Le moteur ne
 * sévit jamais : la malformation est une donnée, pas un événement.
 */
public final class TreeDiagnostics {

    private TreeDiagnostics() {
    }

    /**
     * Les plages malformées d'une analyse, dans l'ordre du source.
     *
     * @param doc      le document porteur
     * @param analysis l'analyse courante
     * @return les plages en erreur (peut être vide, jamais null)
     */
    public static List<SyntaxIssue> issues(Document doc, Analysis analysis) {
        List<SyntaxIssue> problemes = new ArrayList<>();
        for (Chunk chunk : analysis.tree().chunks()) {
            int base = analysis.tree().chunkStartOffset(doc, chunk);
            collecter(chunk.root(), base, problemes);
        }
        return List.copyOf(problemes);
    }

    private static void collecter(SynNode noeud, int base, List<SyntaxIssue> problemes) {
        if (noeud.isError()) {
            problemes.add(new SyntaxIssue(base + noeud.start(),
                    Math.max(1, noeud.end() - noeud.start()), "malformed structure"));
            return;
        }
        for (SynNode enfant : noeud.children()) {
            collecter(enfant, base, problemes);
        }
    }
}
