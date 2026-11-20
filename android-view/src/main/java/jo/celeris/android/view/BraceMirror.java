package jo.celeris.android.view;

import jo.celeris.android.view.internal.model.BracePair;
import java.util.List;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.SelRange;

/**
 * Couche de chrome des accolades appariées (D-23, la reprise du
 * propriétaire — le petit rectangle qui encadre la paire) : quand le
 * curseur vit juste avant ou juste après une borne, chaque borne de
 * sa paire se contour d'un rectangle fin à l'encre du curseur — et
 * quand la paire s'étale sur plusieurs rangées, un trait vertical
 * joint les deux rectangles : vertical au bord gauche de l'ouvrante,
 * coude horizontal vers la fermante quand leur colonne diffère. La
 * ligne se voit (épaisseur et encre assumées) — elle GUIDE l'œil de
 * l'ouvrante à la fermante, elle ne chuchote pas.
 *
 * <p>La paire se cherche à chaque image par le balayage borné de
 * {@link BracePair} (PIEGE-15 : le réceptacle se réutilise,
 * jamais alloué dans la boucle). La géométrie suit la projection :
 * une borne cachée sous un pli replié ne se trace pas, une rangée hors
 * fenêtre ne se trace pas — mais une seule jambe suffit : sans les
 * deux rectangles, le trait ne se tire pas non plus.</p>
 *
 * <p>Un curseur seul surligne : une sélection vide (le curseur
 * d'écriture) ouvre le surlignage, une plage sélectionnée ne le
 * perturbe pas — la plage porte déjà l'attention, elle n'a pas besoin
 * du miroir.</p>
 */
public final class BraceMirror extends PainterLayer {

    /** L'alpha du contour des rectangles — l'encre du curseur, lisible. */
    private static final float ALPHA_RECTANGLE = 0.55f;

    /** L'alpha du trait de jonction — guide visible, discret sur le texte. */
    private static final float ALPHA_TRAIT = 0.4f;

    /** Le réceptacle de la paire courante — réutilisé (PIEGE-15). */
    private final int[] pair = new int[2];

    /**
     * Construit la couche des accolades appariées.
     *
     * @param brushes la fabrique de pinceaux (la couche prend le sien)
     */
    public BraceMirror(BrushFactory brushes) {
        super(brushes);
    }

    @Override
    protected int baseColor(FrameState frame) {
        return frame.theme().cursorColor();
    }

    @Override
    protected void onPaint(DrawSurface surface, FrameState frame) {
        List<SelRange> ranges = frame.selection().ranges();
        if (ranges.size() != 1 || !ranges.get(0).isEmpty()) {
            return;
        }
        Document document = frame.document();
        if (!BracePair.appariee(document, ranges.get(0).caret(), pair)) {
            return;
        }
        EditorMetrics metrics = frame.metrics();
        // les deux rectangles, chacun à sa borne — invisibles (cachées
        // ou hors fenêtre), la jambe ne se tire pas non plus
        boolean ouvranteVisible = tracerBorne(surface, frame, metrics,
                document, pair[0]);
        boolean fermanteVisible = tracerBorne(surface, frame, metrics,
                document, pair[1]);
        if (ouvranteVisible && fermanteVisible) {
            tracerJonction(surface, frame, metrics, document);
        }
    }

