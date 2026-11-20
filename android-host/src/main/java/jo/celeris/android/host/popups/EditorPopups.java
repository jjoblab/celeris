package jo.celeris.android.host.popups;

import android.graphics.Rect;

import java.util.List;
import java.util.function.Supplier;

import jo.celeris.android.host.spi.HostDiagnostic;
import jo.celeris.android.view.EditorTheme;
import jo.celeris.android.view.EditorSettings;
import jo.celeris.android.view.EditorView;
import jo.celeris.android.view.EditorSettingsPanel;
import jo.celeris.android.widgets.CompletionDocModel;
import jo.celeris.android.widgets.CompletionDocView;
import jo.celeris.android.widgets.CompletionListView;
import jo.celeris.android.widgets.IntentionsMenuView;
import jo.celeris.android.widgets.MenuEtages;
import jo.celeris.android.widgets.NotificationView;
import jo.celeris.android.widgets.PopupController;
import jo.celeris.android.widgets.PopupLayer;
import jo.celeris.android.widgets.PopupManager;
import jo.celeris.android.widgets.PopupPalette;
import jo.celeris.android.widgets.PopupPlacer;
import jo.celeris.android.widgets.SelectionBarView;
import jo.celeris.android.widgets.SignatureAideModel;
import jo.celeris.android.widgets.SignatureHelpView;

import jo.celeris.core.language.model.LanguageSupport;

import jo.celeris.core.syntax.structure.OutlineEntry;

import jo.celeris.core.text.EditListener;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.SelectionListener;
import jo.celeris.core.text.TextSession;

import org.jspecify.annotations.Nullable;

/**
 * L'orchestrateur des popups de l'éditeur : LE point d'entrée de
 * l'hôte — il possède le manager du kit, les vues des genres et les
 * satellites de pose ; les gestes de contenu (gabarit, relance lint)
 * s'y posent par configuration, jamais devinés (la directive du
 * propriétaire du 2026-11-14 : les fonctionnalités vivent dans les
 * modules de l'éditeur).
 *
 */
public final class EditorPopups {

    /** La respiration des ancres aux bords de la couche, en dp. */
    private static final float MARGE_DP = 8f;

    private final EditorView editor;
    private final TextSession session;
    private final PopupManager popups;
    private final PopupLayer couche;
    private final int margePx;
    /** Le coach de l'hôte — les tables du repli local. */
    private CoachPopups coach;
    /** Le poseur de documents ancrés — survol, aperçu, rendu @Preview. */
    private final DocAncre docAnchor;
    /** L'ampoule d'intentions de la gouttière — le tap ouvre le menu
     *  (l'audit Android Studio du 2026-11-14). */
    private final AmpouleIntentions ampouleIntentions;
    /** Les indices en ligne (inlay hints) — le type du « var » se lit
     *  en fin de ligne utile (l'audit du même jour). */
    private final InlayHints inlayHints;

