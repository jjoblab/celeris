package jo.celeris.android.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import jo.celeris.core.ai.SuggestionFantome;
import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.syntax.structure.OutlineEntry;
import jo.celeris.core.text.EditListener;
import jo.celeris.core.text.TextSession;

import org.jspecify.annotations.Nullable;

/**
 * La vue d'éditeur : une seule vue Canvas (ADR-006), propriétaire du
 * pipeline à couches, du cache de rangées et de l'état d'image.
 *
 * <p>La vue est mince par construction : toute la logique vérifiable
 * sans appareil vit dans les classes pures du module ; la vue les
 * assemble, attache la surface au canvas de l'image et route le
 * tactile et le clavier. Le défilement est borné au contenu ; le
 * simple appui pose le curseur et ouvre le clavier, l'appui long
 * sélectionne le mot sous le doigt et retire le clavier — le dragage
 * qui suit étend la sélection, ancre figée à l'armement (PIEGE-03) —
 * ou tire une poignée, la pilule « ajouter un curseur » s'y offre
 * (A-01), le pincement zoome par paliers (D-15), le panoramique à
 * deux doigts défile, le clavier matériel passe par la table de
 * raccourcis (D-17). Le retour à la ligne souple (A-18) se règle par
 * {@link #setSoftWrap(boolean)}. L'IME s'attache par
 * {@link #onCreateInputConnection(EditorInfo)} — la composition vit
 * dans {@link CompositionState}, la colle dans
 * {@link EditorInputConnection} ; l'accessibilité (G-05) vit dans
 * {@link AccessibilityHost}.</p>
 *
 * <p>Les tailles de police sont en SP : la vue les convertit en
 * pixels par la densité de l'appareil — une taille se lit pareil sur
 * tous les écrans (PIEGE-23). Le curseur d'écriture — une barre par
 * plage vide — clignote tant que la vue tient le focus, et l'appui
 * qui prend le focus ouvre le clavier logiciel.</p>
 *
 * <p>Le langage se branche par {@link #setLanguage(LanguageSupport)}
 * (M7.6 : le support complet) : le pont {@link EditorSyntax} en tire
 * coloration, plis et marqueurs ; la configuration nourrit la frappe.</p>
 */
public final class EditorView extends View {

    /** La police par défaut : mono, 14 sp — la conversion en pixels
     *  suit la densité de l'appareil (PIEGE-23). */
    static final FontSpec POLICE_PAR_DEFAUT = new FontSpec("mono", 14f);
    /** La respiration du dévoilement de curseur, en dp (M7.5, §5). */
    static final float MARGE_RETOUR_DP = 8f;
    /** Le délai de retenue d'analyse par défaut (M7.5, §9) : la
     *  publication arrive au calme du doigt, pas une passe par frappe. */
    public static final long DELAI_RETENU_PAR_DEFAUT_MS = 250L;

    // Visible du paquet : l'assemblage d'image y attache la surface (C9).
    final CanvasSurface surface = new CanvasSurface();
    // Visible du paquet : l'assemblage d'image (FrameAssembler) l'écrit.
    final FrameState etatImage = new FrameState();
    // Visible du paquet : l'assemblage d'image y trace les couches (C9).
    final ScrollFrame trace;
    final SelectionDrag glissement = new SelectionDrag();
    // Visible du paquet : la projection vit dans l'assemblage d'image.
    final FoldModel plis = new FoldModel();
    final EditorSyntax syntaxe;
    // Visible du paquet : le banc de test y lit la mise en page servie (ADR-013).
    final RowLayoutCache cache = new RowLayoutCache();
    final CursorPill pilule = new CursorPill();
    private final CompositionState composition = new CompositionState();
    // Visible du paquet : l'assemblage d'image y trace le chrome (C9).
    final ChromePainter chrome = new ChromePainter();
    private final GutterPainter gouttiere;
    final MarquesGouttiere marquesGouttiere = new MarquesGouttiere();
    private final AccessibilityHost accessibilite = new AccessibilityHost(this);
    private final EditListener ecouteurAnnonce = (debut, fin, retire, insere)
            -> sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
    // Visible du paquet : l'assemblage d'image y pose son pinceau (C9).
    final TextBrush pinceauChrome;
    private final GestureDetector longPresses;
    private final ScaleGestureDetector pincements;
    private KeyBindings raccourcis = KeyBindings.defaults();
    // — les rappels de gestes de l'hôte vivent dans leur porteur (ADR-002)
    final RappelsHote rappels = new RappelsHote();
    // — le zoom au pincement vit dans son collaborateur (D-15, PIEGE-24)
    final PinchZoomHandler zoom = new PinchZoomHandler(this);
    // — la gouttière stable vit dans son collaborateur (PIEGE-25)
    final EditorGutter gouttiereEtat = new EditorGutter();
    // — les fractions du défilement vivent dans leur collaborateur (C9)
    private final ViewportFractions fractions = new ViewportFractions(this);

