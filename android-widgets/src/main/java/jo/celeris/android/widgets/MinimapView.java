package jo.celeris.android.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import java.util.function.IntConsumer;

import org.jspecify.annotations.Nullable;

/**
 * La minimap (chantier C9, le rendu du dépôt de départ) : la silhouette
 * COLORÉE du document au bord droit — chaque rangée de la carte
 * ({@link MinimapModel}) se rend en une rangée de segments encrés,
 * chaque segment en une fine barre horizontale proportionnelle à ses
 * colonnes, la hauteur d'un trait fixe comme là-bas (2,5 dp). Le fond
 * de la bande assombrit celui de l'hôte, un liseré le sépare ; la
 * fenêtre visible se rend remplie PUIS contournée — et le contour ne
 * fuit jamais vers la frame suivante (la discipline de restauration
 * du dépôt de départ, ★ : le paint partagé reprêté en STROKE y rendait
 * la sélection suivante vide).
 *
 * <p>Pas un panneau : une bande nue au bord droit de la couche, pleine
 * hauteur. L'appui et le glissement sautent : la ligne visée part au
 * rappel, le cran se lit à l'ordonnée du doigt. Ne cède ni à la frappe
 * ni au défilement : elle vit avec le document, l'hôte l'ouvre et la
 * ferme.</p>
 */
public final class MinimapView extends View {

    /** La largeur de la bande, en dp — celle du dépôt de départ. */
    private static final float LARGEUR_DP = 60f;

    /** La marge horizontale du contenu, en dp. */
    private static final float MARGE_DP = 5f;

    /** La hauteur d'un trait de rangée, en dp — celle du dépôt de départ. */
    private static final float HAUTEUR_TRAIT_DP = 2.5f;

    /** L'assombrissement du fond de la bande — 0,85 comme là-bas. */
    private static final float ASSOMBRISSEMENT_FOND = 0.85f;

    /** L'alpha du REMPLI de la fenêtre visible. */
    private static final float ALPHA_FENETRE_REMPLI = 0.25f;

    /** L'alpha du CONTOUR de la fenêtre visible. */
    private static final float ALPHA_FENETRE_CONTOUR = 0.6f;

    private final Paint pinceau = new Paint(Paint.ANTI_ALIAS_FLAG);
    /** Le rect de la fenêtre visible — réutilisé, jamais alloué par image. */
    private final RectF fenetreRect = new RectF();
    private final int largeurPx;
    private final int margePx;
    private final float hauteurTraitPx;
    /** L'étendue verticale posée par l'hôte — la couche donne sa hauteur. */
    private int spanPx = 1;
    /** Les crans de la carte — charges et segments, dans l'ordre. */
    private MinimapModel.MapRow[] crans = new MinimapModel.MapRow[0];
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
        this.hauteurTraitPx = HAUTEUR_TRAIT_DP * densite;
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
     * @param model  la carte, charges et segments compris
     * @param lignes le nombre de lignes du document — la cible du saut
     */
    public void setModel(MinimapModel model, int lignes) {
        var rangees = model.rangees();
        this.crans = rangees.toArray(new MinimapModel.MapRow[0]);
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
        int total = Math.max(1, crans.length);
        int totales = Math.max(1, lignesTotales);
        this.cranFenetre = MinimapModel.cranDeLigne(
                Math.max(0, premiereLigneVisible), totales, total);
        int dernier = MinimapModel.cranDeLigne(
                Math.max(0, premiereLigneVisible + Math.max(1, lignesVisibles) - 1),
                totales, total);
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
        // Le fond de la bande : celui de l'hôte assombri (0,85 comme le
        // dépôt de départ) — la bande se voit, sans crier.
        canvas.drawRect(0f, 0f, getWidth(), getHeight(),
                pinceauFond(pinceau, palette));
        // Le liseré de séparation au bord gauche — 1 px, comme là-bas.
        pinceau.setColor(palette.border());
        canvas.drawLine(0f, 0f, 0f, getHeight(), pinceau);
        if (crans.length == 0) {
            return;
        }
        float cranPx = getHeight() / (float) crans.length;
        // le trait se CENTRE dans son cran : la silhouette garde un
        // rythme régulier même quand les crans dépassent le trait
        float traitY = Math.max(0f, (cranPx - hauteurTraitPx) * 0.5f);
        float disponible = getWidth() - 2f * margePx;
        for (int cran = 0; cran < crans.length; cran++) {
            float haut = cran * cranPx + traitY;
            if (haut > getHeight()) {
                break;
            }
            MinimapModel.MapRow rangee = crans[cran];
            if (rangee.load() <= 0) {
                continue;
            }
            // la rangée de segments : chaque segment se rend à SON
            // encre, sa largeur suit ses colonnes — plafonnée à la bande
            float x = margePx;
            for (MinimapModel.MapSegment segment : rangee.segments()) {
                if (segment.colonnes() <= 0) {
                    continue;
                }
                float largeur = Math.min(
                        segment.colonnes() * disponible
                                / (float) MinimapModel.CHARGE_MAX,
                        getWidth() - margePx - x);
                if (largeur < 0.5f) {
                    largeur = 0.5f;
                }
                pinceau.setColor(encreDe(segment.encre()));
                canvas.drawRect(x, haut, x + largeur,
                        Math.min(getHeight(), haut + hauteurTraitPx), pinceau);
                x += largeur;
                if (x >= getWidth() - margePx) {
                    break;
                }
            }
        }
        dessinerFenetre(canvas);
    }

