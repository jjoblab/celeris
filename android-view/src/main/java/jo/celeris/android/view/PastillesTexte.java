package jo.celeris.android.view;

import jo.celeris.core.text.Document;

/**
 * La couche des pastilles de couleur du texte (C-05 relue — le constat
 * du propriétaire du 2026-11-13 : « lorsque je tape une couleur par
 * exemple #fff, #ffffff encadré le avec la couleur comme dans
 * l'ancien projet ») : chaque littéral de couleur de la fenêtre
 * visible se rend ENCADRÉ de sa couleur — fond en voile léger, trait
 * plein d'un pixel — le texte garde son encre par-dessous, la couleur
 * se voit autour. Le motif est celui du dépôt de départ : le cadre
 * couvre le littéral entier, dièse compris, respiré de deux dp
 * horizontaux et d'un dp vertical, angles droits.
 *
 * <p>La détection vit dans {@link PastillesCouleur} — les quatre formes
 * du littéral Android, la résolution de la plateforme, TOUTES les
 * pastilles d'une ligne (le premier littéral ne suffit plus). La
 * fenêtre seule se balaie, les lignes repliées sous un pli ne portent
 * pas de cadre — leur composite parle pour elles ; le cadre se borne à
 * la tranche de SA rangée (PIEGE-04) : un littéral coupé par le retour
 * à la ligne encadre sa part visible de chaque rangée.</p>
 */
public final class PastillesTexte extends PainterLayer {

    /** La part d'alpha du voile de fond : la couleur se voit, le
     *  texte reste lisible — le quart du dépôt de départ. */
    private static final float PART_VOILE = 0.25f;

    /** La respiration horizontale du cadre, en dp. */
    private static final float RESPIRATION_X_DP = 2f;

    /** La respiration verticale du cadre, en dp. */
    private static final float RESPIRATION_Y_DP = 1f;

    private final float respirationX;
    private final float respirationY;

    /**
     * Construit la couche des pastilles de texte.
     *
     * @param brushes la fabrique de pinceaux (la couche prend le sien)
     * @param densite la densité de l'appareil, pour les respirations
     */
    public PastillesTexte(BrushFactory brushes, float densite) {
        super(brushes);
        this.respirationX = RESPIRATION_X_DP * densite;
        this.respirationY = RESPIRATION_Y_DP * densite;
    }

    @Override
    protected int baseColor(FrameState frame) {
        return frame.theme().baseTextColor();
    }

    @Override
    protected void onPaint(DrawSurface surface, FrameState frame) {
        Document document = frame.document();
        VisualProjection projection = frame.projection();
        EditorMetrics metrics = frame.metrics();
        for (int rangee = frame.firstRow();
                rangee < frame.firstRow() + frame.visibleRows(); rangee++) {
            int ligne = projection.firstLineOfRow(rangee);
            if (projection.lastLineOfRow(rangee) > ligne) {
                // la rangée porte un pli replié : son composite parle
                continue;
            }
            int debutTranche = projection.spanStartAtRow(rangee);
            int finTranche = projection.spanEndAtRow(rangee);
            String texte = document.lineText(ligne);
            for (PastilleCouleur pastille : PastillesCouleur.tous(ligne,
                    texte)) {
                encadrer(surface, frame, metrics, rangee, debutTranche,
                        finTranche, pastille);
            }
        }
    }

    /**
     * Encadre un littéral de SA rangée : le fond en voile de la couleur,
     * le trait plein d'un pixel — le cadre respire autour du littéral,
     * rogné à la tranche visible quand la coupure l'emporte (PIEGE-04).
     */
    private void encadrer(DrawSurface surface, FrameState frame,
            EditorMetrics metrics, int rangee, int debutTranche,
            int finTranche, PastilleCouleur pastille) {
        int colonne = pastille.colonne() - 1;
        int fin = colonne + pastille.longueur();
        int debutRogne = Math.max(colonne, debutTranche);
        int finRogne = Math.min(fin, finTranche);
        if (finRogne <= debutRogne) {
            return;
        }
        float haut = metrics.rowTop(rangee) - frame.scrollY();
        float bas = haut + metrics.lineHeight();
        float gauche = frame.gutterWidth()
                + (debutRogne - debutTranche) * metrics.charWidth()
                - frame.scrollX() - respirationX;
        float droite = frame.gutterWidth()
                + (finRogne - debutTranche) * metrics.charWidth()
                - frame.scrollX() + respirationX;
        // le fond : la couleur en voile — le texte par-dessous transparaît
        brush().setColor(voile(pastille.argb()));
        surface.drawRect(gauche, haut + respirationY, droite,
                bas - respirationY, brush());
        // le trait : la couleur pleine, un pixel, quatre côtés
        brush().setColor(pastille.argb());
        brush().setStrokeWidth(1f);
        surface.drawLine(gauche, haut + respirationY, droite,
                haut + respirationY, brush());
        surface.drawLine(droite, haut + respirationY, droite,
                bas - respirationY, brush());
        surface.drawLine(droite, bas - respirationY, gauche,
                bas - respirationY, brush());
        surface.drawLine(gauche, bas - respirationY, gauche,
                haut + respirationY, brush());
    }

    /** La couleur en voile : son canal alpha prend la part du voile. */
    private static int voile(int couleur) {
        int alpha = (int) (((couleur >>> 24) & 0xFF) * PART_VOILE);
        return (couleur & 0x00FFFFFF) | (alpha << 24);
    }
}
