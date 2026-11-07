package jo.celeris.android.view;

/**
 * Géométrie du défilement : bornage du décalage et fenêtre de rangées
 * visibles (logique pure, testée sans SDK).
 *
 * <p>Le décalage ne sort jamais du contenu : le contenu plus court que
 * la fenêtre ne défile pas (décalage nul). La fenêtre visible couvre
 * les rangées entières plus la rangée entamée en chaque bord — une de
 * marge en régime établi (PIEGE-15 : calculs en primitives).</p>
 */
public final class ScrollGeometry {

    private ScrollGeometry() {
    }

    /**
     * Borné le décalage au contenu : jamais négatif, jamais au-delà de
     * contenu moins fenêtre.
     *
     * @param value           le décalage demandé, en pixels
     * @param contentExtent   l'étendue du contenu, en pixels
     * @param viewportExtent  l'étendue de la fenêtre, en pixels
     * @return le décalage borné à [0, max(0, contenu − fenêtre)]
     */
    public static float clamp(float value, float contentExtent, float viewportExtent) {
        float maximum = Math.max(0f, contentExtent - viewportExtent);
        return Math.max(0f, Math.min(value, maximum));
    }

    /**
     * Le décalage maximal possible.
     *
     * @param contentExtent  l'étendue du contenu, en pixels
     * @param viewportExtent l'étendue de la fenêtre, en pixels
     * @return max(0, contenu − fenêtre)
     */
    public static float maxScroll(float contentExtent, float viewportExtent) {
        return Math.max(0f, contentExtent - viewportExtent);
    }

    /**
     * La première rangée visible pour un décalage vertical donné.
     *
     * @param scrollY    le décalage vertical, en pixels
     * @param lineHeight la hauteur de rangée, en pixels
     * @return l'indice de la première rangée visible, ≥ 0
     */
    public static int firstRow(float scrollY, float lineHeight) {
        if (scrollY <= 0f) {
            return 0;
        }
        return (int) Math.floor(scrollY / lineHeight);
    }

    /**
     * Le nombre de rangées à tracer pour couvrir la fenêtre, rangées
     * entamées comprises.
     *
     * @param viewportHeight la hauteur de la fenêtre, en pixels
     * @param lineHeight     la hauteur de rangée, en pixels
     * @return le nombre de rangées visibles, ≥ 1
     */
    public static int visibleRows(float viewportHeight, float lineHeight) {
        return (int) Math.ceil(viewportHeight / lineHeight) + 1;
    }

    /**
     * Le nombre de rangées à tracer, borné au contenu : la fenêtre ne
     * descend jamais sous la dernière rangée — sans ce bornage, les
     * rangées au-delà du contenu projettent la dernière ligne sur des
     * tranches croissantes (répétition du contenu sans coupure,
     * hors bornes sous coupure).
     *
     * @param firstRow la première rangée visible, ≥ 0
     * @param visibles le nombre de rangées couvrant la fenêtre
     * @param rowCount le nombre de rangées du contenu, ≥ 0
     * @return le nombre de rangées à tracer, ≥ 0
     */
    public static int borneesAuContenu(int firstRow, int visibles, int rowCount) {
        int premiere = Math.max(0, Math.min(firstRow, rowCount));
        return Math.max(0, Math.min(visibles, rowCount - premiere));
    }

    /**
     * L'étendue horizontale du contenu : la plus large rangée, en
     * pixels — bornée au budget de colonnes sous coupures (A-18).
     *
     * @param projection la projection rangées ↔ lignes courante
     * @param metrics    les métriques d'éditeur courantes
     * @return la largeur du contenu, en pixels
     */
    public static float largeurContenu(VisualProjection projection,
            EditorMetrics metrics) {
        int portee = projection.columns() <= 0
                ? projection.longestLineLength()
                : Math.min(projection.longestLineLength(), projection.columns());
        return portee * metrics.charWidth();
    }

    /**
     * La largeur visible du contenu, gouttière déduite.
     *
     * @param viewportWidth la largeur de la fenêtre, en pixels
     * @param gutterWidth   la largeur de gouttière, en pixels
     * @return la largeur visible, jamais négative
     */
    public static float largeurVisible(float viewportWidth, float gutterWidth) {
        return Math.max(0f, viewportWidth - gutterWidth);
    }
}