    private final CompletionListView listeCompletion;
    private final CompletionDocView docCompletion;
    private final SignatureHelpView aideSignature;
    /** Le menu d'intentions au curseur — son satellite porte la
     *  pose (l'hôte pose les actions, la carte les sert). */
    private final CarteIntentions carteIntentions;
    /** La fiche de diagnostic du lieu — le satellite la pose
     *  (l'audit Android Studio : les problèmes s'empilent). */
    private final InfoBulle infoBulle;
    /** La barre de sélection et son débordement — le satellite les
     *  pose (les actions y vivent vives, le menu y épargne le
     *  caret, l'audit du propriétaire du 2026-11-14). */
    private final BarreSelection barreSelection;
    /** Le bord droit de la couche — bande d'erreurs, minimap, pouce
     *  (le chrome y vit seul). */
    private final RightEdge rightEdge;
    /** Le sélecteur de couleur — au double-tap d'un littéral. */
    private final ColorPickerLogic colorPicker;
    /** Le fil d'Ariane — le chemin du plan qui englobe le curseur. */
    private final FilAriane filAriane;
    private final Notifications notifications;
    /** L'info de type du symbole au curseur — le moteur du cœur la
     *  sert, la première surface portée chez l'orchestrateur. */
    private final TypeInfo typeInfo;
    /** Les usages du symbole au curseur — la seconde surface portée
     *  : le serveur les sert, l'étrangère vit muette (l'audit Android
     *  Studio compte comme l'IDE : le compte du serveur, tous
     *  fichiers). */
    private final Usages usages;
    /** La documentation externe du symbole au curseur — la troisième
     *  surface portée : la consultation vit le régime qui se
     *  REPOSITIONNE au défilement. */
    private final DocExterne docExterne;
    /** Les hiérarchies de types et d'appels du symbole au curseur —
     *  la quatrième surface portée : l'arbre de l'IDE de référence,
     *  la racine vit à l'en-tête (l'audit Android Studio). */
    private final CarteHierarchies hierarchies;
    /** La carte de génération et de gabarits au caret — la cinquième
     *  surface portée : le geste de gabarit s'y configure (A-22, un
     *  seul canal). */
    private final Gabarits templates;
    /** La sûreté de la suppression et du déplacement — la sixième
     *  surface portée : les usages qui barrent, les cibles du
     *  déménagement. */
    private final Surete surete;
    /** Le masquage d'inspection de la ligne du caret — la septième
     *  surface portée : la marque naît, le lint l'honore à sa
     *  relance (le geste de relance s'y configure). */
    private final Masquage masquage;
    /** Les détails d'exception de l'exécution — la huitième
     *  surface portée : l'échec simulé porte la trace du document
     *  réel (le pont meurt avec elle). */
    private final ExceptionCarte exception;
    /** La carte des réglages — la table centrale s'y bascule. */
    private final SettingsPanel settings;
    /** Le repositionneur des surfaces ouvertes — l'avis de fenêtre. */
    private final Repositionneur repositionneur;
    /** L'écouteur d'édition posé sur la session — mémorisé pour que
     *  {@link #detach()} le retire (chantier C1 : la session vivante
     *  ne retient plus l'orchestrateur après le détachement). */
    private final EditListener ecouteurEdition =
            (start, end, retire, insere) -> surEdition();
    /** L'écouteur de sélection posé sur la session — mémorisé pour le
     *  même retrait. */
    private final SelectionListener ecouteurSelection = this::surSelection;
    // L'abonné de l'avis de fenêtre (ADR-0022, phase 2, T3) : la voie
    // multi-abonnés — le repositionnement du kit ne s'écrase plus
    // avec l'écouteur de l'hôte, le détachement le retire.
    private final Runnable windowSubscriber = this::onWindow;

    // L'abonné de la source unique des réglages (ADR-0022, phase 2,
    // T2) : le thème et la taille posés sur vue.settings() y
    // redérivent la palette — plus de double pose à la main, le
    // détachement le retire.
    private final EditorSettings.ChangeListener settingsSubscriber =
            source -> {
                onTheme(source.theme());
                onTextSize(source.fontSizeSp());
            };
    /** Le détachement a-t-il déjà eu lieu — l'idempotence s'y lit. */
    private boolean detache;
    /** Le premier avis de fenêtre a-t-il déjà posé le chrome du
     *  montage — les suivants ne re-posent que l'ouvert. */
    private boolean montageRafraichi;

    /** Le thème courant — la palette des popups en dérive. */
    private EditorTheme theme;
    /** La taille de texte de l'éditeur, en sp — la typographie des popups s'y accorde. */
    private float textSizeSp;

    private List<HostDiagnostic> diagnostics = List.of();
    /** Le plan du fichier ouvert — la hiérarchie y lit sa forme. */
    private List<OutlineEntry> plan = List.of();
    private List<IntentionsMenuView.Item> intentions = List.of();
    /** Le nom du fichier ouvert — le filtre des diagnostics s'y fait. */
    private String nomDuFichier = "";
    /** La session de langage branchée — null retombe sur le coach. */
    private @Nullable LanguageSession server;
    /** Le langage du fichier ouvert — la syntaxe des hiérarchies s'y
     *  lit, null sans langage. */
    private @Nullable LanguageSupport language;
    /** Le geste du gabarit de méthode (A-22) — l'hôte le pose par
     *  configuration, null sans geste. */
    private @Nullable Runnable methodTemplateGesture;
    /** La relance de l'analyse lint — l'hôte la pose par
     *  configuration, le masquage ne vaut qu'appliqué. */
    private @Nullable Runnable relanceLint;
    /** L'uri du document ouvert auprès du serveur de langage. */
    private String uriDocument = "";
    /** La frappe en cours : la sélection qui suit ne rouvre pas l'info-bulle. */
    private boolean typingInProgress;
    /** L'acceptation d'un candidat vient d'avoir lieu : l'édition ne rouvre pas la liste. */
    private boolean acceptationEnCours;

    /**
     * Construit l'orchestrateur au coach muet (ADR-0022, point 9 de la
     * revue) : l'argument opaque disparaît pour qui n'en veut pas — le
     * coach muet rend le minimum, les mots du document en variables.
     *
     * @param editor      la vue d'éditeur ouverte
     * @param session      la session d'édition courante
     * @param couche       la couche porte-popups posée sur l'éditeur
     * @param theme        le thème d'éditeur courant
     * @param textSizeSp la taille de texte de l'éditeur, en sp
     */
    public EditorPopups(EditorView editor, TextSession session,
            PopupLayer couche, EditorTheme theme, float textSizeSp) {
        this(editor, session, couche, theme, textSizeSp,
                CoachPopups.muet());
    }

