package jo.celeris.android.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

import org.jspecify.annotations.Nullable;

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
 *
 * <p>La fluidité du propriétaire (2026-11-13 : « ajoute une petite
 * animation pour les popups pour qu'il soit plus fluide ») : la carte
 * NAÎT — voile qui monte, 120 ms, l'horloge du dessin la pilote, la
 * couche l'arme à sa pose — et MEURT — voile qui descend, 100 ms,
 * refermée par la couche AVANT son retrait. Le banc fige la naissance
 * ({@link #finirEntree()}) : le panneau dessiné hors couche reste
 * STEADY, jamais un banc ne rend un voile à demi.</p>
 */
abstract class PopupPanel extends View {

    /** La durée de la naissance de la carte, en millisecondes. */
    private static final long DUREE_NAISSANCE_MS = 120L;

    /** La durée de la mort de la carte, en millisecondes. */
    private static final long DUREE_MORT_MS = 100L;

    /** La montée de la naissance, en dp (le geste du dépôt de départ). */
    private static final float MONTEE_DP = 6f;

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
    /** Le voile de la naissance et de la mort — l'alpha de la carte. */
    private final Paint voileAnime = new Paint();
    private final RectF coins = new RectF();
    /** Le chemin de la carte — le clip arrondi du contenu (motif du dépôt de départ). */
    private final Path cheminCarte = new Path();
    private final float densite;
    /** La naissance s'arme à la pose — l'horloge du dessin l'ouvre. */
    private boolean naissanceArmee;
    /** L'instant de la naissance, —1 hors entrée. */
    private long naissanceA = -1L;
    /** La mort s'arme au renvoi — l'horloge du dessin l'ouvre. */
    private boolean morteArmee;
    /** L'instant de la mort, —1 hors sortie. */
    private long morteA = -1L;
    /** Le retrait différé de la vue morte — la couche y passe. */
    private @Nullable Runnable retrait;

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

    /** L'alpha courant du voile — 1 en steady, la naissance × mort
     *  l'abaissent pendant leur horloge. */
    private float alphaVoile = 1f;

    /** La montée courante de la naissance, en pixels. */
    private float monteeVoile;

    /**
     * Arme la naissance de la carte — la couche y appelle à sa pose :
     * la prochaine image ouvre l'horloge, la carte monte en voile.
     * Le panneau dessiné hors couche reste STEADY sans elle.
     */
    void naissance() {
        naissanceArmee = true;
    }

    /**
     * Arme la mort de la carte — la couche y appelle AVANT son retrait :
     * la carte descend en voile, puis le retrait différé part.
     *
     * @param retrait le retrait de la vue, à la fin du voile
     */
    void mourir(Runnable retrait) {
        this.retrait = retrait;
        morteArmee = true;
        postInvalidateOnAnimation();
    }

    /**
     * Défait la mort armée — la carte re-posée AVANT la fin de son
     * voile ne se retire pas d'elle-même : le retrait différé se
     * dissout, l'horloge se ferme (le filtre de complétion re-pose la
     * MÊME vue à chaque frappe, jamais la carte vivante ne meurt de
     * sa re-pose).
     */
    void annulerMort() {
        morteArmee = false;
        morteA = -1L;
        retrait = null;
    }

    /**
     * Vrai pendant la naissance ou la mort de la carte — le banc y lit
     * l'animation en cours, la couche y distingue le voile du steady.
     */
    boolean voileEnCours() {
        return naissanceArmee || naissanceA >= 0L
                || morteArmee || morteA >= 0L;
    }

    /**
     * Fige la naissance — le banc y passe : le panneau rendu hors
     * couche reste STEADY, la carte se dessine sans voile.
     */
    void finirEntree() {
        naissanceArmee = false;
        naissanceA = -1L;
    }

    @Override
    protected final void onDraw(Canvas canvas) {
        avancerVoile();
        boolean anime = alphaVoile < 1f || monteeVoile > 0f;
        int sauve = -1;
        if (anime) {
            voileAnime.setAlpha(Math.round(alphaVoile * 255f));
            sauve = canvas.saveLayer(0f, 0f, getWidth(), getHeight(),
                    voileAnime);
            canvas.translate(0f, -monteeVoile);
        }
        dessinerCarte(canvas);
        if (anime) {
            canvas.restoreToCount(sauve);
        }
    }

    /**
     * Le voile avance d'une image : la naissance armée ouvre son
     * horloge (120 ms de montée), la mort armée conclut la sienne
     * (100 ms de descente, puis le retrait différé part) — les champs
     * portent l'alpha et la montée que le tracé sert.
     */
    private void avancerVoile() {
        alphaVoile = 1f;
        monteeVoile = 0f;
        if (naissanceArmee) {
            naissanceA = getDrawingTime();
            naissanceArmee = false;
        }
        if (naissanceA >= 0L) {
            long ecoule = getDrawingTime() - naissanceA;
            if (ecoule >= DUREE_NAISSANCE_MS) {
                naissanceA = -1L;
            } else {
                // la carte naît d'en haut, adoucie (le dépôt de départ)
                alphaVoile = Math.max(0f, Math.min(1f,
                        ecoule / (float) DUREE_NAISSANCE_MS));
                monteeVoile = (1f - alphaVoile) * MONTEE_DP * densite;
                postInvalidateOnAnimation();
            }
        }
        avancerMort();
    }

    /**
     * La mort avance d'une image : le voile descend, 100 ms — la
     * conclusion pose le retrait différé, la carte reste STEADY
     * jusqu'à ce qu'il l'emporte (jamais un retrait brusque).
     */
    private void avancerMort() {
        if (morteArmee) {
            morteA = getDrawingTime();
            morteArmee = false;
        }
        if (morteA < 0L) {
            return;
        }
        long ecoule = getDrawingTime() - morteA;
        if (ecoule >= DUREE_MORT_MS) {
            morteA = -1L;
            Runnable retraitDiffere = retrait;
            retrait = null;
            if (retraitDiffere != null) {
                post(retraitDiffere);
            }
            return;
        }
        alphaVoile *= Math.max(0f, 1f - ecoule / (float) DUREE_MORT_MS);
        postInvalidateOnAnimation();
    }

    /**
     * La carte se trace : le verre et son liseré hairline, le contenu
     * clippé aux coins arrondis (le motif du dépôt de départ — la bande
     * de sélection d'une rangée ne déborde jamais sur les coins).
     */
    private void dessinerCarte(Canvas canvas) {
        coins.set(0f, 0f, getWidth(), getHeight());
        float rayon = rayonCartePx();
        cheminCarte.reset();
        cheminCarte.addRoundRect(coins, rayon, rayon, Path.Direction.CW);
        canvas.drawPath(cheminCarte, pinceauFond);
        canvas.drawPath(cheminCarte, pinceauLisere);
        int sauveContenu = canvas.save();
        canvas.clipPath(cheminCarte);
        canvas.translate(paddingPx, paddingYPx);
        drawContent(canvas);
        canvas.restoreToCount(sauveContenu);
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
