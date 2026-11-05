package jo.celeris.android.view;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.Chunk;
import jo.celeris.core.syntax.NodeCategory;
import jo.celeris.core.syntax.SynNode;
import jo.celeris.core.text.Document;

/**
 * Les lignes collantes (M5, A-16) : les en-têtes des structures qui
 * englobent la première ligne visible restent collés en haut du
 * rendu — lus dans le même arbre que la coloration. Le budget d'image
 * est tenu : au plus « max » lignes collées, les plus profondes
 * l'emportent. Pur et testable sans SDK : la vue le possède, le
 * peintre le lit.
 */
public final class StickyLines {

    private StickyLines() {
    }

    /**
     * Les lignes d'en-tête à coller au-dessus d'une ligne visible :
     * celles des nœuds structurants qui l'englobent strictement
     * (ouverts avant, fermés après elle), bornées aux plus profondes.
     *
     * @param doc          le document porteur
     * @param analysis     l'analyse courante
     * @param ligneVisible la première ligne visible
     * @param max          le budget de lignes collées, au moins 1
     * @return les lignes d'en-tête, de la plus externe à la plus profonde
     */
    public static List<Integer> entetes(Document doc, Analysis analysis, int ligneVisible,
            int max) {
        TreeSet<Integer> lignes = new TreeSet<>();
        for (Chunk chunk : analysis.tree().chunks()) {
            int base = analysis.tree().chunkStartOffset(doc, chunk);
            rassembler(chunk.root(), base, doc, ligneVisible, lignes);
        }
        List<Integer> toutes = new ArrayList<>(lignes);
        if (toutes.size() <= max) {
            return List.copyOf(toutes);
        }
        return List.copyOf(toutes.subList(toutes.size() - max, toutes.size()));
    }

    /** Rassemble les lignes d'en-tête des nœuds englobant strictement la ligne visible. */
    private static void rassembler(SynNode noeud, int base, Document doc, int ligneVisible,
            TreeSet<Integer> lignes) {
        if (noeud.category() != NodeCategory.OTHER) {
            int debut = doc.lineForOffset(base + noeud.start());
            int fin = doc.lineForOffset(base + Math.max(noeud.start(), noeud.end() - 1));
            if (debut < ligneVisible && ligneVisible <= fin) {
                lignes.add(debut);
            }
        }
        for (SynNode enfant : noeud.children()) {
            rassembler(enfant, base, doc, ligneVisible, lignes);
        }
    }
}
