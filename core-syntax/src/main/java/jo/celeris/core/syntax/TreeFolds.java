package jo.celeris.core.syntax;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import jo.celeris.core.text.Document;

import org.jspecify.annotations.Nullable;

/**
 * Régions de pli calculées depuis l'arbre et le lexique (A-15) : chaque
 * nœud structurant de deux lignes ou plus — bloc, type, fonction, tag —
 * devient une région, de même que chaque commentaire multi-lignes. Le
 * calcul relit l'analyse de la révision courante : les plis MANUELS de la
 * vue, eux, se projettent par les ancres du cœur texte à travers les
 * éditions (ADR-003) et vivent dans le modèle de plis de la vue.
 */
public final class TreeFolds {

    private TreeFolds() {
    }

    /**
     * Les régions de pli d'une analyse, triées par ligne de début.
     *
     * @param doc      le document porteur
     * @param analysis l'analyse courante
     * @return les régions (en-tête visible, lignes suivantes repliées)
     */
    public static List<FoldSpan> regions(Document doc, Analysis analysis) {
        return regions(doc, analysis, null);
    }

    /**
     * Les régions de pli d'une analyse, celles de l'arbre et des
     * commentaires, plus celles de l'indentation quand le langage se
     * replie au décalage (M7.6, H-08) : les régions se fondent, une
     * même ligne de début garde la plus longue.
     *
     * @param doc      le document porteur
     * @param analysis l'analyse courante
     * @param settings le réglage des plis par indentation, null pour
     *                 les langages qui ne s'y replient pas
     * @return les régions (en-tête visible, lignes suivantes repliées)
     */
    public static List<FoldSpan> regions(Document doc, Analysis analysis,
            IndentationFolds.@Nullable Settings settings) {
        List<FoldSpan> regions = new ArrayList<>();
        for (Chunk chunk : analysis.tree().chunks()) {
            int base = analysis.tree().chunkStartOffset(doc, chunk);
            collecter(doc, chunk.root(), base, regions);
        }
        collecterCommentaires(analysis.lex(), regions);
        if (settings != null) {
            regions.addAll(IndentationFolds.regions(doc, analysis.lex(), settings));
        }
        regions.sort(Comparator.comparingInt(FoldSpan::startLine)
                .thenComparingInt(FoldSpan::endLine));
        List<FoldSpan> uniques = new ArrayList<>();
        for (FoldSpan region : regions) {
            if (!uniques.isEmpty()) {
                FoldSpan precedente = uniques.get(uniques.size() - 1);
                if (precedente.equals(region)) {
                    continue;
                }
                if (precedente.startLine() == region.startLine()) {
                    uniques.set(uniques.size() - 1, region);
                    continue;
                }
            }
            uniques.add(region);
        }
        return List.copyOf(uniques);
    }

    private static void collecter(Document doc, SynNode noeud, int base,
                                  List<FoldSpan> regions) {
        NodeCategory categorie = noeud.category();
        if (pliable(categorie)) {
            int debut = doc.lineForOffset(base + noeud.start());
            int fin = doc.lineForOffset(base + noeud.end() - 1);
            if (fin > debut) {
                regions.add(new FoldSpan(debut, fin));
            }
        }
        for (SynNode enfant : noeud.children()) {
            collecter(doc, enfant, base, regions);
        }
    }

    private static boolean pliable(NodeCategory categorie) {
        return categorie == NodeCategory.BLOCK || categorie == NodeCategory.TYPE
                || categorie == NodeCategory.FUNCTION || categorie == NodeCategory.TAG;
    }

    private static void collecterCommentaires(LexDocument lex,
                                              List<FoldSpan> regions) {
        int ligneDebut = -1;
        for (int ligne = 0; ligne < lex.lineCount(); ligne++) {
            boolean commentaire = false;
            for (Token jeton : lex.line(ligne).tokens()) {
                if (jeton.kind() == TokenKind.COMMENT || jeton.kind() == TokenKind.DOC) {
                    commentaire = true;
                    break;
                }
            }
            if (commentaire && ligneDebut < 0) {
                ligneDebut = ligne;
            } else if (!commentaire && ligneDebut >= 0) {
                if (ligne - 1 > ligneDebut) {
                    regions.add(new FoldSpan(ligneDebut, ligne - 1));
                }
                ligneDebut = -1;
            }
        }
        if (ligneDebut >= 0 && lex.lineCount() - 1 > ligneDebut) {
            regions.add(new FoldSpan(ligneDebut, lex.lineCount() - 1));
        }
    }
}
