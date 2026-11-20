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
import java.util.function.IntPredicate;

import jo.celeris.core.ai.GhostSuggestion;
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
 * ou tire une poignée, le pincement zoome par paliers (D-15), le panoramique à
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
    // B-01 (l'analyse croisée) : bornes de la dernière édition, pour
    // l'épissure du cache de rangées — la première ligne touchée et
    // le décalage de lignes qu'amène l'édition. L'écouteur d'annonce
    // les écrit ; l'assemblage d'image les lit à la frame suivante.
    int derniereEditionPremiereLigne = Integer.MAX_VALUE;
    int derniereEditionDeltaLignes;
    final EditorSyntax syntaxe;
    // Visible du paquet : le banc de test y lit la mise en page servie (ADR-013).
    final RowLayoutCache cache = new RowLayoutCache();
    private final CompositionState composition = new CompositionState();
    // Visible du paquet : l'assemblage d'image y trace le chrome (C9).
    final ChromePainter chrome = new ChromePainter();
    private final GutterPainter gouttiere;
    final GutterIntegrations marquesGouttiere = new GutterIntegrations();
    private final AccessibilityHost accessibilite = new AccessibilityHost(this);

    // — la session courante et sa projection visuelle (B-01 : la
    //  session vit AVANT l'écouteur d'annonce, qui y lit les bornes
    //  d'édition pour l'épissure du cache)
    TextSession session = TextSession.of("", System::nanoTime);
    VisualProjection projection = new VisualProjection(session.document(), plis, 0);
    private final EditListener ecouteurAnnonce = (debut, fin, retire, insere) -> {
        // B-01 : les bornes de l'édition nourrissent l'épissure du
        // cache — première ligne touchée et décalage de lignes. Les
        // lots multi-édits (rejeu d'annulation, script) ne
        // s'épissent pas : bornes composites, l'invalidation totale
        // reste la voie sûre.
        int ligne = session.document().lineForOffset(Math.min(debut,
                session.document().length()));
        int lignesAjoutees = (int) insere.chars().filter(c -> c == '\n').count()
                - (int) retire.chars().filter(c -> c == '\n').count();
        if (derniereEditionPremiereLigne == Integer.MAX_VALUE) {
            derniereEditionPremiereLigne = ligne;
            derniereEditionDeltaLignes = lignesAjoutees;
        } else if (ligne != derniereEditionPremiereLigne
                || derniereEditionDeltaLignes != 0 || lignesAjoutees != 0) {
            derniereEditionPremiereLigne = -1;
        }
        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
    };
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
    // — le préfetch de viewport (B-03) : la marge se préchauffe au
    //   calme, hors de la boucle de dessin
    final ViewportPrefetch prefetch = new ViewportPrefetch(this);

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
    final KeyboardCursor curseurClavier = new KeyboardCursor(this);
    /** Le collaborateur du routage tactile. */
    final EditorKeys touches = new EditorKeys(this);
    /** Le collaborateur de la loupe du dragage (D-16, le
     *  redressement du 2026-11-13) — la bulle au-dessus du doigt. */
    final Loupe loupe = new Loupe();
    // — le glisser-déposer vit dans son collaborateur (D-20)
    final GlisserDeposer depot = new GlisserDeposer(this);
    /** Le collaborateur du texte fantôme (F-01) : affichage et gestes. */
    final GhostEditor fantome;
    /** La couche des valeurs de débogage en ligne (E-03) : le rendu
     *  en fin de ligne pendant la pause — l'hôte pose, la couche
     *  trace. */
    private final ValeursPainter valeursPeintre;
    /** La couche des indices en ligne (inlay hints — l'audit Android
     *  Studio du 2026-11-14) : le type du « var » se lit en fin de
     *  ligne utile — l'hôte pose, la couche trace. */
    private final InlayHintsPainter indicesPeintre;
    /** Le collaborateur de diagnostic de scène (M7.5, §9) : retenue
     *  d'analyse, compteur d'images décolorées, lectures de vérité. */
    final SceneDiagnostics diagnostics;
    /** Le collaborateur des ancres de popups (M7.5, §7). */
    final CaretAnchors ancres = new CaretAnchors(this);
    /** Les réglages de l'éditeur (l'audit du propriétaire du
     *  2026-11-14 : « un système central editor settings ») — UNE
     *  table, un point de vérité : la vue y lit ses portes (numéros,
     *  retour de ligne, fantôme IA, indices en ligne), l'orchestrateur
     *  des popups et l'hôte lisent les mêmes. */
    final ReglagesEditeur reglages = new ReglagesEditeur();
    // — la source unique des réglages (ADR-0022, phase 2, T2) : le
    // thème, la police, la taille, le retour de ligne, les ligatures
    // et les portes y vivent — la vue applique, les popups suivent,
    // l'hôte écoute. Le champ s'arme au constructeur (l'écouteur
    // d'application y naît) ; visible du paquet pour l'assemblage.
    final EditorSettings reglagesUniques;
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
        fantome = new GhostEditor(this, pinceaux);
        // La respiration naît à la densité de l'appareil (PIEGE-23, M7.5 §5).
        gouttiereEtat.setClasse(ClasseLargeur.MOYEN, densite());
        gouttiere = GutterPainter.monter(pinceaux, this);
        trace = ScrollFrame.assemble(gouttiere, pinceaux, () -> syntaxe.guides,
                sourceLiee::pieceDuPli, densite(), composition, cache, sourceLiee, fantome.peintre(), syntaxe);
        CouchesDuTexte couches = monterCouchesAuDessusDuTexte(pinceaux);
        valeursPeintre = couches.valeurs();
        occurrencesPeintre = couches.occurrences();
        indicesPeintre = couches.indices();
        pinceauChrome = pinceaux.newBrush();
        // Les métriques naissent à la densité de l'appareil : la police
        // logique (sp) se convertit une fois pour toutes (PIEGE-23).
        metrics = polices.metricsOf(POLICE_PAR_DEFAUT);
        viseur = new HitTest(metrics);
        syntaxe.attacher(session);
        session.addEditListener(ecouteurAnnonce);
        longPresses = GesturesFactory.appuisLongs(context, this);
        pincements = GesturesFactory.pincements(context, this);
        // D-20 : la vue reçoit les dépôts — le texte s'insère au point
        // de chute, les fichiers partent à l'hôte par le rappel.
        setOnDragListener(depot);
        // T2 (ADR-0022, phase 2) : la source unique des réglages —
        // la vue y applique ce qui s'y pose (thème, police, retour de
        // ligne, ligatures, portes), les popups et l'hôte s'y
        // abonnent. L'écouteur d'application naît ici, une fois :
        // c'est LA façon dont la vue apprend ses réglages.
        reglagesUniques = new EditorSettings();
        reglagesUniques.addListener(source -> appliquerReglages(source));
    }

    /** Les couches qui se tracent APRÈS le texte (le constructeur
     *  y allongeait) : les valeurs de débogage en ligne (E-03, la
     *  valeur se lit par-dessus la ligne utile à l'encre de
     *  gouttière), la poignée des occurrences du mot au caret (C11,
     *  la couche vit dans le pipeline, la vue garde l'armement), et
     *  les indices en ligne (le type du « var » se lit en fin de
     *  ligne utile, à l'encre muette italique — jamais dans le flux,
     *  le texte ne se décale pas). */
    private CouchesDuTexte monterCouchesAuDessusDuTexte(BrushFactory pinceaux) {
        ValeursPainter valeurs = new ValeursPainter(pinceaux);
        trace.addLayer(valeurs);
        OccurrencesPainter occurrences = trace.coucheOccurrences();
        InlayHintsPainter indices = new InlayHintsPainter(pinceaux);
        trace.addLayer(indices);
        return new CouchesDuTexte(valeurs, occurrences, indices);
    }

    /** Les couches posées au-dessus du texte (C11, E-03) — le
     *  constructeur y assigne ses champs finaux. */
    private record CouchesDuTexte(ValeursPainter valeurs,
            OccurrencesPainter occurrences, InlayHintsPainter indices) {
    }

    /** Applique les réglages de la source — la vue reste maîtresse
     *  de l'application (champs d'image, métriques, porte des
     *  commutateurs), la source porte l'état (T2). */
    private void appliquerReglages(EditorSettings source) {
        // le thème : le champ d'image se repeint
        theme = source.theme();
        // la police : la mécanique vit dans ViewFonts (PIEGE-05/28)
        polices.setFont(source.font());
        // le retour de ligne : la porte se souvient, le budget suit
        reglages.poser(ReglagesEditeur.Porte.RETOUR_LIGNE,
                source.softWrap());
        retourALaLigne = source.softWrap();
        scrollX = 0f;
        // les ligatures : l'usine de pinceaux décide du re-tracé
        usinePinceaux.setLigatures(source.ligatures());
        invalidate();
    }

    /**
     * Affiche la session donnée : le modèle, sa sélection et son
     * historique d'annulation. Sans effet après {@link #release()} —
     * la vue libérée est inerte.
     *
     * @param nouvelleSession la session d'édition à afficher
     */
    public void setSession(@Nullable TextSession nouvelleSession) {
        setSession(nouvelleSession, syntaxe.language());
    }

    /**
     * Affiche la session ET sa langue d'un seul geste (le retour
     * terrain du 2026-11-19 : « la coloration se refait encore et
     * encore à chaque affichage » — la pose séparée tokenisait le
     * document neuf avec la langue de l'onglet PRÉCÉDENT, puis le
     * re-tokenisait avec la bonne : DEUX passes par changement
     * d'onglet, trois quand la langue arrivait en retard). La pose
     * combinée rattache le document UNE fois, avec SA langue.
     * Sans effet après {@link #release()}.
     *
     * @param nouvelleSession la session d'édition à afficher
     * @param langue          le support du langage du document, null
     *                        pour aucun
     */
    public void setSession(@Nullable TextSession nouvelleSession,
            @Nullable LanguageSupport langue) {
        if (liberee) {
            return;
        }
        // D11 (l'analyse croisée) : null VIDE la vue (l'onglet fermé
        // du départ) — une session vide inerte y prend la place, le
        // NPE du premier écouteur ne crashe plus l'hôte.
        TextSession posee = nouvelleSession != null
                ? nouvelleSession
                : TextSession.of("");
        session.removeEditListener(ecouteurAnnonce);
        // La composition IME meurt avec le document quitté : ses
        // offsets n'appartiennent pas au document qui arrive (A-19,
        // l'analyse croisée — le backspace juste après un changement
        // d'onglet appliquait les bornes de la composition morte au
        // document neuf et y détruisait des caractères).
        composition.finish();
        session = posee;
        session.addEditListener(ecouteurAnnonce);
        syntaxe.attacher(session, langue);
        images.reinitialiserScene(true);
    }

    /**
     * Branche le langage — le support complet (M7.6) : sa source donne
     * coloration, plis et diagnostics (B-01, A-15, D-02, D-07), sa
     * configuration nourrit la frappe ; null coupe l'analyse. La
     * langue INCHANGÉE ne purge rien (le retour terrain du
     * 2026-11-19 : chaque pose vidait le cache de mise en page et
     * re-tokenisait le document — même langue, même travail refait).
     * Sans effet après {@link #release()}.
     * @param nouvelleLangue le support du langage, null pour aucun
     */
    public void setLanguage(@Nullable LanguageSupport nouvelleLangue) {
        if (liberee || nouvelleLangue == syntaxe.language()) {
            return;
        }
        syntaxe.setLanguage(nouvelleLangue);
        images.reinitialiserScene(false);
    }

    /** Le concentrateur d'intelligence de la vue (M7.5, §8) — la frappe
     *  intelligente s'y pose, les popups y lisent la complétion. */
    public IntelligenceHub intelligence() {
        return intelligence;
    }

    /**
     * Abonne un écouteur des changements de taille de police (D-15 —
     * ADR-0022, phase 2, T3) : le pincement, la molette Ctrl et le
     * clavier y notifient CHACUN leurs abonnés — la façade du kit et
     * la persistance de l'hôte vivent ensemble.
     *
     * @param ecouteur l'écouteur, jamais null
     */
    public void addFontSizeChangeListener(java.util.function.Consumer<Float> ecouteur) {
        polices.addFontSizeChangeListener(ecouteur);
    }

    /**
     * Désabonne un écouteur des changements de taille de police.
     *
     * @param ecouteur l'écouteur retiré
     */
    public void removeFontSizeChangeListener(java.util.function.Consumer<Float> ecouteur) {
        polices.removeFontSizeChangeListener(ecouteur);
    }

    /** Pose une police de plateforme sous un nom de famille (D-14) : la
     *  mesure et le tracé la suivent (PIEGE-22) — la voie de l'hôte pour
     *  sa police à ligatures ; la famille active se redérive aussitôt. */
    public void poserPolice(String famille, Typeface police) {
        polices.poserPolice(famille, police);
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

    /**
     * Abonne un écouteur des changements de fenêtre (M7.5, §7 —
     * ADR-0022, phase 2, T3) : défilement, zoom, police, retour à la
     * ligne, redimensionnement. Le repositionneur des popups du kit
     * et l'écouteur de l'hôte vivent ENSEMBLE — plus d'écrasement
     * silencieux (point 13 de la revue).
     *
     * @param ecouteur l'écouteur, jamais null
     */
    public void addViewportChangeListener(Runnable ecouteur) {
        fenetre.addListener(ecouteur);
    }

    /**
     * Désabonne un écouteur des changements de fenêtre.
     *
     * @param ecouteur l'écouteur retiré
     */
    public void removeViewportChangeListener(Runnable ecouteur) {
        fenetre.removeListener(ecouteur);
    }

        /**
     * D-03 (l'analyse croisée) : l'état d'affichage part au Bundle de
     * la rotation — le défilement (fractions, indépendantes de la
     * densité et de la taille d'écran), le zoom, le retour de ligne,
     * les numéros. La vue sauvegarde ELLE-MÊME son état transitoire :
     * l'hôte n'a plus rien à porter, son Bundle transporte le nôtre.
     *
     * @param etat le Bundle de la rotation à remplir
     */
    public void saveState(android.os.Bundle etat) {
        etat.putFloat("celeris.scroll.fractionY", scrollFractionY());
        // le défilement horizontal vit en pixels (aucune fraction X
        // publique) : la largeur affichée d'une rotation à l'autre
        // varie peu, l'offset reste parlant
        etat.putFloat("celeris.scroll.pxX", scrollX);
        etat.putFloat("celeris.zoom.sp", settings().fontSizeSp());
        etat.putBoolean("celeris.retourLigne", settings().softWrap());
        etat.putBoolean("celeris.numeros", numerosVisibles);
    }

    /**
     * D-03 : l'état sauvegardé revient — après rotation, la vue
     * recréée retrouve son défilement, son zoom et ses portes. Un
     * Bundle sans nos clés ne casse rien : les défauts tiennent.
     *
     * @param etat le Bundle de la rotation
     */
    public void restoreState(android.os.Bundle etat) {
        if (etat.containsKey("celeris.zoom.sp")) {
            settings().setFontSizeSp(etat.getFloat("celeris.zoom.sp"));
        }
        if (etat.containsKey("celeris.retourLigne")) {
            settings().setSoftWrap(etat.getBoolean("celeris.retourLigne"));
        }
        if (etat.containsKey("celeris.numeros")) {
            setNumerosVisibles(etat.getBoolean("celeris.numeros"));
        }
        if (etat.containsKey("celeris.scroll.fractionY")) {
            scrollToFractionY(etat.getFloat("celeris.scroll.fractionY"));
        }
        if (etat.containsKey("celeris.scroll.pxX")) {
            scrollX = etat.getFloat("celeris.scroll.pxX");
            invalidate();
        }
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

    /** La couche des occurrences du mot au caret (C11) — muette à
     *  la naissance, l'hôte l'arme. */
    private final OccurrencesPainter occurrencesPeintre;

    /** Affiche ou masque les numéros de lignes (D-01) : masqués, la gouttière
     *  se réduit à la coulisse des marqueurs (plis D-07, erreurs D-02). */
    public void setNumerosVisibles(boolean visibles) {
        // la porte des réglages se souvient : la bascule y vit
        reglages.poser(ReglagesEditeur.Porte.NUMEROS_LIGNES, visibles);
        numerosVisibles = visibles;
        gouttiere.setNumerosVisibles(visibles);
        invalidate();
    }

    /** Arme ou coupe le voile des occurrences du mot au caret (C11) :
     *  les exemplaires du mot courant s'illuminent d'un voile discret
     *  (la teinte de sélection à l'alpha 0x33) — muet par défaut,
     *  l'hôte décide. La couche se tait d'elle-même pendant les
     *  sélections étendues et sur les non-identifiants.
     *
     * @param visibles vrai pour armer le voile d'occurrences
     */
    public void setOccurrencesVisibles(boolean visibles) {
        occurrencesPeintre.setVisible(visibles);
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

    /**
     * Abonne un consommateur des fichiers déposés (D-20 — ADR-0022,
     * phase 2, T3) : chaque uri déposée y part, l'hôte décide de
     * l'ouverture.
     *
     * @param consommateur le consommateur d'uri, jamais null
     */
    public void addFileDropListener(Consumer<String> consommateur) {
        depot.addFileDropListener(consommateur);
    }

    /**
     * Désabonne un consommateur des fichiers déposés.
     *
     * @param consommateur le consommateur retiré
     */
    public void removeFileDropListener(Consumer<String> consommateur) {
        depot.removeFileDropListener(consommateur);
    }

    /**
     * Abonne un consommateur du plan du fichier (B-16 — ADR-0022,
     * phase 2, T3), notifié à chaque publication d'analyse.
     *
     * @param consommateur le consommateur, jamais null
     */
    public void addPlanListener(Consumer<List<OutlineEntry>> consommateur) {
        syntaxe.addPlanListener(consommateur);
    }

    /**
     * Désabonne un consommateur du plan du fichier.
     *
     * @param consommateur le consommateur retiré
     */
    public void removePlanListener(Consumer<List<OutlineEntry>> consommateur) {
        syntaxe.removePlanListener(consommateur);
    }

    /**
     * Abonne un verdict de double-tap (ADR-0022, phase 2, T3) :
     * consommé par AU MOINS UN abonné, la cascade du tap simple ne
     * suit pas.
     *
     * @param verdict le verdict d'offset, jamais null
     */
    public void addDoubleTapListener(IntPredicate verdict) {
        rappels.addDoubleTapListener(verdict);
    }

    /**
     * Désabonne un verdict de double-tap.
     *
     * @param verdict le verdict retiré
     */
    public void removeDoubleTapListener(IntPredicate verdict) {
        rappels.removeDoubleTapListener(verdict);
    }

    /**
     * Abonne un consommateur de bascule de point d'arrêt (D-04 —
     * ADR-0022, phase 2, T3) : l'appui long de gouttière rend la
     * ligne visée à CHACUN ses abonnés.
     *
     * @param consommateur le consommateur de ligne, jamais null
     */
    public void addBreakpointToggleListener(IntConsumer consommateur) {
        rappels.addBreakpointToggleListener(consommateur);
    }

    /**
     * Désabonne un consommateur de bascule de point d'arrêt.
     *
     * @param consommateur le consommateur retiré
     */
    public void removeBreakpointToggleListener(IntConsumer consommateur) {
        rappels.removeBreakpointToggleListener(consommateur);
    }

    /**
     * Abonne un verdict des touches matérielles (PIEGE-27 — ADR-0022,
     * phase 2, T3) : la touche remonte à CHACUN ses abonnés avant la
     * table de raccourcis ; consommée par au moins un, la cascade
     * s'arrête.
     *
     * @param verdict le verdict, jamais null
     */
    public void addPopupKeyListener(IntPredicate verdict) {
        rappels.addPopupKeyListener(verdict);
    }

    /**
     * Désabonne un verdict des touches matérielles.
     *
     * @param verdict le verdict retiré
     */
    public void removePopupKeyListener(IntPredicate verdict) {
        rappels.removePopupKeyListener(verdict);
    }

    /** La loupe du dragage (D-16) — l'hôte y règle le zoom, ×2 par défaut. */
    public Loupe loupe() {
        return loupe;
    }

    /** Les intégrations de la gouttière : marques, arrêts, clic (D-03 à D-05). */
    public GutterIntegrations marquesGouttiere() {
        return marquesGouttiere;
    }

    /** Dépose (ou retire) la suggestion fantôme visible (F-01) : la
     *  première ligne à l'ancre, les suivantes au bord gauche du
     *  texte, à l'encre fanée. */
    public void setFantome(@Nullable GhostSuggestion suggestion) {
        // la porte des réglages ferme le fantôme : la suggestion d'une
        // porte fermée ne se trace jamais
        fantome.setSuggestion(reglages.ouverte(
                ReglagesEditeur.Porte.FANTOME_IA) ? suggestion : null);
    }

    /** Dépose (ou retire) les valeurs de débogage en ligne (E-03) : la
     *  pause les pose en fin de ligne utile, la reprise les retire —
     *  l'hôte transpose, la vue ne connaît pas le débogueur. */
    public void setValeursEnLigne(@Nullable List<ValeurLigne> valeurs) {
        // la porte des réglages ferme la couche : les valeurs d'une
        // porte fermée ne se tracent pas
        valeursPeintre.setValeurs(reglages.ouverte(
                ReglagesEditeur.Porte.VALEURS_PAUSE) ? valeurs : null);
        invalidate();
    }

    /**
     * Dépose (ou retire) les indices en ligne (inlay hints — l'audit
     * Android Studio du 2026-11-14) : le type du « var » se lit en
     * fin de ligne utile, à l'encre muette italique — jamais dans le
     * flux, le texte ne se décale pas. L'hôte transpose (l'analyse
     * locale des « var », la réponse inlayHint du serveur), la couche
     * trace.
     *
     * @param indices les indices par ligne, ou {@code null} pour
     *                retirer
     */
    public void setIndicesInlay(@Nullable List<IndiceInlay> indices) {
        // la porte des réglages ferme la couche : l'hôte peut poser, la
        // vue n'arme pas la lecture assistée d'une porte fermée
        indicesPeintre.setIndices(reglages.ouverte(
                ReglagesEditeur.Porte.INDICES_EN_LIGNE) ? indices : null);
        invalidate();
    }

    /**
     * Les réglages de l'éditeur — la table centrale des commutateurs
     * (l'audit du propriétaire du 2026-11-14 : « un système central
     * editor settings ») : la vue, l'orchestrateur des popups et
     * l'hôte y lisent les mêmes portes.
     *
     * @return la table des portes, vivante
     */
    public ReglagesEditeur reglages() {
        return reglages;
    }

    /**
     * Les réglages de l'éditeur — LA source unique (ADR-0022, phase
     * 2, T2) : thème, police, taille, retour de ligne, ligatures et
     * portes de fonctionnalités s'y posent, la vue applique, les
     * popups et l'hôte s'y abonnent.
     *
     * @return les réglages vivants de la vue
     */
    public EditorSettings settings() {
        return reglagesUniques;
    }

    /**
     * Abonne un écouteur des gestes du fantôme (F-01 — ADR-0022,
     * phase 2, T3) : Tab et l'appui sur le fantôme notifient CHACUN
     * leurs abonnés.
     *
     * @param ecouteur l'écouteur, jamais null
     */
    public void addGhostGestureListener(Consumer<GhostGesture> ecouteur) {
        fantome.addGhostGestureListener(ecouteur);
    }

    /**
     * Désabonne un écouteur des gestes du fantôme.
     *
     * @param ecouteur l'écouteur retiré
     */
    public void removeGhostGestureListener(Consumer<GhostGesture> ecouteur) {
        fantome.removeGhostGestureListener(ecouteur);
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

    /** Prépare l'état d'image (gouttière AVANT le budget de colonnes,
     *  PIEGE-25) — l'assemblage vit dans {@link FrameAssembler} ; visible
     *  du paquet : les collaborateurs y préparent avant de lire. */
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
        // La touche d'abord aux popups de l'hôte (PIEGE-27 : délégation
        // sans le focus), puis la table de raccourcis décide.
        if (rappels.consommerTouche(composition, keyCode)) {
            return true;
        }
        EditorAction action = KeyActions.action(this, raccourcis, composition,
                keyCode, event);
        return action == null
                ? super.onKeyDown(keyCode, event)
                : KeyActions.appliquer(this, action);
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

    /**
     * La libération de fin de vie (chantiers C1/C2 de la revue du
     * 2026-11-17) : la session ne retient plus la vue (les écouteurs
     * d'édition et le pont de syntaxe se détachent), le pont referme
     * ses moteurs, les fils d'analyse du kit (« celeris-analyse » et
     * son minuteur) s'éteignent. Idempotent : un second appel ne fait
     * rien.
     *
     * <p>Le détachement de la fenêtre n'est PAS une libération : la
     * vue détachée puis rattachée continue d'éditer (le cycle fenêtre
     * reste celui d'Android). La libération est explicite —
     * {@code release()} quand la vue quitte définitivement la scène
     * (ou via l'orchestrateur qui la porte). Après release(), la vue
     * est inerte : {@link #setSession} et {@link #setLanguage} se
     * refusent, un nouveau montage demande une nouvelle vue.</p>
     */
    public void release() {
        if (liberee) {
            return;
        }
        liberee = true;
        session.removeEditListener(ecouteurAnnonce);
        // D11 (suite, l'analyse croisée) : le champ ne retient plus la
        // session morte — une session vide inerte y prend la place
        // (comme setSession(null)), le document de l'onglet fermé
        // cesse de vivre retenu par une vue libérée.
        session = TextSession.of("");
        syntaxe.fermer();
        diagnostics.shutdown();
        prefetch.arreter();
    }

    /** La vue est-elle libérée — lecture du banc (chantier C1). */
    boolean estLiberee() {
        return liberee;
    }

    /** Le drapeau de libération (chantier C1) : release() s'y lit. */
    private boolean liberee;

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
