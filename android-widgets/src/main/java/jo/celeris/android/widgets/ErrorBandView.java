package jo.celeris.android.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

import java.util.function.IntConsumer;

import org.jspecify.annotations.Nullable;

/**
 * La bande d'erreurs (portage du dessin du dépôt de départ — D-08) :
 * les marques du modèle déjà livré ({@link ErrorBandModel}) le long du
 * défilement, en TIRETS de 4 dp posés au bord droit — la bande entière
 * reste tactile sur 10 dp, la marque suit sa GRAVITÉ à l'encre (erreur,
 * avertissement, information) ; densité bornée à un cran — l'appui
 * saute au diagnostic du cran le plus proche. Pas un panneau : une
 * bande nue au bord droit de la couche, pleine hauteur. Ne cède ni à
 * la frappe ni au défilement : elle vit avec les diagnostics, l'hôte
 * l'ouvre et la ferme.
 *
 * <p>Les marques se posent en tableaux d'entiers (crans, offsets,
 * gravités) — la vue ne retient que ce qu'elle peint et ce qu'elle
 * vise.</p>
 */
public final class ErrorBandView extends View {

    /** La largeur tactile de la bande, en dp. */
    private static final float LARGEUR_TACTILE_DP = 10f;

    /** La largeur peinte d'une marque, en dp. */
    private static final float LARGEUR_MARQUE_DP = 4f;

    /** La hauteur minimale d'une marque, en dp. */
    private static final float HAUTEUR_MARQUE_DP = 3f;

    private final Paint pinceau = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int largeurPx;
    private final int largeurMarquePx;
    private final float hauteurMarquePx;
    /** L'étendue verticale posée par l'hôte — la couche donne sa hauteur. */
    private int spanPx = 1;
    /** Le cran de chaque marque, dans l'ordre du modèle. */
    private int[] crans = new int[0];
    /** L'offset de saut de chaque marque, dans l'ordre du modèle. */
    private int[] offsets = new int[0];
    /** La gravité de chaque marque, dans l'ordre du modèle. */
    private int[] gravites = new int[0];
    private int cranCount = 1;
    private @Nullable IntConsumer onJump;
    private PopupPalette palette;

    public ErrorBandView(Context context, PopupPalette palette) {
        super(context, null);
        float densite = getResources().getDisplayMetrics().density;
        this.largeurPx = Math.round(LARGEUR_TACTILE_DP * densite);
        this.largeurMarquePx = Math.round(LARGEUR_MARQUE_DP * densite);
        this.hauteurMarquePx = HAUTEUR_MARQUE_DP * densite;
        poserPalette(palette);
    }

    /**
     * Remplace la palette — les encres de gravité suivent le thème de
     * l'éditeur hôte.
     *
     * @param nouvelle la palette dérivée du thème courant
     */
    public void setPalette(PopupPalette nouvelle) {
        poserPalette(nouvelle);
        invalidate();
    }

    private void poserPalette(PopupPalette nouvelle) {
        this.palette = nouvelle;
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
        this.gravites = new int[marques.size()];
        for (int i = 0; i < marques.size(); i++) {
            this.crans[i] = marques.get(i).cran();
            this.offsets[i] = marques.get(i).startOffset();
            this.gravites[i] = marques.get(i).gravite();
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
        for (int i = 0; i < crans.length; i++) {
            float haut = crans[i] * cranPx;
            pinceau.setColor(encreDe(gravites[i]));
            canvas.drawRect(getWidth() - largeurMarquePx, haut, getWidth(),
                    Math.min(getHeight(), haut
                            + Math.max(cranPx, hauteurMarquePx)), pinceau);
        }
    }

    /** L'encre d'une gravité (la source unique du mapping, 3 = erreur). */
    private int encreDe(int gravite) {
        if (gravite == ErrorBandModel.GRAVITE_ERREUR) {
            return palette.error();
        }
        return gravite == 2 ? palette.warning() : palette.info();
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
