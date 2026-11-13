package jo.celeris.android.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/**
 * Le panneau de base des popups (portage de la carte du dépôt de
 * départ) : surface verre + liseré hairline, rayon de carte aux jetons
 * ({@link PopupTokens#RAYON_CARTE_DP}), respirations asymétriques —
 * le contenu vit chez les sous-classes, qui mesurent puis dessinent.
 * Les paliers typographiques du kit ({@link PopupTokens}) dérivent de
 * la taille de base portée par la palette ; les filets de séparation
 * et les bandes teintées s'y tracent. PIEGE-27 par construction : le
 * panneau ne demande JAMAIS le focus ; la couche le rend non
 * focalisable en le montrant, le focus d'entrée reste à l'éditeur.
 *
 * <p>La taille de texte suit le sp : la conversion en pixels se fait
 * une fois, à la densité et à l'échelle de police de l'appareil.</p>
 */
abstract class PopupPanel extends View {

    /** La palette posée — l'hôte la dérive de son thème, le rafraîchissement la remplace. */
    PopupPalette palette;
    /** La respiration horizontale du panneau, en pixels. */
    int paddingPx;
    /** La respiration verticale d'une section, en pixels. */
    int paddingYPx;
    /** La hauteur d'une rangée de corps, en pixels. */
    int rowHeightPx;

    private final Paint pinceauFond = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pinceauLisere = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pinceauTexte = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pinceauBande = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF coins = new RectF();
    /** Le chemin de la carte — le clip arrondi du contenu (motif du dépôt de départ). */
    private final Path cheminCarte = new Path();
    private final float densite;

    PopupPanel(Context context, PopupPalette palette) {
        super(context, null);
        this.densite = getResources().getDisplayMetrics().density;
        poserPalette(palette);
    }

    /**
     * Remplace la palette du panneau — le verre suit le thème de
     * l'éditeur hôte, les encres et la typographie avec lui.
     *
     * @param nouvelle la palette dérivée du thème courant
     */
    public final void setPalette(PopupPalette nouvelle) {
        poserPalette(nouvelle);
        requestLayout();
        invalidate();
    }

    /** Pose la palette et redérive pinceaux, respirations et rangées. */
    private void poserPalette(PopupPalette nouvelle) {
        this.palette = nouvelle;
        this.paddingPx = Math.round(PopupTokens.RESPIRATION_X_DP * densite);
        this.paddingYPx = Math.round(PopupTokens.RESPIRATION_Y_DP * densite);
        pinceauFond.setColor(palette.background());
        pinceauLisere.setStyle(Paint.Style.STROKE);
        // Le liseré hairline du dépôt de départ : UN pixel, pas un
        // pixel indépendant de la densité — le trait fin qui porte le
        // contour du verre.
        pinceauLisere.setStrokeWidth(1f);
        pinceauLisere.setColor(palette.border());
        pinceauTexte.setColor(palette.text());
        poserTaille(pinceauTexte, PopupTokens.TEXTE_CORPS);
        Paint.FontMetrics metriques = pinceauTexte.getFontMetrics();
        this.rowHeightPx = Math.round(metriques.bottom - metriques.top);
    }

    /** La taille de texte d'un palier, en pixels. */
    final float tailleTexte(float tier) {
        return palette.textBaseSp() * tier * densite
                * getResources().getConfiguration().fontScale;
    }

    /** Pose la taille d'un palier sur le pinceau donné. */
    private void poserTaille(Paint pinceau, float tier) {
        pinceau.setTextSize(tailleTexte(tier));
    }

    /** Le pinceau de texte du panneau — la couleur se pose par contenu. */
    final Paint textPaint() {
        return pinceauTexte;
    }

    /** Le pinceau de remplissage des bandes et badges. */
    final Paint bandPaint() {
        return pinceauBande;
    }

    /** Le pinceau de trait des filets — hairline, couleur du liseré. */
    final Paint linePaint() {
        return pinceauLisere;
    }

    /**
     * Un filet horizontal de séparation entre deux sections (la
     * couleur du liseré, un pixel) — le motif du dépôt de départ.
     *
     * @param canvas le canvas de la frame
     * @param y      l'ordonnée du filet, dans le repère du contenu
     */
    final void tracerFilet(Canvas canvas, float y) {
        pinceauLisere.setStrokeWidth(1f);
        canvas.drawLine(0f, y, contentWidth(), y, pinceauLisere);
    }

    @Override
    protected final void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        coins.set(0f, 0f, getWidth(), getHeight());
        float rayon = rayonCartePx();
        cheminCarte.reset();
        cheminCarte.addRoundRect(coins, rayon, rayon, Path.Direction.CW);
        canvas.drawPath(cheminCarte, pinceauFond);
        canvas.drawPath(cheminCarte, pinceauLisere);
        int sauve = canvas.save();
        // Le contenu se clippe à la carte arrondie (le motif du dépôt
        // de départ) : la bande de sélection d'une rangée ne déborde
        // jamais sur les coins.
        canvas.clipPath(cheminCarte);
        canvas.translate(paddingPx, paddingYPx);
        drawContent(canvas);
        canvas.restore();
    }

    /** Dessine le contenu, origine translatée aux respirations comprises. */
    abstract void drawContent(Canvas canvas);

    /** La largeur de contenu mesurée, respirations EXCLUES. */
    abstract int contentWidth();

    /** La hauteur de contenu mesurée, respirations EXCLUES. */
    abstract int contentHeight();

    /**
     * Le rayon de la carte — la valeur du kit par défaut, la capsule
     * pour la barre de sélection (le dépôt de départ : la pilule prend
     * le rayon de sa hauteur).
     */
    float rayonCartePx() {
        return PopupTokens.RAYON_CARTE_DP * densite;
    }

    @Override
    protected final void onMeasure(int widthSpec, int heightSpec) {
        int largeur = contentWidth() + 2 * paddingPx;
        int hauteur = contentHeight() + 2 * paddingYPx;
        setMeasuredDimension(resolveSize(largeur, widthSpec),
                resolveSize(hauteur, heightSpec));
    }
}
