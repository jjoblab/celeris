package jo.celeris.android.view;

import jo.celeris.core.text.TextSession;

import jo.celeris.android.view.internal.access.EditorAccess;
import jo.celeris.android.view.internal.caret.CaretAnchors;
import jo.celeris.android.view.internal.diagnostics.SceneDiagnostics;
import jo.celeris.android.view.internal.fonts.ViewFonts;
import jo.celeris.android.view.internal.gestures.DragDrop;
import jo.celeris.android.view.internal.gestures.GhostEditor;
import jo.celeris.android.view.internal.gestures.PinchZoomHandler;
import jo.celeris.android.view.internal.gutter.EditorGutter;
import jo.celeris.android.view.internal.input.EditorKeys;
import jo.celeris.android.view.internal.input.KeyboardCursor;
import jo.celeris.android.view.internal.model.HostCallbacks;
import jo.celeris.android.view.internal.paint.ChromePainter;
import jo.celeris.android.view.internal.viewport.ViewportPrefetch;
import jo.celeris.android.view.internal.viewport.ViewportWatch;

/**
 * L'implémentation du pont d'accès interne (P7) : elle vit au
 * paquet racine et seule touche les champs package-privés de la
 * vue. Les familles déplacées ({@link jo.celeris.android.view.internal.input.EditorKeys},
 * {@link jo.celeris.android.view.internal.frame.FrameAssembler}…) reçoivent
 * {@link EditorAccess} — jamais la vue ni ses champs.
 *
 * <p>Le contrat public d'{@code EditorView} ne bouge pas : le pont
 * est package-privé au racine, l'interface vit en internal (filtre
 * ADR-009). C'est la « fenêtre d'accès interne » de la décision
 * propriétaire n° 1 du 2026-11-20 : internes seulement.</p>
 */
final class EditorViewAccess implements EditorAccess {

    private final EditorView vue;

    EditorViewAccess(EditorView vue) {
        this.vue = vue;
    }

    @Override
    public TextSession session() {
        return vue.session;
    }

    @Override
    public VisualProjection projection() {
        return vue.projection;
    }

    @Override
    public void setProjection(VisualProjection valeur) {
        vue.projection = valeur;
    }

    @Override
    public EditorMetrics metrics() {
        return vue.metrics;
    }

    @Override
    public void setMetrics(EditorMetrics valeur) {
        vue.metrics = valeur;
    }

    @Override
    public FontSpec policeLogique() {
        return vue.policeLogique;
    }

    @Override
    public void setPoliceLogique(FontSpec valeur) {
        vue.policeLogique = valeur;
    }

    @Override
    public EditorTheme theme() {
        return vue.theme;
    }

    @Override
    public void setTheme(EditorTheme valeur) {
        vue.theme = valeur;
    }

    @Override
    public long revisionSuivie() {
        return vue.revisionSuivie;
    }

    @Override
    public void setRevisionSuivie(long valeur) {
        vue.revisionSuivie = valeur;
    }

    @Override
    public boolean retourALaLigne() {
        return vue.retourALaLigne;
    }

    @Override
    public void setRetourALaLigne(boolean valeur) {
        vue.retourALaLigne = valeur;
    }

    @Override
    public boolean publicationRecue() {
        return vue.publicationRecue;
    }

    @Override
    public void setPublicationRecue(boolean valeur) {
        vue.publicationRecue = valeur;
    }

    @Override
    public EditorSyntax syntaxe() {
        return vue.syntaxe;
    }

    @Override
    public FoldModel folds() {
        return vue.folds;
    }

    @Override
    public RowLayoutCache cache() {
        return vue.cache;
    }

    @Override
    public RebindingRowSource sourceLiee() {
        return vue.sourceLiee;
    }

    @Override
    public float scrollX() {
        return vue.scrollX;
    }

    @Override
    public void setScrollX(float valeur) {
        vue.scrollX = valeur;
    }

    @Override
    public float scrollY() {
        return vue.scrollY;
    }

    @Override
    public void setScrollY(float valeur) {
        vue.scrollY = valeur;
    }

    @Override
    public float largeurGouttiere() {
        return vue.largeurGouttiere;
    }

    @Override
    public void setLargeurGouttiere(float valeur) {
        vue.largeurGouttiere = valeur;
    }

    @Override
    public boolean visibleNumbers() {
        return vue.visibleNumbers;
    }

    @Override
    public void setVisibleNumbers(boolean valeur) {
        vue.visibleNumbers = valeur;
    }

    @Override
    public int derniereEditionPremiereLigne() {
        return vue.derniereEditionPremiereLigne;
    }

    @Override
    public void setDerniereEditionPremiereLigne(int valeur) {
        vue.derniereEditionPremiereLigne = valeur;
    }