    /**
     * La fenêtre visible : remplie à un quart d'alpha de la teinte de
     * sélection, PUIS contournée à six dixièmes — façon scrollbar, et
     * le paint reprêté en STROKE y retourne à FILL avant de rendre la
     * main (la discipline ★ du dépôt de départ : le contour du
     * viewport ne doit jamais fuir vers la frame suivante).
     */
    private void dessinerFenetre(Canvas canvas) {
        if (cranFenetre < 0 || cransFenetre <= 0 || crans.length == 0) {
            return;
        }
        float cranPx = getHeight() / (float) crans.length;
        float haut = cranFenetre * cranPx;
        float bas = Math.min(getHeight(),
                (cranFenetre + cransFenetre) * cranPx);
        fenetreRect.set(0f, haut, getWidth(), bas);
        pinceau.setColor(avecAlpha(palette.selection(),
                ALPHA_FENETRE_REMPLI));
        pinceau.setStyle(Paint.Style.FILL);
        canvas.drawRect(fenetreRect, pinceau);
        pinceau.setStyle(Paint.Style.STROKE);
        pinceau.setStrokeWidth(1f);
        pinceau.setColor(avecAlpha(palette.selection(),
                ALPHA_FENETRE_CONTOUR));
        canvas.drawRect(fenetreRect, pinceau);
        // ★ le paint partagé retrouve son FILL — aucune trace du
        // contour ne fuit vers la frame suivante
        pinceau.setStyle(Paint.Style.FILL);
        pinceau.setStrokeWidth(0f);
    }

    /**
     * L'encre d'une classe de segment : le code à l'encre de texte
     * assombrie de moitié (la silhouette reste discrète), la chaîne à
     * l'accent, le commentaire à l'atténuée, le nombre à l'info, le
     * mot-clé à son encre.
     */
    private int encreDe(int classe) {
        return switch (classe) {
            case MinimapModel.ENCRE_CHAINE -> palette.accent();
            case MinimapModel.ENCRE_COMMENTAIRE -> palette.muted();
            case MinimapModel.ENCRE_NOMBRE -> palette.info();
            case MinimapModel.ENCRE_MOTCLE -> palette.keyword();
            default -> assombrir(palette.text(), 0.55f);
        };
    }

    /** Le fond de la bande : celui de la palette, opaque et assombri. */
    private static Paint pinceauFond(Paint pinceau, PopupPalette palette) {
        pinceau.setStyle(Paint.Style.FILL);
        pinceau.setStrokeWidth(0f);
        pinceau.setColor(assombrirRenduOpaque(palette.background(),
                ASSOMBRISSEMENT_FOND));
        return pinceau;
    }

    /** Assombrit une couleur ARGB — opaque par construction. */
    private static int assombrir(int couleur, float facteur) {
        int r = (int) (((couleur >> 16) & 0xFF) * facteur);
        int g = (int) (((couleur >> 8) & 0xFF) * facteur);
        int b = (int) ((couleur & 0xFF) * facteur);
        return (couleur & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    /** Rend opaque puis assombrit — le fond de bande ne voit jamais au travers. */
    private static int assombrirRenduOpaque(int couleur, float facteur) {
        int opaque = (couleur & 0x00FFFFFF) | 0xFF000000;
        return assombrir(opaque, facteur);
    }

    /** L'alpha d'une teinte portée à son canal. */
    private static int avecAlpha(int couleur, float alpha) {
        int a = (int) (((couleur >>> 24) & 0xFF)
                * Math.max(0f, Math.min(1f, alpha)));
        return (a << 24) | (couleur & 0x00FFFFFF);
    }

    /** La ligne visée par l'ordonnée du doigt — la première du bouquet du cran. */
    private int ligneVisee(float y) {
        int cran = Math.round(y * crans.length / (float) getHeight());
        cran = Math.max(0, Math.min(crans.length - 1, cran));
        return (int) ((long) cran * lignes / Math.max(1, crans.length));
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
