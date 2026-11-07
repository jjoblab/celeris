package jo.celeris.android.view;

import android.graphics.Rect;

import java.util.List;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.SelRange;

/**
 * Les ancres des popups (M7.5, §7) : le rectangle du curseur et celui
 * de la sélection, en coordonnées de la vue — la géométrie pure de
 * {@link CaretGeometry} assemblée en rectangles, défilement déduit.
 * L'ancre du caret est le bord gauche du caractère visé, hauteur de
 * rangée ; l'ancre de sélection est l'union des extrémités de la
 * plage. La vue y pose ses popups ; le placement vit chez
 * {@code PopupPlacer}.
 */
final class PopupAnchors {

    private PopupAnchors() {
    }

    /**
     * Le rectangle du curseur principal.
     *
     * @param document   l'instantané affiché
     * @param projection la projection rangées ↔ lignes
     * @param metrics    les métriques courantes
     * @param gutter     la largeur de gouttière, en pixels
     * @param scrollX    le défilement horizontal
     * @param scrollY    le défilement vertical
     * @param ranges     les plages sélectionnées (la première est la principale)
     * @return le rectangle du curseur, largeur nulle
     */
    static Rect caretRect(Document document, VisualProjection projection,
            EditorMetrics metrics, float gutter, float scrollX, float scrollY,
            List<SelRange> ranges) {
        int offset = ranges.isEmpty() ? 0 : ranges.get(0).caret();
        int x = Math.round(CaretGeometry.xOfOffset(document, projection,
                metrics, gutter, scrollX, offset));
        int y = Math.round(CaretGeometry.yOfOffset(document, projection,
                metrics, scrollY, offset));
        return new Rect(x, y, x, y + Math.round(metrics.lineHeight()));
    }

    /**
     * Le rectangle de la sélection principale : l'union des
     * extrémités de la première plage, hauteur de la rangée basse.
     *
     * @param document   l'instantané affiché
     * @param projection la projection rangées ↔ lignes
     * @param metrics    les métriques courantes
     * @param gutter     la largeur de gouttière, en pixels
     * @param scrollX    le défilement horizontal
     * @param scrollY    le défilement vertical
     * @param ranges     les plages sélectionnées (la première est la principale)
     * @return le rectangle de la sélection, celui du curseur si vide
     */
    static Rect selectionRect(Document document, VisualProjection projection,
            EditorMetrics metrics, float gutter, float scrollX, float scrollY,
            List<SelRange> ranges) {
        SelRange plage = ranges.isEmpty() ? SelRange.caretAt(0) : ranges.get(0);
        int debutX = Math.round(CaretGeometry.xOfOffset(document, projection,
                metrics, gutter, scrollX, plage.start()));
        int debutY = Math.round(CaretGeometry.yOfOffset(document, projection,
                metrics, scrollY, plage.start()));
        int finX = Math.round(CaretGeometry.xOfOffset(document, projection,
                metrics, gutter, scrollX, plage.end()));
        int finY = Math.round(CaretGeometry.yOfOffset(document, projection,
                metrics, scrollY, plage.end()));
        int gauche = Math.min(debutX, finX);
        int droite = Math.max(debutX, finX);
        int haut = Math.min(debutY, finY);
        int bas = Math.max(debutY, finY) + Math.round(metrics.lineHeight());
        return new Rect(gauche, haut, droite, bas);
    }
}
