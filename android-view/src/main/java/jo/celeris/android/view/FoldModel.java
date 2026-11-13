package jo.celeris.android.view;

import java.util.ArrayList;
import java.util.List;

/**
 * Modèle de plis : plages de lignes repliées sous une ligne d'en-tête,
 * triées par début, sans chevauchement.
 *
 * <p>Le pli {@code (début, fin)} garde la ligne d'en-tête visible et
 * cache les lignes {@code début+1} à {@code fin} ; seuls les plis
 * strictement croissants sont admis, et tout chevauchement avec un pli
 * existant est rejeté.</p>
 */
public final class FoldModel {

    /** Un pli : la ligne d'en-tête et la dernière ligne cachée. */
    record Pli(int debut, int fin) {
    }

    private final List<Pli> plis = new ArrayList<>();

    /**
     * Replie les lignes après l'en-tête donné.
     *
     * @param startLine la ligne d'en-tête, qui reste visible
     * @param endLine   la dernière ligne cachée
     * @throws IllegalArgumentException si la plage est vide ou inversée
     * @throws IllegalStateException    si elle chevauche un pli existant
     */
    public void fold(int startLine, int endLine) {
        if (endLine <= startLine) {
            throw new IllegalArgumentException(
                    "fold span must be increasing: " + startLine + ".." + endLine);
        }
        for (Pli pli : plis) {
            if (startLine <= pli.fin() && pli.debut() <= endLine) {
                throw new IllegalStateException(
                        "fold " + startLine + ".." + endLine + " overlaps " + pli);
            }
        }
        int rang = 0;
        while (rang < plis.size() && plis.get(rang).debut() < startLine) {
            rang++;
        }
        plis.add(rang, new Pli(startLine, endLine));
    }

    /**
     * Déplie le pli porté par l'en-tête donné.
     *
     * @param startLine la ligne d'en-tête du pli à déplier
     * @throws IllegalStateException si aucun pli ne porte cet en-tête
     */
    public void unfold(int startLine) {
        for (int i = 0; i < plis.size(); i++) {
            if (plis.get(i).debut() == startLine) {
                plis.remove(i);
                return;
            }
        }
        throw new IllegalStateException("no fold at header line " + startLine);
    }

    /**
     * Vrai si la ligne d'en-tête donnée porte un pli.
     *
     * @param startLine la ligne d'en-tête à interroger
     * @return vrai si un pli y est replié
     */
    public boolean isFolded(int startLine) {
        for (Pli pli : plis) {
            if (pli.debut() == startLine) {
                return true;
            }
        }
        return false;
    }

    /**
     * La dernière ligne cachée par le pli de l'en-tête donné.
     *
     * @param startLine la ligne d'en-tête à interroger
     * @return sa dernière ligne cachée, —1 si aucun pli ne la porte
     */
    public int finDe(int startLine) {
        for (Pli pli : plis) {
            if (pli.debut() == startLine) {
                return pli.fin();
            }
        }
        return -1;
    }

    /** Les plis triés par début, pour la projection visuelle. */
    List<Pli> plis() {
        return plis;
    }
}
