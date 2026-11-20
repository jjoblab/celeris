package jo.celeris.android.view.internal.access;

import jo.celeris.core.text.TextSession;

import jo.celeris.android.view.CanvasSurface;
import jo.celeris.android.view.EditorMetrics;
import jo.celeris.android.view.EditorSettings;
import jo.celeris.android.view.EditorSyntax;
import jo.celeris.android.view.EditorTheme;
import jo.celeris.android.view.EditorView;
import jo.celeris.android.view.FoldModel;
import jo.celeris.android.view.FontSpec;
import jo.celeris.android.view.FrameState;
import jo.celeris.android.view.GutterIntegrations;
import jo.celeris.android.view.HitTest;
import jo.celeris.android.view.IntelligenceHub;
import jo.celeris.android.view.Magnifier;
import jo.celeris.android.view.RebindingRowSource;
import jo.celeris.android.view.RowLayoutCache;
import jo.celeris.android.view.ScrollFrame;
import jo.celeris.android.view.SelectionDrag;
import jo.celeris.android.view.TextBrush;
import jo.celeris.android.view.VisualProjection;
import jo.celeris.android.view.internal.gutter.EditorGutter;
import jo.celeris.android.view.internal.model.HostCallbacks;
import jo.celeris.android.view.internal.paint.ChromePainter;
import jo.celeris.android.view.internal.viewport.ViewportWatch;
import jo.celeris.android.view.internal.caret.CaretAnchors;
import jo.celeris.android.view.internal.diagnostics.SceneDiagnostics;
import jo.celeris.android.view.internal.fonts.ViewFonts;
import jo.celeris.android.view.internal.frame.FrameAssembler;
import jo.celeris.android.view.internal.gestures.DragDrop;
import jo.celeris.android.view.internal.gestures.GhostEditor;
import jo.celeris.android.view.internal.gestures.PinchZoomHandler;
import jo.celeris.android.view.internal.input.EditorKeys;
import jo.celeris.android.view.internal.input.KeyboardCursor;
import jo.celeris.android.view.internal.viewport.ViewportPrefetch;

/**
 * Le pont d'accès interne de la vue (décision propriétaire n° 1,
 * P7) : la surface EXACTE que les familles déplacées touchent de
 * {@code EditorView} — jamais publicisée au contrat (filtre ADR-009),
 * elle vit entre le paquet racine et ses familles internal. L'accès
 * aux champs de la vue reste un droit du paquet racine ; le pont le
 * prête, borné à cette interface.
 *
 * <p>Vue-collaborateur par délégation : les familles ({@link EditorKeys},
 * {@link PinchZoomHandler}, {@link FrameAssembler}…) ne gardent QUE ce
 * handle — plus aucun champ {@code vue} : le compilateur borne le
 * couplage à cette interface, et l'interface borne ce que la vue
 * partage.</p>
 */
public interface EditorAccess {

    /** session — lecture du champ de la vue. */
    TextSession session();

    /** projection — lecture du champ de la vue. */
    VisualProjection projection();

    /** Pose projection — écriture du champ de la vue. */
    void setProjection(VisualProjection valeur);

    /** metrics — lecture du champ de la vue. */
    EditorMetrics metrics();

    /** Pose metrics — écriture du champ de la vue. */
    void setMetrics(EditorMetrics valeur);

    /** policeLogique — lecture du champ de la vue. */
    FontSpec policeLogique();

    /** Pose policeLogique — écriture du champ de la vue. */
    void setPoliceLogique(FontSpec valeur);

    /** theme — lecture du champ de la vue. */
    EditorTheme theme();

    /** Pose theme — écriture du champ de la vue. */
    void setTheme(EditorTheme valeur);

    /** revisionSuivie — lecture du champ de la vue. */
    long revisionSuivie();

    /** Pose revisionSuivie — écriture du champ de la vue. */
    void setRevisionSuivie(long valeur);

    /** retourALaLigne — lecture du champ de la vue. */
    boolean retourALaLigne();

    /** Pose retourALaLigne — écriture du champ de la vue. */
    void setRetourALaLigne(boolean valeur);

    /** publicationRecue — lecture du champ de la vue. */
    boolean publicationRecue();

    /** Pose publicationRecue — écriture du champ de la vue. */
    void setPublicationRecue(boolean valeur);

    /** syntaxe — lecture du champ de la vue. */
    EditorSyntax syntaxe();

    /** folds — lecture du champ de la vue. */
    FoldModel folds();

    /** cache — lecture du champ de la vue. */
    RowLayoutCache cache();

    /** sourceLiee — lecture du champ de la vue. */
    RebindingRowSource sourceLiee();

    /** scrollX — lecture du champ de la vue. */
    float scrollX();

    /** Pose scrollX — écriture du champ de la vue. */
    void setScrollX(float valeur);

    /** scrollY — lecture du champ de la vue. */
    float scrollY();

    /** Pose scrollY — écriture du champ de la vue. */
    void setScrollY(float valeur);

    /** largeurGouttiere — lecture du champ de la vue. */
    float largeurGouttiere();

    /** Pose largeurGouttiere — écriture du champ de la vue. */
    void setLargeurGouttiere(float valeur);

    /** visibleNumbers — lecture du champ de la vue. */
    boolean visibleNumbers();

    /** Pose visibleNumbers — écriture du champ de la vue. */
    void setVisibleNumbers(boolean valeur);

    /** derniereEditionPremiereLigne — lecture du champ de la vue. */
    int derniereEditionPremiereLigne();

    /** Pose derniereEditionPremiereLigne — écriture du champ de la vue. */
    void setDerniereEditionPremiereLigne(int valeur);

