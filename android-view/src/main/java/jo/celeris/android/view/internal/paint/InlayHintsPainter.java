package jo.celeris.android.view.internal.paint;

import java.util.List;

import jo.celeris.android.view.BrushFactory;
import jo.celeris.android.view.CaretGeometry;
import jo.celeris.android.view.DrawSurface;
import jo.celeris.android.view.EditorMetrics;
import jo.celeris.android.view.FrameState;
import jo.celeris.android.view.InlayHintMark;
import jo.celeris.android.view.PainterLayer;
import jo.celeris.android.view.VisualProjection;

import org.jspecify.annotations.Nullable;

import jo.celeris.core.text.Document;

/**
 * La couche des indices en ligne (inlay hints — l'audit Android Studio
 * du 2026-11-14, complété le même jour) : le rendu se dessine à
 * l'OFFSET porté par l'indice — le type du « var » en fin de ligne
 * utile, le nom d'un paramètre muet devant son argument — sur la même
 * ligne de base que le code (PIEGE-11), à l'encre de gouttière et en
 * ITALIQUE — l'indice se lit sans se confondre avec le code, il ne le
 * décale jamais : jamais dans le flux, le texte reste maître de ses
 * colonnes.
 *
 * <p>L'indice vit HORS du texte comme la valeur de débogage (E-03) :
 * une respiration de deux cellules le sépare du code, la ligne hors
 * document ne se trace pas — jamais d'indice deviné. L'hôte décide
 * (les « var » de l'analyse locale, la réponse du serveur), la couche
 * trace.</p>
 *
 * <p>Le CADRE TRANSLUCIDE du caret proche (l'audit du même jour : «
 * ajoute un petit rectangle translucide entourant le texte du inlay
 * hint quand le caret est près de lui, par exemple devant ») : quand
 * le curseur vit à deux colonnes de l'indice ou moins, un rectangle
 * au voile doux l'entoure — l'œil y pose, l'IDE de référence y
 * surligne son inlay cliquable.</p>
 */
public final class InlayHintsPainter  extends PainterLayer {

    /** La respiration entre la fin de ligne et l'indice, en cellules
     *  d'encre. */
    private static final int RESPIRATION_CELLULES = 2;

    /** La portée du caret proche, en colonnes — au-delà, le cadre ne
     *  se trace pas. */
    private static final int PORTEE_CARET = 2;

    private @Nullable List<InlayHintMark> inlayHints;

    /**
     * Construit la couche des indices.
     *
     * @param brushes la fabrique de pinceaux (la couche prend le sien)
     */
    public InlayHintsPainter(BrushFactory brushes) {
        super(brushes);
    }

    /**
     * Dépose (ou retire) les indices visibles.
     *
     * @param nouveaux les indices par ligne, ou {@code null} pour
     *                 retirer
     */
    public void setIndices(@Nullable List<InlayHintMark> nouveaux) {
        inlayHints = nouveaux;
    }

    @Override
    protected int baseColor(FrameState frame) {
        // l'encre de gouttière : l'indice se lit sans se confondre
        // avec le code
        return frame.theme().gutterTextColor();
    }

    @Override
    protected void onPaint(DrawSurface surface, FrameState frame) {
        List<InlayHintMark> courants = inlayHints;
        if (courants == null || courants.isEmpty()) {
            return;
        }
        Document document = frame.document();
        VisualProjection projection = frame.projection();
        EditorMetrics metrics = frame.metrics();
        int caret = caretDe(frame);
        for (int i = 0; i < courants.size(); i++) {
            InlayHintMark indice = courants.get(i);
            int line = indice.line() - 1;
            if (line < 0 || line >= document.lineCount()) {
                continue;
            }
            int lineStart = document.lineStart(line);
            // l'offset de l'indice vit DANS la ligne — la colonne du
            // texte où l'indice se lit (fin de ligne utile pour un
            // type, l'argument pour un nom de paramètre)
            int ancre = Math.min(lineStart + Math.max(0, indice.offset()),
                    document.lineEnd(line));
            int rangee = CaretGeometry.rowOfOffset(document, projection, ancre);
            float x = CaretGeometry.xOfOffset(document, projection, metrics,
                    frame.gutterWidth(), frame.scrollX(), ancre)
                    + RESPIRATION_CELLULES * metrics.charWidth();
            float baseY = metrics.rowBaseline(rangee) - frame.scrollY();
            // le CADRE TRANSLUCIDE du caret proche — le curseur vit à
            // deux colonnes de l'indice ou moins : le voile doux de la
            // sélection l'entoure (l'audit du 2026-11-14)
            if (Math.abs(ancre - caret) <= PORTEE_CARET) {
                tracerCadre(surface, x, baseY, indice.rendu(), metrics,
                        frame);
            }
            // l'indice se lit en ITALIQUE — le style se relâche après
            // le tracé (PIEGE-01 : rien ne fuit vers la couche suivante)
            brush().setStyle(false, true, false);
            surface.drawText(indice.rendu(), x, baseY, brush());
            brush().setStyle(false, false, false);
        }
    }

    /** Le caret primaire de l'image — la première plage seule compte. */
    private static int caretDe(FrameState frame) {
        var ranges = frame.selection().ranges();
        return ranges.isEmpty() ? -1 : ranges.get(0).caret();
    }

    /** Le rectangle translucide autour du rendu — le voile doux de la
     *  sélection, une cellule de respiration, arrondi au rayon de
     *  puce. */
    private void tracerCadre(DrawSurface surface, float x, float baseY,
            String rendu, EditorMetrics metrics, FrameState frame) {
        float width = widthOf(rendu, metrics);
        float respiration = metrics.charWidth() * 0.4f;
        brush().setColor(frame.theme().selectionColor());
        surface.drawRect(x - respiration, baseY - metrics.ascent() * 0.9f,
                x + width + respiration, baseY + metrics.descent() * 0.9f,
                brush());
    }

    /** La largeur du rendu au pinceau posé — le texte italique muet se
     *  mesure à la volée, l'indice ne vit pas dans le flux. */
    private float widthOf(String rendu, EditorMetrics metrics) {
        return rendu.length() * metrics.charWidth() * 0.55f;
    }
}
