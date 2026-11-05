package jo.celeris.core.syntax;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.SelRange;

/**
 * La sélection syntaxique (M5, A-03) : la chaîne des étendues
 * englobantes — le mot courant, puis les nœuds structurants du plus
 * petit au plus grand, jusqu'au document. Étendre monte d'un cran,
 * réduire descend. L'arbre tolérant du socle n'expose pas de nœuds
 * d'expression (ADR-004) : l'étape « expression » se réduit au plus
 * petit nœud englobant.
 */
public final class TreeSelection {

    private TreeSelection() {
    }

    /**
     * La chaîne des étendues englobantes d'une position : le jeton qui
     * la porte (le mot courant), les nœuds structurants qui la
     * contiennent du plus petit au plus grand, puis le document —
     * strictement croissante, sans répétition.
     *
     * @param doc      le document porteur
     * @param analysis l'analyse courante
     * @param offset   la position interrogée
     * @return la chaîne croissante, éventuellement réduite au document
     */
    public static List<SelRange> chaine(Document doc, Analysis analysis, int offset) {
        List<SelRange> candidats = new ArrayList<>();
        SelRange jeton = jetonContenant(doc, analysis, offset);
        if (jeton != null) {
            candidats.add(jeton);
        }
        for (Chunk chunk : analysis.tree().chunks()) {
            int base = analysis.tree().chunkStartOffset(doc, chunk);
            rassembler(chunk.root(), base, offset, offset, candidats);
        }
        candidats.add(new SelRange(0, doc.length()));
        return trier(candidats);
    }

    /**
     * L'étendue syntaxique suivante : la plus petite étendue qui
     * contient strictement la sélection donnée, ou null si la sélection
     * couvre déjà le document.
     *
     * @param doc      le document porteur
     * @param analysis l'analyse courante
     * @param plage    la sélection courante
     * @return l'étendue englobante suivante, ou null au sommet
     */
    public static @Nullable SelRange suivante(Document doc, Analysis analysis,
            SelRange plage) {
        for (SelRange candidat : chaine(doc, analysis, plage.start())) {
            if (candidat.start() <= plage.start() && plage.end() <= candidat.end()
                    && !candidat.equals(plage)) {
                // la chaîne est triée : le premier qui contient est le plus petit
                return candidat;
            }
        }
        return null;
    }

    /**
     * L'étendue syntaxique précédente : la plus grande étendue contenue
     * dans la sélection donnée, ou null si la sélection est le mot.
     *
     * @param doc      le document porteur
     * @param analysis l'analyse courante
     * @param plage    la sélection courante
     * @return l'étendue contenue précédente, ou null à la base
     */
    public static @Nullable SelRange precedente(Document doc, Analysis analysis,
            SelRange plage) {
        List<SelRange> contenus = new ArrayList<>();
        int premiereLigne = doc.lineForOffset(plage.start());
        int derniereLigne = doc.lineForOffset(Math.max(plage.start(), plage.end() - 1));
        for (int l = premiereLigne; l <= derniereLigne && l < analysis.lex().lineCount(); l++) {
            int debutLigne = doc.lineStart(l);
            for (Token jeton : analysis.lex().line(l).tokens()) {
                SelRange etendue = new SelRange(debutLigne + jeton.start(),
                        debutLigne + jeton.end());
                ajouterSiContenu(etendue, plage, contenus);
            }
        }
        for (Chunk chunk : analysis.tree().chunks()) {
            int base = analysis.tree().chunkStartOffset(doc, chunk);
            rassemblerContenus(chunk.root(), base, plage, contenus);
        }
        SelRange meilleure = null;
        for (SelRange candidat : contenus) {
            if (meilleure == null || taille(candidat) > taille(meilleure)) {
                meilleure = candidat;
            }
        }
        return meilleure;
    }

    /** Le jeton qui porte la position, ou null entre deux jetons. */
    private static @Nullable SelRange jetonContenant(Document doc, Analysis analysis,
            int offset) {
        int ligne = doc.lineForOffset(offset);
        if (ligne >= analysis.lex().lineCount()) {
            return null;
        }
        int debutLigne = doc.lineStart(ligne);
        for (Token jeton : analysis.lex().line(ligne).tokens()) {
            int debut = debutLigne + jeton.start();
            int fin = debutLigne + jeton.end();
            if (debut <= offset && offset < fin) {
                return new SelRange(debut, fin);
            }
        }
        return null;
    }

    /** Rassemble les nœuds structurants dont la plage couvre [debut, fin]. */
    private static void rassembler(SynNode noeud, int base, int debut, int fin,
            List<SelRange> candidats) {
        if (noeud.category() != NodeCategory.OTHER) {
            int absoluDebut = base + noeud.start();
            int absoluFin = base + noeud.end();
            if (absoluDebut <= debut && fin <= absoluFin) {
                candidats.add(new SelRange(absoluDebut, absoluFin));
            }
        }
        for (SynNode enfant : noeud.children()) {
            rassembler(enfant, base, debut, fin, candidats);
        }
    }

    /** Rassemble les nœuds structurants contenus dans la plage donnée. */
    private static void rassemblerContenus(SynNode noeud, int base, SelRange plage,
            List<SelRange> contenus) {
        if (noeud.category() != NodeCategory.OTHER) {
            SelRange etendue = new SelRange(base + noeud.start(), base + noeud.end());
            ajouterSiContenu(etendue, plage, contenus);
        }
        for (SynNode enfant : noeud.children()) {
            rassemblerContenus(enfant, base, plage, contenus);
        }
    }

    /** Ajoute l'étendue si elle est contenue dans la plage, distincte d'elle. */
    private static void ajouterSiContenu(SelRange etendue, SelRange plage,
            List<SelRange> contenus) {
        if (plage.start() <= etendue.start() && etendue.end() <= plage.end()
                && !etendue.equals(plage)) {
            contenus.add(etendue);
        }
    }

    /** La taille d'une étendue. */
    private static int taille(SelRange plage) {
        return plage.end() - plage.start();
    }

    /** Trie par taille croissante, dédoublonne, exige la croissance stricte. */
    private static List<SelRange> trier(List<SelRange> candidats) {
        candidats.sort((a, b) -> Integer.compare(
                a.end() - a.start(), b.end() - b.start()));
        List<SelRange> chaine = new ArrayList<>(candidats.size());
        for (SelRange candidat : candidats) {
            if (chaine.isEmpty()
                    || chaine.get(chaine.size() - 1).end()
                            - chaine.get(chaine.size() - 1).start()
                    < candidat.end() - candidat.start()) {
                chaine.add(candidat);
            }
        }
        return List.copyOf(chaine);
    }
}
