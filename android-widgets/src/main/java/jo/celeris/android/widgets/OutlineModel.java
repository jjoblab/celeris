package jo.celeris.android.widgets;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import jo.celeris.core.syntax.structure.OutlineEntry;

import org.jspecify.annotations.Nullable;

/**
 * Modèle du plan consommateur (B-16) : les entrées sortent du même arbre
 * que la coloration (l'hôte les reçoit du pont session ↔ vue), la
 * profondeur d'affichage se déduit de l'imbrication des bornes — une
 * entrée vit dans la première entrée ouverte qui la contient — et la
 * mise à jour est incrémentale : le préfixe intact d'un plan à l'autre
 * se conserve, le consommateur reçoit le compte de ce qui change.
 *
 * <p>Pur et testable sans SDK : la vue le possède, le peintre le lit.</p>
 */
public final class OutlineModel {

    /** Une rangée du plan : l'entrée et sa profondeur d'affichage. */
    public record Row(OutlineEntry input, int profondeur) {
    }

    private List<Row> lines = List.of();

    /**
     * Applique un nouveau plan : le préfixe identique se conserve, le
     * reste remplace.
     *
     * @param entrees les entrées du plan, dans l'ordre du source
     * @return le changement — préfixe conservé, remplacées, ajoutées
     */
    public OutlineChange appliquer(List<OutlineEntry> entrees) {
        int ancien = lines.size();
        int prefixe = 0;
        int borne = Math.min(ancien, entrees.size());
        while (prefixe < borne
                && lines.get(prefixe).input().equals(entrees.get(prefixe))) {
            prefixe++;
        }
        lines = avecProfondeurs(entrees);
        return new OutlineChange(prefixe, ancien - prefixe,
                entrees.size() - prefixe);
    }

    /** Les rangées du plan, immuables. */
    public List<Row> lines() {
        return lines;
    }

    /** Le nombre d'entrées du plan. */
    public int nombre() {
        return lines.size();
    }

    /**
     * L'entrée à l'ordonnée donnée : la rangée visée par un appui.
     *
     * @param y            l'ordonnée de l'appui, en pixels
     * @param hauteurLigne la hauteur d'une rangée, en pixels
     * @return l'entrée visée, null hors bornes
     */
    public @Nullable OutlineEntry auPoint(float y, float hauteurLigne) {
        if (hauteurLigne <= 0f || lines.isEmpty()) {
            return null;
        }
        int indice = (int) (y / hauteurLigne);
        if (indice < 0 || indice >= lines.size()) {
            return null;
        }
        return lines.get(indice).input();
    }

    /**
     * Les profondeurs d'affichage par imbrication : chaque entrée vit
     * dans la première entrée précédente qui la contient.
     */
    private static List<Row> avecProfondeurs(List<OutlineEntry> entrees) {
        List<Row> result = new ArrayList<>(entrees.size());
        Deque<OutlineEntry> ouverts = new ArrayDeque<>();
        for (OutlineEntry input : entrees) {
            while (!ouverts.isEmpty() && !contient(ouverts.peek(), input)) {
                ouverts.pop();
            }
            result.add(new Row(input, ouverts.size()));
            ouverts.push(input);
        }
        return List.copyOf(result);
    }

    /** Vrai quand l'ancêtre couvre l'enfant, bornes strictement distinctes. */
    private static boolean contient(OutlineEntry ancetre, OutlineEntry enfant) {
        boolean couvre = ancetre.startOffset() <= enfant.startOffset()
                && enfant.endOffset() <= ancetre.endOffset();
        boolean confondues = ancetre.startOffset() == enfant.startOffset()
                && ancetre.endOffset() == enfant.endOffset();
        return couvre && !confondues;
    }
}
