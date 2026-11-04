package jo.celeris.android.view;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

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
 * géométries, projection, gestuelle, cache) ; la vue les assemble,
 * attache la surface au canvas de l'image et route le tactile. Le
 * défilement vertical est borné au contenu ; le tactile à un doigt pose
 * le curseur puis étend la sélection (ancre figée au DOWN, PIEGE-03),
 * le panoramique à deux doigts défile.</p>
 */
public final class EditorView extends View {

    private static final FontSpec POLICE_PAR_DEFAUT = new FontSpec("mono", 14f);
    private static final float MARGE_GOUTTIERE = 8f;

    private final AndroidFontRuler regle = new AndroidFontRuler();
    private final CanvasSurface surface = new CanvasSurface();
    private final FrameState etatImage = new FrameState();
    private final ScrollFrame trace = new ScrollFrame();
    private final SelectionDrag glissement = new SelectionDrag();
    private final FoldModel plis = new FoldModel();
    private final RowLayoutCache cache = new RowLayoutCache();

    private TextSession session = TextSession.of("", System::nanoTime);
    private VisualProjection projection = new VisualProjection(1, plis);
    private EditorMetrics metrics = EditorMetrics.of(POLICE_PAR_DEFAUT, regle);
    private HitTest viseur = new HitTest(metrics);
    private DocumentRowSource source = new DocumentRowSource(session.document(), projection,
            metrics);
    private EditorTheme theme = EditorThemes.CLAIR;
    private long revisionSuivie;

    private float scrollY;
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
        trace.addLayer(new TextPainter(pinceaux, cache, source));
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
     * de la règle (PIEGE-05), le cache se vide par révision suivie.
     *
     * @param police la spécification de police
     */
    public void setFont(FontSpec police) {
        metrics = EditorMetrics.of(police, regle);
        viseur = new HitTest(metrics);
        revisionSuivie = 0L;
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
        int premiere = ScrollGeometry.firstRow(scrollY, metrics.lineHeight());
        int visibles = ScrollGeometry.visibleRows(getHeight(), metrics.lineHeight());
        etatImage.beginFrame(document, session.selection(), projection,
                document.revision(), metrics, theme, premiere, visibles,
                largeurGouttiere, 0f, scrollY);
        surface.attach(canvas);
        trace.drawFrame(surface, etatImage);
        surface.detach();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
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
    public boolean performClick() {
        return super.performClick();
    }

    private void suivreRevision(Document document) {
        if (document.revision() != revisionSuivie) {
            cache.invalidate(revisionSuivie);
            revisionSuivie = document.revision();
            projection = new VisualProjection(document.lineCount(), plis);
            source = new DocumentRowSource(document, projection, metrics);
        }
    }

    private boolean surAppui(MotionEvent event) {
        dernierY = event.getY();
        panoramique = false;
        int offset = offsetAt(event.getX(), event.getY());
        glissement.down(offset);
        session.setSelection(MultiSelection.of(SelRange.caretAt(offset)));
        invalidate();
        return true;
    }

    private boolean surDoigtPose(MotionEvent event) {
        if (event.getPointerCount() >= 2) {
            panoramique = true;
            glissement.up();
            dernierY = event.getY();
        }
        return true;
    }

    private boolean surDeplacement(MotionEvent event) {
        if (panoramique && event.getPointerCount() >= 2) {
            float hauteurContenu = projection.rowCount() * metrics.lineHeight();
            scrollY = ScrollGeometry.clamp(scrollY + dernierY - event.getY(),
                    hauteurContenu, getHeight());
            dernierY = event.getY();
            invalidate();
            return true;
        }
        if (glissement.isActive()) {
            session.setSelection(MultiSelection.of(
                    glissement.dragTo(offsetAt(event.getX(), event.getY()))));
            invalidate();
        }
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
        Document document = session.document();
        int debut = document.lineStart(ligne);
        int longueur = document.lineEnd(ligne) - debut;
        int colonne = viseur.columnAt(x - largeurGouttiere, longueur);
        return debut + colonne;
    }
}
