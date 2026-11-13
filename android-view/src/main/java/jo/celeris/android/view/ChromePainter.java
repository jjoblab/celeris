package jo.celeris.android.view;

import java.util.List;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.SelRange;

/**
 * Le chrome propre à la vue : le curseur d'écriture — une barre par
 * plage vide, clignotante — et les poignées de la plage unique non
 * vide — tracés APRÈS le pipeline, au pinceau possédé par la vue
 * (PIEGE-01).
 *
 * <p>Le redressement du propriétaire (2026-11-13 : « il y a un
 * rectangle bleu foncé qui est dessiné qui ne devrait pas se faire »)
 * : la pilule « ajouter un curseur » — un rectangle plein posé sur le
 * mot sélectionné — se RETIRE du chrome ; l'appui long sélectionne le
 * mot et offre la BARRE DE SÉLECTION du dépôt de départ, la vue n'y
 * peint plus rien. Les poignées prennent le design du dépôt de
 * départ : des CERCLES PLEINS sous la rangée d'ancre — jamais un
 * rectangle ne couvre le mot ni le caractère sélectionné.</p>
 */
final class ChromePainter {

    /**
     * Trace le chrome : le curseur s'il est visible, les poignées s'il
     * n'existe qu'une seule plage non vide.
     *
     * @param surface   la surface de dessin de l'image
     * @param brush     le pinceau de chrome, couleur à poser
     * @param theme     le thème courant
     * @param metrics   les métriques d'éditeur courantes
     * @param document  l'instantané de document affiché
     * @param projection la projection rangées ↔ lignes courante
     * @param plages    les plages sélectionnées
     * @param gutterWidth la largeur de gouttière, en pixels
     * @param scrollX   le défilement horizontal, en pixels
     * @param scrollY   le défilement vertical, en pixels
     * @param curseurVisible l'état clignotant du curseur d'écriture
     */
    void tracer(DrawSurface surface, TextBrush brush, EditorTheme theme,
            EditorMetrics metrics, Document document,
            VisualProjection projection, List<SelRange> plages,
            float gutterWidth, float scrollX, float scrollY,
            boolean curseurVisible) {
        if (curseurVisible) {
            tracerCurseurs(surface, brush, theme, metrics, document,
                    projection, plages, gutterWidth, scrollX, scrollY);
        }
        if (plages.size() == 1 && !plages.get(0).isEmpty()) {
            brush.setColor(theme.cursorColor());
            poignee(surface, brush, document, projection, metrics, gutterWidth,
                    scrollX, scrollY, plages.get(0).start());
            poignee(surface, brush, document, projection, metrics, gutterWidth,
                    scrollX, scrollY, plages.get(0).end());
        }
    }

    /**
     * Le curseur d'écriture : une barre verticale par plage VIDE —
     * chaque curseur d'une sélection multiple trace le sien (A-01) —
     * posée au bord gauche du caractère visé, pleine hauteur de
     * rangée, d'une épaisseur qui suit la police.
     */
    private void tracerCurseurs(DrawSurface surface, TextBrush brush,
            EditorTheme theme, EditorMetrics metrics, Document document,
            VisualProjection projection, List<SelRange> plages,
            float gutterWidth, float scrollX, float scrollY) {
        brush.setColor(theme.cursorColor());
        float demiEpaisseur = Math.max(2f, metrics.charWidth() / 12f);
        for (SelRange plage : plages) {
            if (!plage.isEmpty()) {
                continue;
            }
            float x = CaretGeometry.xOfOffset(document, projection, metrics,
                    gutterWidth, scrollX, plage.caret());
            float y = CaretGeometry.yOfOffset(document, projection, metrics,
                    scrollY, plage.caret());
            surface.drawRect(x - demiEpaisseur, y, x + demiEpaisseur,
                    y + metrics.lineHeight(), brush);
        }
    }

    /**
     * Une poignée de sélection : un CERCLE PLEIN sous la rangée
     * d'ancre (le design du dépôt de départ — jamais un rectangle
     * plein qui couvrirait le mot ou le caractère sélectionné).
     */
    private void poignee(DrawSurface surface, TextBrush brush, Document document,
            VisualProjection projection, EditorMetrics metrics, float gutterWidth,
            float scrollX, float scrollY, int offset) {
        float x = SelectionHandles.xOfOffset(document, projection, metrics,
                gutterWidth, scrollX, offset);
        float rayon = SelectionHandles.rayonPoignee(metrics);
        float cy = SelectionHandles.centreYPoignee(document, projection,
                metrics, scrollY, offset);
        surface.drawCircle(x, cy, rayon, true, brush);
    }
}
