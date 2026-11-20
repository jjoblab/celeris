package jo.celeris.android.view.internal.gutter;

import jo.celeris.android.view.GutterLanes;
import jo.celeris.android.view.GutterLayout;
import jo.celeris.android.view.WidthClass;

/**
 * Le collaborateur de gouttière de la vue (M7.5, §5) : il porte la
 * respiration (en dp, convertie une fois en pixels à la densité de
 * l'appareil), le niveau d'eau des chiffres du champ de numéros et la
 * configuration de coulisse — puis il rend la largeur STABLE.
 *
 * <p>PIEGE-25 par construction : la largeur se calcule sur les LIGNES
 * du document (jamais les rangées visuelles), le plancher est de trois
 * chiffres, le niveau d'eau croît immédiatement quand une ligne l'exige
 * et ne retombe qu'au changement de session ou de langage, ou sur
 * demande explicite. La coulisse découle de la configuration
 * ({@link GutterLanes}), jamais de l'arrivée d'un résultat.</p>
 */
public final class EditorGutter  {

    private float margeDp = WidthClass.MOYEN.gutterMarginDp();
    private float margePx;
    private int digits = GutterLayout.DIGITS_FLOOR;
    private GutterLanes lanes = GutterLanes.AUCUNE;
    private float currentLane;
    private float currentFoldBand;

    /**
     * Adapte la respiration à la classe de largeur (D-19) : la valeur
     * vit en dp, la conversion se fait une fois, en pixels entiers.
     *
     * @param classe  la classe de largeur courante
     * @param density la densité de l'appareil
     */
    public void setClasse(WidthClass classe, float density) {
        margeDp = classe.gutterMarginDp();
        margePx = Math.round(margeDp * Math.max(0f, density));
    }

    /** La respiration courante, en pixels entiers. */
    public float margePx() {
        return margePx;
    }

    /**
     * Dépose la configuration de coulisse de l'image à venir — les
     * intégrations ACTIVÉES, pas celles qui parlent.
     *
     * @param nouvelles la configuration
     */
    public void setCoulisses(GutterLanes nouvelles) {
        lanes = nouvelles;
    }

    /**
     * Suit le document et rend la largeur de gouttière de l'image :
     * le niveau d'eau des chiffres croît au besoin, jamais ne
     * rétrécit.
     *
     * @param lineCount le nombre de lignes du document
     * @param charWidth la largeur d'un caractère, en pixels
     * @param density   la densité de l'appareil
     * @param numbers   vrai pour réserver le champ de numéros
     * @return la largeur de gouttière, en pixels entiers
     */
    public float suivre(int lineCount, float charWidth, float density,
            boolean numbers) {
        digits = Math.max(digits, GutterLayout.digitsFor(lineCount));
        currentLane = lanes.laneWidth(charWidth, margePx);
        currentFoldBand = lanes.foldStripWidth(charWidth, margePx);
        return numbers
                ? GutterLayout.width(digits, charWidth, density, margeDp,
                        lanes)
                : GutterLayout.laneOnly(density, margeDp, charWidth, lanes);
    }

    /** La largeur de coulisse réservée, en pixels (0 sans coulisse). */
    public float currentLane() {
        return currentLane;
    }

    /** La largeur de la bande de plis réservée, en pixels (0 sans syntaxe). */
    public float currentFoldBand() {
        return currentFoldBand;
    }

    /** Retombe au plancher : changement de session ou de langage. */
    public void reinitialiser() {
        digits = GutterLayout.DIGITS_FLOOR;
    }
}
