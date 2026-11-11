package jo.celeris.android.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

import java.util.function.IntConsumer;

import org.jspecify.annotations.Nullable;

/**
 * La minimap (chantier C9) : la silhouette du document au bord droit
 * — chaque rangée de la carte ({@link MinimapModel}) se rend en un
 * trait horizontal dont la largeur suit la charge du bouquet, la
 * fenêtre visible se rend à l'encre d'accent. Pas un panneau : une
 * bande nue au bord droit de la couche, pleine hauteur. L'appui et le
 * glissement sautent : la ligne visée part au rappel, le cran se lit
 * à l'ordonnée du doigt. Ne cède ni à la frappe ni au défilement :
 * elle vit avec le document, l'hôte l'ouvre et la ferme.
 */
public final class MinimapView extends View {

    /** La largeur de la bande, en dp. */
    private static final float LARGEUR_DP = 44f;

    /** La marge horizontale du contenu, en dp. */
    private static final float MARGE_DP = 6f;

    private final Paint pinceau = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int largeurPx;
    private final int margePx;
    /** L'étendue verticale posée par l'hôte — la couche donne sa hauteur. */
    private int spanPx = 1;
    /** Les charges de la carte, dans l'ordre des crans. */
    private int[] charges = new int[0];
    /** Le cran de la premiere ligne visible, —1 sans fenêtre posée. */
    private int cranFenetre = -1;
    /** Le nombre de crans de la fenêtre visible, 0 sans fenêtre. */
    private int cransFenetre = 0;
    /** Le nombre de lignes du document — la cible du saut. */
    private int lignes = 1;
    /** Le doigt tient la bande : chaque mouvement saute. */
    private boolean doigtPose;
    private @Nullable IntConsumer onJump;
    private PopupPalette palette;

    public MinimapView(Context context, PopupPalette palette) {
        super(context, null);
        float densite = getResources().getDisplayMetrics().density;
        this.largeurPx = Math.round(LARGEUR_DP * densite);
        this.margePx = Math.round(MARGE_DP * densite);
        poserPalette(palette);
    }

    /**
     * Remplace la palette — les encres suivent le thème de l'éditeur hôte.
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
     * Pose la silhouette d'un modèle déjà chargé.
     *
     * @param model  la carte, charges comprises
     * @param lignes le nombre de lignes du document — la cible du saut
     */
    public void setModel(MinimapModel model, int lignes) {
        var rangees = model.rangees();
        this.charges = new int[rangees.size()];
        for (int i = 0; i < rangees.size(); i++) {
            this.charges[i] = rangees.get(i).load();
        }
        this.lignes = Math.max(1, lignes);
        requestLayout();
        invalidate();
    }

    /**
     * Pose la fenêtre visible : les crans que le document montre.
     *
     * @param premiereLigneVisible la premiere ligne visible
     * @param lignesVisibles       le nombre de lignes visibles
     * @param lignesTotales        le nombre de lignes du document
     */
    public void setViewport(int premiereLigneVisible, int lignesVisibles,
            int lignesTotales) {
        int crans = Math.max(1, charges.length);
        int totales = Math.max(1, lignesTotales);
        this.cranFenetre = MinimapModel.cranDeLigne(
                Math.max(0, premiereLigneVisible), totales, crans);
        int dernier = MinimapModel.cranDeLigne(
                Math.max(0, premiereLigneVisible + Math.max(1, lignesVisibles) - 1),
                totales, crans);
        this.cransFenetre = Math.max(1, dernier - cranFenetre + 1);
        invalidate();
    }

    /**
     * Pose l'étendue verticale de la bande — la hauteur que la couche
     * lui donne.
     *
     * @param spanPx l'étendue, en pixels
     */
    public void setSpanPx(int spanPx) {
        this.spanPx = Math.max(1, spanPx);
        requestLayout();
        invalidate();
    }

    /**
     * Le rappel du saut — reçoit la LIGNE visée par le cran touché.
     *
     * @param rappel le consommateur de ligne
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
        if (charges.length == 0) {
            return;
        }
        float cranPx = getHeight() / (float) charges.length;
        float hauteurTrait = Math.max(1f, cranPx - 1f);
        float disponible = getWidth() - 2f * margePx;
        for (int cran = 0; cran < charges.length; cran++) {
            float haut = cran * cranPx;
            boolean dansFenetre = cran >= cranFenetre
                    && cran < cranFenetre + cransFenetre;
            pinceau.setColor(dansFenetre ? palette.accent() : palette.muted());
            float largeur = charges[cran] * disponible
                    / (float) MinimapModel.CHARGE_MAX;
            if (largeur <= 0f) {
                continue;
            }
            canvas.drawRect(margePx, haut, margePx + largeur,
                    Math.min(getHeight(), haut + hauteurTrait), pinceau);
        }
    }

    /** La ligne visée par l'ordonnée du doigt — la première du bouquet du cran. */
    private int ligneVisee(float y) {
        int cran = Math.round(y * charges.length / (float) getHeight());
        cran = Math.max(0, Math.min(charges.length - 1, cran));
        return (int) ((long) cran * lignes / Math.max(1, charges.length));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                doigtPose = true;
                return sauter(event.getY());
            case MotionEvent.ACTION_MOVE:
                return doigtPose && sauter(event.getY());
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (doigtPose) {
                    doigtPose = false;
                    performClick();
                    return true;
                }
                return false;
            default:
                return false;
        }
    }

    /** Saute à la ligne visée — le rappel part, le cran se lit au doigt. */
    private boolean sauter(float y) {
        if (onJump == null || getHeight() <= 0) {
            return true;
        }
        onJump.accept(ligneVisee(y));
        return true;
    }
}