    /**
     * Trace le rectangle d'une borne : contour fin à l'encre du
     * curseur, exactement sur son glyphe — colonne, rangée, coupure du
     * retour à la ligne et plis compris.
     *
     * @return vrai quand la borne s'est tracée (visible à l'image)
     */
    private boolean tracerBorne(DrawSurface surface, FrameState frame,
            EditorMetrics metrics, Document document, int borne) {
        int line = document.lineForOffset(borne);
        VisualProjection projection = frame.projection();
        int rangnee = projection.rowOfLine(line);
        // une ligne cachée sous un pli se projette sur la rangée de son
        // en-tête : la rangée couvre plus de lignes — la borne vit
        // dessous, elle ne se trace pas
        if (line > projection.lastLineOfRow(rangnee)) {
            return false;
        }
        int column = borne - document.lineStart(line);
        int colonnes = projection.columns();
        if (colonnes > 0) {
            rangnee += column / colonnes;
        }
        if (rangnee < frame.firstRow()
                || rangnee >= frame.firstRow() + frame.visibleRows()) {
            return false;
        }
        int debutTranche = projection.spanStartAtRow(rangnee);
        // B-05 : la rangée de suite démarre au retrait de sa ligne.
        float gauche = frame.gutterWidth()
                + projection.indentAtRow(rangnee) * metrics.charWidth()
                + (column - debutTranche) * metrics.charWidth()
                - frame.scrollX();
        float haut = metrics.rowTop(rangnee) - frame.scrollY();
        float droite = gauche + metrics.charWidth();
        float bas = haut + metrics.lineHeight();
        brush().setColor(avecAlpha(frame.theme().cursorColor(),
                ALPHA_RECTANGLE));
        brush().setStrokeWidth(Math.max(1.5f, metrics.charWidth() * 0.12f));
        surface.drawLine(gauche, haut, droite, haut, brush());
        surface.drawLine(droite, haut, droite, bas, brush());
        surface.drawLine(droite, bas, gauche, bas, brush());
        surface.drawLine(gauche, bas, gauche, haut, brush());
        return true;
    }

    /**
     * Le trait de jonction des deux rectangles : vertical au bord
     * gauche de l'OUVRANTE, du bas de son rectangle au haut de celui
     * de la fermante — puis coude horizontal vers la fermante quand
     * leur colonne diffère. Les deux jambes se posent d'un trait
     * visible, l'épaisseur tient au moins un pixel et demi.
     */
    private void tracerJonction(DrawSurface surface, FrameState frame,
            EditorMetrics metrics, Document document) {
        int ligneOuvrante = document.lineForOffset(pair[0]);
        int colonneOuvrante = pair[0] - document.lineStart(ligneOuvrante);
        int ligneFermante = document.lineForOffset(pair[1]);
        int colonneFermante = pair[1] - document.lineStart(ligneFermante);
        VisualProjection projection = frame.projection();
        int rangneeOuvrante = projection.rowOfLine(ligneOuvrante);
        int rangneeFermante = projection.rowOfLine(ligneFermante);
        int colonnes = projection.columns();
        if (colonnes > 0) {
            rangneeOuvrante += colonneOuvrante / colonnes;
            rangneeFermante += colonneFermante / colonnes;
        }
        if (rangneeFermante <= rangneeOuvrante) {
            return; // même rangée : les deux rectangles se lisent seuls
        }
        int debutTrancheOuvrante = projection.spanStartAtRow(rangneeOuvrante);
        float xOuvrante = frame.gutterWidth()
                + (colonneOuvrante - debutTrancheOuvrante)
                        * metrics.charWidth()
                - frame.scrollX();
        int debutTrancheFermante = projection.spanStartAtRow(rangneeFermante);
        float xFermante = frame.gutterWidth()
                + (colonneFermante - debutTrancheFermante)
                        * metrics.charWidth()
                - frame.scrollX();
        float yHaut = metrics.rowTop(rangneeOuvrante) + metrics.lineHeight()
                - frame.scrollY();
        float yBas = metrics.rowTop(rangneeFermante) - frame.scrollY();
        brush().setColor(avecAlpha(frame.theme().cursorColor(), ALPHA_TRAIT));
        brush().setStrokeWidth(Math.max(1.25f, metrics.charWidth() * 0.09f));
        // la jambe verticale : du bas de l'ouvrante au haut de la fermante
        surface.drawLine(xOuvrante, yHaut, xOuvrante, yBas, brush());
        // le coude : quand la fermante vit à une autre colonne, le trait
        // la rejoint horizontalement — la jonction touche DEUX rectangles
        float coude = xOuvrante - xFermante;
        if (Math.abs(coude) > metrics.charWidth() * 0.5f) {
            surface.drawLine(xOuvrante, yBas, xFermante, yBas, brush());
        }
    }

    /**
     * L'alpha d'une couleur ARGB portée à son canal — la couche
     * s'atténue sans toucher au thème.
     */
    private static int avecAlpha(int couleur, float alpha) {
        int a = (int) (((couleur >>> 24) & 0xFF)
                * Math.max(0f, Math.min(1f, alpha)));
        return (a << 24) | (couleur & 0x00FFFFFF);
    }
}
