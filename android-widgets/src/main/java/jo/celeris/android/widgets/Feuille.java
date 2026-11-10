package jo.celeris.android.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Build;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.jspecify.annotations.Nullable;

/**
 * La feuille de l'ADR-018 : une surface fonctionnelle modale qui glisse
 * depuis un bord de l'écran — la feuille du bas et la feuille du haut en
 * dérivent, la popup ancrée vit déjà dans la couche ({@link PopupLayer}).
 *
 * <p>La feuille n'est PAS une popup ancrée : elle peut prendre le focus —
 * elle porte des champs et des commandes, c'est une surface modale
 * complète, le voile bloque la scène derrière. PIEGE-27 ne s'y applique
 * pas : le piège interdit à la COUCHE de voler le focus de l'éditeur ;
 * la feuille se pose sur le cadre de contenu de la fenêtre, hors de la
 * couche, et rend le focus en se fermant.</p>
 *
 * <p>Le chrome vit ici : poignée de glissement (aire tactile du kit,
 * pilule décorative muette à l'accessibilité — la maquette la porte
 * {@code aria-hidden}), rangée de titre avec bouton de fermeture, corps
 * défilant. Les fermetures : bouton, voile, glissement au-delà du seuil,
 * retour — l'hôte câble la touche retour sur {@link #surRetour()}. Le
 * verre et le liseré se dessinent comme la carte des popups (un pixel,
 * les encres de la palette) ; les jetons vivent dans {@link PopupTokens},
 * la maquette de référence est {@code docs/maquettes/popups.html}.</p>
 */
abstract class Feuille extends LinearLayout {

    /** La poignée : aire tactile du kit, pilule muette, geste de fermeture. */
    private final Poignee poignee;
    /** Le titre de la feuille — l'hôte le pose. */
    private final TextView vueTitre;
    /** Le bouton de fermeture — sa cible fait au moins celle du kit. */
    private final TextView boutonFermer;
    /** Le corps défilant — l'hôte y pose son contenu. */
    private final ScrollView corps;

    /** Le voile de la scène — posé dans l'hôte, cliqué il ferme. */
    private @Nullable View voile;
    /** L'hôte courant — un cadre de type FrameLayout. */
    private @Nullable ViewGroup hote;
    /** Le rappel de fermeture — l'hôte y réapplique son état. */
    private @Nullable Runnable surFermeture;
    private boolean ouverte;

    /** La palette posée — le verre suit le thème de l'éditeur hôte. */
    private PopupPalette palette;

    private final Paint pinceauFond = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pinceauLisere = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pinceauPoignee = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path cheminCarte = new Path();
    private final RectF coins = new RectF();
    private final float densite;
    /** Les rayons de la carte en pixels — calculés une fois, jamais mutés. */
    private @Nullable float[] rayonsPxCache;

    Feuille(Context context, PopupPalette palette) {
        super(context);
        setOrientation(LinearLayout.VERTICAL);
        // La feuille est modale et porte des champs : elle prend le
        // focus — hors de la couche, PIEGE-27 ne la concerne pas.
        setFocusable(true);
        setFocusableInTouchMode(true);
        setWillNotDraw(false);
        this.densite = getResources().getDisplayMetrics().density;
        this.palette = palette;

        poignee = new Poignee(context);
        vueTitre = new TextView(context);
        boutonFermer = new TextView(context);
        corps = new ScrollView(context);

        poserChrome(context);
        poserContour();
        // Le voile et la feuille respirent avec les barres : la feuille
        // se cale sur la barre de son bord (bas : barres et clavier,
        // haut : barre d'état), même quand l'hôte consomme les encarts.
        setOnApplyWindowInsetsListener((vue, encarts) -> {
            poserEncarts(encarts);
            return encarts;
        });
        poserPalette(palette);
    }

