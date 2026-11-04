package jo.celeris.android.view;

/**
 * Métriques d'éditeur dérivées d'une spécification de police et d'une
 * règle injectée (ADR-008 : mesure injectée, jamais lue directement).
 *
 * <p>PIEGE-05 : les métriques de police du dépôt de départ étaient
 * recalculées à chaque changement de taille, invalidant les valeurs
 * injectées dans les tests. Ici, les métriques sont immuables et liées à
 * leur spécification : un changement de taille produit une nouvelle
 * instance par reconsultation de la règle — jamais une mutation en place
 * dont l'état d'avant survivrait. Après tout changement de taille, la
 * discipline est de redériver (et de réinjecter en test).</p>
 */
public final class EditorMetrics {

    private final FontSpec font;
    private final float charWidth;
    private final float lineHeight;
    private final float ascent;
    private final float descent;

    private EditorMetrics(FontSpec font, float charWidth, float lineHeight,
            float ascent, float descent) {
        this.font = font;
        this.charWidth = charWidth;
        this.lineHeight = lineHeight;
        this.ascent = ascent;
        this.descent = descent;
    }

    /**
     * Dérive les métriques par consultation de la règle pour la
     * spécification donnée.
     *
     * @param font  la spécification de police
     * @param ruler la règle injectée
     * @return les métriques dérivées
     */
    public static EditorMetrics of(FontSpec font, FontRuler ruler) {
        return new EditorMetrics(font, ruler.charWidth(font), ruler.lineHeight(font),
                ruler.ascent(font), ruler.descent(font));
    }

    /** La spécification de police dont ces métriques dérivent. */
    public FontSpec font() {
        return font;
    }

    /** La largeur d'un caractère, en pixels. */
    public float charWidth() {
        return charWidth;
    }

    /** La hauteur de ligne, en pixels. */
    public float lineHeight() {
        return lineHeight;
    }

    /** L'ascendance : distance au-dessus de la ligne de base. */
    public float ascent() {
        return ascent;
    }

    /** La descendance : distance sous la ligne de base. */
    public float descent() {
        return descent;
    }

    /**
     * Le bord supérieur de la rangée donnée.
     *
     * @param row le numéro de rangée visuelle
     * @return l'ordonnée du haut de la rangée, en pixels
     */
    public float rowTop(int row) {
        return row * lineHeight;
    }

    /**
     * La ligne de base de la rangée donnée.
     *
     * @param row le numéro de rangée visuelle
     * @return l'ordonnée de la ligne de base, en pixels
     */
    public float rowBaseline(int row) {
        return row * lineHeight + ascent;
    }
}
