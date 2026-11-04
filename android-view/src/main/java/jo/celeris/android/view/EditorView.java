package jo.celeris.android.view;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextSession;

import org.jspecify.annotations.Nullable;

/**
 * La vue d'éditeur : une seule vue Canvas (ADR-006), propriétaire du
 * pipeline à couches, du cache de rangées et de l'état d'image.
 *
 * <p>La vue est mince par construction : toute la logique vérifiable
 * sans appareil vit dans les classes pures du module (métriques,
 * géométries, projection, gestuelle, moteur de déplacement, table de
 * raccourcis) ; la vue les assemble, attache la surface au canvas de
 * l'image et route le tactile et le clavier. Le défilement vertical et
 * horizontal est borné au contenu ; le tactile à un doigt pose le
 * curseur puis étend la sélection (ancre figée au DOWN, PIEGE-03) ou
 * tire une poignée de plage, l'appui long arme la pilule « ajouter un
 * curseur » (A-01), le pincement zoome par paliers le point sous le
 * doigt stable (D-15), le panoramique à deux doigts défile, et le
 * clavier matériel passe par la table de raccourcis (D-17). Le retour
 * à la ligne souple par offsets (A-18) se règle par
 * {@link #setSoftWrap(boolean)}.</p>
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
    private final RowLayoutCache cache = new RowLayoutCache();
    private final CursorPill pilule = new CursorPill();
    private final TextBrush pinceauChrome;
    private final GestureDetector longPresses;
    private final ScaleGestureDetector pincements;

    private KeyBindings raccourcis = KeyBindings.defaults();
    private SelectionHandles.HandleId poigneeTiree = SelectionHandles.HandleId.NONE;
    private float tailleAuDepartDuPincement;
    private boolean pincementActif;

    private TextSession session = TextSession.of("", System::nanoTime);
    private VisualProjection projection =
            new VisualProjection(session.document(), plis, 0);
    private EditorMetrics metrics = EditorMetrics.of(POLICE_PAR_DEFAUT, regle);
    private HitTest viseur = new HitTest(metrics);
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
    private boolean panoramique;

    public EditorView(Context context) {
        this(context, null);
    }

    public EditorView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        BrushFactory pinceaux = new PaintBrushes();
        trace.addLayer(new GutterPainter(pinceaux, MARGE_GOUTTIERE));
        trace.addLayer(new SelectionPainter(pinceaux));
        trace.addLayer(new TextPainter(pinceaux, cache, sourceLiee));
        pinceauChrome = pinceaux.newBrush();
        longPresses = new GestureDetector(context, new GestureDetector
                .SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent event) {
                pilule.show(event.getX(), event.getY());
                invalidate();
            }
        });
        pincements = new ScaleGestureDetector(context,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                pincementActif = true;
                tailleAuDepartDuPincement = metrics.font().size();
                return true;
            }

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                zoomer(detector.getFocusX(), detector.getFocusY(),
                        detector.getScaleFactor());
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                pincementActif = false;
            }
        });
    }

    /**
     * Affiche la session donnée : le modèle, sa sélection et son
     * historique d'annulation.
     *
     * @param nouvelleSession la session d'édition à afficher
     */
    public void setSession(TextSession nouvelleSession) {
        session = nouvelleSession;
        revisionSuivie = 0L;
        scrollX = 0f;
        scrollY = 0f;
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
        Document document = session.document();
        suivreRevision(document);
        float hauteurContenu = projection.rowCount() * metrics.lineHeight();
        scrollY = ScrollGeometry.clamp(scrollY, hauteurContenu, getHeight());
        largeurGouttiere = GutterGeometry.width(Math.max(1, projection.rowCount()),
                metrics.charWidth(), MARGE_GOUTTIERE);
        scrollX = ScrollGeometry.clamp(scrollX, largeurContenu(), largeurVisible());
        int premiere = ScrollGeometry.firstRow(scrollY, metrics.lineHeight());
        int visibles = ScrollGeometry.visibleRows(getHeight(), metrics.lineHeight());
        etatImage.beginFrame(document, session.selection(), projection,
                document.revision(), metrics, theme, premiere, visibles,
                largeurGouttiere, scrollX, scrollY);
        surface.attach(canvas);
        trace.drawFrame(surface, etatImage);
        dessinerChrome();
        surface.detach();
    }

    /** Le chrome propre à la vue : pilule armée, poignées de plage. */
    private void dessinerChrome() {
        if (pilule.isArmed()) {
            pinceauChrome.setColor(theme.cursorColor());
            surface.drawRect(pilule.left(), pilule.top(), pilule.right(),
                    pilule.bottom(), pinceauChrome);
            surface.drawText("+", pilule.x() - metrics.charWidth() / 2f,
                    pilule.bottom() - metrics.descent(), pinceauChrome);
        }
        List<SelRange> plages = session.selection().ranges();
        if (plages.size() == 1 && !plages.get(0).isEmpty()) {
            pinceauChrome.setColor(theme.cursorColor());
            dessinerPoignee(plages.get(0).start());
            dessinerPoignee(plages.get(0).end());
        }
    }

    private void dessinerPoignee(int offset) {
        Document document = session.document();
        float x = SelectionHandles.xOfOffset(document, projection, metrics,
                largeurGouttiere, scrollX, offset);
        float y = SelectionHandles.yOfOffset(document, projection, metrics,
                scrollY, offset);
        surface.drawRect(x - 3f, y, x + 3f, y + metrics.lineHeight(), pinceauChrome);
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
        KeyBinding touche = new KeyBinding(keyCode, event.isShiftPressed(),
                event.isCtrlPressed(), event.isAltPressed());
        EditorAction action = raccourcis.actionOf(touche);
        if (action == null) {
            return super.onKeyDown(keyCode, event);
        }
        return appliquerAction(action);
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
            sourceLiee.set(new DocumentRowSource(document, projection, metrics,
                    colonnesVoulues));
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
        int offset = offsetAt(x, y);
        glissement.down(offset);
        if (pilule.isArmed()) {
            session.setSelection(ajouterCurseur(offset));
        } else {
            session.setSelection(MultiSelection.of(SelRange.caretAt(offset)));
        }
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
    private void zoomer(float focalX, float focalY, float facteur) {
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

    /** D-17 — applique l'action clavier sur la session et la sélection. */
    private boolean appliquerAction(EditorAction action) {
        Document document = session.document();
        switch (action) {
            case DELETE_BACKWARD:
                session.deleteBackward();
                break;
            case DELETE_FORWARD:
                session.deleteForward();
                break;
            case UNDO:
                session.undo();
                break;
            case REDO:
                session.redo();
                break;
            case SELECT_ALL:
                session.setSelection(MultiSelection.of(
                        new SelRange(0, document.length())));
                break;
            default:
                int pageRows = ScrollGeometry.visibleRows(getHeight(),
                        metrics.lineHeight());
                session.setSelection(CaretsMotion.move(document, projection,
                        session.selection(), action, pageRows));
                break;
        }
        invalidate();
        return true;
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
        int rangee = ScrollGeometry.firstRow(scrollY + y, metrics.lineHeight());
        int lignes = projection.rowCount();
        if (rangee >= lignes) {
            rangee = lignes - 1;
        }
        if (rangee < 0) {
            rangee = 0;
        }
        int ligne = projection.firstLineOfRow(rangee);
        int debutTranche = projection.spanStartAtRow(rangee);
        Document document = session.document();
        int longueurLigne = document.lineEnd(ligne) - document.lineStart(ligne);
        int finTranche = Math.min(projection.spanEndAtRow(rangee), longueurLigne);
        int colonne = viseur.columnAt(x - largeurGouttiere + scrollX,
                finTranche - debutTranche);
        return document.lineStart(ligne) + debutTranche + colonne;
    }
}
