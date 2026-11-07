package jo.celeris.android.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

import java.util.List;
import java.util.function.IntConsumer;

import org.jspecify.annotations.Nullable;

/**
 * La bande d'erreurs (M7.5, §7, reprise — D-08) : les marques du
 * modèle déjà livré ({@link ErrorBandModel}) le long du défilement,
 * densité bornée à un cran — l'appui saute au diagnostic du cran le
 * plus proche. Pas un panneau : une bande nue au bord droit de la
 * couche, pleine hauteur. Ne cède ni à la frappe ni au défilement :
 * elle vit avec les diagnostics, l'hôte l'ouvre et la ferme.
 */
public final class ErrorBandView extends View {

    /** La largeur de la bande, en dp. */
    private static final float LARGEUR_DP = 18f;

    private final Paint pinceau = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final PopupPalette palette;
    private final int largeurPx;
    /** L'étendue verticale posée par l'hôte — la couche donne sa hauteur. */
    private int spanPx = 1;
    private List<ErrorBandModel.Marque> marques = List.of();
    private int crans = 1;
    private @Nullable IntConsumer onJump;

    public ErrorBandView(Context context, PopupPalette palette) {
        super(context, null);
        this.palette = palette;
        this.largeurPx = Math.round(LARGEUR_DP
                * getResources().getDisplayMetrics().density);
        pinceau.setColor(palette.error());
    }

    /**
     * Pose la bande d'un modèle déjà densifié.
     *
     * @param model la bande, crans compris
     * @param cranCount le nombre de crans du modèle (l'échelle verticale)
     */
    public void setModel(ErrorBandModel model, int cranCount) {
        this.marques = model.marques();
        this.crans = Math.max(1, cranCount);
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
        float cranPx = getHeight() / (float) crans;
        for (ErrorBandModel.Marque marque : marques) {
            float haut = marque.cran() * cranPx;
            canvas.drawRect(0f, haut, getWidth(),
                    Math.min(getHeight(), haut + Math.max(cranPx, 2f)),
                    pinceau);
        }
    }

    /** La marque du cran le plus proche de l'ordonnée, null si vide. */
    private ErrorBandModel.@Nullable Marque nearest(float y) {
        if (marques.isEmpty() || getHeight() <= 0) {
            return null;
        }
        int cranVise = Math.round(y * crans / (float) getHeight());
        ErrorBandModel.Marque plusProche = marques.get(0);
        int meilleurEcart = Integer.MAX_VALUE;
        for (ErrorBandModel.Marque marque : marques) {
            int ecart = Math.abs(marque.cran() - cranVise);
            if (ecart < meilleurEcart) {
                meilleurEcart = ecart;
                plusProche = marque;
            }
        }
        return plusProche;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() != MotionEvent.ACTION_UP || onJump == null) {
            return event.getActionMasked() == MotionEvent.ACTION_DOWN;
        }
        ErrorBandModel.Marque visee = nearest(event.getY());
        if (visee != null) {
            onJump.accept(visee.startOffset());
            performClick();
            return true;
        }
        return false;
    }
}