    /**
     * Construit l'orchestrateur : le manager naît sur la machine et la
     * couche de la scène, les vues se lient à leurs genres (jamais une
     * carte pour deux), les écouteurs y courtent.
     *
     * @param editor      la vue d'éditeur ouverte
     * @param session      la session d'édition courante
     * @param couche       la couche porte-popups posée sur l'éditeur
     * @param theme        le thème d'éditeur courant
     * @param textSizeSp la taille de texte de l'éditeur, en sp
     * @param coach        le coach de l'hôte — {@link CoachPopups#muet()}
     */
    public EditorPopups(EditorView editor, TextSession session,
            PopupLayer couche, EditorTheme theme, float textSizeSp,
            CoachPopups coach) {
        this.editor = editor;
        this.session = session;
        this.couche = couche;
        this.theme = theme;
        this.textSizeSp = textSizeSp;
        this.coach = coach;
        this.margePx = Math.round(MARGE_DP
                * couche.getResources().getDisplayMetrics().density);
        PopupController controleur = new PopupController();
        popups = new PopupManager(controleur, couche);
        PopupPalette palette = PalettesPopups.of(theme, textSizeSp);
        listeCompletion = new CompletionListView(couche.getContext(), palette);
        docCompletion = new CompletionDocView(couche.getContext(), palette);
        aideSignature = new SignatureHelpView(couche.getContext(), palette);
        carteIntentions = new CarteIntentions(editor, popups, margePx, palette, () -> intentions);
        infoBulle = new InfoBulle(editor, session, popups, margePx,
                palette, this::diagnostics, this::nomVif, () -> intentions);
        barreSelection = new BarreSelection(editor, session, popups,
                palette, margePx, this::openHover);
        // le lien des genres : une carte par genre — le manager l'exige
        popups.lier(PopupController.PopupKind.COMPLETION_LIST, listeCompletion);
        popups.lier(PopupController.PopupKind.COMPLETION_DOC, docCompletion);
        popups.lier(PopupController.PopupKind.SIGNATURE_HELP, aideSignature);
        colorPicker = new ColorPickerLogic(editor, session, popups, margePx, palette);
        filAriane = new FilAriane(popups, session, margePx, palette);
        notifications = new Notifications(popups, margePx, palette);
        typeInfo = new TypeInfo(editor, session, popups, margePx, palette);
        usages = new Usages(editor, session, popups, margePx, palette);
        docExterne = new DocExterne(editor, session, popups, margePx, palette, coach);
        hierarchies = new CarteHierarchies(editor, session, popups,
                margePx, palette, this::planVif, this::nomVif, () -> language);
        templates = new Gabarits(editor, session, popups, margePx,
                palette, this::planVif, () -> language, () -> methodTemplateGesture);
        surete = new Surete(editor, session, popups, margePx, palette,
                this::planVif, () -> language, notifications::notifyListeners);
        masquage = new Masquage(editor, session, popups, margePx, palette,
                this::diagnostics, this::nomVif, () -> relanceLint,
                notifications::notifyListeners);
        settings = new SettingsPanel(editor, popups, margePx, palette);
        docAnchor = new DocAncre(couche.getContext(), popups, editor, session, margePx, palette, coach);
        rightEdge = new RightEdge(popups, editor, session, margePx, palette, this::sauterDiagnostic);
        ampouleIntentions = new AmpouleIntentions(editor, carteIntentions, this::diagnostics);
        inlayHints = new InlayHints(editor, session.document());
        exception = new ExceptionCarte(editor, session, popups, margePx,
                palette, this::planVif, this::nomVif, notifications::notifyListeners);
        repositionneur = mountRepositioner();
        wireListeners();
    }


    /** Monte le repositionneur de l'avis de fenêtre — les surfaces
     *  ouvertes s'y re-posent, la consultation y suit toujours. */
    private Repositionneur mountRepositioner() {
        return new Repositionneur(popups, this::placerCompletion,
                filAriane::refresh, () -> rightEdge.rafraichirBande(
                        diagnostics, nomDuFichier, layerWidth(),
                        layerHeight()), barreSelection::refresh,
                carteIntentions::open, List.of(docExterne::repositionner,
                        templates::repositionner, surete::repositionner,
                        masquage::repositionner, exception::repositionner));
    }

    /** Les écouteurs de la scène : les vues y préviennent, la session
     *  et l'éditeur y répondent — le montage du constructeur s'y
     *  déleste, la lecture y reste d'un tenant. Chaque écouteur posé
     *  se mémorise : le détachement les retire tous (chantier C1). */
    private void wireListeners() {
        listeCompletion.setOnPick(this::choisirCompletion);
        session.addEditListener(ecouteurEdition);
        session.addSelectionListener(ecouteurSelection);
        editor.addViewportChangeListener(windowSubscriber);
        editor.settings().addListener(settingsSubscriber);
    }


