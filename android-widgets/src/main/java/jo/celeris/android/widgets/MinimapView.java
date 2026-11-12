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
 * La minimap (chantier C9, reprise du design) : la silhouette COLORÉE
 * du document au bord droit, tisée SERRÉE — chaque rangée de la carte
 * ({@link MinimapModel}) remplit ENTIÈREMENT son cran : une fine
 * brique de segments encrés par rangée, jamais de trou entre deux
 * briques, jamais de chevauchement quand les crans se resserrent.
 * Le document court garde sa hauteur HONNÊTE : une rangée par cran
 * (une par ligne, le plus souvent), la carte ne s'étire pas pour
 * remplir la bande — elle commence en haut, comme une vraie
 * miniature ; le document long resserre ses crans dans l'étendue
 * posée.
 *
 * <p>Le fond de la bande assombrit celui de l'hôte, un liseré le
 * sépare. Les encres se posent ATTÉNUÉES — la silhouette se lit à
 * la couleur du code sans crier : la chaîne à l'accent adouci, le
 * mot-clé à son encre tamisée, le commentaire à l'atténuée, le
 * nombre à l'info adoucie, le code nu à l'encre de texte assombrie.
 * La fenêtre visible se rend en CARTOUCHE : remplissage ténu puis
 * contour arrondi — et le contour ne fuit jamais vers la frame
 * suivante (la discipline de restauration du paint, ★ : le paint
 * partagé reprêté en STROKE y rendait la sélection suivante vide).</p>
 *
 * <p>Pas un panneau : une bande nue au bord droit de la couche,
 * pleine hauteur. L'appui et le glissement sautent : la ligne visée
 * part au rappel, le cran se lit à l'ordonnée du doigt. Ne cède ni à
 * la frappe ni au défilement : elle vit avec le document, l'hôte
 * l'ouvre et la ferme.</p>
 */
public final class MinimapView extends View {

    /** La largeur de la bande, en dp — celle du dépôt de départ. */
    private static final float LARGEUR_DP = 60f;

    /** La marge horizontale du contenu, en dp. */
    private static final float MARGE_DP = 5f;

    /** La hauteur nominale d'une rangée de la carte, en dp. */
    private static final float HAUTEUR_TRAIT_DP = 2.5f;

    /** L'assombrissement du fond de la bande — 0,85 comme là-bas. */
    private static final float ASSOMBRISSEMENT_FOND = 0.85f;

    /** L'alpha du REMPLI de la fenêtre visible. */
    private static final float ALPHA_FENETRE_REMPLI = 0.18f;

    /** L'alpha du CONTOUR de la fenêtre visible. */
    private static final float ALPHA_FENETRE_CONTOUR = 0.55f;

    /** Le rayon de la cartouche de la fenêtre visible, en dp. */
    private static final float RAYON_FENETRE_DP = 3f;

    /** L'atténuation des encres de silhouette — la couleur sans le cri. */
    private static final float ATTENUATION_ENCRE = 0.8f;

    /** L'atténuation de l'encre du commentaire — déjà atténuée au thème. */
    private static final float ATTENUATION_COMMENTAIRE = 0.9f;

