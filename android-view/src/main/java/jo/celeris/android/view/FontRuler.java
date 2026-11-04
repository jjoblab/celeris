package jo.celeris.android.view;

/**
 * Règle de police : la mesure injectée par l'hôte (ADR-008).
 *
 * <p>L'adaptateur concret vit côté vue Android ; les modèles de la vue
 * restent purs et déterministes en test grâce à cette injection — la
 * mesure de police n'est jamais lue directement.</p>
 */
public interface FontRuler {

    /**
     * Largeur rendue d'un caractère de la police spécifiée.
     *
     * @param spec la spécification de police
     * @return la largeur en pixels
     */
    float charWidth(FontSpec spec);

    /**
     * Hauteur de ligne de la police spécifiée.
     *
     * @param spec la spécification de police
     * @return la hauteur en pixels
     */
    float lineHeight(FontSpec spec);

    /**
     * Ascendance : la distance au-dessus de la ligne de base.
     *
     * @param spec la spécification de police
     * @return l'ascendance en pixels
     */
    float ascent(FontSpec spec);

    /**
     * Descendance : la distance sous la ligne de base.
     *
     * @param spec la spécification de police
     * @return la descendance en pixels
     */
    float descent(FontSpec spec);
}
