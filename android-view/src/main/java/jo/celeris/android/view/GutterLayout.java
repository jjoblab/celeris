package jo.celeris.android.view;

/**
 * La géométrie stable de la gouttière (M7.5, §5) : la largeur du champ
 * de numéros se calcule sur les LIGNES du document — jamais sur les
 * rangées visuelles (retour à la ligne, plis) — avec un plancher de
 * chiffres, des marges en dp converties une fois en pixels entiers et
 * une coulisse qui découle de la configuration, pas des résultats.
 *
 * <p>PIEGE-25 par construction : la gouttière ne dépend jamais de la
 * projection visuelle ni d'un état d'analyse. Le nombre de chiffres
 * croît immédiatement quand une ligne l'exige ; le rétrécissement
 * n'arrive qu'au plus haut niveau d'eau vu depuis le dernier
 * changement de session ou de langage — l'appelant porte ce niveau
 * d'eau, la géométrie reste pure.</p>
 */
public final class GutterLayout {

    /** Le plancher de chiffres : trois au moins, la dixième ligne ne décale rien. */
    public static final int DIGITS_FLOOR = 3;

    private GutterLayout() {
    }

    /**
     * Le nombre de chiffres du champ pour un compte de lignes.
     *
     * @param lineCount le nombre de lignes du document
     * @return le nombre de chiffres, au moins le plancher
     */
    public static int digitsFor(int lineCount) {
        return Math.max(DIGITS_FLOOR,
                GutterGeometry.digits(Math.max(1, lineCount)));
    }

    /**
     * La largeur de la gouttière avec numéros.
     *
     * @param digits    le nombre de chiffres du champ (niveau d'eau compris)
     * @param charWidth la largeur d'un caractère, en pixels
     * @param density   la densité de l'appareil
     * @param paddingDp la respiration de chaque côté, en dp
     * @param lanes     la configuration de coulisse
     * @return la largeur totale, en pixels entiers
     */
    public static int width(int digits, float charWidth, float density,
            float paddingDp, GutterLanes lanes) {
        int marge = paddingPx(density, paddingDp);
        return Math.round(digits * charWidth + 2f * marge
                + lanes.laneWidth(charWidth, marge));
    }

    /**
     * La largeur de la gouttière sans numéros : la coulisse et sa
     * respiration, rien d'autre (D-01).
     *
     * @param density   la densité de l'appareil
     * @param paddingDp la respiration, en dp
     * @param charWidth la largeur d'un caractère, en pixels
     * @param lanes     la configuration de coulisse
     * @return la largeur totale, en pixels entiers
     */
    public static int laneOnly(float density, float paddingDp,
            float charWidth, GutterLanes lanes) {
        int marge = paddingPx(density, paddingDp);
        return Math.round(2f * marge + lanes.laneWidth(charWidth, marge));
    }

    /** La conversion unique de la respiration : dp vers pixels entiers. */
    private static int paddingPx(float density, float paddingDp) {
        return Math.round(paddingDp * Math.max(0f, density));
    }
}
