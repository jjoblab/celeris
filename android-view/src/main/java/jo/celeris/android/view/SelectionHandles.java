package jo.celeris.android.view;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.SelRange;

/**
 * Poignées de sélection : la visée désigne l'extrémité touchée (début,
 * fin ou aucune) et le glissement ne bouge que son extrémité — les
 * positions suivent les tranches de rangée (A-18).
 */
public final class SelectionHandles {

    /** Le rayon de visée d'une poignée, en pixels. */
    public static final float HIT_RADIUS = 24f;

    /** Une extrémité de plage : début, fin, ou aucune. */
    public enum HandleId {
        START, END, NONE
    }

    private SelectionHandles() {
    }

    /**
     * L'abscisse de l'extrémité à l'offset donné (colonne dans la
     * tranche, défilement déduit).
     *
     * @param document    l'instantané de document affiché
     * @param projection  la projection rangées ↔ lignes
     * @param metrics     les métriques d'éditeur courantes
     * @param gutterWidth la largeur de gouttière, en pixels
     * @param scrollX     le défilement horizontal, en pixels
     * @param offset      l'offset de l'extrémité
     * @return l'abscisse de la poignée, en pixels
     */
    public static float xOfOffset(Document document, VisualProjection projection,
            EditorMetrics metrics, float gutterWidth, float scrollX, int offset) {
        return CaretGeometry.xOfOffset(document, projection, metrics, gutterWidth,
                scrollX, offset);
    }

    /**
     * L'ordonnée du haut de la rangée de l'extrémité.
     *
     * @param document   l'instantané de document affiché
     * @param projection la projection rangées ↔ lignes
     * @param metrics    les métriques d'éditeur courantes
     * @param scrollY    le défilement vertical, en pixels
     * @param offset     l'offset de l'extrémité
     * @return l'ordonnée de la poignée, en pixels
     */
    public static float yOfOffset(Document document, VisualProjection projection,
            EditorMetrics metrics, float scrollY, int offset) {
        return CaretGeometry.yOfOffset(document, projection, metrics, scrollY, offset);
    }

    /**
     * La poignée touchée par le point donné : la plus proche des deux
     * extrémités à portée de visée, aucune entre les deux.
     *
     * @param px          l'abscisse du doigt, en pixels
     * @param py          l'ordonnée du doigt, en pixels
     * @param gutterWidth la largeur de gouttière, en pixels
     * @param scrollX     le défilement horizontal, en pixels
     * @param scrollY     le défilement vertical, en pixels
     * @param document    l'instantané de document affiché
     * @param projection  la projection rangées ↔ lignes
     * @param metrics     les métriques d'éditeur courantes
     * @param start       l'offset de début de la plage
     * @param end         l'offset de fin de la plage
     * @return l'extrémité touchée, NONE si aucune
     */
    public static HandleId hitTest(float px, float py, float gutterWidth,
            float scrollX, float scrollY, Document document,
            VisualProjection projection, EditorMetrics metrics, int start, int end) {
        boolean debut = aPortee(px, py, gutterWidth, scrollX, scrollY, document,
                projection, metrics, start);
        boolean fin = aPortee(px, py, gutterWidth, scrollX, scrollY, document,
                projection, metrics, end);
        if (debut && fin) {
            // Les deux à portée : la plus proche gagne.
            float dxDebut = Math.abs(px - xOfOffset(document, projection, metrics,
                    gutterWidth, scrollX, start));
            float dxFin = Math.abs(px - xOfOffset(document, projection, metrics,
                    gutterWidth, scrollX, end));
            return dxDebut <= dxFin ? HandleId.START : HandleId.END;
        }
        if (debut) {
            return HandleId.START;
        }
        if (fin) {
            return HandleId.END;
        }
        return HandleId.NONE;
    }

    /**
     * La plage après le glissement d'une poignée : l'extrémité touchée
     * devient le curseur (le doigt la mène), l'autre devient l'ancre.
     *
     * @param plage     la plage avant le glissement
     * @param handle    la poignée glissée
     * @param newOffset la position visée par le doigt
     * @return la nouvelle plage
     */
    public static SelRange dragged(SelRange plage, HandleId handle, int newOffset) {
        if (handle == HandleId.START) {
            return new SelRange(plage.end(), newOffset);
        }
        if (handle == HandleId.END) {
            return new SelRange(plage.start(), newOffset);
        }
        return plage;
    }

    private static boolean aPortee(float px, float py, float gutterWidth,
            float scrollX, float scrollY, Document document,
            VisualProjection projection, EditorMetrics metrics, int offset) {
        float hx = xOfOffset(document, projection, metrics, gutterWidth, scrollX,
                offset);
        float hy = yOfOffset(document, projection, metrics, scrollY, offset)
                + metrics.lineHeight() / 2f;
        float dx = px - hx;
        float dy = py - hy;
        return dx * dx + dy * dy <= HIT_RADIUS * HIT_RADIUS;
    }
}
