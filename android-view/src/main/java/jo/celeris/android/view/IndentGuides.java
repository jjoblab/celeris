package jo.celeris.android.view;

import jo.celeris.core.text.Document;

/**
 * Couche de chrome des guides d'indentation (chantier C13, A-17) : les
 * traits verticaux qui alignent les niveaux — un guide par cran
 * d'indentation ({@value #UNIT} colonnes), tracé à mi-chemin entre les
 * colonnes de bord, de la première à la dernière rangée de chaque
 * niveau. Les rangées contiguës d'un même niveau se joignent
 * visuellement en une ligne continue.
 *
 * <p>PIEGE-04 : le niveau se lit PAR RANGÉE — la projection décide de
 * la ligne (un pli replié couvre ses lignes, le guide traverse le pli
 * au niveau le plus faible de son bouquet) ; PIEGE-15 : aucune
 * allocation dans la boucle — le niveau se mesure en colonnes de
 * blancs de tête, en primitives.</p>
 */
public final class IndentGuides extends PainterLayer {

    /** Le cran d'un niveau d'indentation, en colonnes. */
    public static final int UNIT = 4;

    /** L'alpha du guide — une ombre discrète de l'encre de base. */
    private static final int ALPHA = 0x1A;

    /**
     * Construit la couche des guides.
     *
     * @param brushes la fabrique de pinceaux (la couche prend le sien)
     */
    public IndentGuides(BrushFactory brushes) {
        super(brushes);
    }

    @Override
    protected int baseColor(FrameState frame) {
        // l'ombre discrète de l'encre de base : le guide aligne, il
        // ne lit pas — la transparence vit dans le canal alpha
        return (frame.theme().baseTextColor() & 0x00FFFFFF) | (ALPHA << 24);
    }

    @Override
    protected void onPaint(DrawSurface surface, FrameState frame) {
        Document document = frame.document();
        EditorMetrics metrics = frame.metrics();
        VisualProjection projection = frame.projection();
        int premiere = frame.firstRow();
        int derniere = premiere + frame.visibleRows();
        for (int rangee = premiere; rangee < derniere
                && rangee < projection.rowCount(); rangee++) {
            peindreRangee(surface, frame, metrics, projection, document,
                    rangee);
        }
    }

    /**
     * Peint les guides d'une rangée : un trait par niveau de son
     * indentation, de son bord haut à son bord bas.
     */
    private void peindreRangee(DrawSurface surface, FrameState frame,
            EditorMetrics metrics, VisualProjection projection,
            Document document, int rangee) {
        int niveaux = niveauDe(projection, document, rangee);
        float haut = metrics.rowTop(rangee) - frame.scrollY();
        float bas = haut + metrics.lineHeight();
        for (int niveau = 1; niveau <= niveaux; niveau++) {
            float x = frame.gutterWidth()
                    + (niveau * UNIT - 0.5f) * metrics.charWidth()
                    - frame.scrollX();
            surface.drawLine(x, haut, x, bas, brush());
        }
    }

    /**
     * Le niveau d'indentation d'une rangée : le plus faible blanc de
     * tête des lignes de son bouquet (un pli replié compte toutes ses
     * lignes — le guide traverse le pli), en crans de
     * {@value #UNIT} colonnes.
     */
    private static int niveauDe(VisualProjection projection, Document document,
            int rangee) {
        int premiere = projection.firstLineOfRow(rangee);
        int derniere = projection.lastLineOfRow(rangee);
        int plusFaible = Integer.MAX_VALUE;
        for (int ligne = premiere; ligne <= derniere
                && ligne < document.lineCount(); ligne++) {
            plusFaible = Math.min(plusFaible, blancsDeTete(document, ligne));
        }
        if (plusFaible == Integer.MAX_VALUE || plusFaible < UNIT) {
            return 0;
        }
        return plusFaible / UNIT;
    }

    /** Les colonnes de blancs de tête d'une ligne, en primitives. */
    private static int blancsDeTete(Document document, int ligne) {
        int debut = document.lineStart(ligne);
        int fin = document.lineEnd(ligne);
        int colonnes = 0;
        for (int i = debut; i < fin; i++) {
            char c = document.charAt(i);
            if (c == ' ') {
                colonnes++;
            } else if (c == '\t') {
                colonnes += UNIT - colonnes % UNIT;
            } else {
                break;
            }
        }
        return colonnes;
    }
}
