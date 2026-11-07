package jo.celeris.android.view;

import java.util.List;

import jo.celeris.core.syntax.TokenKind;

/**
 * Couche de texte : trace les rangées visibles, mises en page servies
 * par le cache — à l'encre de base, ou à l'encre de chaque plage
 * colorée quand l'analyse en fournit (B-01).
 *
 * <p>En régime établi (PIEGE-15), chaque rangée visible sort du cache
 * sans allocation — la source n'est consultée qu'au premier affichage
 * d'une rangée, et le dépôt qui s'ensuit est le seul moment
 * d'allocation du chemin. Les textes tracés sont les instances mêmes du
 * cache : la preuve d'identité du piège s'y appuie. Une rangée sans
 * plage reste sur le chemin rapide d'un seul tracé.</p>
 *
 * <p>PIEGE-01 : la couche possède ses pinceaux — un par sorte de jeton,
 * pris à sa fabrique à sa construction — et repose la couleur de base
 * à chaque image ; la couleur d'une plage se pose sur SON pinceau à
 * l'instant précis du tracé, aucune couleur résiduelle ne se propage,
 * même sous la pire fabrique.</p>
 */
public final class TextPainter extends PainterLayer {

    private final RowLayoutCache cache;
    private final RowSource source;
    private final TextBrush[] pinceauxParSorte;

    /**
     * Construit la couche de texte.
     *
     * @param brushes la fabrique de pinceaux (la couche prend les siens :
     *                un par sorte de jeton, plus celui de la base)
     * @param cache   le cache de mises en page par rangée
     * @param source  la source des rangées manquantes au cache
     */
    public TextPainter(BrushFactory brushes, RowLayoutCache cache, RowSource source) {
        super(brushes);
        this.cache = cache;
        this.source = source;
        TokenKind[] sortes = TokenKind.values();
        this.pinceauxParSorte = new TextBrush[sortes.length];
        for (int i = 0; i < sortes.length; i++) {
            pinceauxParSorte[i] = brushes.newBrush();
        }
    }

    @Override
    protected int baseColor(FrameState frame) {
        return frame.theme().baseTextColor();
    }

    @Override
    protected void onPaint(DrawSurface surface, FrameState frame) {
        EditorMetrics metrics = frame.metrics();
        FontSpec police = metrics.font();
        // PIEGE-21, volet coloration : chaque pinceau par sorte reçoit la
        // police de l'image avant tout tracé — un pinceau jamais habillé
        // tracerait à la police par défaut de la plateforme pendant que
        // les positions avancent à la police de l'éditeur : les mots
        // colorés s'éparpilleraient.
        for (TextBrush pinceau : pinceauxParSorte) {
            pinceau.setFont(police);
        }
        int theme = frame.theme().id();
        long revision = frame.revision();
        int colonnes = frame.projection().columns();
        int premiere = frame.firstRow();
        int derniere = premiere + frame.visibleRows();
        float baseY = metrics.rowBaseline(premiere) - frame.scrollY();
        float hauteur = metrics.lineHeight();
        float bordGauche = frame.gutterWidth() - frame.scrollX();
        SyntaxPalette palette = frame.theme().syntaxe();
        for (int rangee = premiere; rangee < derniere; rangee++) {
            RowLayout layout = cache.find(revision, rangee, theme, police, colonnes);
            if (layout == null) {
                RowKey cle = new RowKey(revision, rangee, theme, police, colonnes);
                layout = source.layoutOf(cle);
                cache.store(cle, layout);
            }
            float y = baseY + (rangee - premiere) * hauteur;
            if (layout.runs().isEmpty() && layout.spans().isEmpty()) {
                // Chemin rapide : une rangée purement gauche-à-droite et
                // sans coloration se trace d'un seul tenant, l'instance
                // du cache.
                surface.drawText(layout.text(), bordGauche, y, brush());
            } else if (layout.runs().isEmpty()) {
                tracerPlages(surface, layout, bordGauche, y, metrics.charWidth(),
                        palette);
            } else {
                tracerSegments(surface, layout, bordGauche, y,
                        metrics.charWidth(), palette);
            }
        }
    }

    /**
     * Trace la rangée plage par plage, de gauche à droite : chaque plage
     * se peint à l'encre de sa sorte, le tracé avance de la largeur
     * rendue — aucune allocation, la variante à bornes de la surface
     * sert la plage telle quelle.
     */
    private void tracerPlages(DrawSurface surface, RowLayout layout,
            float bordGauche, float y, float largeurCar, SyntaxPalette palette) {
        String texte = layout.text();
        float x = bordGauche;
        for (ColoredSpan plage : layout.spans()) {
            TextBrush pinceau = pinceauxParSorte[plage.kind().ordinal()];
            pinceau.setColor(palette.couleur(plage.kind()));
            surface.drawText(texte, plage.start(), plage.end(), x, y, pinceau);
            x += (plage.end() - plage.start()) * largeurCar;
        }
    }

    /**
     * Trace la rangée segment par segment, de gauche à droite dans
     * l'ordre visuel (ADR-008) : le texte logique ne bouge jamais, la
     * position de tracé avance — aucune allocation, la variante à
     * bornes de la surface sert le segment tel quel. Sous coloration,
     * chaque segment se peint par l'intersection de ses plages : la
     * couleur suit le découpage logique, l'ordre reste visuel.
     */
    private void tracerSegments(DrawSurface surface, RowLayout layout,
            float bordGauche, float y, float largeurCar, SyntaxPalette palette) {
        String texte = layout.text();
        List<ColoredSpan> plages = layout.spans();
        float x = bordGauche;
        for (VisualRuns.Run segment : layout.runs()) {
            if (plages.isEmpty()) {
                surface.drawText(texte, segment.start(), segment.end(), x, y,
                        brush());
                x += (segment.end() - segment.start()) * largeurCar;
                continue;
            }
            for (ColoredSpan plage : plages) {
                int debut = Math.max(plage.start(), segment.start());
                int fin = Math.min(plage.end(), segment.end());
                if (fin <= debut) {
                    continue;
                }
                TextBrush pinceau = pinceauxParSorte[plage.kind().ordinal()];
                pinceau.setColor(palette.couleur(plage.kind()));
                surface.drawText(texte, debut, fin, x, y, pinceau);
                x += (fin - debut) * largeurCar;
            }
        }
    }
}
