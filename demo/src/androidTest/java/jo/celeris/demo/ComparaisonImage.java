package jo.celeris.demo;

import android.graphics.Bitmap;
import android.graphics.Rect;

import java.util.HashMap;
import java.util.Map;

/**
 * La comparaison des captures du second étage (C11) : la couleur
 * dominante d'une bande (l'arrière-plan, l'encre reste minoritaire),
 * l'abscisse de la première encre, le décalage horizontal qui
 * aligne le mieux deux bandes, la fraction de pixels fortement
 * différents — tout en échantillonnage, pour rester rapide sur la
 * vitre de l'appareil.
 */
final class ComparaisonImage {

    /** Un pixel tranche quand la somme de ses canaux bouge d'autant. */
    static final int SEUIL_FORT = 48;

    private ComparaisonImage() {
    }

    /** Le coût d'un pixel contre un autre : la somme des trois canaux. */
    static int cout(int premier, int second) {
        int cout = Math.abs(((premier >> 16) & 0xFF) - ((second >> 16) & 0xFF));
        cout += Math.abs(((premier >> 8) & 0xFF) - ((second >> 8) & 0xFF));
        cout += Math.abs((premier & 0xFF) - (second & 0xFF));
        return cout;
    }

    /**
     * La couleur dominante de la bande : l'arrière-plan de l'éditeur
     * couvre la surface, l'encre des glyphes reste minoritaire.
     */
    static int couleurDominante(Bitmap image, Rect bande) {
        Map<Integer, Integer> compteurs = new HashMap<>();
        for (int y = bande.top + 2; y < bande.bottom - 2; y += 3) {
            for (int x = bande.left + 2; x < bande.right - 2; x += 3) {
                compteurs.merge(image.getPixel(x, y), 1, Integer::sum);
            }
        }
        int dominante = image.getPixel(bande.left + 2, bande.top + 2);
        int meilleur = 0;
        for (Map.Entry<Integer, Integer> paire : compteurs.entrySet()) {
            if (paire.getValue() > meilleur) {
                meilleur = paire.getValue();
                dominante = paire.getKey();
            }
        }
        return dominante;
    }

    /**
     * L'abscisse de la première encre de la bande — le numéro de la
     * gouttière ou le premier caractère : tout ce qui bouge quand la
     * gouttière bouge. Le retour vaut -1 si la bande ne porte aucune
     * encre.
     */
    static int abscissePremiereEncre(Bitmap image, Rect bande) {
        int fond = couleurDominante(image, bande);
        for (int x = bande.left + 2; x < bande.right - 2; x++) {
            for (int y = bande.top + 2; y < bande.bottom - 2; y += 2) {
                if (cout(image.getPixel(x, y), fond) >= SEUIL_FORT) {
                    return x;
                }
            }
        }
        return -1;
    }

    /**
     * Le décalage horizontal qui aligne le mieux la bande de la
     * deuxième image sur la première : le contenu qui bouge se
     * mesure en pixels, quelle que soit la couleur qui le porte.
     */
    static int decalageMinimal(Bitmap premiere, Bitmap seconde, Rect bande,
            int rayon) {
        int marge = rayon + 4;
        int gauche = bande.left + marge;
        int droit = bande.right - marge;
        long meilleure = Long.MAX_VALUE;
        int meilleurDecalage = 0;
        for (int essai = -rayon; essai <= rayon; essai++) {
            long coutTotal = 0L;
            for (int y = bande.top + 4; y < bande.bottom - 4; y += 2) {
                for (int x = gauche; x < droit; x += 2) {
                    coutTotal += cout(premiere.getPixel(x, y),
                            seconde.getPixel(x + essai, y));
                }
            }
            if (coutTotal < meilleure) {
                meilleure = coutTotal;
                meilleurDecalage = essai;
            }
        }
        return meilleurDecalage;
    }

    /**
     * La fraction, en millièmes, de pixels fortement différents entre
     * deux images sur une bande : la décélération d'un mot se voit
     * cent pour cent, l'anticrénelage stable reste sous le millième.
     */
    static int fractionForte(Bitmap premiere, Bitmap seconde, Rect bande) {
        int differents = 0;
        int total = 0;
        for (int y = bande.top + 2; y < bande.bottom - 2; y += 2) {
            for (int x = bande.left + 2; x < bande.right - 2; x += 2) {
                if (cout(premiere.getPixel(x, y),
                        seconde.getPixel(x, y)) >= SEUIL_FORT) {
                    differents++;
                }
                total++;
            }
        }
        return Math.round((differents * 1000L) / Math.max(1, total));
    }
}
