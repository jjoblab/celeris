package jo.celeris.android.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/**
 * Le panneau de base des popups (M7.5, §7, reprise) : fond arrondi,
 * liseré et respiration — le contenu vit chez les sous-classes, qui
 * mesurent puis dessinent. PIEGE-27 par construction : le panneau ne
 * demande JAMAIS le focus ; la couche le rend non focalisable en le
 * montrant, le focus d'entrée reste à l'éditeur.
 *
 * <p>La taille de texte suit le sp : la conversion en pixels se fait
 * une fois, à la densité et à l'échelle de police de l'appareil.</p>
 */
abstract class PopupPanel extends View {

    /** La respiration intérieure du panneau, en dp. */
    static final float PADDING_DP = 10f;
    /** Le rayon des coins, en dp. */
    static final float RAYON_DP = 8f;
    /** La taille de texte des popups, en sp. */
    static final float TEXTE_SP = 14f;

    private final Paint pinceauFond = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pinceauLisere = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF coins = new RectF();
    /** La palette posée à la construction — l'hôte la dérive de son thème. */
    final PopupPalette palette;
    /** La respiration intérieure, en pixels. */
    final int paddingPx;
    /** La hauteur d'une rangée de texte, en pixels. */
    final int rowHeightPx;
    private final Paint pinceauTexte = new Paint(Paint.ANTI_ALIAS_FLAG);

    PopupPanel(Context context, PopupPalette palette) {
        super(context, null);
        this.palette = palette;
        float densite = getResources().getDisplayMetrics().density;
        this.paddingPx = Math.round(PADDING_DP * densite);
        pinceauFond.setColor(palette.background());
        pinceauLisere.setStyle(Paint.Style.STROKE);
        pinceauLisere.setStrokeWidth(Math.max(1f, densite));
        pinceauLisere.setColor(palette.border());
        pinceauTexte.setColor(palette.text());
        float echelle = getResources().getConfiguration().fontScale;
        pinceauTexte.setTextSize(TEXTE_SP * densite * echelle);
        Paint.FontMetrics metriques = pinceauTexte.getFontMetrics();
        this.rowHeightPx = Math.round(metriques.bottom - metriques.top);
    }

    /** Le pinceau de texte du panneau — la couleur se pose par contenu. */
    final Paint textPaint() {
        return pinceauTexte;
    }

    @Override
    protected final void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        coins.set(0f, 0f, getWidth(), getHeight());
        canvas.drawRoundRect(coins, rayonPx(), rayonPx(), pinceauFond);
        canvas.drawRoundRect(coins, rayonPx(), rayonPx(), pinceauLisere);
        int sauve = canvas.save();
        canvas.translate(paddingPx, paddingPx);
        drawContent(canvas);
        canvas.restore();
    }

    /** Dessine le contenu, origine translate aux respiration comprises. */
    abstract void drawContent(Canvas canvas);

    /** La largeur de contenu mesurée, respiration EXCLUE. */
    abstract int contentWidth();

    /** La hauteur de contenu mesurée, respiration EXCLUE. */
    abstract int contentHeight();

    @Override
    protected final void onMeasure(int widthSpec, int heightSpec) {
        int largeur = contentWidth() + 2 * paddingPx;
        int hauteur = contentHeight() + 2 * paddingPx;
        setMeasuredDimension(resolveSize(largeur, widthSpec),
                resolveSize(hauteur, heightSpec));
    }

    private float rayonPx() {
        return RAYON_DP * getResources().getDisplayMetrics().density;
    }
}
