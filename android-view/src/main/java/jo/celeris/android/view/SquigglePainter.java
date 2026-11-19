package jo.celeris.android.view;

import java.util.List;

import jo.celeris.core.syntax.SyntaxIssue;
import jo.celeris.core.text.Document;

/**
 * Couche de chrome des SQUIGGLES (D-05, l'analyse croisée) : le
 * soulignement ondulé sous les plages en diagnostic — le texte
 * porte sa malformation à l'œil, la gouttière n'est plus seule à
 * parler. La TEINTE suit la sévérité (D-06) : rouge pour l'erreur,
 * ambre pour l'avertissement, gris pour l'information.
 *
 * <p>L'ondulé se trace SOUS le texte (après lui dans le pipeline,
 * en fine dentelle sur la ligne de base) : il signale, il ne lit
 * pas. La géométrie suit celle des voiles (tranche de rangée,
 * PIEGE-04) ; la lecture des diagnostics se sert de l'état de la
 * syntaxe (dernière publication — les plages périmées d'une édition
 * en vol ne se tracent pas plus que la gouttière ne marque).</p>
 */
public final class SquigglePainter extends PainterLayer {

    /** La hauteur de l'ondulé, en pixels à la densité de l'appareil. */
    private static final float HEIGHT = 2.5f;

    /** La longueur d'une demi-onde. */
    private static final float DEMI_ONDE = 3f;

    /** L'état de la syntaxe — la source des diagnostics tracés. */
    private final EditorSyntax syntaxe;

    /**
     * Construit la couche des squiggles.
     *
     * @param brushes la fabrique de pinceaux (la couche prend le sien)
     * @param syntaxe l'état de syntaxe porteur des diagnostics
     */
    public SquigglePainter(BrushFactory brushes, EditorSyntax syntaxe) {
        super(brushes);
        this.syntaxe = syntaxe;
    }

    @Override
    protected int baseColor(FrameState frame) {
        // la teinte d'ERREUR par défaut — la couleur effective se
        // choisit par diagnostic dans onPaint (D-06)
        return teinteDe(SyntaxIssue.Severity.ERROR);
    }

    /** La teinte d'une sévérité (D-06) : rouge, ambre, gris. */
    static int teinteDe(SyntaxIssue.Severity severite) {
        return switch (severite) {
            case ERROR -> 0xFFF44336;
            case WARNING -> 0xFFFFB300;
            case INFO -> 0xFF9E9E9E;
        };
    }

    @Override
    protected void onPaint(DrawSurface surface, FrameState frame) {
        List<SyntaxIssue> diagnostics = syntaxe.issues();
        if (diagnostics.isEmpty()) {
            return;
        }
        Document document = frame.document();
        int length = document.length();
        for (SyntaxIssue diagnostic : diagnostics) {
            int start = diagnostic.startOffset();
            int end = Math.min(start + Math.max(1, diagnostic.length()),
                    length);
            if (end <= start) {
                continue;
            }
            teinter(teinteDe(diagnostic.severity()));
            paintRange(surface, frame, start, end);
        }
    }

    /** L'ondulé sous une plage : la dentelle ondule sur la ligne de
     *  base de chaque tranche de rangée traversée. */
    private void paintRange(DrawSurface surface, FrameState frame,
            int start, int end) {
        Document document = frame.document();
        EditorMetrics metrics = frame.metrics();
        VisualProjection projection = frame.projection();
        int premiere = frame.firstRow();
        int derniere = premiere + frame.visibleRows();
        int startLine = document.lineForOffset(start);
        int endLine = document.lineForOffset(end - 1);
        for (int line = startLine; line <= endLine; line++) {
            int rangnee = projection.rowOfLine(line);
            if (rangnee < premiere || rangnee >= derniere) {
                continue;
            }
            int lineStart = document.lineStart(line);
            int finLigne = document.lineEnd(line);
            int colonneDebut = Math.max(start, lineStart) - lineStart;
            int colonneFin = Math.min(end, finLigne) - lineStart;
            int debutTranche = projection.spanStartAtRow(rangnee);
            colonneDebut = Math.max(colonneDebut, debutTranche);
            colonneFin = Math.min(colonneFin, projection.spanEndAtRow(rangnee));
            if (colonneFin <= colonneDebut) {
                continue;
            }
            float indent = projection.indentAtRow(rangnee) * metrics.charWidth();
            float gauche = frame.gutterWidth()
                    + indent + (colonneDebut - debutTranche) * metrics.charWidth()
                    - frame.scrollX();
            float droite = frame.gutterWidth()
                    + indent + (colonneFin - debutTranche) * metrics.charWidth()
                    - frame.scrollX();
            float base = metrics.rowTop(rangnee) - frame.scrollY()
                    + metrics.lineHeight() - HEIGHT;
            onduler(surface, gauche, droite, base);
        }
    }

    /** La dentelle : une suite d'arcs courts sur la ligne de base,
     *  en primitives bornées — aucune allocation dans la boucle. */
    private void onduler(DrawSurface surface, float gauche, float droite,
            float base) {
        float x = gauche;
        float demi = Math.min(DEMI_ONDE, (droite - gauche) / 2f);
        boolean versLeBas = false;
        while (x < droite) {
            float bout = Math.min(x + demi * 2f, droite);
            surface.drawLine(x, versLeBas ? base + HEIGHT : base,
                    bout, versLeBas ? base : base + HEIGHT, brush());
            x = bout;
            versLeBas = !versLeBas;
        }
    }

    /** Pose la teinte courante du pinceau (la couche en change par
     *  diagnostic) : la dentelle reste fine — le trait de
     *  l'ondulé ne dépasse jamais la moitié de sa hauteur. */
    private void teinter(int couleur) {
        brush().setColor(couleur);
        brush().setStrokeWidth(1.5f);
    }
}
