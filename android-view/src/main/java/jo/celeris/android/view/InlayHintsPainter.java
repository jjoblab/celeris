package jo.celeris.android.view;

import java.util.List;

import org.jspecify.annotations.Nullable;

import jo.celeris.core.text.Document;

/**
 * La couche des indices en ligne (inlay hints — l'audit Android Studio
 * du 2026-11-14) : le rendu se dessine en fin de ligne UTILE, sur la
 * même ligne de base que le code (PIEGE-11), à l'encre de gouttière
 * et en ITALIQUE — l'indice se lit sans se confondre avec le code, il
 * ne le décale jamais : jamais dans le flux, le texte reste maître de
 * ses colonnes. Le simulateur de référence (la parité fait foi) y pose
 * « var resultat = solde; » avec « : int » — fin de ligne utile,
 * encre muette, italique.
 *
 * <p>L'indice vit HORS du texte comme la valeur de débogage (E-03) :
 * une respiration de deux cellules le sépare du code, les blancs de
 * queue ne comptent pas, la ligne hors document ne se trace pas —
 * jamais d'indice deviné. L'hôte décide (les « var » de l'analyse
 * locale, la réponse du serveur), la couche trace.</p>
 */
final class InlayHintsPainter extends PainterLayer {

    /** La respiration entre la fin de ligne et l'indice, en cellules
     *  d'encre. */
    private static final int RESPIRATION_CELLULES = 2;

    private @Nullable List<IndiceInlay> indices;

    /**
     * Construit la couche des indices.
     *
     * @param brushes la fabrique de pinceaux (la couche prend le sien)
     */
    InlayHintsPainter(BrushFactory brushes) {
        super(brushes);
    }

    /**
     * Dépose (ou retire) les indices visibles.
     *
     * @param nouveaux les indices par ligne, ou {@code null} pour
     *                 retirer
     */
    void setIndices(@Nullable List<IndiceInlay> nouveaux) {
        indices = nouveaux;
    }

    @Override
    protected int baseColor(FrameState frame) {
        // l'encre de gouttière : l'indice se lit sans se confondre
        // avec le code
        return frame.theme().gutterTextColor();
    }

    @Override
    protected void onPaint(DrawSurface surface, FrameState frame) {
        List<IndiceInlay> courants = indices;
        if (courants == null || courants.isEmpty()) {
            return;
        }
        Document document = frame.document();
        VisualProjection projection = frame.projection();
        EditorMetrics metrics = frame.metrics();
        for (int i = 0; i < courants.size(); i++) {
            IndiceInlay indice = courants.get(i);
            int ligne = indice.ligne() - 1;
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
            // l'indice se lit en ITALIQUE — le style se relâche après
            // le tracé (PIEGE-01 : rien ne fuit vers la couche suivante)
            brush().setStyle(false, true, false);
            surface.drawText(indice.rendu(), x, baseY, brush());
            brush().setStyle(false, false, false);
        }
    }
}