    @Override
    public int derniereEditionDeltaLignes() {
        return vue.derniereEditionDeltaLignes;
    }

    @Override
    public void setDerniereEditionDeltaLignes(int valeur) {
        vue.derniereEditionDeltaLignes = valeur;
    }

    @Override
    public SelectionDrag glissement() {
        return vue.glissement;
    }

    @Override
    public HitTest viseur() {
        return vue.viseur;
    }

    @Override
    public void setViseur(HitTest valeur) {
        vue.viseur = valeur;
    }

    @Override
    public HostCallbacks rappels() {
        return vue.rappels;
    }

    @Override
    public PinchZoomHandler zoom() {
        return vue.zoom;
    }

    @Override
    public KeyboardCursor curseurClavier() {
        return vue.curseurClavier;
    }

    @Override
    public EditorKeys touches() {
        return vue.touches;
    }

    @Override
    public Magnifier magnifier() {
        return vue.magnifier;
    }

    @Override
    public GhostEditor ghost() {
        return vue.ghost;
    }

    @Override
    public DragDrop depot() {
        return vue.depot;
    }

    @Override
    public CaretAnchors ancres() {
        return vue.ancres;
    }

    @Override
    public IntelligenceHub intelligence() {
        return vue.intelligence;
    }

    @Override
    public ViewportPrefetch prefetch() {
        return vue.prefetch;
    }

    @Override
    public SceneDiagnostics diagnostics() {
        return vue.diagnostics;
    }

    @Override
    public ViewFonts polices() {
        return vue.polices;
    }

    @Override
    public CanvasSurface surface() {
        return vue.surface;
    }

    @Override
    public ScrollFrame trace() {
        return vue.trace;
    }

    @Override
    public ChromePainter chrome() {
        return vue.chrome;
    }

    @Override
    public TextBrush pinceauChrome() {
        return vue.pinceauChrome;
    }

    @Override
    public EditorGutter gutterState() {
        return vue.gutterState;
    }

    @Override
    public GutterIntegrations gutterMarks() {
        return vue.gutterMarks;
    }

    @Override
    public ViewportWatch window() {
        return vue.window;
    }

    @Override
    public FrameState etatImage() {
        return vue.etatImage;
    }

    @Override
    public EditorSettings reglagesUniques() {
        return vue.reglagesUniques;
    }

    @Override
    public float densite() {
        return vue.densite();
    }

    @Override
    public void preparerEtatImage() {
        vue.preparerEtatImage();
    }

    @Override
    public FrameState etatImageCourante() {
        return vue.etatImageCourante();
    }




    @Override
    public void invalidate() {
        vue.invalidate();
    }

    @Override
    public boolean performClick() {
        return vue.performClick();
    }

    @Override
    public int getWidth() {
        return vue.getWidth();
    }

    @Override
    public int getHeight() {
        return vue.getHeight();
    }

    @Override
    public android.content.Context getContext() {
        return vue.getContext();
    }

    @Override
    public boolean requestFocus() {
        return vue.requestFocus();
    }

    @Override
    public boolean hasFocus() {
        return vue.hasFocus();
    }

    @Override
    public android.os.IBinder getWindowToken() {
        return vue.getWindowToken();
    }

    @Override
    public android.os.Handler getHandler() {
        return vue.getHandler();
    }

    @Override
    public void setFont(String famille, android.graphics.Typeface police) {
        vue.reglePolices.setFont(famille, police);
    }

    @Override
    public boolean surTab() {
        return vue.ghost.surTab();
    }

    @Override
    public boolean onCommit(String texte) {
        return vue.intelligence.onCommit(texte);
    }

    @Override
    public boolean tabTyped() {
        return vue.intelligence.tabTyped();
    }

    @Override
    public boolean untabTyped() {
        return vue.intelligence.untabTyped();
    }

    @Override
    public boolean deleteForwardTyped() {
        return vue.intelligence.deleteForwardTyped();
    }

    @Override
    public boolean enterTyped() {
        return vue.intelligence.enterTyped();
    }

    @Override
    public boolean backspaceTyped() {
        return vue.intelligence.backspaceTyped();
    }

    @Override
    public android.content.res.Resources getResources() {
        return vue.getResources();
    }

    @Override
    public boolean postDelayed(Runnable action, long delaiMs) {
        return vue.postDelayed(action, delaiMs);
    }

    @Override
    public void removeCallbacks(Runnable action) {
        vue.removeCallbacks(action);
    }

    @Override
    public boolean dispatchKeyEvent(android.view.KeyEvent evenement) {
        return vue.dispatchKeyEvent(evenement);
    }

    @Override
    public void sendAccessibilityEvent(int typeEvenement) {
        vue.sendAccessibilityEvent(typeEvenement);
    }

    @Override
    public EditorView vue() {
        return vue;
    }
}
