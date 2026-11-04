package jo.celeris.android.view;

/**
 * Géométrie de la gouttière : largeur du champ de numéros et alignement
 * à droite des numéros (D-01 — numéros absolus, alignement par rangée).
 *
 * <p>Calculs purs en primitives, appelés à chaque image (PIEGE-15) :
 * aucune allocation.</p>
 */
public final class GutterGeometry {

    private static final int BASE = 10;

    private GutterGeometry() {
    }

    /**
     * Le nombre de chiffres du numéro donné.
     *
     * @param number le numéro de ligne, ≥ 1
     * @return le nombre de chiffres (1 pour 1 à 9, 2 pour 10 à 99…)
     */
    public static int digits(int number) {
        int chiffres = 1;
        while (number >= BASE) {
            number /= BASE;
            chiffres++;
        }
        return chiffres;
    }

    /**
     * La largeur de la gouttière pour le plus grand numéro à afficher.
     *
     * @param maxNumber le plus grand numéro de ligne affichable
     * @param charWidth la largeur d'un caractère, en pixels
     * @param padding   la respiration de chaque côté du champ, en pixels
     * @return la largeur totale de la gouttière, en pixels
     */
    public static float width(int maxNumber, float charWidth, float padding) {
        return digits(maxNumber) * charWidth + 2f * padding;
    }

    /**
     * L'abscisse du numéro pour l'aligner à droite dans la gouttière.
     *
     * @param labelLength le nombre de caractères du numéro affiché
     * @param gutterWidth la largeur totale de la gouttière
     * @param charWidth   la largeur d'un caractère, en pixels
     * @param padding     la respiration à droite du champ, en pixels
     * @return l'abscisse de départ du numéro
     */
    public static float numberX(int labelLength, float gutterWidth, float charWidth,
            float padding) {
        return gutterWidth - padding - labelLength * charWidth;
    }
}
