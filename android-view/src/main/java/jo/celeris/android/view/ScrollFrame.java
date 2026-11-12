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

    /**
     * Assemble le pipeline de couches de la vue (chantier C9 — le
     * montage vit chez son assemblage) : gouttière, sélection,
     * composition, texte, fantôme — dans l'ordre du tracé.
     *
     * @param gouttiere   le peintre de gouttière
     * @param pinceaux    la fabrique de pinceaux de la vue
     * @param composition l'état de composition de la vue
     * @param cache       le cache de rangées de la vue
     * @param source      la source de rangées liée de la vue
     * @param fantome     la couche du texte fantôme
     * @return le pipeline assemblé
     */
    public static ScrollFrame assemble(GutterPainter gouttiere,
            BrushFactory pinceaux, CompositionState composition,
            RowLayoutCache cache, RebindingRowSource source,
            PainterLayer fantome) {
        ScrollFrame pipeline = new ScrollFrame();
        pipeline.addLayer(gouttiere);
        // Les guides d'indentation (A-17) se tracent SOUS la sélection
        // et le texte : ils alignent, ils ne lisent pas.
        pipeline.addLayer(new IndentGuides(pinceaux));
        pipeline.addLayer(new SelectionPainter(pinceaux));
        // Les accolades appariées (D-14) : leurs rectangles se tracent
        // sur la bande de sélection, SOUS le texte — le glyphe prime.
        pipeline.addLayer(new AccoladesMiroir(pinceaux));
        pipeline.addLayer(new ComposingPainter(pinceaux, composition));
        pipeline.addLayer(new TextPainter(pinceaux, cache, source));
        pipeline.addLayer(fantome);
        return pipeline;
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