    TextSession session = TextSession.of("", System::nanoTime);
    VisualProjection projection = new VisualProjection(session.document(), plis, 0);
    /** La police logique, en sp — les métriques en dérivent par densité. */
    FontSpec policeLogique = POLICE_PAR_DEFAUT;
    // métriques provisoires : le constructeur les redérive à la densité (PIEGE-23)
    EditorMetrics metrics = EditorMetrics.of(POLICE_PAR_DEFAUT,
            new AndroidFontRuler());
    HitTest viseur = new HitTest(metrics);
    // Visible du paquet : l'assemblage d'image y pose la source liée.
    final RebindingRowSource sourceLiee = new RebindingRowSource(
            new DocumentRowSource(session.document(), projection, metrics, 0));
    // Visible du paquet : le thème se lit à l'assemblage d'image.
    EditorTheme theme = EditorThemes.CLAIR;
    // Visible du paquet : l'assemblage d'image suit la révision.
    long revisionSuivie;
    // Visible du paquet : le retour à la ligne décide du budget de colonnes.
    boolean retourALaLigne;
    /** Publication d'analyse reçue hors fil : la source se réhabille à
     *  l'image suivante, les mises en page sans couleur de la révision
     *  courante se remplacent (volatile : posée sur le fil de travail). */
    volatile boolean publicationRecue;
    /** L'écouteur des changements de taille de police (D-15). */
    private @Nullable Consumer<Float> onFontSizeChanged;
    /** Le concentrateur d'intelligence (M7.5, §8) — visible du paquet,
     *  la colle et les actions clavier y passent. */
    final IntelligenceHub intelligence = new IntelligenceHub(this);
    /** Le collaborateur de l'avis de fenêtre (M7.5, §7) : les popups
     *  se repositionnent au changement réel, jamais au clignotement.
     *  Visible du paquet : l'assemblage d'image y pose l'avis. */
    final ViewportWatch fenetre = new ViewportWatch();
    /** Le collaborateur de l'assemblage d'image (ADR-006) : gouttière,
     *  révision, défilement borné, fenêtre d'analyse, avis de fenêtre. */
    private final FrameAssembler images = new FrameAssembler(this);

    float scrollX;
    float scrollY;
    float largeurGouttiere;
    boolean numerosVisibles = true;
    /** Le collaborateur du curseur et du clavier : clignotement, IME. */
    final CurseurClavier curseurClavier = new CurseurClavier(this);
    /** Le collaborateur du routage tactile. */
    final TouchesEditeur touches = new TouchesEditeur(this);
    // — le glisser-déposer vit dans son collaborateur (D-20)
    final GlisserDeposer depot = new GlisserDeposer(this);
    /** Le collaborateur du texte fantôme (F-01) : affichage et gestes. */
    final FantomeEditeur fantome;
    /** Le collaborateur de diagnostic de scène (M7.5, §9) : retenue
     *  d'analyse, compteur d'images décolorées, lectures de vérité. */
    final SceneDiagnostics diagnostics;
    /** Le collaborateur des ancres de popups (M7.5, §7). */
    final CaretAnchors ancres = new CaretAnchors(this);
    // — la règle porte la table des familles : la mesure et le tracé la
    // lisent ensemble (D-14, PIEGE-22)
    final AndroidFontRuler reglePolices = new AndroidFontRuler();
    /** Le collaborateur des polices (PIEGE-05, PIEGE-23, PIEGE-28). */
    final ViewFonts polices = new ViewFonts(this, reglePolices);
    /** L'usine des pinceaux du texte (PIEGE-01) — la bascule des
     *  ligatures y vit (D-14). */
    private final PaintBrushes usinePinceaux;

    public EditorView(Context context) {
        this(context, null);
    }