    /** Pose le chrome : poignée, rangée de titre, corps défilant. */
    private void poserChrome(Context context) {
        int cible = Math.round(PopupTokens.CIBLE_MIN_FEUILLE_DP * densite);
        int respireX = Math.round(PopupTokens.RESPIRATION_X_DP * densite);
        int respireY = Math.round(PopupTokens.RESPIRATION_Y_DP * densite);

        vueTitre.setLayoutParams(
                new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        boutonFermer.setText("\u00d7");
        boutonFermer.setGravity(Gravity.CENTER);
        boutonFermer.setMinimumWidth(cible);
        boutonFermer.setMinimumHeight(cible);
        boutonFermer.setOnClickListener(vue -> cacher());
        LinearLayout rangeeTitre = new LinearLayout(context);
        rangeeTitre.setOrientation(LinearLayout.HORIZONTAL);
        rangeeTitre.setGravity(Gravity.CENTER_VERTICAL);
        rangeeTitre.setPadding(respireX, 0, respireX / 2, 0);
        rangeeTitre.addView(vueTitre);
        rangeeTitre.addView(boutonFermer, new LayoutParams(cible, cible));

        corps.setFillViewport(true);
        corps.setPadding(respireX, 0, respireX, respireY);
        int aire = Math.round(PopupTokens.POIGNEE_AIRE_DP * densite);
        addView(poignee, new LayoutParams(LayoutParams.MATCH_PARENT, aire));
        addView(rangeeTitre, new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        addView(corps, new LayoutParams(
                LayoutParams.MATCH_PARENT, 0, 1f));
    }

    /** Pose le contour arrondi : les enfants se clippent aux coins de
     *  la carte. Avant l'API 30, le chemin n'existe pas — le repli
     *  arrondit les quatre coins, à un poil près le même verre. */
    private void poserContour() {
        setClipToOutline(true);
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View vue, Outline contour) {
                if (Build.VERSION.SDK_INT >= 30) {
                    // Le chemin est convexe (coins ouverts d'un seul
                    // côté) : le clip tient au premier niveau qui le porte.
                    cheminCarte.reset();
                    coins.set(0f, 0f, vue.getWidth(), vue.getHeight());
                    cheminCarte.addRoundRect(coins, rayonsPx(),
                            Path.Direction.CW);
                    contour.setPath(cheminCarte);
                } else {
                    contour.setRoundRect(0, 0, vue.getWidth(),
                            vue.getHeight(),
                            PopupTokens.RAYON_FEUILLE_DP * densite);
                }
            }
        });
    }

    // ------------------------------------------------------------ formes

    /** La gravité de la feuille dans son hôte FrameLayout. */
    abstract int gravite();

    /** La largeur au plus de la feuille, en dp. */
    abstract float largeurMaxDp();

    /** Les rayons de la carte, huit valeurs (quatre coins, x puis y). */
    abstract float[] rayonsDp();

    /** Vrai quand la feuille glisse depuis le BAS de l'écran. */
    abstract boolean coteBas();

    // ------------------------------------------------------------ surface

    /**
     * Remplace la palette de la feuille — le verre suit le thème de
     * l'éditeur hôte, les encres et la typographie avec lui.
     *
     * @param nouvelle la palette dérivée du thème courant
     */
    public final void setPalette(PopupPalette nouvelle) {
        poserPalette(nouvelle);
        invalidate();
    }

    /** Pose le titre de la feuille. */
    public final void titre(CharSequence nouveau) {
        vueTitre.setText(nouveau);
    }

    /**
     * Pose le libellé du bouton de fermeture — l'accessibilité l'exige
     * (ADR-008) : le glyphe seul ne dit rien au lecteur d'écran.
     */
    public final void libelleFermeture(CharSequence libelle) {
        boutonFermer.setContentDescription(libelle);
    }

    /**
     * Pose le contenu de la feuille dans son corps défilant — l'ancien
     * s'en va, l'hôte garde la main sur les tiens.
     *
     * @param contenu la vue de contenu, remplie par l'hôte
     */
    public final void contenu(View contenu) {
        corps.removeAllViews();
        corps.addView(contenu, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));
    }

    /**
     * Le rappel de fermeture — invoqué quand la feuille se referme, par
     * le bouton, le voile, le glissement ou le retour.
     *
     * @param rappel invoqué à la fermeture, null pour l'ôter
     */
    public final void setSurFermeture(@Nullable Runnable rappel) {
        surFermeture = rappel;
    }

    // ------------------------------------------------------------ ouverture

    /**
     * Montre la feuille dans l'hôte donné : le voile se pose sur la
     * scène, la feuille glisse depuis son bord. L'hôte doit être un
     * cadre superposé — le cadre de contenu de la fenêtre
     * ({@code android.R.id.content}) convient toujours.
     *
     * @param hoteCourant le cadre hôte, de type FrameLayout
     */
    public final void montrer(ViewGroup hoteCourant) {
        if (ouverte) {
            return;
        }
        if (!(hoteCourant instanceof FrameLayout)) {
            throw new IllegalStateException("Sheet host must be a FrameLayout");
        }
        this.hote = hoteCourant;
        voile = new View(hoteCourant.getContext());
        voile.setBackgroundColor(couleurVoile());
        voile.setOnClickListener(vue -> cacher());
        hoteCourant.addView(voile, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        hoteCourant.addView(this, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, gravite()));
        ouverte = true;
        setTranslationY(decalageEntreePx());
        if (animationsActives()) {
            animate().translationY(0f)
                    .setDuration(PopupTokens.DUREE_FEUILLE_MS);
        } else {
            setTranslationY(0f);
        }
        // Les encarts arrivent par l'écouteur quand la fenêtre les
        // distribue ; l'hôte peut les consommer avant — la racine les
        // tient toujours.
        post(() -> {
            if (getRootWindowInsets() != null) {
                poserEncarts(getRootWindowInsets());
            }
        });
    }

    /**
     * Referme la feuille — le voile s'en va, la feuille glisse vers son
     * bord, le rappel de fermeture court à la fin du geste.
     */
    public final void cacher() {
        ViewGroup hoteCourant = hote;
        View voileCourant = voile;
        if (!ouverte || hoteCourant == null || voileCourant == null) {
            return;
        }
        int decalage = decalageEntreePx();
        ouverte = false;
        hote = null;
        voile = null;
        hoteCourant.removeView(voileCourant);
        // La fermeture LOGIQUE est immédiate — le rappel et l'état ne
        // dépendent d'aucune horloge ; le glissement de sortie n'est
        // qu'une grâce cosmétique qui détache la vue à sa fin.
        Runnable apres = surFermeture;
        if (apres != null) {
            apres.run();
        }
        if (animationsActives()) {
            setTranslationY(0f);
            animate().translationY(decalage)
                    .setDuration(PopupTokens.DUREE_FEUILLE_MS)
                    .withEndAction(() -> hoteCourant.removeView(Feuille.this));
        } else {
            hoteCourant.removeView(this);
        }
    }

    /** Vrai quand la feuille est montrée. */
    public final boolean estOuverte() {
        return ouverte;
    }

    /**
     * La touche retour rencontre la feuille : ouverte, elle se referme
     * et le retour s'y consomme ; fermée, il poursuit son chemin.
     *
     * @return vrai quand la feuille a consommé le retour
     */
    public final boolean surRetour() {
        if (!ouverte) {
            return false;
        }
        cacher();
        return true;
    }

    // ------------------------------------------------------------ mesure

    @Override
    protected void onMeasure(int largeurSpec, int hauteurSpec) {
        super.onMeasure(largeurSpec, hauteurSpec);
        int largeur = Math.min(getMeasuredWidth(),
                MeasureSpec.getSize(largeurSpec));
        largeur = Math.min(largeur, Math.round(largeurMaxDp() * densite));
        int hauteurMax = Math.round(MeasureSpec.getSize(hauteurSpec)
                * PopupTokens.PART_HAUTEUR_FEUILLE);
        int hauteur = Math.min(getMeasuredHeight(), hauteurMax);
        if (largeur != getMeasuredWidth() || hauteur != getMeasuredHeight()) {
            // Seconde passe aux dimensions retenues : les enfants s'y
            // replient — le corps défilant prend ce qui reste.
            super.onMeasure(
                    MeasureSpec.makeMeasureSpec(largeur, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(hauteur, MeasureSpec.EXACTLY));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        cheminCarte.reset();
        coins.set(0f, 0f, getWidth(), getHeight());
        cheminCarte.addRoundRect(coins, rayonsPx(), Path.Direction.CW);
        canvas.drawPath(cheminCarte, pinceauFond);
        pinceauLisere.setStyle(Paint.Style.STROKE);
        pinceauLisere.setStrokeWidth(1f);
        canvas.drawPath(cheminCarte, pinceauLisere);
    }

    // ------------------------------------------------------------ internes

    /** La vue de titre — lisibilité des bancs du kit. */
    final TextView titreVue() {
        return vueTitre;
    }

    /** Le bouton de fermeture — lisibilité des bancs du kit. */
    final TextView boutonFermeture() {
        return boutonFermer;
    }

    /** La poignée de glissement — lisibilité des bancs du kit. */
    final View poigneeVue() {
        return poignee;
    }

    /** Le voile courant, null feuille fermée — lisibilité des bancs. */
    final @Nullable View voileVue() {
        return voile;
    }

    private void poserPalette(PopupPalette nouvelle) {
        palette = nouvelle;
        pinceauFond.setColor(nouvelle.background());
        pinceauLisere.setColor(nouvelle.border());
        vueTitre.setTextColor(nouvelle.text());
        boutonFermer.setTextColor(nouvelle.text());
        vueTitre.setTextSize(nouvelle.textBaseSp()
                * PopupTokens.TEXTE_TITRE);
    }

    /** La couleur du voile — la maquette : encre à 46 % sur verre clair,
     *  noir à 52 % sur verre sombre. */
    private int couleurVoile() {
        boolean sombre = (palette.background() & 0xFFFFFF) < 0x808080;
        return sombre ? 0x85000000 : 0x750F1218;
    }

    /** Vrai quand le système veut encore ses animations — le mouvement
     *  réduit les coupe toutes (maquette, ADR-008). */
    private boolean animationsActives() {
        return Settings.Global.getFloat(getContext().getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f;
    }

    /** Cale la feuille sur la barre de son bord — bas : barres et
     *  clavier ; haut : barre d'état. */
    @SuppressWarnings("deprecation")
    // Les encarts d'avant l'API 30 ne se lisent que par les méthodes
    // depreciees du SDK — la garde de version les garde vivantes.
    private void poserEncarts(WindowInsets encarts) {
        int encart;
        if (Build.VERSION.SDK_INT >= 30) {
            if (coteBas()) {
                encart = encarts.getInsets(WindowInsets.Type.systemBars()
                        | WindowInsets.Type.ime()).bottom;
            } else {
                encart = encarts.getInsets(WindowInsets.Type.systemBars()).top;
            }
        } else {
            encart = coteBas() ? encarts.getSystemWindowInsetBottom()
                    : encarts.getSystemWindowInsetTop();
        }
        if (coteBas()) {
            setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(),
                    Math.max(getPaddingBottom(), encart));
        } else {
            setPadding(getPaddingLeft(), Math.max(getPaddingTop(), encart),
                    getPaddingRight(), getPaddingBottom());
        }
    }

    /** Le décalage d'entrée — hors écran avant le glissement : la
     *  hauteur de l'hôte du côté d'où la feuille glisse. */
    private int decalageEntreePx() {
        ViewGroup hoteCourant = hote;
        int hauteur = hoteCourant != null && hoteCourant.getHeight() > 0
                ? hoteCourant.getHeight() : Math.max(1, getHeight());
        return coteBas() ? hauteur : -hauteur;
    }

    private float[] rayonsPx() {
        float[] cache = rayonsPxCache;
        if (cache == null) {
            float[] dp = rayonsDp();
            cache = new float[dp.length];
            for (int i = 0; i < dp.length; i++) {
                cache[i] = dp[i] * densite;
            }
            rayonsPxCache = cache;
        }
        return cache;
    }

    /** Le seuil de fermeture du glissement, en pixels. */
    private int seuilGlissPx() {
        return Math.round(PopupTokens.SEUIL_GLISS_FEUILLE_DP * densite);
    }

    /** Le sens du geste qui emporte la feuille : +1 vers le bas (feuille
     *  du bas), -1 vers le haut (feuille du haut). */
    private int sensFermeture() {
        return coteBas() ? 1 : -1;
    }

    /**
     * La poignée de glissement : une aire tactile du kit porte la
     * pilule décorative ; le geste vers la fermeture translate la
     * feuille, lâché au-delà du seuil il l'emporte, sinon elle
     * revient. Muette à l'accessibilité — la maquette la dit
     * {@code aria-hidden} : fermer reste au bouton, au voile et au
     * retour.
     */
    private final class Poignee extends View {

        private float debutY;
        private float translationDebut;
        private boolean gesteEnCours;

        Poignee(Context context) {
            super(context);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float demiLargeur =
                    PopupTokens.POIGNEE_LARGEUR_DP * densite / 2f;
            float demiEpaisseur =
                    PopupTokens.POIGNEE_EPAISSEUR_DP * densite / 2f;
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            coins.set(cx - demiLargeur, cy - demiEpaisseur,
                    cx + demiLargeur, cy + demiEpaisseur);
            pinceauPoignee.setColor(palette.border());
            canvas.drawRoundRect(coins, demiEpaisseur, demiEpaisseur,
                    pinceauPoignee);
        }

        @Override
        public boolean onTouchEvent(MotionEvent evenement) {
            switch (evenement.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    debutY = evenement.getRawY();
                    translationDebut = getTranslationY();
                    gesteEnCours = true;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (!gesteEnCours) {
                        return true;
                    }
                    float avance = (evenement.getRawY() - debutY)
                            * sensFermeture();
                    if (avance < 0f) {
                        avance = 0f;
                    }
                    setTranslationY(translationDebut
                            + avance * sensFermeture());
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (!gesteEnCours) {
                        return true;
                    }
                    gesteEnCours = false;
                    performClick();
                    float emportee = (evenement.getRawY() - debutY)
                            * sensFermeture();
                    if (emportee > seuilGlissPx()) {
                        cacher();
                    } else if (animationsActives()) {
                        animate().translationY(0f)
                                .setDuration(PopupTokens.DUREE_FEUILLE_MS);
                    } else {
                        setTranslationY(0f);
                    }
                    return true;
                default:
                    return false;
            }
        }
    }
}
