package jo.celeris.android.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

import java.util.function.IntConsumer;

import org.jspecify.annotations.Nullable;

/**
 * La bande d'erreurs (M7.5, §7, reprise — D-08) : les marques du
 * modèle déjà livré ({@link ErrorBandModel}) le long du défilement,
 * densité bornée à un cran — l'appui saute au diagnostic du cran le
 * plus proche. Pas un panneau : une bande nue au bord droit de la
 * couche, pleine hauteur. Ne cède ni à la frappe ni au défilement :
 * elle vit avec les diagnostics, l'hôte l'ouvre et la ferme.
 *
 * <p>Les marques se posent en tableaux d'entiers (crans et offsets) —
 * la vue ne retient que ce qu'elle peint et ce qu'elle vise.</p>
 */
public final class ErrorBandView extends View {

    /** La largeur de la bande, en dp. */
    private static final float LARGEUR_DP = 18f;

    private final Paint pinceau = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int largeurPx;
    /** L'étendue verticale posée par l'hôte — la couche donne sa hauteur. */
    private int spanPx = 1;
    /** Le cran de chaque marque, dans l'ordre du modèle. */
    private int[] crans = new int[0];
    /** L'offset de saut de chaque marque, dans l'ordre du modèle. */
    private int[] offsets = new int[0];
    private int cranCount = 1;
    private @Nullable IntConsumer onJump;

    public ErrorBandView(Context context, PopupPalette palette) {
        super(context, null);
        this.largeurPx = Math.round(LARGEUR_DP
                * getResources().getDisplayMetrics().density);
        pinceau.setColor(palette.error());
    }

    /**
     * Pose la bande d'un modèle déjà densifié.
     *
     * @param model     la bande, crans compris
     * @param cranCount le nombre de crans du modèle (l'échelle verticale)
     */
    public void setModel(ErrorBandModel model, int cranCount) {
        var marques = model.marques();
        this.crans = new int[marques.size()];
        this.offsets = new int[marques.size()];
        for (int i = 0; i < marques.size(); i++) {
            this.crans[i] = marques.get(i).cran();
            this.offsets[i] = marques.get(i).startOffset();
        }
        this.cranCount = Math.max(1, cranCount);
        requestLayout();
        invalidate();
    }

    /**
     * Pose l'étendue verticale de la bande — la hauteur que la couche
     * lui donne, respirations déduites.
     *
     * @param spanPx l'étendue, en pixels
     */
    public void setSpanPx(int spanPx) {
        this.spanPx = Math.max(1, spanPx);
        requestLayout();
        invalidate();
    }

    /**
     * Le rappel du saut — reçoit l'OFFSET du diagnostic visé.
     *
     * @param rappel le consommateur d'offset
     */
    public void setOnJump(@Nullable IntConsumer rappel) {
        onJump = rappel;
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension(resolveSize(largeurPx, widthSpec),
                resolveSize(spanPx, heightSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cranPx = getHeight() / (float) cranCount;
        for (int cran : crans) {
            float haut = cran * cranPx;
            canvas.drawRect(0f, haut, getWidth(),
                    Math.min(getHeight(), haut + Math.max(cranPx, 2f)),
                    pinceau);
        }
    }

    /** L'index de la marque du cran le plus proche de l'ordonnée, —1 si vide. */
    private int nearestIndex(float y) {
        if (crans.length == 0 || getHeight() <= 0) {
            return -1;
        }
        int cranVise = Math.round(y * cranCount / (float) getHeight());
        int meilleur = 0;
        int meilleurEcart = Integer.MAX_VALUE;
        for (int i = 0; i < crans.length; i++) {
            int ecart = Math.abs(crans[i] - cranVise);
            if (ecart < meilleurEcart) {
                meilleurEcart = ecart;
                meilleur = i;
            }
        }
        return meilleur;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() != MotionEvent.ACTION_UP || onJump == null) {
            return event.getActionMasked() == MotionEvent.ACTION_DOWN;
        }
        int index = nearestIndex(event.getY());
        if (index >= 0) {
            onJump.accept(offsets[index]);
            performClick();
            return true;
        }
        return false;
    }
}
