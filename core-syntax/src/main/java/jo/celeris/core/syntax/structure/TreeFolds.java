package jo.celeris.core.syntax.structure;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.Chunk;
import jo.celeris.core.syntax.LexDocument;
import jo.celeris.core.syntax.NodeCategory;
import jo.celeris.core.syntax.SynNode;
import jo.celeris.core.syntax.Token;
import jo.celeris.core.syntax.TokenKind;
import jo.celeris.core.text.Document;

import org.jspecify.annotations.Nullable;

/**
 * Régions de pli calculées depuis l'arbre et le lexique (A-15) : chaque
 * nœud structurant de deux lignes ou plus — bloc, type, fonction, tag —
 * devient une région, de même que chaque commentaire multi-lignes. Les
 * RÉGIONS NOMMÉES (D-04, l'analyse croisée) se replient aussi :
 * {@code // region Nom} / {@code // endregion} et
 * {@code // <editor-fold desc="...">} / {@code // </editor-fold>},
 * dans les langages à commentaires ligne {@code //} ; le bloc
 * d'IMPORTS Java (une suite de lignes {@code import} contiguës)
 * porte son pli sur la première ligne. Le
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
        // D-04 : les régions nommées et le bloc d'imports se replient
        // — la parité avec le dépôt de départ.
        collecterRegionsNommees(doc, analysis.lex(), regions);
        collecterImports(doc, analysis.lex(), regions);
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

    /**
     * Les régions nommées (D-04) : {@code // region} ouvre,
     * {@code // endregion} ferme ; la variante Studio
     * {@code // <editor-fold ...>} / {@code // </editor-fold>} suit.
     * L'ouvrante porte le pli, la fermante EST la dernière ligne
     * repliée. Une fermante orpheline ne plie rien.
     */
    private static void collecterRegionsNommees(Document doc, LexDocument lex,
                                                List<FoldSpan> regions) {
        int ligneOuverture = -1;
        boolean ouvertureEditorFold = false;
        for (int ligne = 0; ligne < lex.lineCount(); ligne++) {
            String texte = texteCommentaire(doc, lex, ligne);
            if (texte == null) {
                continue;
            }
            String epuree = texte.trim();
            if (ligneOuverture < 0) {
                if (epuree.startsWith("region ") || epuree.equals("region")) {
                    ligneOuverture = ligne;
                    ouvertureEditorFold = false;
                } else if (epuree.startsWith("<editor-fold")) {
                    ligneOuverture = ligne;
                    ouvertureEditorFold = true;
                }
            } else if (ouvertureEditorFold
                    ? epuree.startsWith("</editor-fold")
                    : epuree.startsWith("endregion")) {
                if (ligne > ligneOuverture + 1) {
                    regions.add(new FoldSpan(ligneOuverture, ligne));
                }
                ligneOuverture = -1;
            }
        }
    }

    /**
     * Le bloc d'imports Java (D-04) : une suite contiguë de lignes
     * dont le premier JETON est le mot {@code import} — la première
     * porte le pli, la dernière referme. Les imports Java uniquement :
     * le premier jeton porte le mot exact.
     */
    private static void collecterImports(Document doc, LexDocument lex,
                                         List<FoldSpan> regions) {
        int premiere = -1;
        int derniere = -1;
        for (int ligne = 0; ligne < lex.lineCount(); ligne++) {
            boolean ligneImport = false;
            for (Token jeton : lex.line(ligne).tokens()) {
                int debutLigne = doc.lineStart(ligne);
                if (jeton.kind() == TokenKind.KEYWORD && "import".equals(
                        doc.subText(debutLigne + jeton.start(),
                                debutLigne + jeton.end()))) {
                    ligneImport = true;
                }
                break;
            }
            if (ligneImport) {
                if (premiere < 0) {
                    premiere = ligne;
                }
                derniere = ligne;
            } else if (premiere >= 0) {
                if (derniere > premiere) {
                    regions.add(new FoldSpan(premiere, derniere));
                }
                premiere = -1;
                derniere = -1;
            }
        }
        if (premiere >= 0 && derniere > premiere) {
            regions.add(new FoldSpan(premiere, derniere));
        }
    }

    /** Le texte d'un commentaire {@code //} de la ligne, null si la
     *  ligne n'en porte pas — la matière des régions nommées. */
    private static @org.jspecify.annotations.Nullable String texteCommentaire(
            Document doc, LexDocument lex, int ligne) {
        // les bornes du jeton sont RELATIVES À SA LIGNE (contrat
        // Token) : la lecture du document part du début de ligne
        int debutLigne = doc.lineStart(ligne);
        for (Token jeton : lex.line(ligne).tokens()) {
            if (jeton.kind() == TokenKind.COMMENT) {
                String texte = doc.subText(debutLigne + jeton.start(),
                        debutLigne + jeton.end());
                // la matière de la région vit APRÈS les barres : le
                // commentaire « // region X » se lit « region X »
                return texte.startsWith("//") ? texte.substring(2) : texte;
            }
        }
        return null;
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
