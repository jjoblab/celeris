package jo.celeris.android.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
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

import jo.celeris.core.ai.SuggestionFantome;
import jo.celeris.core.syntax.LanguageSyntax;
import jo.celeris.core.syntax.OutlineEntry;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.EditListener;
import jo.celeris.core.text.SelRange;
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
 * tactile pose le curseur puis étend la sélection (ancre figée au
 * DOWN, PIEGE-03) ou tire une poignée, l'appui long arme la pilule
 * « ajouter un curseur » (A-01), le pincement zoome par paliers
 * (D-15), le panoramique à deux doigts défile, le clavier matériel
 * passe par la table de raccourcis (D-17). Le retour à la ligne
 * souple (A-18) se règle par {@link #setSoftWrap(boolean)}. L'IME
 * s'attache par {@link #onCreateInputConnection(EditorInfo)} — la
 * composition vit dans {@link CompositionState}, la colle dans
 * {@link EditorInputConnection} ; l'accessibilité (G-05) vit dans
 * {@link AccessibilityHost}.</p>
 *
 * <p>Les tailles de police sont en SP : la vue les convertit en
 * pixels par la densité de l'appareil — une taille se lit pareil sur
 * tous les écrans (PIEGE-23). Le curseur d'écriture — une barre par
 * plage vide — clignote tant que la vue tient le focus, et l'appui
 * qui prend le focus ouvre le clavier logiciel.</p>
 *
 * <p>La syntaxe (M4) se branche par {@link #setLanguage(LanguageSyntax)} :
 * le pont {@link EditorSyntax} habille la source de rangées des plages
 * de jetons (B-01) et sert la coulisse de marqueurs — plis (D-07,
 * A-15), lignes en erreur (D-02) — l'appui sur un chevron bascule.</p>
 */
public final class EditorView extends View {

    /** La police par défaut : mono, 14 sp — la conversion en pixels
     *  suit la densité de l'appareil (PIEGE-23). */
    static final FontSpec POLICE_PAR_DEFAUT = new FontSpec("mono", 14f);
    /** La respiration de gouttière, adaptée à la classe de largeur (D-19). */
    float margeGouttiere = ClasseLargeur.MOYEN.margeGouttiere();
    private static final float MARGE_RETOUR = 8f;

    private final AndroidFontRuler regle = new AndroidFontRuler();
    private final CanvasSurface surface = new CanvasSurface();
    private final FrameState etatImage = new FrameState();
    private final ScrollFrame trace = new ScrollFrame();
    final SelectionDrag glissement = new SelectionDrag();
    private final FoldModel plis = new FoldModel();
    final EditorSyntax syntaxe;
    // Visible du paquet : le banc de test y lit la mise en page servie (ADR-013).
    final RowLayoutCache cache = new RowLayoutCache();
    final CursorPill pilule = new CursorPill();
    private final CompositionState composition = new CompositionState();
    private final ChromePainter chrome = new ChromePainter();
    private final GutterPainter gouttiere;
    final MarquesGouttiere marquesGouttiere = new MarquesGouttiere();
    private final AccessibilityHost accessibilite = new AccessibilityHost(this);
    private final EditListener ecouteurAnnonce = (debut, fin, retire, insere)
            -> sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
    private final TextBrush pinceauChrome;
    private final GestureDetector longPresses;
    private final ScaleGestureDetector pincements;

    private KeyBindings raccourcis = KeyBindings.defaults();
    // — état de gestes, visible du paquet pour GestesFabrique (D-15/A-01)
    float tailleAuDepartDuPincement;
    boolean pincementActif;

    TextSession session = TextSession.of("", System::nanoTime);
    VisualProjection projection = new VisualProjection(session.document(), plis, 0);
    /** La police logique, en sp — les métriques en dérivent par densité. */
    FontSpec policeLogique = POLICE_PAR_DEFAUT;
    EditorMetrics metrics = EditorMetrics.of(POLICE_PAR_DEFAUT, regle);
    HitTest viseur = new HitTest(metrics);
    private final RebindingRowSource sourceLiee = new RebindingRowSource(
            new DocumentRowSource(session.document(), projection, metrics, 0));
    private EditorTheme theme = EditorThemes.CLAIR;
    private long revisionSuivie;
    private boolean retourALaLigne;
    /** Publication d'analyse reçue hors fil : la source se réhabille à
     *  l'image suivante, les mises en page sans couleur de la révision
     *  courante se remplacent (volatile : posée sur le fil de travail). */
    private volatile boolean publicationRecue;

    float scrollX;
    float scrollY;
    float largeurGouttiere;
    private boolean numerosVisibles = true;
    /** Le collaborateur du curseur et du clavier : clignotement, IME. */
    final CurseurClavier curseurClavier = new CurseurClavier(this);
    /** Le collaborateur du routage tactile. */
    private final TouchesEditeur touches = new TouchesEditeur(this);
    /** Le collaborateur du texte fantôme (F-01) : affichage et gestes. */
    final FantomeEditeur fantome;

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
        syntaxe = new EditorSyntax(plis,
                executeurAnalyse != null ? executeurAnalyse
                        : ExecuteurAnalyse.parDefaut());
        setFocusable(true);
        setFocusableInTouchMode(true);
        syntaxe.setSurAnalysePrete(() -> {
            publicationRecue = true;
            postInvalidateOnAnimation();
        });
        BrushFactory pinceaux = new PaintBrushes();
        fantome = new FantomeEditeur(this, pinceaux);
        gouttiere = new GutterPainter(pinceaux, margeGouttiere,
                () -> marquesGouttiere.marques(syntaxe.marques()),
                () -> marquesGouttiere.coulisse(syntaxe.coulisse(
                        metrics.charWidth(), margeGouttiere), metrics.charWidth()),
                marquesGouttiere::pastille,
                marquesGouttiere::reperePreview);
        trace.addLayer(gouttiere);
        trace.addLayer(new SelectionPainter(pinceaux));
        trace.addLayer(new ComposingPainter(pinceaux, composition));
        trace.addLayer(new TextPainter(pinceaux, cache, sourceLiee));
        trace.addLayer(fantome.peintre());
        pinceauChrome = pinceaux.newBrush();
        // Les métriques naissent à la densité de l'appareil : la police
        // logique (sp) se convertit une fois pour toutes (PIEGE-23).
        metrics = EditorMetrics.of(enPixels(POLICE_PAR_DEFAUT), regle);
        viseur = new HitTest(metrics);
        syntaxe.attacher(session);
        session.addEditListener(ecouteurAnnonce);
        longPresses = GestesFabrique.appuisLongs(context, this);
        pincements = GestesFabrique.pincements(context, this);
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
        revisionSuivie = Long.MIN_VALUE;
        cache.clear();
        scrollX = 0f;
        scrollY = 0f;
        invalidate();
    }

    /**
     * Analyse le langage donne : coloration, plis et diagnostics du
     * meme arbre (B-01, A-15, D-02, D-07) ; null coupe l'analyse.
     * @param nouvelleLangue le langage, null pour aucun
     */
    public void setLanguage(@Nullable LanguageSyntax nouvelleLangue) {
        syntaxe.setLanguage(nouvelleLangue);
        revisionSuivie = Long.MIN_VALUE;
        cache.clear();
        invalidate();
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
     * Change la police : les métriques se redérivent par reconsultation
     * de la règle (PIEGE-05), le cache se vide par révision suivie, le
     * budget de colonnes se redérive à l'image suivante.
     *
     * @param police la spécification de police, taille en sp
     */
    public void setFont(FontSpec police) {
        policeLogique = police;
        metrics = EditorMetrics.of(enPixels(police), regle);
        viseur = new HitTest(metrics);
        revisionSuivie = 0L;
        invalidate();
    }

    /**
     * La conversion sp vers pixels à la densité courante : la mesure et
     * le tracé vivent en pixels, l'interface vit en sp — la conversion
     * vit dans {@link TaillesSp}, pure et testée (PIEGE-23), jamais un
     * pixel cru ne franchit cette porte.
     */
    private FontSpec enPixels(FontSpec police) {
        DisplayMetrics metriques = getResources().getDisplayMetrics();
        // L'échelle de police de l'accessibilité vit dans la
        // configuration (le champ scaledDensity est dépréqué) : la
        // conversion reste sp × densité × échelle, pure et testée.
        float echelle = getResources().getConfiguration().fontScale;
        return new FontSpec(police.family(), TaillesSp.enPixels(police.size(),
                metriques.density, echelle));
    }

    /**
     * Active ou coupe le retour à la ligne souple par offsets (A-18) :
     * le budget de colonnes redérive de la fenêtre, la projection se
     * reconstruit et le défilement horizontal se rabat — le contenu
     * tient désormais dans la fenêtre.
     *
     * @param actif vrai pour couper les lignes à la fenêtre
     */
    public void setSoftWrap(boolean actif) {
        retourALaLigne = actif;
        scrollX = 0f;
        invalidate();
    }

    /**
     * Remplace la table de raccourcis clavier (D-17 : remappage
     * reconfigurable, persistant chez l'hôte par encodage).
     *
     * @param nouvelles la table à appliquer
     */
    public void setKeyBindings(KeyBindings nouvelles) {
        raccourcis = nouvelles;
    }

    /** La table de raccourcis courante (D-17). */
    public KeyBindings keyBindings() {
        return raccourcis;
    }

    /**
     * Affiche ou masque les numéros de lignes (D-01) : masqués, la
     * gouttière se réduit à la coulisse des marqueurs (plis D-07,
     * erreurs D-02).
     *
     * @param visibles vrai pour tracer les numéros (défaut)
     */
    public void setNumerosVisibles(boolean visibles) {
        numerosVisibles = visibles;
        gouttiere.setNumerosVisibles(visibles);
        invalidate();
    }

    /**
     * Adapte la gouttière à la classe de largeur de fenêtre (D-19 —
     * pliables, grands écrans, mode bureau) : le compact resserre le
     * champ de numéros, l'étendu le respire — le pliable déplié change
     * de classe d'un geste, la gouttière suit.
     *
     * @param classe la classe de largeur courante
     */
    public void setClasseLargeur(ClasseLargeur classe) {
        margeGouttiere = classe.margeGouttiere();
        gouttiere.setMarge(margeGouttiere);
        invalidate();
    }

    /** Le rappel du plan du fichier (B-16), parti à chaque publication d'analyse. */
    public void setSurPlan(@Nullable Consumer<List<OutlineEntry>> consommateur) {
        syntaxe.setSurPlan(consommateur);
    }

    /** Les intégrations de la gouttière : marques, arrêts, clic (D-03 à D-05). */
    public MarquesGouttiere marquesGouttiere() {
        return marquesGouttiere;
    }

    /**
     * Dépose (ou retire) la suggestion fantôme visible (F-01) : la
     * première ligne à l'ancre, les suivantes au bord gauche du texte,
     * à l'encre fanée.
     *
     * @param suggestion la suggestion, ou {@code null} pour fermer
     */
    public void setFantome(@Nullable SuggestionFantome suggestion) {
        fantome.setSuggestion(suggestion);
    }

    /**
     * Le rappel des gestes du fantôme (F-01) : Tab et l'appui sur le
     * fantôme acceptent le premier mot, l'hôte exécute — la vue ne
     * touche à aucune session de modèle.
     *
     * @param surGeste le rappel des gestes, ou {@code null} pour couper
     */
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
     * jamais plein écran, sélection initiale annoncée. En lecture
     * seule, la vue se déclare non éditable (TYPE_NULL, pas de
     * connexion).
     */
    @Override
    public @Nullable InputConnection onCreateInputConnection(
            EditorInfo outAttrs) {
        if (session.isReadOnly()) {
            outAttrs.inputType = InputType.TYPE_NULL;
            return null;
        }
        outAttrs.inputType = InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE;
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN
                | EditorInfo.IME_ACTION_NONE;
        List<SelRange> plages = session.selection().ranges();
        if (!plages.isEmpty()) {
            outAttrs.initialSelStart = plages.get(0).start();
            outAttrs.initialSelEnd = plages.get(0).end();
        }
        return new EditorInputConnection(this, composition);
    }

    /**
     * Dévoile le curseur primaire : réajustement minimum du
     * défilement (CaretReveal), puis invalidation.
     */
    void revealCaret() {
        List<SelRange> plages = session.selection().ranges();
        int curseur = plages.isEmpty() ? 0 : plages.get(0).caret();
        CaretReveal.Revealed devoile = CaretReveal.reveal(curseur,
                session.document(), projection, metrics, largeurGouttiere,
                getWidth(), getHeight(), scrollX, scrollY, MARGE_RETOUR);
        scrollX = devoile.scrollX();
        scrollY = devoile.scrollY();
        invalidate();
    }

    /**
     * L'état d'image courant — accès de paquet pour l'hôte
     * d'accessibilité (G-05) : l'état se prépare, puis se lit.
     */
    FrameState etatImageCourante() {
        preparerEtatImage();
        return etatImage;
    }

    /** Défile d'une page, bornée au contenu. */
    void defilerPage(boolean enAvant) {
        float hauteurContenu = projection.rowCount() * metrics.lineHeight();
        float pas = ScrollGeometry.visibleRows(getHeight(),
                metrics.lineHeight()) * metrics.lineHeight();
        scrollY = ScrollGeometry.clamp(
                enAvant ? scrollY + pas : scrollY - pas,
                hauteurContenu, getHeight());
        invalidate();
    }

    /** Annonce le changement de sélection au lecteur d'écran (G-05). */
    void annoncerSelection() {
        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED);
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
        // Le budget de colonnes dérive de la largeur : l'image suivante
        // reconstruit la projection s'il a changé.
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(theme.backgroundColor());
        preparerEtatImage();
        surface.attach(canvas);
        trace.drawFrame(surface, etatImage);
        pinceauChrome.setFont(metrics.font());
        chrome.tracer(surface, pinceauChrome, theme, metrics, pilule,
                etatImage.document(), projection, session.selection().ranges(),
                largeurGouttiere, scrollX, scrollY, curseurClavier.visible());
        surface.detach();
    }

    /**
     * Prépare l'état d'image : suivi de révision, bornage du
     * défilement, gouttière et fenêtre visible.
     */
    private void preparerEtatImage() {
        Document document = session.document();
        suivreRevision(document);
        float hauteurContenu = projection.rowCount() * metrics.lineHeight();
        scrollY = ScrollGeometry.clamp(scrollY, hauteurContenu, getHeight());
        // La coulisse effective compte les intégrations (D-03 à D-05).
        float coulisse = marquesGouttiere.coulisse(syntaxe.coulisse(
                metrics.charWidth(), margeGouttiere), metrics.charWidth());
        largeurGouttiere = numerosVisibles
                ? GutterGeometry.width(Math.max(1, projection.rowCount()),
                        metrics.charWidth(), margeGouttiere, coulisse)
                : GutterGeometry.laneOnly(margeGouttiere, coulisse);
        scrollX = ScrollGeometry.clamp(scrollX,
                ScrollGeometry.largeurContenu(projection, metrics),
                ScrollGeometry.largeurVisible(getWidth(), largeurGouttiere));
        int premiere = ScrollGeometry.firstRow(scrollY, metrics.lineHeight());
        int visibles = ScrollGeometry.visibleRows(getHeight(), metrics.lineHeight());
        syntaxe.fenetre(projection, premiere, visibles);
        etatImage.beginFrame(document, session.selection(), projection,
                document.revision(), metrics, theme, premiere, visibles,
                largeurGouttiere, scrollX, scrollY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        longPresses.onTouchEvent(event);
        pincements.onTouchEvent(event);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (fantome.touche(event.getX(), event.getY())) {
                    return true;
                }
                return touches.surAppui(event);
            case MotionEvent.ACTION_POINTER_DOWN:
                return touches.surDoigtPose(event);
            case MotionEvent.ACTION_MOVE:
                return touches.surDeplacement(event);
            case MotionEvent.ACTION_POINTER_UP:
                return touches.surDoigtLeve();
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                performClick();
                return touches.surRelachement();
            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Toute frappe matérielle clôt la composition d'abord : la
        // composition est une interaction de plage primaire.
        if (composition.isComposing()) {
            composition.finish();
            invalidate();
        }
        KeyBinding touche = new KeyBinding(keyCode, event.isShiftPressed(),
                event.isCtrlPressed(), event.isAltPressed());
        EditorAction action = raccourcis.actionOf(touche);
        if (action == null) {
            return super.onKeyDown(keyCode, event);
        }
        return KeyActions.appliquer(this, action);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private void suivreRevision(Document document) {
        int colonnesVoulues = retourALaLigne ? budgetColonnes() : 0;
        // La publication arrive sur le fil de travail : la source se
        // réhabille même à révision constante — les mises en page
        // déposées sans couleur pendant le vol se remplacent (le cache
        // les servait telles quelles, la couleur ne revenait qu'à
        // l'édition suivante).
        boolean recue = publicationRecue;
        if (recue) {
            publicationRecue = false;
            cache.invalidate(document.revision());
        }
        if (recue || document.revision() != revisionSuivie
                || colonnesVoulues != projection.columns()) {
            cache.invalidate(revisionSuivie);
            revisionSuivie = document.revision();
            projection = new VisualProjection(document, plis, colonnesVoulues);
            sourceLiee.set(syntaxe.habiller(
                    new DocumentRowSource(document, projection, metrics,
                            colonnesVoulues), document));
        }
    }

    /** Le budget de colonnes de la fenêtre, au moins une colonne (PIEGE-08). */
    private int budgetColonnes() {
        float disponible = getWidth() - largeurGouttiere - MARGE_RETOUR;
        if (disponible <= 0f || metrics.charWidth() <= 0f) {
            return 1;
        }
        return Math.max(1, (int) Math.floor(disponible / metrics.charWidth()));
    }

    /** D-15 — zoome par paliers, le point sous le doigt stable. */
    void zoomer(float focalX, float focalY, float facteur) {
        float nouvelleTaille = PinchZoom.steppedSize(tailleAuDepartDuPincement,
                facteur);
        if (nouvelleTaille == policeLogique.size()) {
            return;
        }
        float ancienCharWidth = metrics.charWidth();
        float ancienLineHeight = metrics.lineHeight();
        float ancienScrollX = scrollX;
        float ancienScrollY = scrollY;
        setFont(new FontSpec(policeLogique.family(), nouvelleTaille));
        scrollX = PinchZoom.scrollXKeepingColumn(focalX, largeurGouttiere,
                ancienScrollX, ancienCharWidth, metrics.charWidth());
        scrollY = PinchZoom.scrollYKeepingRow(focalY, ancienScrollY,
                ancienLineHeight, metrics.lineHeight());
        invalidate();
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
