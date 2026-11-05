package jo.celeris.android.view;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * L'historique de navigation (M5, B-14) : la pile bornée des positions
 * visitées — retour et avance, la visite qui coupe l'avance, la
 * fermeture de fichier qui vide tout. Les positions répétées ne
 * s'empilent pas.
 */
public final class NavigationHistory {

    private final int profondeurMax;

    private final List<Integer> positions = new ArrayList<>();

    private int curseur = -1;

    /**
     * Un historique de profondeur maximale donnée.
     *
     * @param maxDepth la profondeur maximale, au moins 1
     * @throws IllegalArgumentException si {@code maxDepth < 1}
     */
    public NavigationHistory(int maxDepth) {
        if (maxDepth < 1) {
            throw new IllegalArgumentException("maxDepth must be >= 1: " + maxDepth);
        }
        this.profondeurMax = maxDepth;
    }

    /**
     * Enregistre une position visitée : la répétition de la courante ne
     * s'empile pas, toute autre visite coupe l'avance.
     *
     * @param offset la position visitée
     */
    public void visit(int offset) {
        if (curseur >= 0 && positions.get(curseur) == offset) {
            return;
        }
        // la visite coupe l'avance
        while (positions.size() > curseur + 1) {
            positions.remove(positions.size() - 1);
        }
        positions.add(offset);
        if (positions.size() > profondeurMax) {
            positions.remove(0);
        }
        curseur = positions.size() - 1;
    }

    /**
     * Recule d'une position.
     *
     * @return la nouvelle position courante, ou null au fond
     */
    public @Nullable Integer back() {
        if (curseur <= 0) {
            return null;
        }
        curseur--;
        return positions.get(curseur);
    }

    /**
     * Avance d'une position.
     *
     * @return la nouvelle position courante, ou null au sommet
     */
    public @Nullable Integer forward() {
        if (curseur >= positions.size() - 1) {
            return null;
        }
        curseur++;
        return positions.get(curseur);
    }

    /**
     * Vrai s'il existe une position en arrière.
     *
     * @return la disponibilité du retour
     */
    public boolean canBack() {
        return curseur > 0;
    }

    /**
     * Vrai s'il existe une position en avant.
     *
     * @return la disponibilité de l'avance
     */
    public boolean canForward() {
        return curseur < positions.size() - 1;
    }

    /**
     * Le nombre de positions retenues.
     *
     * @return la profondeur courante
     */
    public int profondeur() {
        return positions.size();
    }

    /**
     * La fermeture de fichier vide l'historique : les positions d'un
     * fichier fermé ne se suivent plus (B-14).
     */
    public void fichierFerme() {
        positions.clear();
        curseur = -1;
    }
}
