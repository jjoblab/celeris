package jo.celeris.android.view.internal.viewport;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jspecify.annotations.Nullable;

/**
 * L'avis de changement de fenêtre (M7.5, §7) : la vue prévient l'hôte
 * quand ce qui porte les popups bouge — défilements, gouttière,
 * métriques de police, budget de colonnes, taille. L'avis part UNE
 * fois par état réel : le retrait d'une image au clignotement du
 * curseur ne notifie jamais deux fois le même état.
 *
 * <p>Pur et testable sans SDK : la signature se compare en flottants
 * et entiers. Les abonnés y sont MULTIPLES (ADR-0022, phase 2, T3 —
 * point 13 de la revue : le repositionneur des popups du kit et
 * l'écouteur de l'hôte vivaient sur le même emplacement unique,
 * s'écrasant l'un l'autre) ; la voie 1.x {@code setListener} reste
 * pour compatibilité, le temps du recensement des hôtes.</p>
 */
public final class ViewportWatch {

    /** Les abonnés de l'avis — multiples, sans écrasement (T3). */
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    /** L'abonné 1.x à emplacement unique — repli compatibilité. */
    private @Nullable Runnable listenerUnique;

    private boolean notified;
    private float scrollX;
    private float scrollY;
    private float gutter;
    private float charWidth;
    private float lineHeight;
    private int columns = -1;
    private int width;
    private int height;

    /** Pose (ou retire) l'écouteur unique 1.x — compatibilité : le
     *  premier abonné posé vit avec les abonnés 2.0. */
    public void setListener(@Nullable Runnable ecouteur) {
        listenerUnique = ecouteur;
    }

    /** Abonne un écouteur des changements de fenêtre (voie 2.0, T3). */
    public void addListener(Runnable ecouteur) {
        listeners.add(Objects.requireNonNull(ecouteur));
    }

    /** Désabonne un écouteur des changements de fenêtre (voie 2.0). */
    public void removeListener(Runnable ecouteur) {
        listeners.remove(ecouteur);
    }

    /**
     * Compare l'état de fenêtre courant à la signature du dernier avis :
     * au changement réel, les écouteurs partent — sinon le silence tient.
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
    public void review(float scrollX, float scrollY, float gutter, float charWidth,
            float lineHeight, int columns, int width, int height) {
        if (listeners.isEmpty() && listenerUnique == null) {
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
        Runnable unique = listenerUnique;
        if (unique != null) {
            unique.run();
        }
        for (Runnable ecouteur : listeners) {
            ecouteur.run();
        }
    }
}