    /** Le thème tourne : la palette se redérive — le verre suit.
     *  Privé depuis la 2.0 (ADR-0024) : la source unique de la vue
     *  (T2) y mène seule — l'abonné des réglages est le seul
     *  appelant, l'hôte écrit vue.settings().setTheme(...). */
    private void onTheme(EditorTheme nouveau) {
        theme = nouveau;
        refreshPalette();
    }

    /** La taille de texte tourne : la typographie des popups suit.
     *  Privé depuis la 2.0 (ADR-0024) : la source unique de la vue
     *  (T2) y mène seule. */
    private void onTextSize(float tailleSp) {
        textSizeSp = tailleSp;
        refreshPalette();
    }

    private void refreshPalette() {
        PopupPalette palette = PalettesPopups.of(theme, textSizeSp);
        listeCompletion.setPalette(palette);
        docCompletion.setPalette(palette);
        aideSignature.setPalette(palette);
        carteIntentions.surPalette(palette);
        infoBulle.surPalette(palette);
        rightEdge.surPalette(palette);
        barreSelection.surPalette(palette);
        colorPicker.surPalette(palette);
        filAriane.surPalette(palette);
        notifications.surPalette(palette);
        typeInfo.surPalette(palette);
        usages.surPalette(palette);
        docExterne.surPalette(palette);
        hierarchies.surPalette(palette);
        templates.surPalette(palette);
        surete.surPalette(palette);
        masquage.surPalette(palette);
        exception.surPalette(palette);
        settings.surPalette(palette);
        docAnchor.surPalette(palette);
    }

    /** Le manager du kit — l'activité y branche la toolbar et la
     *  délégation clavier. */
    public PopupManager manager() {
        return popups;
    }

    /** Les intentions du menu — l'hôte les fournit, libellés compris. */
    public void setIntentions(List<IntentionsMenuView.Item> items) {
        intentions = List.copyOf(items);
    }

    /** Branche la session de langage : complétions, survol et aide
     *  passent par le fil du protocole ; la langue se lit du fichier
     *  ouvert, null retombe sur « text ».
     *
     * @param sessionLangage la session de langage démarrée
     * @param uri            l'uri du document ouvert
     * @param language        le support du langage, null pour « text »
     */
    public void connectServer(LanguageSession sessionLangage, String uri,
            @Nullable LanguageSupport langueParam) {
        this.server = sessionLangage;
        this.uriDocument = uri;
        this.language = langueParam;
        docAnchor.connectServer(sessionLangage, uri);
        usages.connectServer(sessionLangage, uri);
        docExterne.connectServer(sessionLangage, uri);
        sessionLangage.open(uri,
                langueParam == null ? "texte" : langueParam.id(),
                session.document().text());
    }

    /**
     * Pose la LANGUE de l'éditeur sans serveur : le coach des popups
     * devient celui de la langue — ses mots réservés servent la
     * complétion de base dès le premier caractère (la documentation et
     * les signatures restent les tables de l'hôte ou le serveur
     * branché). La façade y passe à chaque ouverture : l'hôte n'écrit
     * rien.
     *
     * @param language le support de langage posé, null pour « text »
     */
    public void setLanguage(@Nullable LanguageSupport langueParam) {
        this.language = langueParam;
        this.coach = new CoachLangage(langueParam);
    }

    /** Pose les actions de la barre — principales en pastilles,
     *  étages repliés derrière la puce.
     *
     * @param principales les actions visibles de la barre
     * @param debordement les étages repliés, servis par le menu */
    public void setSelectionActions(
            List<SelectionBarView.Item> principales,
            List<MenuEtages.Etage> debordement) {
        barreSelection.configurerActions(principales, debordement);
    }

    /** Les actions VIVES de la barre — relues à CHAQUE pose : Coller
     * ne se propose que lorsque le presse-papiers est plein, l'étage
     * de navigation ne se montre que sur un symbole au caret (l'audit
     * du 2026-11-14 : la visibilité contextuelle naît de la donnée).
     *
     * @param principales le fournisseur des actions visibles
     * @param debordement le fournisseur des étages repliés */
    public void setLiveSelectionActions(
            Supplier<List<SelectionBarView.Item>> principales,
            Supplier<List<MenuEtages.Etage>> debordement) {
        barreSelection.configurerActionsVives(principales, debordement);
    }

    /** Re-pose la barre ouverte — les actions vives se relisent : le
     *  presse-papiers a changé, Coller se propose ou se retire. */
    public void refreshSelectionBar() {
        if (popups.estOuvert(PopupController.PopupKind.SELECTION_BAR)) {
            barreSelection.refresh();
        }
    }

