package jo.celeris.android.view;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Bundle;
import android.text.InputType;
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import jo.celeris.core.syntax.LanguageSyntax;
import jo.celeris.core.syntax.OutlineEntry;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.EditListener;
import jo.celeris.core.text.MultiSelection;
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
 * <p>La syntaxe (M4) se branche par {@link #setLanguage(LanguageSyntax)} :
 * le pont {@link EditorSyntax} habille la source de rangées des plages
 * de jetons (B-01) et sert la coulisse de marqueurs — plis (D-07,
 * A-15), lignes en erreur (D-02) — l'appui sur un chevron bascule.</p>
 */
public final class EditorView extends View {

    private static final FontSpec POLICE_PAR_DEFAUT = new FontSpec("mono", 14f);
    private static final float MARGE_GOUTTIERE = 8f;
    private static final float MARGE_RETOUR = 8f;

    private final AndroidFontRuler regle = new AndroidFontRuler();
    private final CanvasSurface surface = new CanvasSurface();
    private final FrameState etatImage = new FrameState();
    private final ScrollFrame trace = new ScrollFrame();
    private final SelectionDrag glissement = new SelectionDrag();
    private final FoldModel plis = new FoldModel();
    private final EditorSyntax syntaxe = new EditorSyntax(plis, ExecuteurAnalyse.parDefaut());
    private final RowLayoutCache cache = new RowLayoutCache();
    final CursorPill pilule = new CursorPill();
    private final CompositionState composition = new CompositionState();
    private final ChromePainter chrome = new ChromePainter();
    private final GutterPainter gouttiere;
    private final AccessibilityHost accessibilite = new AccessibilityHost(this);
    private final EditListener ecouteurAnnonce = (debut, fin, retire, insere)
            -> sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
    private final TextBrush pinceauChrome;
    private final GestureDetector longPresses;
    private final ScaleGestureDetector pincements;

    private KeyBindings raccourcis = KeyBindings.defaults();
    private SelectionHandles.HandleId poigneeTiree = SelectionHandles.HandleId.NONE;
    // — état de gestes, visible du paquet pour GestesFabrique (D-15/A-01)
    float tailleAuDepartDuPincement;
    boolean pincementActif;

    private TextSession session = TextSession.of("", System::nanoTime);
    private VisualProjection projection = new VisualProjection(session.document(), plis, 0);
    EditorMetrics metrics = EditorMetrics.of(POLICE_PAR_DEFAUT, regle);
    HitTest viseur = new HitTest(metrics);
    private final RebindingRowSource sourceLiee = new RebindingRowSource(
            new DocumentRowSource(session.document(), projection, metrics, 0));
    private EditorTheme theme = EditorThemes.CLAIR;
    private long revisionSuivie;
    private boolean retourALaLigne;

    private float scrollX;
    private float scrollY;
    private float dernierX;
    private float dernierY;
    private float largeurGouttiere;
    private boolean numerosVisibles = true;
    private boolean panoramique;

    public EditorView(Context context) {
        this(context, null);
    }

    public EditorView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setFocusable(true);
        setFocusableInTouchMode(true);
        syntaxe.setSurAnalysePrete(this::postInvalidateOnAnimation);
        BrushFactory pinceaux = new PaintBrushes();
        gouttiere = new GutterPainter(pinceaux, MARGE_GOUTTIERE, syntaxe::marques,
                () -> syntaxe.coulisse(metrics.charWidth(), MARGE_GOUTTIERE));
        trace.addLayer(gouttiere);
        trace.addLayer(new SelectionPainter(pinceaux));
        trace.addLayer(new ComposingPainter(pinceaux, composition));
        trace.addLayer(new TextPainter(pinceaux, cache, sourceLiee));
        pinceauChrome = pinceaux.newBrush();
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
     * @param police la spécification de police
     */
    public void setFont(FontSpec police) {
        metrics = EditorMetrics.of(police, regle);
        viseur = new HitTest(metrics);
        revisionSuivie = 0L;
        invalidate();
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

    /** Le rappel du plan du fichier (B-16), parti à chaque publication d'analyse. */
    public void setSurPlan(@Nullable Consumer<List<OutlineEntry>> consommateur) {
        syntaxe.setSurPlan(consommateur);
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
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
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
        chrome.tracer(surface, pinceauChrome, theme, metrics, pilule,
                etatImage.document(), projection, session.selection().ranges(),
                largeurGouttiere, scrollX, scrollY);
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
        float coulisse = syntaxe.coulisse(metrics.charWidth(), MARGE_GOUTTIERE);
        largeurGouttiere = numerosVisibles
                ? GutterGeometry.width(Math.max(1, projection.rowCount()),
                        metrics.charWidth(), MARGE_GOUTTIERE, coulisse)
                : GutterGeometry.laneOnly(MARGE_GOUTTIERE, coulisse);
        scrollX = ScrollGeometry.clamp(scrollX, largeurContenu(), largeurVisible());
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
                return surAppui(event);
            case MotionEvent.ACTION_POINTER_DOWN:
                return surDoigtPose(event);
            case MotionEvent.ACTION_MOVE:
                return surDeplacement(event);
            case MotionEvent.ACTION_POINTER_UP:
                return surDoigtLeve();
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                performClick();
                return surRelachement();
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
        if (document.revision() != revisionSuivie
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

    /** L'étendue horizontale du contenu : la plus large rangée, en pixels. */
    private float largeurContenu() {
        int portee = projection.columns() <= 0
                ? projection.longestLineLength()
                : Math.min(projection.longestLineLength(), projection.columns());
        return portee * metrics.charWidth();
    }

    /** La largeur visible du contenu, gouttière déduite. */
    private float largeurVisible() {
        return Math.max(0f, getWidth() - largeurGouttiere);
    }

    private boolean surAppui(MotionEvent event) {
        requestFocus();
        dernierX = event.getX();
        dernierY = event.getY();
        panoramique = false;
        float x = event.getX();
        float y = event.getY();
        if (pilule.isArmed() && pilule.contains(x, y)) {
            pilule.hide();
            invalidate();
            return true;
        }
        SelectionHandles.HandleId poignee = poigneeVisee(x, y);
        if (poignee != SelectionHandles.HandleId.NONE) {
            poigneeTiree = poignee;
            return true;
        }
        if (syntaxe.basculerPliAuPoint(x, y, scrollY, metrics.lineHeight(),
                syntaxe.coulisse(metrics.charWidth(), MARGE_GOUTTIERE),
                projection)) {
            invalidate();
            return true;
        }
        int offset = offsetAt(x, y);
        glissement.down(offset);
        if (pilule.isArmed()) {
            session.setSelection(ajouterCurseur(offset));
        } else {
            session.setSelection(MultiSelection.of(SelRange.caretAt(offset)));
        }
        annoncerSelection();
        invalidate();
        return true;
    }

    private boolean surDoigtPose(MotionEvent event) {
        if (event.getPointerCount() >= 2) {
            panoramique = true;
            glissement.up();
            dernierX = event.getX();
            dernierY = event.getY();
        }
        return true;
    }

    private boolean surDeplacement(MotionEvent event) {
        if (pincementActif) {
            return true;
        }
        if (panoramique && event.getPointerCount() >= 2) {
            pilule.hide();
            float hauteurContenu = projection.rowCount() * metrics.lineHeight();
            scrollY = ScrollGeometry.clamp(scrollY + dernierY - event.getY(),
                    hauteurContenu, getHeight());
            scrollX = ScrollGeometry.clamp(scrollX + dernierX - event.getX(),
                    largeurContenu(), largeurVisible());
            dernierX = event.getX();
            dernierY = event.getY();
            invalidate();
            return true;
        }
        if (poigneeTiree != SelectionHandles.HandleId.NONE) {
            tirerPoignee(event.getX(), event.getY());
            return true;
        }
        if (glissement.isActive()) {
            session.setSelection(MultiSelection.of(
                    glissement.dragTo(offsetAt(event.getX(), event.getY()))));
            invalidate();
        }
        return true;
    }

    private void tirerPoignee(float x, float y) {
        List<SelRange> plages = session.selection().ranges();
        if (plages.size() != 1) {
            poigneeTiree = SelectionHandles.HandleId.NONE;
            return;
        }
        session.setSelection(MultiSelection.of(SelectionHandles.dragged(
                plages.get(0), poigneeTiree, offsetAt(x, y))));
        invalidate();
    }

    private SelectionHandles.HandleId poigneeVisee(float x, float y) {
        List<SelRange> plages = session.selection().ranges();
        if (plages.size() != 1 || plages.get(0).isEmpty()) {
            return SelectionHandles.HandleId.NONE;
        }
        return SelectionHandles.hitTest(x, y, largeurGouttiere, scrollX, scrollY,
                session.document(), projection, metrics, plages.get(0).start(),
                plages.get(0).end());
    }

    /** Ajoute un curseur à l'offset visé, les existants tiennent (A-01). */
    private MultiSelection ajouterCurseur(int offset) {
        List<SelRange> plages = new ArrayList<>(session.selection().ranges());
        plages.add(SelRange.caretAt(offset));
        return MultiSelection.of(plages);
    }

    /** D-15 — zoome par paliers, le point sous le doigt stable. */
    void zoomer(float focalX, float focalY, float facteur) {
        float nouvelleTaille = PinchZoom.steppedSize(tailleAuDepartDuPincement,
                facteur);
        if (nouvelleTaille == metrics.font().size()) {
            return;
        }
        float ancienCharWidth = metrics.charWidth();
        float ancienLineHeight = metrics.lineHeight();
        float ancienScrollX = scrollX;
        float ancienScrollY = scrollY;
        setFont(new FontSpec(metrics.font().family(), nouvelleTaille));
        scrollX = PinchZoom.scrollXKeepingColumn(focalX, largeurGouttiere,
                ancienScrollX, ancienCharWidth, metrics.charWidth());
        scrollY = PinchZoom.scrollYKeepingRow(focalY, ancienScrollY,
                ancienLineHeight, metrics.lineHeight());
        invalidate();
    }

    private boolean surDoigtLeve() {
        panoramique = false;
        glissement.up();
        return true;
    }

    private boolean surRelachement() {
        glissement.up();
        panoramique = false;
        poigneeTiree = SelectionHandles.HandleId.NONE;
        return true;
    }

    private int offsetAt(float x, float y) {
        return viseur.offsetAt(session.document(), projection, metrics,
                largeurGouttiere, scrollX, scrollY, x, y);
    }
}
