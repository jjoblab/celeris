package jo.celeris.android.view;

import jo.celeris.core.text.Document;

/**
 * Géométrie des curseurs : la conversion offset → position suit les
 * rangées visuelles (A-18 — sous coupures, la position vit sur SA
 * tranche), le miroir exact de la visée {@link HitTest}.
 */
public final class CaretGeometry {

    private CaretGeometry() {
    }

    /**
     * La rangée visuelle qui porte l'offset donné : la rangée de sa
     * ligne plus l'indice de sa tranche.
     *
     * @param document   l'instantané de document affiché
     * @param projection la projection rangées ↔ lignes
     * @param offset     l'offset de caractère
     * @return le numéro de rangée visuelle
     */
    public static int rowOfOffset(Document document, VisualProjection projection,
            int offset) {
        int ligne = document.lineForOffset(offset);
        int colonne = offset - document.lineStart(ligne);
        int colonnes = projection.columns();
        int tranche = colonnes > 0 ? colonne / colonnes : 0;
        return projection.rowOfLine(ligne) + tranche;
    }

    /**
     * L'abscisse du curseur à l'offset donné : la colonne DANS la
     * tranche, depuis le bord gauche du texte, défilement déduit.
     *
     * @param document   l'instantané de document affiché
     * @param projection la projection rangées ↔ lignes
     * @param metrics    les métriques d'éditeur courantes
     * @param gutterWidth la largeur de gouttière, en pixels
     * @param scrollX    le défilement horizontal, en pixels
     * @param offset     l'offset de caractère
     * @return l'abscisse du curseur, en pixels
     */
    public static float xOfOffset(Document document, VisualProjection projection,
            EditorMetrics metrics, float gutterWidth, float scrollX, int offset) {
        int ligne = document.lineForOffset(offset);
        int colonne = offset - document.lineStart(ligne);
        int colonnes = projection.columns();
        int dansTranche = colonnes > 0 ? colonne % colonnes : colonne;
        return gutterWidth + dansTranche * metrics.charWidth() - scrollX;
    }

    /**
     * L'ordonnée du haut de la rangée qui porte l'offset donné,
     * défilement déduit.
     *
     * @param projection la projection rangées ↔ lignes
     * @param metrics    les métriques d'éditeur courantes
     * @param scrollY    le défilement vertical, en pixels
     * @param offset     l'offset de caractère (borné au document par la
     *                   projection)
     * @return l'ordonnée du haut de la rangée, en pixels
     */
    public static float yOfOffset(Document document, VisualProjection projection,
            EditorMetrics metrics, float scrollY, int offset) {
        int rangee = rowOfOffset(document, projection, offset);
        return metrics.rowTop(rangee) - scrollY;
    }
}