    public EditorView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, null);
    }

    /** Construit la vue avec l'exécuteur du travail d'analyse (ADR-007,
     *  ADR-013) : null garde le fil démon — les tests de vue y pilotent
     *  la publication par l'exécuteur manuel du banc. */
    EditorView(Context context, @Nullable AttributeSet attrs,
            @Nullable Executor executeurAnalyse) {
        super(context, attrs);
        // M7.5, §9 : la retenue par défaut — l'hôte qui injecte son
        // exécuteur garde la main entière, jamais détournée.
        diagnostics = new SceneDiagnostics(this, executeurAnalyse);
        syntaxe = new EditorSyntax(plis, diagnostics.executor());
        setFocusable(true);
        setFocusableInTouchMode(true);
        syntaxe.setSurAnalysePrete(() -> {
            publicationRecue = true;
            postInvalidateOnAnimation();
        });
        BrushFactory pinceaux = usinePinceaux = new PaintBrushes(reglePolices);
        fantome = new FantomeEditeur(this, pinceaux);
        // La respiration naît à la densité de l'appareil : la valeur vit
        // en dp, convertie une fois, en pixels entiers (PIEGE-23, M7.5 §5).
        gouttiereEtat.setClasse(ClasseLargeur.MOYEN, densite());
        gouttiere = GutterPainter.monter(pinceaux, this);
        trace = ScrollFrame.assemble(gouttiere, pinceaux, () -> syntaxe.guides,
                sourceLiee::pieceDuPli, composition, cache, sourceLiee, fantome.peintre());
        pinceauChrome = pinceaux.newBrush();
        // Les métriques naissent à la densité de l'appareil : la police
        // logique (sp) se convertit une fois pour toutes (PIEGE-23).
        metrics = polices.metricsOf(POLICE_PAR_DEFAUT);
        viseur = new HitTest(metrics);
        syntaxe.attacher(session);
        session.addEditListener(ecouteurAnnonce);
        longPresses = GestesFabrique.appuisLongs(context, this);
        pincements = GestesFabrique.pincements(context, this);
        // D-20 : la vue reçoit les dépôts — le texte s'insère au point
        // de chute, les fichiers partent à l'hôte par le rappel.
        setOnDragListener(depot);
    }

    /**
     * Affiche la session donnée : le modèle, sa sélection et son
     * historique d'annulation.
     *
     * @param nouvelleSession la session d'édition à afficher
     */
    public void setSession(TextSession nouvelleSession) {
        session.removeEditListener(ecouteurAnnonce);
        session = nouvelleSession;
        session.addEditListener(ecouteurAnnonce);
        syntaxe.attacher(session);
        images.reinitialiserScene(true);
    }

    /**
     * Branche le langage — le support complet (M7.6) : sa source donne
     * coloration, plis et diagnostics (B-01, A-15, D-02, D-07), sa
     * configuration nourrit la frappe ; null coupe l'analyse.
     * @param nouvelleLangue le support du langage, null pour aucun
     */
    public void setLanguage(@Nullable LanguageSupport nouvelleLangue) {
        syntaxe.setLanguage(nouvelleLangue);
        images.reinitialiserScene(false);
    }

    /** Le concentrateur d'intelligence de la vue (M7.5, §8) — la frappe
     *  intelligente s'y pose, les popups y lisent la complétion. */
    public IntelligenceHub intelligence() {
        return intelligence;
    }

    /**
     * Applique le thème donné (identifiant de clé de cache compris).
     *
     * @param nouveauTheme le thème résolu
     */
    public void setTheme(EditorTheme nouveauTheme) {
        theme = nouveauTheme;
        invalidate();
    }

    /**
     * Change la police (PIEGE-05, PIEGE-28) — la mécanique vit dans
     * {@link ViewFonts}.
     *
     * @param police la spécification de police, taille en sp
     */
    public void setFont(FontSpec police) {
        polices.setFont(police);
    }

    /**
     * L'écouteur des changements de taille de police (D-15) — l'hôte y
     *  persiste la taille.
     */
    public void setOnFontSizeChanged(@Nullable Consumer<Float> ecouteur) {
        polices.setOnFontSizeChanged(ecouteur);
    }

    /** Bascule les ligatures du texte (D-14) : « liga » ouvre les
     *  ligatures de la police posée par l'hôte, « -liga » les ferme ;
     *  toutes les peintures du texte suivent, fermées par défaut. */
    public void setLigatures(boolean actives) {
        if (usinePinceaux.setLigatures(actives)) {
            invalidate();
        }
    }

    /** Pose une police de plateforme sous un nom de famille (D-14) : la
     *  mesure et le tracé la suivent (PIEGE-22) — la voie de l'hôte pour
     *  sa police à ligatures ; la famille active se redérive aussitôt. */
    public void poserPolice(String famille, Typeface police) {
        polices.poserPolice(famille, police);
    }

    /**
     * Active ou coupe le retour à la ligne souple par offsets (A-18) —
     * le budget de colonnes redérive, le défilement horizontal se
     * rabat. */
    public void setSoftWrap(boolean actif) {
        retourALaLigne = actif;
        scrollX = 0f;
        invalidate();
    }

    /** Remplace la table de raccourcis clavier (D-17 : remappage
     *  reconfigurable, persistant chez l'hôte par encodage). */
    public void setKeyBindings(KeyBindings nouvelles) {
        raccourcis = nouvelles;
    }

    /** La table de raccourcis courante (D-17). */
    public KeyBindings keyBindings() {
        return raccourcis;
    }

    /** Le rappel des changements de fenêtre (M7.5, §7) : défilement,
     *  zoom, police, retour à la ligne, redimensionnement — l'hôte y
     *  repositionne ses popups, un avis par changement réel. */
    public void setSurViewportChange(@Nullable Runnable ecouteur) {
        fenetre.setListener(ecouteur);
    }

    /** La fraction verticale du défilement — 0 en tête, 1 au pied (C9). */
    public float scrollFractionY() {
        return fractions.fraction();
    }

    /** La part visible du contenu vertical — 1 quand tout tient (C9). */
    public float visibleFractionY() {
        return fractions.partVisible();
    }

    /**
     * Défile à la fraction verticale (C9) : 0 en tête, 1 au pied — le
     * bornage et l'avis de fenêtre partent à l'image suivante.
     *
     * @param fraction la fraction visée du défilement
     */
    public void scrollToFractionY(float fraction) {
        fractions.defilerA(fraction);
    }

    /** La première ligne visible — la rangée du haut de la fenêtre (C9). */
    public int premiereLigneVisible() {
        return fractions.premiereLigneVisible();
    }

    /** Le nombre de lignes visibles, au moins une (C9). */
    public int lignesVisibles() {
        return fractions.lignesVisibles();
    }

    /** Le réglage de lenteur d'analyse (M7.5, §9) : zéro rend la main immédiate,
     *  l'exécuteur injecté par l'hôte reste le sien. */
    public void setRetenueAnalyseMs(long delaiMs) {
        diagnostics.setRetenueMs(delaiMs);
    }

    /** Le délai de retenue courant, en millisecondes — zéro : main immédiate. */
    public long retenueAnalyseMs() {
        return diagnostics.retenueMs();
    }

    /** La taille de police courante, en sp (M7.5, §9). */
    public float fontSizeSp() {
        return diagnostics.fontSizeSp();
    }

    /** La largeur de gouttière à l'image courante, en pixels (M7.5, §9). */
    public float gutterWidth() {
        return diagnostics.gutterWidth();
    }

    /** Le nombre d'images décolorées depuis l'ouverture (M7.5, §9) —
     *  le rattrapage PIEGE-26 le garde à zéro. */
    public long discoloredFrames() {
        return diagnostics.discoloredFrames();
    }

    /** Le rectangle du curseur principal (M7.5, §7) — l'ancre des
     *  popups du caret ; la géométrie vit dans {@link CaretAnchors}. */
    public Rect caretBounds() {
        return ancres.caretBounds();
    }

    /** Le rectangle de la sélection principale (M7.5, §7) — l'ancre de
     *  la barre de sélection ; une plage vide rend le rectangle du
     *  curseur. */
    public Rect selectionBounds() {
        return ancres.selectionBounds();
    }

    /** Affiche ou masque les numéros de lignes (D-01) : masqués, la gouttière
     *  se réduit à la coulisse des marqueurs (plis D-07, erreurs D-02). */
    public void setNumerosVisibles(boolean visibles) {
        numerosVisibles = visibles;
        gouttiere.setNumerosVisibles(visibles);
        invalidate();
    }

    /** Adapte la gouttière à la classe de largeur de fenêtre (D-19) :
     *  le compact resserre le champ de numéros, l'étendu le respire. */
    public void setClasseLargeur(ClasseLargeur classe) {
        gouttiereEtat.setClasse(classe, densite());
        gouttiere.setMarge(gouttiereEtat.margePx());
        invalidate();
    }

    /**
     * Retombe au plancher de chiffres de la gouttière (PIEGE-25) : le
     * niveau d'eau ne se vide que là, au changement de session ou de
     * langage — jamais pendant la frappe.
     */
    public void resetGutterWidth() {
        gouttiereEtat.reinitialiser();
        invalidate();
    }

    /** La densité de l'appareil — la porte unique des conversions dp. */
    float densite() {
        return getResources().getDisplayMetrics().density;
    }

    /** Le rappel des fichiers déposés (D-20) : chaque uri déposée y
     *  part, l'hôte décide de l'ouverture — jamais la vue ; le texte
     *  déposé s'insère au point de chute.
     *
     * @param rappel le consommateur d'uri, null pour couper
     */
    public void setSurFichierDepose(@Nullable Consumer<String> rappel) {
        depot.poserSurFichier(rappel);
    }

    /** Le rappel du plan du fichier (B-16), parti à chaque publication d'analyse. */
    public void setSurPlan(@Nullable Consumer<List<OutlineEntry>> consommateur) {
        syntaxe.setSurPlan(consommateur);
    }

    /**
     * Le rappel d'appui long (M7.6) : l'offset sélectionné y part —
     * le moment juste de la documentation au survol. Null coupe.
     *
     * @param consommateur le consommateur d'offset, null pour couper
     */
    public void setSurAppuiLong(@Nullable IntConsumer consommateur) {
        rappels.poserAppuiLong(consommateur);
    }

    /**
     * Le rappel de bascule de point d'arrêt (D-04, la reprise) :
     * l'appui long de gouttière rend la LIGNE visée à l'hôte — c'est
     * lui qui pose, retire ou désactive l'arrêt. Null coupe.
     *
     * @param consommateur le consommateur de ligne, null pour couper
     */
    public void setSurBasculeArret(@Nullable IntConsumer consommateur) {
        rappels.poserBasculeArret(consommateur);
    }

    /** Les intégrations de la gouttière : marques, arrêts, clic (D-03 à D-05). */
    public MarquesGouttiere marquesGouttiere() {
        return marquesGouttiere;
    }

    /** Dépose (ou retire) la suggestion fantôme visible (F-01) : la
     *  première ligne à l'ancre, les suivantes au bord gauche du
     *  texte, à l'encre fanée. */
    public void setFantome(@Nullable SuggestionFantome suggestion) {
        fantome.setSuggestion(suggestion);
    }

    /** Le rappel des gestes du fantôme (F-01) : Tab et l'appui sur le
     *  fantôme acceptent le premier mot, l'hôte exécute — la vue ne
     *  touche à aucune session de modèle. */
    public void setSurGesteFantome(@Nullable Consumer<GesteFantome> surGeste) {
        fantome.setSurGeste(surGeste);
    }

    /** La session affichée — accès de paquet pour la colle IME. */
    TextSession session() {
        return session;
    }

    /** Vrai si la vue accepte l'IME : session éditable. */
    @Override
    public boolean onCheckIsTextEditor() {
        return !session.isReadOnly();
    }

    /**
     * La connexion d'entrée IME : texte de code sans suggestions,
     * jamais plein écran, sélection initiale annoncée — la fabrique
     * vit dans {@link EditorInputConnection}, la lecture seule rend
     * la vue non éditable (TYPE_NULL).
     */
    @Override
    public @Nullable InputConnection onCreateInputConnection(
            EditorInfo outAttrs) {
        return EditorInputConnection.creer(this, composition, outAttrs);
    }

    /** L'état d'image courant — l'hôte d'accessibilité (G-05) y lit. */
    FrameState etatImageCourante() {
        preparerEtatImage();
        return etatImage;
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        accessibilite.remplir(info);
    }

    @Override
    public AccessibilityNodeProvider getAccessibilityNodeProvider() {
        return accessibilite.fournisseur();
    }

    @Override
    public boolean performAccessibilityAction(int action, Bundle args) {
        if (accessibilite.execute(action, args)) {
            return true;
        }
        return super.performAccessibilityAction(action, args);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Le budget de colonnes dérive de la largeur : image suivante.
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        images.dessiner(canvas);
    }

    /**
     * Prépare l'état d'image : gouttière AVANT le budget de colonnes
     * (le cycle est cassé, PIEGE-25), suivi de révision, bornage du
     * défilement et fenêtre visible — l'assemblage vit dans
     * {@link FrameAssembler}. Visible du paquet : les collaborateurs
     * (ancres, diagnostic) y préparent avant de lire.
     */
    void preparerEtatImage() {
        images.preparer();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        longPresses.onTouchEvent(event);
        pincements.onTouchEvent(event);
        return touches.router(event, super::onTouchEvent);
    }

    /** D-15 — la molette avec Ctrl zoome d'un palier (voir PinchZoomHandler). */
    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        return zoom.surMolette(event) || super.onGenericMotionEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // La frappe passe au collaborateur (plafond ADR-002) : la composition
        // se clôt, la table de raccourcis décide.
        EditorAction action = KeyActions.action(this, raccourcis, composition,
                keyCode, event);
        return action == null
                ? super.onKeyDown(keyCode, event)
                : KeyActions.appliquer(this, action);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        curseurClavier.surAttache();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        curseurClavier.surDetache();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        curseurClavier.surVisibiliteFenetre(visibility);
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction,
            @Nullable Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        curseurClavier.surFocus(gainFocus);
    }
}
