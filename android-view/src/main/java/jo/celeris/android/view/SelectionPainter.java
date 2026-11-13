package jo.celeris.android.view;

import java.util.List;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.SelRange;

/**
 * Couche de chrome de sélection : trace le fond des plages sélectionnées
 * pour chaque rangée visible.
 *
 * <p>La conversion rangée → ligne passe par la projection (PIEGE-04) ;
 * l'intersection plage ↔ rangée se calcule en primitives — aucune
 * allocation dans la boucle (PIEGE-15).</p>
 */
public final class SelectionPainter extends PainterLayer {

    /**
     * L'alpha du voile de sélection — le redressement du constat du
     * propriétaire (2026-11-13 : « un rectangle bleu foncé qui cache le
     * mot ou caractère sélectionné ») : la teinte du thème y pose son
     * voile au lieu de son plein, le texte tracé par-dessus reste
     * lisible — le mot sélectionné se voit ENTIÈREMENT, le fond ne le
     * recouvre plus d'un opaque.
     */
    private static final int ALPHA = 0x99;

    /**
     * Construit la couche de sélection.
     *
     * @param brushes la fabrique de pinceaux (la couche prend le sien)
     */
    public SelectionPainter(BrushFactory brushes) {
        super(brushes);
    }

    @Override
    protected int baseColor(FrameState frame) {
        // le voile de la teinte de sélection : la transparence vit dans
        // le canal alpha — le texte au-dessus reste seul à parler
        return (frame.theme().selectionColor() & 0x00FFFFFF) | (ALPHA << 24);
    }

    @Override
    protected void onPaint(DrawSurface surface, FrameState frame) {
        List<SelRange> plages = frame.selection().ranges();
        for (int i = 0; i < plages.size(); i++) {
            peindrePlage(surface, frame, plages.get(i));
        }
    }

    private void peindrePlage(DrawSurface surface, FrameState frame, SelRange plage) {
        if (plage.isEmpty()) {
            return;
        }
        Document document = frame.document();
        EditorMetrics metrics = frame.metrics();
        VisualProjection projection = frame.projection();
        int premiere = frame.firstRow();
        int derniere = premiere + frame.visibleRows();
        for (int rangee = premiere; rangee < derniere; rangee++) {
            peindreRangnee(surface, frame, metrics, projection, plage, rangee, document);
        }
    }

    /**
     * Peint l'intersection de la plage avec SA tranche de rangée
     * (PIEGE-04) : le fond ne déborde jamais de la rangée, ni à gauche
     * ni à droite de la coupure.
     */
    private void peindreRangnee(DrawSurface surface, FrameState frame,
            EditorMetrics metrics, VisualProjection projection, SelRange plage,
            int rangee, Document document) {
        int ligne = projection.firstLineOfRow(rangee);
        int debutRangee = document.lineStart(ligne);
        int finRangee = document.lineEnd(ligne);
        int colonneDebut = Math.max(plage.start(), debutRangee) - debutRangee;
        int colonneFin = Math.min(plage.end(), finRangee) - debutRangee;
        // La bande se borne à la tranche de la rangée, jamais la ligne entière.
        int debutTranche = projection.spanStartAtRow(rangee);
        colonneDebut = Math.max(colonneDebut, debutTranche);
        colonneFin = Math.min(colonneFin, projection.spanEndAtRow(rangee));
        if (colonneFin <= colonneDebut) {
            return;
        }
        // La rangée trace sa tranche depuis son bord gauche : la bande
        // se positionne en colonnes DANS la tranche.
        float gauche = frame.gutterWidth()
                + (colonneDebut - debutTranche) * metrics.charWidth()
                - frame.scrollX();
        float haut = metrics.rowTop(rangee) - frame.scrollY();
        float droite = frame.gutterWidth()
                + (colonneFin - debutTranche) * metrics.charWidth()
                - frame.scrollX();
        float bas = haut + metrics.lineHeight();
        surface.drawRect(gauche, haut, droite, bas, brush());
    }
}
