package jo.celeris.android.view;

import org.jspecify.annotations.Nullable;

/**
 * Couche de chrome du placeholder de pli (le constat du propriétaire
 * du 2026-11-13) : la bande qui teinte le « {…} » d'une ligne repliée
 * — un fond discret à la teinte de sélection, pleine hauteur de
 * rangée, qui distingue la chip du pli d'une ligne normale (le motif
 * du dépôt de départ). La géométrie vit dans la source unique
 * {@link FoldedRowSource#pieceDe} — la couche la relit à chaque
 * image, collée à l'instantané affiché ; jamais la pièce d'une autre
 * révision ne se trace (PIEGE-20).
 *
 * <p>La bande se borne à la PREMIÈRE tranche de la ligne d'en-tête
 * (PIEGE-04) : une en-tête coupée par le retour à la ligne ne teinte
 * que sa première rangée. Aucune allocation dans la boucle (PIEGE-15)
 * : la pièce se lit à l'indice de la rangée.</p>
 */
public final class FoldPlaceholder extends PainterLayer {

    /**
     * La source des pièces de pli de l'image à venir — la vue y branche
     * le pont de syntaxe, qui ne répond que collé à la publication
     * courante.
     */
    @FunctionalInterface
    public interface Source {

        /**
         * La pièce du pli de la ligne d'en-tête donnée.
         *
         * @param ligne la ligne d'en-tête à interroger
         * @return sa pièce, null si la ligne ne porte pas de pli composé
         */
        FoldedRowSource.@Nullable PieceDePli piece(int ligne);
    }

    /**
     * L'alpha de la bande — une ombre de repli : la teinte de sélection
     * y pose son voile, l'encre du placeholder reste seule à parler.
     */
    private static final int ALPHA = 0x66;

    private final Source source;

    /**
     * Construit la couche du placeholder de pli.
     *
     * @param brushes la fabrique de pinceaux (la couche prend le sien)
     * @param source  la source des pièces de pli courantes
     */
    public FoldPlaceholder(BrushFactory brushes, Source source) {
        super(brushes);
        this.source = source;
    }

    @Override
    protected int baseColor(FrameState frame) {
        // le voile de la teinte de sélection : la bande teinte, elle ne
        // sélectionne pas — la transparence vit dans le canal alpha
        return (frame.theme().selectionColor() & 0x00FFFFFF) | (ALPHA << 24);
    }

    @Override
    protected void onPaint(DrawSurface surface, FrameState frame) {
        EditorMetrics metrics = frame.metrics();
        VisualProjection projection = frame.projection();
        for (int rangee = frame.firstRow();
                rangee < frame.firstRow() + frame.visibleRows(); rangee++) {
            if (projection.spanStartAtRow(rangee) != 0) {
                continue;
            }
            int ligne = projection.firstLineOfRow(rangee);
            FoldedRowSource.PieceDePli piece = source.piece(ligne);
            if (piece == null) {
                continue;
            }
            float gauche = frame.gutterWidth()
                    + piece.colonne() * metrics.charWidth() - frame.scrollX();
            float droite = frame.gutterWidth()
                    + (piece.colonne() + piece.texte().length())
                            * metrics.charWidth() - frame.scrollX();
            float haut = metrics.rowTop(rangee) - frame.scrollY();
            surface.drawRect(gauche, haut, droite, haut + metrics.lineHeight(),
                    brush());
        }
    }
}
