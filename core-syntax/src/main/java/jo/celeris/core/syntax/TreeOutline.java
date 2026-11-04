package jo.celeris.core.syntax;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.core.text.Document;

/**
 * Plan du fichier (B-16) : les entrées sortent du MÊME arbre que la
 * coloration — déclarations nommées avec catégorie, bornes absolues et
 * lignes — recalculées à chaque révision, incrémentalement. Le plan ne
 * connaît pas la profondeur d'affichage : c'est l'arbre qui porte la
 * hiérarchie, la liste la parcourt.
 */
public final class TreeOutline {

    private TreeOutline() {
    }

    /**
     * Les entrées du plan d'une analyse, dans l'ordre du source.
     *
     * @param doc      le document porteur
     * @param analysis l'analyse courante
     * @return les déclarations nommées, imbriquées aplaties dans l'ordre
     */
    public static List<OutlineEntry> entries(Document doc, Analysis analysis) {
        List<OutlineEntry> entrees = new ArrayList<>();
        for (Chunk chunk : analysis.tree().chunks()) {
            int base = analysis.tree().chunkStartOffset(doc, chunk);
            collecter(doc, chunk.root(), base, entrees);
        }
        return List.copyOf(entrees);
    }

    private static void collecter(Document doc, SynNode noeud, int base,
                                  List<OutlineEntry> entrees) {
        String nom = noeud.outlineName();
        if (nom != null) {
            int debut = base + noeud.start();
            int fin = base + noeud.end();
            entrees.add(new OutlineEntry(noeud.category(), nom, debut, fin,
                    doc.lineForOffset(debut), doc.lineForOffset(Math.max(debut, fin - 1))));
        }
        for (SynNode enfant : noeud.children()) {
            collecter(doc, enfant, base, entrees);
        }
    }
}
