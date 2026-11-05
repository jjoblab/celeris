package jo.celeris.android.widgets;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import jo.celeris.core.syntax.OutlineEntry;

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
    public record Ligne(OutlineEntry entree, int profondeur) {
    }

    private List<Ligne> lignes = List.of();

    /**
     * Applique un nouveau plan : le préfixe identique se conserve, le
     * reste remplace.
     *
     * @param entrees les entrées du plan, dans l'ordre du source
     * @return le changement — préfixe conservé, remplacées, ajoutées
     */
    public OutlineChange appliquer(List<OutlineEntry> entrees) {
        int ancien = lignes.size();
        int prefixe = 0;
        int borne = Math.min(ancien, entrees.size());
        while (prefixe < borne
                && lignes.get(prefixe).entree().equals(entrees.get(prefixe))) {
            prefixe++;
        }
        lignes = avecProfondeurs(entrees);
        return new OutlineChange(prefixe, ancien - prefixe,
                entrees.size() - prefixe);
    }

    /** Les rangées du plan, immuables. */
    public List<Ligne> lignes() {
        return lignes;
    }

    /** Le nombre d'entrées du plan. */
    public int nombre() {
        return lignes.size();
    }

    /**
     * L'entrée à l'ordonnée donnée : la rangée visée par un appui.
     *
     * @param y            l'ordonnée de l'appui, en pixels
     * @param hauteurLigne la hauteur d'une rangée, en pixels
     * @return l'entrée visée, null hors bornes
     */
    public @Nullable OutlineEntry auPoint(float y, float hauteurLigne) {
        if (hauteurLigne <= 0f || lignes.isEmpty()) {
            return null;
        }
        int indice = (int) (y / hauteurLigne);
        if (indice < 0 || indice >= lignes.size()) {
            return null;
        }
        return lignes.get(indice).entree();
    }

    /**
     * Les profondeurs d'affichage par imbrication : chaque entrée vit
     * dans la première entrée précédente qui la contient.
     */
    private static List<Ligne> avecProfondeurs(List<OutlineEntry> entrees) {
        List<Ligne> result = new ArrayList<>(entrees.size());
        Deque<OutlineEntry> ouverts = new ArrayDeque<>();
        for (OutlineEntry entree : entrees) {
            while (!ouverts.isEmpty() && !contient(ouverts.peek(), entree)) {
                ouverts.pop();
            }
            result.add(new Ligne(entree, ouverts.size()));
            ouverts.push(entree);
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
