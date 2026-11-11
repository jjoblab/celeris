package jo.celeris.android.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

import java.util.function.DoubleConsumer;

import org.jspecify.annotations.Nullable;

/**
 * Le défilement rapide (chantier C9) : le pouce au bord droit suit le
 * défilement — sa position lit la fraction verticale, sa hauteur la
 * part visible du document. Le glissement l'emporte : chaque mouvement
 * du doigt rend la fraction visée au rappel, le pouce GROSSIT sous le
 * doigt — le retour du geste, à la manière du kit de la plateforme.
 * Pas un panneau : une bande nue au bord droit de la couche, pleine
 * hauteur, muette sans défilement possible.
 */
public final class FastScrollerView extends View {

    /** La largeur tactile de la bande, en dp. */
    private static final float LARGEUR_DP = 16f;

    /** La largeur peinte du pouce au repos, en dp. */
    private static final float LARGEUR_POUCE_DP = 4f;

    /** La largeur peinte du pouce sous le doigt, en dp — le retour du geste. */
    private static final float LARGEUR_POUCE_POSE_DP = 8f;

    /** La hauteur minimale du pouce, en dp. */
    private static final float HAUTEUR_MIN_DP = 24f;

    private final Paint pinceau = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int largeurPx;
    private final float largeurPoucePx;
    private final float largeurPoucePosePx;
    private final float hauteurMinPx;
    /** L'étendue verticale posée par l'hôte — la couche donne sa hauteur. */
    private int spanPx = 1;
    /** La fraction verticale du défilement — 0 en tête, 1 en pied. */
    private float fraction;
    /** La part visible du document — le pouce la suit. */
    private float partVisible = 1f;
    /** Le doigt tient la bande : le pouce grossit, le glissement emporte. */
    private boolean doigtPose;
    private @Nullable DoubleConsumer onScroll;
    private PopupPalette palette;

    public FastScrollerView(Context context, PopupPalette palette) {
        super(context, null);
        float densite = getResources().getDisplayMetrics().density;
        this.largeurPx = Math.round(LARGEUR_DP * densite);
        this.largeurPoucePx = LARGEUR_POUCE_DP * densite;
        this.largeurPoucePosePx = LARGEUR_POUCE_POSE_DP * densite;
        this.hauteurMinPx = HAUTEUR_MIN_DP * densite;
        poserPalette(palette);
    }

    /**
     * Remplace la palette — l'encre du pouce suit le thème de l'hôte.
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
     * Pose l'état du défilement : la fraction verticale courante et la
     * part visible du document.
     *
     * @param fraction    la fraction du défilement (0 en tête, 1 en pied)
     * @param partVisible la part visible du document (1 quand tout tient)
     */
    public void setEtat(float fraction, float partVisible) {
        this.fraction = borne(fraction);
        this.partVisible = borne(partVisible);
        invalidate();
    }

    private static float borne(float valeur) {
        return Math.max(0f, Math.min(1f, valeur));
    }

    /**
     * Le rappel du défilement — reçoit la FRACTION visée par le doigt.
     *
     * @param rappel le consommateur de fraction
     */
    public void setOnScroll(@Nullable DoubleConsumer rappel) {
        onScroll = rappel;
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension(resolveSize(largeurPx, widthSpec),
                resolveSize(spanPx, heightSpec));
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

    /** La hauteur du pouce — la part visible de la bande, au plancher. */
    private float hauteurPouce() {
        return Math.max(hauteurMinPx, borne(partVisible) * getHeight());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (partVisible >= 1f && !doigtPose) {
            // tout tient : le pouce se tait — rien à dérouler vite
            return;
        }
        float hauteur = hauteurPouce();
        float course = getHeight() - hauteur;
        float haut = course * fraction;
        float largeur = doigtPose ? largeurPoucePosePx : largeurPoucePx;
        float droite = getWidth();
        pinceau.setColor(doigtPose ? palette.accent() : palette.muted());
        canvas.drawRect(droite - largeur, haut, droite,
                haut + hauteur, pinceau);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                doigtPose = true;
                glisser(event.getY());
                return true;
            case MotionEvent.ACTION_MOVE:
                if (doigtPose) {
                    glisser(event.getY());
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (doigtPose) {
                    doigtPose = false;
                    performClick();
                    invalidate();
                    return true;
                }
                return false;
            default:
                return false;
        }
    }

    /**
     * Le glissement rend la fraction visée : le doigt porte le HAUT du
     * pouce — la fraction suit la course du pouce, jamais hors des
     * bornes.
     */
    private void glisser(float y) {
        float hauteur = hauteurPouce();
        float course = Math.max(1f, getHeight() - hauteur);
        float visee = borne((y - hauteur / 2f) / course);
        if (onScroll != null) {
            onScroll.accept(visee);
        }
        invalidate();
    }
}