    /** Ouvre le sélecteur de couleur au double-tap d'un littéral. */
    public boolean openColorPicker(int offset) {
        return colorPicker.ouvrirAuDoubleTap(offset);
    }

    /** Le plan du fichier ouvert — le fil d'Ariane s'y abreuvement,
     *  les hiérarchies y lisent la forme de l'arbre. */
    public void setOutline(List<OutlineEntry> entrees) {
        plan = List.copyOf(entrees);
        filAriane.setOutline(entrees);
        filAriane.refresh();
    }

    /** Les diagnostics de l'hôte — la bande et l'info-bulle s'y abreuvent. */
    public void setDiagnostics(List<HostDiagnostic> nouveaux) {
        diagnostics = List.copyOf(nouveaux);
        rightEdge.rafraichirBande(diagnostics, nomDuFichier,
                layerWidth(), layerHeight());
        ampouleIntentions.refresh();
    }

    /** Les diagnostics publiés — copie vive, l'hôte y superpose sa vue. */
    public List<HostDiagnostic> diagnostics() {
        return diagnostics;
    }

    /** La barre de recherche s'ouvre — l'état suit, le champ reste à l'hôte. */
    public void openSearch() {
        popups.controleur().open(PopupController.PopupKind.SEARCH_BAR);
    }

    /** La barre de recherche se ferme — sur demande seule. */
    public void closeSearch() {
        popups.controleur().close(PopupController.PopupKind.SEARCH_BAR);
    }

    /** La touche matérielle DÉLÉGUÉE par l'éditeur (PIEGE-27) : la
     *  carte interactive ouverte reçoit flèches, validation et
     *  échappement — la déclinée retourne à l'éditeur.
     *
     * @param keyCode le code de la touche déléguée
     * @return vrai quand la touche a servi un popup
     */
    public boolean onKey(int keyCode) {
        return popups.onKey(keyCode);
    }

    /** La feuille modale qui s'ouvre coupe l'intelligence de la couche :
     *  le manager ferme d'un seul tenant, le chrome demeure. */
    public void closeSubsheet() {
        popups.closeSubsheet();
    }

    /** Ouvre ou filtre la complétion : le serveur qui la SERT passe
     *  le premier (sa réponse devient les candidats typés), sinon les
     *  mots du code et le coach — sans candidat, la liste se ferme. */
    public void toggleCompletion() {
        if (ContenuPopups.serverFirst(server)) {
            server.completion(uriDocument, session.document(), caret())
                    .whenComplete((reponse, echec) -> editor.post(
                            () -> placeCompletion(
                                    ContenuPopups.candidats(reponse),
                                    ContenuPopups.docDe(reponse, 0))));
            return;
        }
        placeLocalFold();
    }

    /** Le repli local de la complétion : les mots du code et les
     *  tables du coach — sans candidat, la liste se referme. */
    private void placeLocalFold() {
        List<String> bruts = editor.intelligence()
                .completionCandidates(coach.motsReserves());
        if (bruts.isEmpty()) {
            popups.close(PopupController.PopupKind.COMPLETION_LIST);
            return;
        }
        placeCompletion(coach.candidats(bruts),
                coach.docDe(bruts.get(0)));
    }

    /** Pose la complétion mesurée : les candidats typés gagnent la liste,
     * le préfixe tapé surligne ses runs, le premier candidat documenté
     * ouvre sa documentation au flanc. */
    private void placeCompletion(List<CompletionListView.Candidat> candidats,
            CompletionDocModel doc) {
        if (candidats.isEmpty()) {
            popups.close(PopupController.PopupKind.COMPLETION_LIST);
            return;
        }
        popups.controleur().open(PopupController.PopupKind.COMPLETION_LIST);
        listeCompletion.setItems(candidats);
        listeCompletion.setPrefix(ContenuPopups.prefixeCourant(
                session.document(), caret()));
        placerCompletion();
        if (doc != null) {
            popups.controleur().open(PopupController.PopupKind.COMPLETION_DOC);
            docCompletion.setModel(doc);
            placerCompletion();
        } else {
            popups.close(PopupController.PopupKind.COMPLETION_DOC);
        }
    }

    /** Ouvre le menu d'intentions au curseur — les actions de l'hôte
     *  ({@link #setIntentions}), sans section. */
    public void openIntentions() {
        carteIntentions.open();
    }

    /** Ouvre le survol du symbole au curseur — le « i » de la barre
     *  de sélection y vient (la résolution vit dans {@code DocAncre}). */
    public void openHover() {
        docAnchor.survolAuCaret();
    }

