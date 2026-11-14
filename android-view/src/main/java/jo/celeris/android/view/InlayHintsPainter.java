package jo.celeris.android.view;

import java.util.List;

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
final class InlayHintsPainter extends PainterLayer {

    /** La respiration entre la fin de ligne et l'indice, en cellules
     *  d'encre. */
    private static final int RESPIRATION_CELLULES = 2;

    /** La portée du caret proche, en colonnes — au-delà, le cadre ne
     *  se trace pas. */
    private static final int PORTEE_CARET = 2;

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
        int caret = caretDe(frame);
        for (int i = 0; i < courants.size(); i++) {
            IndiceInlay indice = courants.get(i);
            int ligne = indice.ligne() - 1;
            if (ligne < 0 || ligne >= document.lineCount()) {
                continue;
            }
            int debutLigne = document.lineStart(ligne);
            // l'offset de l'indice vit DANS la ligne — la colonne du
            // texte où l'indice se lit (fin de ligne utile pour un
            // type, l'argument pour un nom de paramètre)
            int ancre = Math.min(debutLigne + Math.max(0, indice.offset()),
                    document.lineEnd(ligne));
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
        var plages = frame.selection().ranges();
        return plages.isEmpty() ? -1 : plages.get(0).caret();
    }

    /** Le rectangle translucide autour du rendu — le voile doux de la
     *  sélection, une cellule de respiration, arrondi au rayon de
     *  puce. */
    private void tracerCadre(DrawSurface surface, float x, float baseY,
            String rendu, EditorMetrics metrics, FrameState frame) {
        float largeur = largeurDu(rendu, metrics);
        float respiration = metrics.charWidth() * 0.4f;
        brush().setColor(frame.theme().selectionColor());
        surface.drawRect(x - respiration, baseY - metrics.ascent() * 0.9f,
                x + largeur + respiration, baseY + metrics.descent() * 0.9f,
                brush());
    }

    /** La largeur du rendu au pinceau posé — le texte italique muet se
     *  mesure à la volée, l'indice ne vit pas dans le flux. */
    private float largeurDu(String rendu, EditorMetrics metrics) {
        return rendu.length() * metrics.charWidth() * 0.55f;
    }
}
