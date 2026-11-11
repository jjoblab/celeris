package jo.celeris.android.widgets;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.core.text.Document;

/**
 * La minimap (chantier C9) : la silhouette du document — chaque rangée
 * de la carte porte la charge de son bouquet de lignes, la longueur
 * de la plus longue ligne plafonnée. Pur et testable sans SDK : la
 * vue la possède, le peintre la lit ; la densité est bornée par
 * construction — une rangée par cran, jamais plus.
 *
 * <p>Le bouquet d'une rangée couvre les lignes du document au prorata
 * : la rangée {@code r} d'une carte de {@code n} rangees couvre les
 * lignes de {@code r * lignes / n} à {@code (r + 1) * lignes / n - 1}
 * — chaque ligne compte une fois, la ligne orpheline d'une division
 * arrondie tombe dans la rangée qui la porte.</p>
 */
public final class MinimapModel {

    /** Le plafond de charge d'une rangée — au-delà, le trait sature. */
    public static final int CHARGE_MAX = 120;

    /** Une rangée de la carte : son cran, la charge de son bouquet. */
    public record MapRow(int slot, int load) {
    }

    private final List<MapRow> rangees;

    private MinimapModel(List<MapRow> rangees) {
        this.rangees = List.copyOf(rangees);
    }

    /**
     * Construit la silhouette d'un document.
     *
     * @param doc     le document porteur
     * @param hauteur la hauteur de la carte en rangees
     * @return la carte, une rangée par cran
     */
    public static MinimapModel of(Document doc, int hauteur) {
        int crans = Math.max(1, hauteur);
        int lignes = Math.max(1, doc.lineCount());
        int[] charges = new int[crans];
        for (int ligne = 0; ligne < doc.lineCount(); ligne++) {
            int cran = cranDeLigne(ligne, lignes, crans);
            String texte = doc.lineText(ligne);
            int charge = Math.min(CHARGE_MAX, texte.stripTrailing().length());
            charges[cran] = Math.max(charges[cran], charge);
        }
        List<MapRow> carte = new ArrayList<>(crans);
        for (int cran = 0; cran < crans; cran++) {
            carte.add(new MapRow(cran, charges[cran]));
        }
        return new MinimapModel(carte);
    }

    /**
     * Le cran d'une ligne : sa rangée dans une carte de {@code crans}
     * rangees pour {@code lignes} lignes — la dernière ligne tombe
     * toujours dans le dernier cran.
     *
     * @param ligne la ligne du document
     * @param lignes le nombre de lignes du document
     * @param crans le nombre de rangees de la carte
     * @return le cran de la ligne, dans les bornes
     */
    public static int cranDeLigne(int ligne, int lignes, int crans) {
        int cran = (int) ((long) ligne * crans / lignes);
        return Math.max(0, Math.min(crans - 1, cran));
    }

    /** Les rangees de la carte, dans l'ordre des crans. */
    public List<MapRow> rangees() {
        return rangees;
    }

    /** Le nombre de rangees de la carte. */
    public int taille() {
        return rangees.size();
    }

    /**
     * La ligne visée par un cran — la premiere ligne de son bouquet :
     * le saut du toucher retombe sur une vraie ligne du document.
     *
     * @param cran  le cran visé
     * @param lignes le nombre de lignes du document
     * @return la premiere ligne du bouquet du cran
     */
    public int ligneDeCran(int cran, int lignes) {
        int borne = Math.max(0, Math.min(rangees.size() - 1, cran));
        return (int) ((long) borne * lignes / Math.max(1, rangees.size()));
    }
}