    /** Le survol au DOUBLE-TAP — le geste seul et le « i » l'ouvrent. */
    public boolean openHoverOnDoubleTap(int offset) {
        return docAnchor.survolAuDoubleTap(offset);
    }

    /** Le poseur de documents ancrés — l'hôte y lit l'aperçu et le
     *  rendu @Preview. */
    public DocAncre docAnchor() {
        return docAnchor;
    }

    /** Le satellite des indices en ligne — la bascule des réglages
     *  y relit la pose (porte fermée : retirés, ouverte : reposés). */
    public InlayHints inlayHints() {
        return inlayHints;
    }

    /**
     * Ouvre l'aide aux paramètres de l'appel ouvert sous le curseur —
     * le serveur de langage par le fil du protocole, le coach en
     * repli local sur réponse vide.
     */
    public void refreshSignature() {
        int caret = caret();
        if (server != null) {
            server.signature(uriDocument, session.document(), caret)
                    .whenComplete((model, echec) -> editor.post(
                            () -> showSignature(model != null ? model
                                    : coach.signatureDe(
                                            session.document(), caret))));
            return;
        }
        showSignature(coach.signatureDe(
                session.document(), caret()));
    }

    /** Pose l'aide aux paramètres — null ferme, le modèle sinon se place. */
    private void showSignature(SignatureAideModel model) {
        if (model == null) {
            popups.close(PopupController.PopupKind.SIGNATURE_HELP);
            return;
        }
        popups.controleur().open(PopupController.PopupKind.SIGNATURE_HELP);
        aideSignature.setModel(model);
        placerCompletion();
    }

    private void surEdition() {
        typingInProgress = true;
        popups.onType();
        if (server != null) {
            server.synchroniser(uriDocument, session.document().text());
        }
        boolean acceptation = acceptationEnCours;
        acceptationEnCours = false;
        // la décision vit en pur chez ContenuPopups : la PORTE des
        // réglages peut rendre la complétion silencieuse, l'acceptation
        // ne repropose rien, la frappe propose comme tout éditeur
        if (ContenuPopups.doitProposerApresEdition(
                editor.settingsPanel().ouverte(
                        EditorSettingsPanel.Porte.COMPLETION_AUTO),
                acceptation, session.document(), caret())) {
            toggleCompletion();
        } else {
            // le blanc et la ponctuation referment la liste — fermer
            // une liste close ne fait rien
            popups.close(PopupController.PopupKind.COMPLETION_LIST);
        }
        refreshSignature();
        filAriane.refresh();
        rightEdge.refresh(layerWidth(), layerHeight());
        inlayHints.refresh(session.document());
    }

    private void surSelection(MultiSelection selection) {
        boolean apresFrappe = typingInProgress;
        typingInProgress = false;
        // l'ampoule et la barre suivent le caret — leurs satellites
        // relisent leurs PORTES des réglages (la table centrale décide)
        ampouleIntentions.surSelection(session, selection);
        barreSelection.place(selection);
        filAriane.refresh();
        if (popups.estOuvert(PopupController.PopupKind.COMPLETION_LIST)) {
            toggleCompletion();
        }
        if (!apresFrappe) {
            infoBulle.refresh(selection);
        }
        if (popups.estOuvert(PopupController.PopupKind.SIGNATURE_HELP)) {
            refreshSignature();
        }
    }

    private void onWindow() {
        popups.onScroll();
        // Le premier avis porte le MONTAGE : la minimap, le pouce et la
        // bande vivent dès la première image — les avis suivants ne
        // re-posent que l'ouvert (repositionnerTout), le rafraîchissement
        // du montage reste idempotent et bon marché.
        if (!montageRafraichi) {
            montageRafraichi = true;
            refreshMount();
        }
        repositionnerTout();
    }

    private void placerCompletion() {
        Rect cursor = editor.caretBounds();
        // le CAP suit la PLACE DISPONIBLE (l'audit du 2026-11-14 : la
        // complétion ne marche jamais sur la frappe) — le calcul vit
        // chez CapCompletion, pur et testé
        listeCompletion.setCap(CapCompletion.of(cursor.top,
                cursor.bottom, layerHeight(), margePx,
                Math.round(CapCompletion.rowHeight(
                        couche.getResources().getDisplayMetrics().density))));
        PopupPlacer.Placement placee = PopupPlacer.placer(
                new PopupPlacer.Cadre(cursor.left, cursor.top, 0,
                        cursor.height()),
                popups.size(PopupController.PopupKind.COMPLETION_LIST),
                popups.size(PopupController.PopupKind.COMPLETION_DOC),
                popups.size(PopupController.PopupKind.SIGNATURE_HELP),
                layerWidth(), layerHeight());
        popups.show(PopupController.PopupKind.COMPLETION_LIST,
                placee.completion().x(), placee.completion().y());
        if (popups.estOuvert(PopupController.PopupKind.COMPLETION_DOC)) {
            popups.show(PopupController.PopupKind.COMPLETION_DOC,
                    placee.documentation().x(), placee.documentation().y());
        }
        if (popups.estOuvert(PopupController.PopupKind.SIGNATURE_HELP)) {
            popups.show(PopupController.PopupKind.SIGNATURE_HELP,
                    placee.signature().x(), placee.signature().y());
        }
    }