    private final Paint pinceau = new Paint(Paint.ANTI_ALIAS_FLAG);
    /** Le rect de la fenêtre visible — réutilisé, jamais alloué par image. */
    private final RectF fenetreRect = new RectF();
    private final int largeurPx;
    private final int margePx;
    private final float hauteurTraitPx;
    private final float rayonFenetrePx;
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
        this.rayonFenetrePx = RAYON_FENETRE_DP * densite;
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
     * lui donne ; la carte n'en prend que ce que ses rangées
     * honnêtement remplissent.
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
        // La hauteur honnête du contenu : ses rangées nominales — une
        // carte de dix lignes ne s'étire pas sur mille pixels, elle
        // prend ses dix briques et laisse le reste à la couche ; la
        // carte longue plafonne à l'étendue posée.
        int hauteur = (int) Math.min((long) spanPx,
                Math.round(crans.length * (double) hauteurTraitPx));
        setMeasuredDimension(resolveSize(largeurPx, widthSpec),
                resolveSize(Math.max(1, hauteur), heightSpec));
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
        // La brique REMPLIT son cran : la silhouette se tisse serrée —
        // jamais de trou entre deux rangées, jamais de chevauchement
        // quand le document resserre ses crans sous le trait nominal.
        float cranPx = getHeight() / (float) crans.length;
        for (int cran = 0; cran < crans.length; cran++) {
            float haut = cran * cranPx;
            if (haut >= getHeight()) {
                break;
            }
            tisserBrique(canvas, crans[cran], haut,
                    Math.min(getHeight(), haut + cranPx));
        }
        dessinerFenetre(canvas);
    }

    /**
     * Tisse la brique d'un cran : la rangée de segments se rend à SON
     * encre adoucie, chaque segment une barre proportionnelle à ses
     * colonnes — plafonnée à la bande, jamais plus fine qu'un demi
     * pixel.
     *
     * @param canvas le canvas de la bande
     * @param rangee la rangée du cran, charge et segments compris
     * @param haut   le bord haut de la brique, en pixels
     * @param bas    le bord bas de la brique, en pixels
     */
    private void tisserBrique(Canvas canvas, MinimapModel.MapRow rangee,
            float haut, float bas) {
        if (rangee.load() <= 0) {
            return; // la rangée vide ne tisse rien — le blanc respire
        }
        float disponible = getWidth() - 2f * margePx;
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
            canvas.drawRect(x, haut, x + largeur, bas, pinceau);
            x += largeur;
            if (x >= getWidth() - margePx) {
                break;
            }
        }
    }

    /**
     * La fenêtre visible en CARTOUCHE : remplie à la teinte de
     * sélection ténue, PUIS contournée arrondi à six dixièmes — façon
     * scrollbar, et le paint reprêté en STROKE y retourne à FILL
     * avant de rendre la main (la discipline ★ du dépôt de départ :
     * le contour du viewport ne doit jamais fuir vers la frame
     * suivante).
     */
    private void dessinerFenetre(Canvas canvas) {
        if (cranFenetre < 0 || cransFenetre <= 0 || crans.length == 0) {
            return;
        }
        float cranPx = getHeight() / (float) crans.length;
        float haut = cranFenetre * cranPx;
        float bas = Math.min(getHeight(),
                (cranFenetre + cransFenetre) * cranPx);
        // la cartouche respire d'un pixel aux bords : l'arrondi ne
        // ronge jamais le liseré de la bande
        fenetreRect.set(1f, haut, getWidth() - 1f, Math.max(haut + 1f, bas));
        pinceau.setColor(avecAlpha(palette.selection(),
                ALPHA_FENETRE_REMPLI));
        pinceau.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(fenetreRect, rayonFenetrePx, rayonFenetrePx,
                pinceau);
        pinceau.setStyle(Paint.Style.STROKE);
        pinceau.setStrokeWidth(1.25f);
        pinceau.setColor(avecAlpha(palette.selection(),
                ALPHA_FENETRE_CONTOUR));
        canvas.drawRoundRect(fenetreRect, rayonFenetrePx, rayonFenetrePx,
                pinceau);
        // ★ le paint partagé retrouve son FILL — aucune trace du
        // contour ne fuit vers la frame suivante
        pinceau.setStyle(Paint.Style.FILL);
        pinceau.setStrokeWidth(0f);
    }

    /**
     * L'encre d'une classe de segment, ADOUCIE : le code à l'encre de
     * texte assombrie de moitié (la silhouette reste discrète), la
     * chaîne à l'accent atténué, le commentaire à l'atténuée, le
     * nombre à l'info adoucie, le mot-clé à son encre tamisée — la
     * couleur se lit, elle ne crie pas.
     */
    private int encreDe(int classe) {
        return switch (classe) {
            case MinimapModel.ENCRE_CHAINE -> avecAlpha(palette.accent(),
                    ATTENUATION_ENCRE);
            case MinimapModel.ENCRE_COMMENTAIRE -> avecAlpha(palette.muted(),
                    ATTENUATION_COMMENTAIRE);
            case MinimapModel.ENCRE_NOMBRE -> avecAlpha(palette.info(),
                    ATTENUATION_ENCRE);
            case MinimapModel.ENCRE_MOTCLE -> avecAlpha(palette.keyword(),
                    ATTENUATION_ENCRE);
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
