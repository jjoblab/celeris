package jo.celeris.android.view;

import java.util.List;

import org.jspecify.annotations.Nullable;

import jo.celeris.core.text.Document;

/**
 * La couche des valeurs de débogage en ligne (E-03) : le rendu se
 * dessine en fin de ligne, sur la même ligne de base que le code
 * (PIEGE-11), à l'encre de gouttière — la valeur se lit sans se
 * confondre avec le code. La pause pose ses valeurs, la reprise les
 * retire : l'hôte décide, la couche trace.
 *
 * <p>L'indice en ligne vit HORS du texte : la valeur ne décale jamais
 * les colonnes, elle se pose APRÈS la fin utile de la ligne (les
 * blancs de queue ne comptent pas), une respiration de deux cellules
 * la sépare du code. La ligne hors document ne se trace pas — jamais
 * de valeur devinée.</p>
 */
final class ValeursPainter extends PainterLayer {

    /** La respiration entre la fin de ligne et la valeur, en cellules
     *  d'encre. */
    private static final int RESPIRATION_CELLULES = 2;

    private @Nullable List<ValeurLigne> valeurs;

    /**
     * Construit la couche des valeurs.
     *
     * @param brushes la fabrique de pinceaux (la couche prend le sien)
     */
    ValeursPainter(BrushFactory brushes) {
        super(brushes);
    }

    /**
     * Dépose (ou retire) les valeurs visibles.
     *
     * @param nouvelles les valeurs par ligne, ou {@code null} pour
     *                 retirer
     */
    void setValeurs(@Nullable List<ValeurLigne> nouvelles) {
        valeurs = nouvelles;
    }

    @Override
    protected int baseColor(FrameState frame) {
        // l'encre de gouttière : la valeur se lit sans se confondre
        return frame.theme().gutterTextColor();
    }

    @Override
    protected void onPaint(DrawSurface surface, FrameState frame) {
        List<ValeurLigne> courantes = valeurs;
        if (courantes == null || courantes.isEmpty()) {
            return;
        }
        Document document = frame.document();
        VisualProjection projection = frame.projection();
        EditorMetrics metrics = frame.metrics();
        for (int i = 0; i < courantes.size(); i++) {
            ValeurLigne valeur = courantes.get(i);
            int ligne = valeur.ligne() - 1;
            if (ligne < 0 || ligne >= document.lineCount()) {
                continue;
            }
            int fin = document.lineStart(ligne)
                    + document.lineText(ligne).stripTrailing().length();
            int rangee = CaretGeometry.rowOfOffset(document, projection, fin);
            float x = CaretGeometry.xOfOffset(document, projection, metrics,
                    frame.gutterWidth(), frame.scrollX(), fin)
                    + RESPIRATION_CELLULES * metrics.charWidth();
            float baseY = metrics.rowBaseline(rangee) - frame.scrollY();
            surface.drawText(valeur.rendu(), x, baseY, brush());
        }
    }
}
