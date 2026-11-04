package jo.celeris.android.view;

import java.util.List;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.SelRange;

/**
 * Le chrome propre à la vue : la pilule « ajouter un curseur » armée
 * (A-01) et les poignées de la plage unique non vide — tracés APRÈS le
 * pipeline, au pinceau possédé par la vue (PIEGE-01).
 */
final class ChromePainter {

    /**
     * Trace le chrome : la pilule si armée, les poignées s'il n'existe
     * qu'une seule plage non vide.
     *
     * @param surface   la surface de dessin de l'image
     * @param brush     le pinceau de chrome, couleur à poser
     * @param theme     le thème courant
     * @param metrics   les métriques d'éditeur courantes
     * @param pilule    l'état de la pilule
     * @param document  l'instantané de document affiché
     * @param projection la projection rangées ↔ lignes courante
     * @param plages    les plages sélectionnées
     * @param gutterWidth la largeur de gouttière, en pixels
     * @param scrollX   le défilement horizontal, en pixels
     * @param scrollY   le défilement vertical, en pixels
     */
    void tracer(DrawSurface surface, TextBrush brush, EditorTheme theme,
            EditorMetrics metrics, CursorPill pilule, Document document,
            VisualProjection projection, List<SelRange> plages,
            float gutterWidth, float scrollX, float scrollY) {
        if (pilule.isArmed()) {
            brush.setColor(theme.cursorColor());
            surface.drawRect(pilule.left(), pilule.top(), pilule.right(),
                    pilule.bottom(), brush);
            surface.drawText("+", pilule.x() - metrics.charWidth() / 2f,
                    pilule.bottom() - metrics.descent(), brush);
        }
        if (plages.size() == 1 && !plages.get(0).isEmpty()) {
            brush.setColor(theme.cursorColor());
            poignee(surface, brush, document, projection, metrics, gutterWidth,
                    scrollX, scrollY, plages.get(0).start());
            poignee(surface, brush, document, projection, metrics, gutterWidth,
                    scrollX, scrollY, plages.get(0).end());
        }
    }

    private void poignee(DrawSurface surface, TextBrush brush, Document document,
            VisualProjection projection, EditorMetrics metrics, float gutterWidth,
            float scrollX, float scrollY, int offset) {
        float x = SelectionHandles.xOfOffset(document, projection, metrics,
                gutterWidth, scrollX, offset);
        float y = SelectionHandles.yOfOffset(document, projection, metrics,
                scrollY, offset);
        surface.drawRect(x - 3f, y, x + 3f, y + metrics.lineHeight(), brush);
    }
}
