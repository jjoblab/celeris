package jo.celeris.android.view;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;

/**
 * La sélection en colonne (M5, A-02) : le glissement rectangulaire
 * devient une plage par rangée — les bornes visuelles exactes, colonne
 * de départ à colonne d'arrivée, vivant dans la tranche de chaque
 * rangée (le retour à la ligne souple décale la colonne écran du début
 * de tranche) et bornées à ce que la rangée porte. La ligne courte
 * s'arrête à sa fin, la rangée au-delà donne une plage vide.
 */
public final class ColumnSelection {

    private ColumnSelection() {
    }

    /**
     * Les plages du glissement en colonne : une par rangée traversée.
     *
     * @param doc        le document, jamais muté
     * @param projection la projection courante (rangées et tranches)
     * @param rowA       la rangée de départ du glissement
     * @param colA       la colonne écran de départ
     * @param rowB       la rangée d'arrivée
     * @param colB       la colonne écran d'arrivée
     * @return les plages, de la première rangée à la dernière
     */
    public static MultiSelection ranges(Document doc, VisualProjection projection,
            int rowA, int colA, int rowB, int colB) {
        int premiereRangee = Math.min(rowA, rowB);
        int derniereRangee = Math.max(rowA, rowB);
        int colonneBasse = Math.max(0, Math.min(colA, colB));
        int colonneHaute = Math.max(0, Math.max(colA, colB));
        List<SelRange> plages = new ArrayList<>(derniereRangee - premiereRangee + 1);
        for (int rangee = premiereRangee; rangee <= derniereRangee; rangee++) {
            int ligne = projection.firstLineOfRow(rangee);
            int debutTranche = projection.spanStartAtRow(rangee);
            int finTranche = projection.spanEndAtRow(rangee);
            if (finTranche == Integer.MAX_VALUE) {
                // longueurs inconnues : la tranche est la ligne entière
                finTranche = doc.lineEnd(ligne) - doc.lineStart(ligne);
            }
            int largeur = Math.max(0, finTranche - debutTranche);
            int colonneDebut = debutTranche + Math.min(colonneBasse, largeur);
            int colonneFin = debutTranche + Math.min(colonneHaute, largeur);
            int debutLigne = doc.lineStart(ligne);
            plages.add(new SelRange(debutLigne + colonneDebut,
                    debutLigne + colonneFin));
        }
        return MultiSelection.of(plages);
    }
}
