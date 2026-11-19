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

    /** La couche d'occurrences du montage (C11) — la poignée de la vue. */
    private OccurrencesPainter coucheOccurrences;

    /** La couche d'occurrences du montage (C11) : la vue y arme le
     *  voile du mot au caret. */
    OccurrencesPainter coucheOccurrences() {
        return coucheOccurrences;
    }

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
     * montage vit chez son assemblage) : gouttière, guides, sélection,
     * accolades, pli, composition, texte, fantôme — dans l'ordre du
     * tracé.
     *
     * @param gutter   le peintre de gouttière
     * @param pinceaux    la fabrique de pinceaux de la vue
     * @param guides      la source des guides structurels courants
     * @param piecesPli   la source des pièces de pli composé courantes
     * @param densite     la densité de l'appareil, pour les pastilles de
     *                    couleur du texte
     * @param composition l'état de composition de la vue
     * @param cache       le cache de rangées de la vue
     * @param source      la source de rangées liée de la vue
     * @param ghost     la couche du texte fantôme
     * @return le pipeline assemblé
     */
    public static ScrollFrame assemble(GutterPainter gutter,
            BrushFactory pinceaux, IndentGuides.GuidesSource guides,
            FoldPlaceholder.Source piecesPli, float densite,
            CompositionState composition, RowLayoutCache cache,
            RebindingRowSource source, PainterLayer ghost,
            EditorSyntax syntaxe) {
        ScrollFrame pipeline = new ScrollFrame();
        pipeline.addLayer(gutter);
        // La bande de ligne courante (le retour terrain du 2026-11-19)
        // se trace SOUS tout le reste : elle éclaire la rangée du
        // caret réduit, elle ne lit pas.
        pipeline.addLayer(new CurrentLineBand(pinceaux));
        // Les guides d'indentation (A-17) se tracent SOUS la sélection
        // et le texte : ils alignent, ils ne lisent pas.
        pipeline.addLayer(new IndentGuides(pinceaux, guides));
        // Les occurrences du mot au caret (C11) se voilent SOUS la
        // sélection : le mot répété parle le langage de la sélection,
        // discrètement — la sélection voile par-dessus.
        OccurrencesPainter occurrences = new OccurrencesPainter(pinceaux);
        pipeline.addLayer(occurrences);
        pipeline.addLayer(new SelectionPainter(pinceaux));
        // Les accolades appariées (D-14) : leurs rectangles se tracent
        // sur la bande de sélection, SOUS le texte — le glyphe prime.
        pipeline.addLayer(new BraceMirror(pinceaux));
        // Le placeholder de pli ({…}) : sa bande se trace sur la sélection
        // et sous le texte — la chip se distingue, l'encre prime.
        pipeline.addLayer(new FoldPlaceholder(pinceaux, piecesPli));
        pipeline.addLayer(new ComposingPainter(pinceaux, composition));
        pipeline.addLayer(new TextPainter(pinceaux, cache, source));
        // Les pastilles de couleur (C-05 relue) se tracent APRÈS le
        // texte : le voile du littéral teinte le glyphe, le trait du
        // cadre l'encadre — la couleur se voit autour du texte.
        // Les squiggles de diagnostic (D-05) se tracent APRÈS le
        // texte : la dentelle ondule sous les glyphes sur la ligne de
        // base — la malformation se voit sans gêner la lecture.
        pipeline.addLayer(new SquigglePainter(pinceaux, syntaxe));
        pipeline.addLayer(new TextChips(pinceaux, densite));
        pipeline.addLayer(ghost);
        pipeline.coucheOccurrences = occurrences;
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