    private void choisirCompletion(int index) {
        String candidat = listeCompletion.itemAt(index);
        if (candidat == null) {
            return;
        }
        int caret = caret();
        int start = ContenuPopups.debutMot(session.document(), caret);
        // l'édition de l'acceptation ne rouvre pas la liste
        acceptationEnCours = true;
        if (!session.replaceRange(start, caret, candidat)) {
            acceptationEnCours = false;
            return;
        }
        // la liste se ferme AVANT la sélection (sa documentation suit —
        // la cascade vit au manager, jamais codée deux fois)
        popups.close(PopupController.PopupKind.COMPLETION_LIST);
        session.setSelection(MultiSelection.of(
                SelRange.caretAt(start + candidat.length())));
    }

    private void sauterDiagnostic(int offset) {
        session.setSelection(MultiSelection.of(SelRange.caretAt(offset)));
    }

    private void repositionnerTout() {
        repositionneur.tout();
    }

    /** Publie une notification au coin bas-droit — l'horloge la referme
     *  seule (4 000 ms) ; ce qui ne touche pas le texte s'y montre. */
    public void notifyListeners(String titre, String message) {
        notifications.notifyListeners(titre, message);
    }

    /** Publie une notification au coin bas-droit avec ses LIENS (l'audit
     *  Android Studio : les actions de la bulle de l'IDE de référence) —
     *  la prise d'un lien referme la bulle puis la fait courir. */
    public void notifyListeners(String titre, String message,
            List<NotificationView.Action> actions) {
        notifications.notifyListeners(titre, message, actions);
    }

    /** Ouvre la carte d'info de type du symbole au curseur — le type
     *  lu à la déclaration par le moteur du cœur, la carte sous le
     *  curseur, bornée. */
    public void openTypeInfo() {
        typeInfo.open();
    }

    /** Ouvre la carte des usages du symbole au curseur — le serveur de
     *  langage sert les lieux, l'occurrence ouverte saute, l'étrangère
     *  vit muette (l'audit Android Studio : le compte du serveur, tous
     *  fichiers ; sans serveur ni symbole, la carte ne se pose pas). */
    public void openUsages() {
        usages.open();
    }

    /** Ouvre la documentation externe du symbole au curseur — le
     *  serveur de langage par le fil du protocole, la table du coach
     *  en repli ; la consultation survit au défilement (elle se
     *  repositionne au curseur). */
    public void openExternalDoc() {
        docExterne.open();
    }

    /** Ouvre la hiérarchie du symbole au curseur — de TYPES quand il
     *  nomme un type, d'APPELS quand il nomme une méthode (le plan
     *  décide) ; l'arbre de l'IDE de référence : la racine vit à
     *  l'en-tête, l'introuvable à la frontière. */
    public void openHierarchy() {
        hierarchies.open();
    }

    /** Ouvre la carte de génération et de gabarits au caret —
     *  constructeur, accesseurs, equals/hashCode/toString, entourage
     *  de la sélection, gabarit de méthode (le geste posé par
     *  {@link #setMethodTemplateGesture}). */
    public void openTemplates() {
        templates.open();
    }

    /** Pose le geste du gabarit de méthode (A-22) — le canal unique de
     *  la refactorisation : la carte de gabarits n'est qu'une peau de
     *  plus, elle ne duplique jamais la logique.
     *
     * @param geste l'insertion du gabarit de méthode, null pour rien
     */
    public void setMethodTemplateGesture(@Nullable Runnable geste) {
        methodTemplateGesture = geste;
    }

    /** La suppression sûre de la désignation au curseur — le verdict
     *  du moteur décide : les usages qui barrent s'affichent en
     *  carte (chacun saute), la voie libre efface en transaction
     *  annulable. */
    public void deleteIn() {
        surete.deleteIn();
    }

    /** Le déplacement sûr de la méthode au curseur — les types
     *  candidats du fichier se proposent, les conditions de sûreté
     *  seules décident. */
    public void moveInto() {
        surete.moveInto();
    }

    /** Le masquage d'inspection de la ligne du caret — les
     *  diagnostics réglés se proposent, le choix pose le commentaire
     *  « noinspection », l'analyse se relance (le geste posé par
     *  {@link #setLintRerun}). */
    public void hideInspection() {
        masquage.open();
    }