    /** derniereEditionDeltaLignes — lecture du champ de la vue. */
    int derniereEditionDeltaLignes();

    /** Pose derniereEditionDeltaLignes — écriture du champ de la vue. */
    void setDerniereEditionDeltaLignes(int valeur);

    /** glissement — lecture du champ de la vue. */
    SelectionDrag glissement();

    /** viseur — lecture du champ de la vue. */
    HitTest viseur();

    /** Pose viseur — écriture du champ de la vue. */
    void setViseur(HitTest valeur);

    /** rappels — lecture du champ de la vue. */
    HostCallbacks rappels();

    /** zoom — lecture du champ de la vue. */
    PinchZoomHandler zoom();

    /** curseurClavier — lecture du champ de la vue. */
    KeyboardCursor curseurClavier();

    /** touches — lecture du champ de la vue. */
    EditorKeys touches();

    /** magnifier — lecture du champ de la vue. */
    Magnifier magnifier();

    /** ghost — lecture du champ de la vue. */
    GhostEditor ghost();

    /** depot — lecture du champ de la vue. */
    DragDrop depot();

    /** ancres — lecture du champ de la vue. */
    CaretAnchors ancres();

    /** intelligence — lecture du champ de la vue. */
    IntelligenceHub intelligence();

    /** prefetch — lecture du champ de la vue. */
    ViewportPrefetch prefetch();

    /** diagnostics — lecture du champ de la vue (final : pas de pose). */
    SceneDiagnostics diagnostics();

    /** polices — lecture du champ de la vue. */
    ViewFonts polices();

    /** surface — lecture du champ de la vue. */
    CanvasSurface surface();

    /** trace — lecture du champ de la vue. */
    ScrollFrame trace();

    /** chrome — lecture du champ de la vue. */
    ChromePainter chrome();

    /** pinceauChrome — lecture du champ de la vue. */
    TextBrush pinceauChrome();

    /** gutterState — lecture du champ de la vue. */
    EditorGutter gutterState();

    /** gutterMarks — lecture du champ de la vue. */
    GutterIntegrations gutterMarks();

    /** window — lecture du champ de la vue. */
    ViewportWatch window();

    /** etatImage — lecture du champ de la vue. */
    FrameState etatImage();

    /** reglagesUniques — lecture du champ de la vue. */
    EditorSettings reglagesUniques();

    /** densite — délégué à la vue. */
    float densite();

    /** preparerEtatImage — délégué à la vue. */
    void preparerEtatImage();

    /** etatImageCourante — délégué à la vue. */
    FrameState etatImageCourante();

    /** getResources — délégué à la vue. */
    android.content.res.Resources getResources();

    // — constantes (EditorView)

    /** La marge du dévoilement de curseur, en dp (M7.5, §5). */
    float MARGE_RETOUR_DP = 8f;

    /** La police par défaut : mono, 14 sp (PIEGE-23) — le pont la
     *  prête en lecture (le champ de la vue est package-privé). */
    /** La police par défaut : mono, 14 sp (PIEGE-23). */
    FontSpec POLICE_PAR_DEFAUT = new FontSpec("mono", 14f);

    /** Le délai de retenue par défaut, ms (M7.5, §9). */
    long DELAI_RETENU_PAR_DEFAUT_MS = 250L;

    /** La constante VISIBLE de View (visibilité de fenêtre). */
    int VISIBLE = 0;

    /** surTab du fantôme — délégué (le fantôme vit en internal). */
    boolean surTab();

    /** onCommit du concentrateur — délégué à la vue. */
    boolean onCommit(String texte);

    /** tabTyped du concentrateur — délégué à la vue. */
    boolean tabTyped();

    /** untabTyped du concentrateur — délégué à la vue. */
    boolean untabTyped();

    /** deleteForwardTyped du concentrateur — délégué à la vue. */
    boolean deleteForwardTyped();

    /** enterTyped du concentrateur — délégué à la vue. */
    boolean enterTyped();

    /** backspaceTyped du concentrateur — délégué à la vue. */
    boolean backspaceTyped();

    /** Pose une police de plateforme sous un nom de famille (D-14) —
     *  délégué à la règle de polices de la vue. */
    void setFont(String famille, android.graphics.Typeface police);

    // — délégations View (API publique d'android.view.View)

    /** invalidate — délégué à la vue. */
    void invalidate();

    /** performClick — délégué à la vue. */
    boolean performClick();

    /** getWidth — délégué à la vue. */
    int getWidth();

    /** getHeight — délégué à la vue. */
    int getHeight();

    /** getContext — délégué à la vue. */
    android.content.Context getContext();

    /** requestFocus — délégué à la vue. */
    boolean requestFocus();

    /** hasFocus — délégué à la vue. */
    boolean hasFocus();

    /** getWindowToken — délégué à la vue. */
    android.os.IBinder getWindowToken();

    /** getHandler — délégué à la vue. */
    android.os.Handler getHandler();

    /** postDelayed — délégué à la vue. */
    boolean postDelayed(Runnable action, long delaiMs);

    /** removeCallbacks — délégué à la vue. */
    void removeCallbacks(Runnable action);
    /** dispatchKeyEvent — délégué à la vue. */
    boolean dispatchKeyEvent(android.view.KeyEvent evenement);

    /** sendAccessibilityEvent — délégué à la vue. */
    void sendAccessibilityEvent(int typeEvenement);

    /** La vue hôte — pour les API plateforme exigées par ces familles
     *  (IME, détecteurs de gestes) ; le reste passe par le pont. */
    EditorView vue();
}
