package jo.celeris.android.view;

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
final class EditorGutter {

    private float margeDp = ClasseLargeur.MOYEN.margeGouttiereDp();
    private float margePx;
    private int chiffres = GutterLayout.DIGITS_FLOOR;
    private GutterLanes coulisses = GutterLanes.AUCUNE;
    private float coulisseCourante;
    private float bandePliCourante;

    /**
     * Adapte la respiration à la classe de largeur (D-19) : la valeur
     * vit en dp, la conversion se fait une fois, en pixels entiers.
     *
     * @param classe  la classe de largeur courante
     * @param density la densité de l'appareil
     */
    void setClasse(ClasseLargeur classe, float density) {
        margeDp = classe.margeGouttiereDp();
        margePx = Math.round(margeDp * Math.max(0f, density));
    }

    /** La respiration courante, en pixels entiers. */
    float margePx() {
        return margePx;
    }

    /**
     * Dépose la configuration de coulisse de l'image à venir — les
     * intégrations ACTIVÉES, pas celles qui parlent.
     *
     * @param nouvelles la configuration
     */
    void setCoulisses(GutterLanes nouvelles) {
        coulisses = nouvelles;
    }

    /**
     * Suit le document et rend la largeur de gouttière de l'image :
     * le niveau d'eau des chiffres croît au besoin, jamais ne
     * rétrécit.
     *
     * @param lineCount le nombre de lignes du document
     * @param charWidth la largeur d'un caractère, en pixels
     * @param density   la densité de l'appareil
     * @param numeros   vrai pour réserver le champ de numéros
     * @return la largeur de gouttière, en pixels entiers
     */
    float suivre(int lineCount, float charWidth, float density,
            boolean numeros) {
        chiffres = Math.max(chiffres, GutterLayout.digitsFor(lineCount));
        coulisseCourante = coulisses.laneWidth(charWidth, margePx);
        bandePliCourante = coulisses.foldStripWidth(charWidth, margePx);
        return numeros
                ? GutterLayout.width(chiffres, charWidth, density, margeDp,
                        coulisses)
                : GutterLayout.laneOnly(density, margeDp, charWidth, coulisses);
    }

    /** La largeur de coulisse réservée, en pixels (0 sans coulisse). */
    float coulisseCourante() {
        return coulisseCourante;
    }

    /** La largeur de la bande de plis réservée, en pixels (0 sans syntaxe). */
    float bandePliCourante() {
        return bandePliCourante;
    }

    /** Retombe au plancher : changement de session ou de langage. */
    void reinitialiser() {
        chiffres = GutterLayout.DIGITS_FLOOR;
    }
}
