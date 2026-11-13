package jo.celeris.android.view;

/**
 * Couche de chrome des guides d'indentation (A-17 relue, H-09 — la
 * structure sert le trait) : les lignes verticales qui alignent les
 * BLOCS du document — un guide par accolade appariée (ou par région
 * d'indentation pour les langages à décalage), tracé à la colonne de
 * la ligne qui ouvre le bloc, DEVANT son premier caractère, de la
 * ligne APRÈS l'accolade ouvrante à la ligne AVANT la fermante — les
 * lignes vides du corps se joignent au trait, un seul segment continu
 * par bloc. Jamais un trait par cran de blancs : le guide suit la
 * structure, une accolade dans une chaîne n'ouvre rien.
 *
 * <p>La table vit dans le pont de syntaxe — elle porte SA révision,
 * le trait ne se tire que collé à l'instantané affiché : jamais la
 * structure d'une autre révision ne s'aligne sur les rangées courantes
 * (PIEGE-20). La projection règle les rangées à l'image (PIEGE-04) :
 * un guide traverse le pli replié, un bloc entier sous un pli ne se
 * trace pas ; une ouvrante coupée par le retour à la ligne descend le
 * départ du trait sous la rangée qui porte le crochet. Aucune
 * allocation dans la boucle (PIEGE-15) : la table se lit à
 * l'indice.</p>
 */
public final class IndentGuides extends PainterLayer {

    /** La source des guides structurels courants, consultée à chaque image. */
    @FunctionalInterface
    public interface GuidesSource {

        /**
         * La table des guides de l'image à venir.
         *
         * @return la table, {@link GuidesStructurels#VIDE} sans
         *         structure à jour
         */
        GuidesStructurels guides();
    }

    /**
     * L'alpha du guide — une ombre de structure : lisible sans lire,
     * l'encre du texte reste seule à parler.
     */
    private static final int ALPHA = 0x2B;

    /** La source des guides courants. */
    private final GuidesSource source;

    /**
     * Construit la couche des guides.
     *
     * @param brushes la fabrique de pinceaux (la couche prend le sien)
     * @param source  la source des guides structurels courants
     */
    public IndentGuides(BrushFactory brushes, GuidesSource source) {
        super(brushes);
        this.source = source;
    }

    @Override
    protected int baseColor(FrameState frame) {
        // l'ombre de structure de l'encre de base : le guide aligne,
        // il ne lit pas — la transparence vit dans le canal alpha
        return (frame.theme().baseTextColor() & 0x00FFFFFF) | (ALPHA << 24);
    }

    @Override
    protected void onPaint(DrawSurface surface, FrameState frame) {
        GuidesStructurels table = source.guides();
        if (table.revision() != frame.document().revision()) {
            return;
        }
        EditorMetrics metrics = frame.metrics();
        VisualProjection projection = frame.projection();
        int colonnes = projection.columns();
        int premiereLigne = projection.firstLineOfRow(frame.firstRow());
        int derniereLigne = projection.lastLineOfRow(
                frame.firstRow() + frame.visibleRows() - 1);
        float hautFenetre = metrics.rowTop(frame.firstRow()) - frame.scrollY();
        float basFenetre = metrics.rowTop(frame.firstRow() + frame.visibleRows())
                - frame.scrollY();
        brush().setStrokeWidth(Math.max(1f, metrics.charWidth() * 0.06f));
        for (int i = 0; i < table.nombre(); i++) {
            GuidesStructurels.Guide guide = table.guide(i);
            if (guide.ligneOuverture() > derniereLigne) {
                break;
            }
            if (guide.ligneApresFin() > premiereLigne) {
                tracerGuide(surface, frame, metrics, projection, colonnes,
                        guide, hautFenetre, basFenetre);
            }
        }
    }

    /**
     * Trace le guide d'un bloc : un seul segment vertical, du bas de
     * la rangée qui porte l'ouvrante au haut de la rangée qui porte
     * la fermante — rogné aux bords de la fenêtre quand le bloc
     * déborde, muet quand rien ne reste à couvrir.
     */
    private void tracerGuide(DrawSurface surface, FrameState frame,
            EditorMetrics metrics, VisualProjection projection, int colonnes,
            GuidesStructurels.Guide guide, float hautFenetre, float basFenetre) {
        int rangneeOuvrante = projection.rowOfLine(guide.ligneOuverture());
        int rangneeFermante = projection.rowOfLine(guide.ligneApresFin());
        if (colonnes > 0) {
            rangneeOuvrante += guide.colonneOuvrante() / colonnes;
            rangneeFermante += guide.colonneFermante() / colonnes;
        }
        if (rangneeFermante <= rangneeOuvrante) {
            return;
        }
        float haut = metrics.rowTop(rangneeOuvrante) + metrics.lineHeight()
                - frame.scrollY();
        float bas = metrics.rowTop(rangneeFermante) - frame.scrollY();
        haut = Math.max(haut, hautFenetre);
        bas = Math.min(bas, basFenetre);
        if (bas <= haut) {
            return;
        }
        float x = frame.gutterWidth()
                + guide.colonne() * metrics.charWidth() - frame.scrollX();
        surface.drawLine(x, haut, x, bas, brush());
    }
}