    /** Pose la relance de l'analyse lint — le masquage ne vaut
     *  qu'appliqué : la règle masquée ne se rapporte plus à la
     *  relance.
     *
     * @param relance la relance de l'analyse, null pour rien
     */
    public void setLintRerun(@Nullable Runnable relance) {
        relanceLint = relance;
    }

    /** Exécute le fichier ouvert — l'échec simulé porte la trace du
     *  document réel, la carte des détails se pose (régime de la
     *  consultation : frappe épargnée, défilement repositionné) ; le
     *  fichier sans méthode ne peut échouer — l'exécution s'annonce
     *  terminée. */
    public void runFile() {
        exception.executer();
    }

    /** Ouvre (ou referme) la carte des réglages — la table centrale
     *  y vit (l'audit du 2026-11-14) : chaque interrupteur bascule
     *  SA porte, la vue et l'orchestrateur suivent.
     *
     * @param libelles les libellés des portes, dans l'ordre de la table */
    public void openSettings(List<String> libelles) {
        if (popups.estOuvert(PopupController.PopupKind.SETTINGS_MENU)) {
            settings.close();
            return;
        }
        settings.open(libelles);
    }

    /** Re-pose la carte ouverte — la bascule changée s'y voit.
     *
     * @param libelles les libellés des portes, dans l'ordre de la table */
    public void refreshSettings(List<String> libelles) {
        settings.refresh(libelles);
    }

    /** Le détachement de fin de vie : tout se ferme, l'avis du tap hors
     *  cartes se coupe, les liens se dissolvent — la session ne retient
     *  plus l'orchestrateur, la scène peut recycler. Idempotent : un
     *  second appel ne fait rien (chantier C1). */
    public void detach() {
        if (detache) {
            return;
        }
        detache = true;
        popups.detach();
        editor.removeViewportChangeListener(windowSubscriber);
        editor.settings().removeListener(settingsSubscriber);
        session.removeEditListener(ecouteurEdition);
        session.removeSelectionListener(ecouteurSelection);
    }

    /** La session servie par l'orchestrateur — la façade y
     *  reconnaît l'orchestrateur de la session courante (le
     *  retour terrain du 2026-11-19 : l'orchestrateur survit au
     *  retour d'un document connu, il ne se recrée QUE pour un
     *  document différent). */
    public TextSession session() {
        return session;
    }

    /** Pose le nom du fichier ouvert — le filtre des diagnostics s'y fait. */
    public void setFileName(String nom) {
        nomDuFichier = nom;
    }

    /**
     * Les mots réservés de la langue ouverte — la sonde du banc de la
     * façade (le coach langue y passe) : la complétion de base vit
     * dès le montage sans table de l'hôte.
     *
     * @return les mots réservés servis à la complétion
     */
    public java.util.List<String> reservedWordsCompletion() {
        return coach.motsReserves();
    }

    /**
     * Les actions courantes de la barre de sélection — la sonde du
     * banc du DÉFAUT du montage : l'hôte qui n'en pose aucune y lit
     * les actions d'édition standard (couper, copier, coller au
     * presse-papiers près, tout copier).
     *
     * @return les actions servies par la barre, jamais null
     */
    public java.util.List<SelectionBarView.Item> barActions() {
        return barreSelection.barActions();
    }

    /**
     * Rafraîchit le CHROME du montage : la minimap, le pouce de
     * défilement rapide, la bande d'erreurs et le fil d'Ariane se
     * posent SANS attendre la première frappe — l'éditeur vivant dès
     * la pose, comme tout éditeur de code. La façade l'appelle à
     * l'ouverture (le temps que la couche soit mesurée) ; l'appel
     * prématuré ne casse rien (les surfaces se re-posent au prochain
     * avis de fenêtre).
     */
    public void refreshMount() {
        rightEdge.refresh(layerWidth(), layerHeight());
        rightEdge.rafraichirBande(diagnostics, nomDuFichier,
                layerWidth(), layerHeight());
        filAriane.refresh();
    }

    /** Le plan VIF du fichier ouvert — les hiérarchies y lisent la
     *  forme de l'arbre au déclenchement. */
    private List<OutlineEntry> planVif() {
        return plan;
    }

    /** Le nom VIF du fichier ouvert — la clé du projet embarqué. */
    private String nomVif() {
        return nomDuFichier;
    }

    private int layerWidth() {
        return couche.getWidth();
    }

    private int layerHeight() {
        return couche.getHeight();
    }

    /** Le caret courant — la première plage de sélection, zéro si vide. */
    private int caret() {
        List<SelRange> ranges = session.selection().ranges();
        return ranges.isEmpty() ? 0 : ranges.get(0).caret();
    }
}
