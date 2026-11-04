package jo.celeris.android.view;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracé d'image : déroule les couches dans l'ordre de leur dépôt.
 *
 * <p>PIEGE-15 : l'itération se fait à l'indice — jamais d'itérateur
 * alloué dans la boucle de dessin — et l'état d'image est l'instance
 * réutilisée de la vue, mutée par {@code beginFrame}.</p>
 */
public final class ScrollFrame {

    private final List<PainterLayer> layers = new ArrayList<>();

    /**
     * Ajoute une couche à la fin du pipeline.
     *
     * @param layer la couche à tracer à chaque image
     */
    public void addLayer(PainterLayer layer) {
        layers.add(layer);
    }

    /** Le nombre de couches du pipeline. */
    public int layerCount() {
        return layers.size();
    }

    /**
     * Trace une image : chaque couche pose sa couleur de base puis
     * trace, dans l'ordre du pipeline.
     *
     * @param surface la surface de dessin de l'image
     * @param frame   l'état d'image, configuré par l'hôte
     */
    public void drawFrame(DrawSurface surface, FrameState frame) {
        for (int i = 0; i < layers.size(); i++) {
            layers.get(i).paint(surface, frame);
        }
    }
}
