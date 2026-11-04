package jo.celeris.android.view;

import jo.celeris.core.text.Document;

/**
 * Le dévoilement du curseur : après une édition IME, le défilement se
 * réajuste AU MINIMUM pour rendre le curseur visible — rien ne bouge
 * s'il l'est déjà, et le défilement ne devient jamais négatif. Le
 * bornage au contenu reste à la géométrie de défilement de l'image
 * suivante (PIEGE-04 : la position vit sur la rangée visuelle, coupures
 * comprises).
 */
public final class CaretReveal {

    private CaretReveal() {
    }

    /**
     * Un défilement réajusté.
     *
     * @param scrollX le défilement horizontal, en pixels
     * @param scrollY le défilement vertical, en pixels
     */
    public record Revealed(float scrollX, float scrollY) {
    }

    /**
     * Réajuste le défilement pour dévoiler le curseur.
     *
     * @param caretOffset    l'offset du curseur à dévoiler
     * @param document       l'instantané de document courant
     * @param projection     la projection rangées ↔ lignes courante
     * @param metrics        les métriques d'éditeur courantes
     * @param gutterWidth    la largeur de gouttière, en pixels
     * @param viewportWidth  la largeur de la fenêtre, en pixels
     * @param viewportHeight la hauteur de la fenêtre, en pixels
     * @param scrollX        le défilement horizontal courant
     * @param scrollY        le défilement vertical courant
     * @param margin         la marge autour du curseur, en pixels
     * @return le défilement réajusté, borné à zéro
     */
    public static Revealed reveal(int caretOffset, Document document,
            VisualProjection projection, EditorMetrics metrics,
            float gutterWidth, int viewportWidth, int viewportHeight,
            float scrollX, float scrollY, float margin) {
        float x = CaretGeometry.xOfOffset(document, projection, metrics,
                gutterWidth, scrollX, caretOffset);
        float nouveauX = scrollX;
        float bordGauche = gutterWidth + margin;
        if (x < bordGauche) {
            nouveauX = scrollX - (bordGauche - x);
        } else if (x > viewportWidth - margin) {
            nouveauX = scrollX + (x - (viewportWidth - margin));
        }

        float haut = CaretGeometry.yOfOffset(document, projection, metrics,
                scrollY, caretOffset);
        float nouveauY = scrollY;
        if (haut < margin) {
            nouveauY = scrollY - (margin - haut);
        } else {
            float bas = haut + metrics.lineHeight();
            float bordBas = viewportHeight - margin;
            if (bas > bordBas) {
                nouveauY = scrollY + (bas - bordBas);
            }
        }
        return new Revealed(Math.max(0f, nouveauX), Math.max(0f, nouveauY));
    }
}
