package jo.celeris.android.view;

import org.jspecify.annotations.Nullable;

/**
 * L'avis de changement de fenêtre (M7.5, §7) : la vue prévient l'hôte
 * quand ce qui porte les popups bouge — défilements, gouttière,
 * métriques de police, budget de colonnes, taille. L'avis part UNE
 * fois par état réel : le retrait d'une image au clignotement du
 * curseur ne notifie jamais deux fois le même état.
 *
 * <p>Pur et testable sans SDK : la signature se compare en flottants
 * et entiers, l'écouteur vit chez la vue.</p>
 */
final class ViewportWatch {

    private @Nullable Runnable listener;
    private boolean notified;
    private float scrollX;
    private float scrollY;
    private float gutter;
    private float charWidth;
    private float lineHeight;
    private int columns = -1;
    private int width;
    private int height;

    /** Pose (ou retire) l'écouteur des changements de fenêtre. */
    void setListener(@Nullable Runnable ecouteur) {
        listener = ecouteur;
    }

    /**
     * Compare l'état de fenêtre courant à la signature du dernier avis :
     * au changement réel, l'écouteur part — sinon le silence tient.
     *
     * @param scrollX   le défilement horizontal, en pixels
     * @param scrollY   le défilement vertical, en pixels
     * @param gutter    la largeur de gouttière courante, en pixels
     * @param charWidth la largeur de caractère courante, en pixels
     * @param lineHeight la hauteur de rangée courante, en pixels
     * @param columns   le budget de colonnes (0 sans retour à la ligne)
     * @param width     la largeur de la vue
     * @param height    la hauteur de la vue
     */
    void review(float scrollX, float scrollY, float gutter, float charWidth,
            float lineHeight, int columns, int width, int height) {
        if (listener == null) {
            return;
        }
        boolean change = !notified
                || this.scrollX != scrollX || this.scrollY != scrollY
                || this.gutter != gutter
                || this.charWidth != charWidth
                || this.lineHeight != lineHeight
                || this.columns != columns
                || this.width != width || this.height != height;
        if (!change) {
            return;
        }
        this.scrollX = scrollX;
        this.scrollY = scrollY;
        this.gutter = gutter;
        this.charWidth = charWidth;
        this.lineHeight = lineHeight;
        this.columns = columns;
        this.width = width;
        this.height = height;
        notified = true;
        listener.run();
    }
}
