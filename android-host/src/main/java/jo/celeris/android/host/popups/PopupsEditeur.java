package jo.celeris.android.host.popups;

import android.graphics.Rect;

import java.util.List;

import jo.celeris.android.host.R;
import jo.celeris.android.host.spi.DiagnosticHote;
import jo.celeris.android.view.EditorTheme;
import jo.celeris.android.view.EditorView;
import jo.celeris.android.widgets.CompletionDocModel;
import jo.celeris.android.widgets.CompletionDocView;
import jo.celeris.android.widgets.CompletionListView;
import jo.celeris.android.widgets.DiagnosticTooltipView;
import jo.celeris.android.widgets.IntentionsMenuView;
import jo.celeris.android.widgets.MenuDebordement;
import jo.celeris.android.widgets.NotificationView;
import jo.celeris.android.widgets.PopupAnchor;
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

import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextSession;

import org.jspecify.annotations.Nullable;

/**
 * L'orchestrateur des popups de l'éditeur (le déménagement du
 * 2026-11-14, décision du propriétaire : les fonctionnalités vivent
 * dans les modules de l'éditeur, l'application hôte n'a presque rien
 * à faire) : les genres vivant dans la couche posée sur l'éditeur, le
 * MANAGER du kit tient la boucle état→vues d'un seul tenant —
 * l'orchestrateur déclenche, la machine décide, la couche porte.
 * PIEGE-27 : aucun popup ne prend le focus, les touches matérielles
 * lui sont DÉLÉGUÉES ({@link #surTouche}) ; la barre de recherche
 * reste l'affaire de l'activité — son champ doit tenir le focus, la
 * couche l'interdit — l'orchestrateur n'en coordonne que l'ÉTAT.
 *
 * <p>Le montage tient en trois temps : construire sur (vue, session,
 * couche, thème, taille de texte) — le coach MUET suffit, branche le
 * contenu par {@link #brancherServeur} et
 * {@link #configurerIntentions} / {@link #configurerActionsSelection},
 * puis délègue : le survol au double-tap, la frappe, la sélection —
 * tout vit déjà dans la vue. Les TABLES de l'hôte (mots réservés,
 * documentation, signatures) passent par son {@link CoachPopups}.</p>
 */
public final class PopupsEditeur {

    /** La respiration des ancres aux bords de la couche, en dp. */
    private static final float MARGE_DP = 8f;

    private final EditorView editeur;
    private final TextSession session;
    private final PopupManager popups;
    private final PopupLayer couche;
    private final int margePx;
    /** Le coach de l'hôte — les tables du repli local. */
    private final CoachPopups coach;
    /** Le poseur de documents ancrés — survol, aperçu, rendu @Preview. */
    private final DocAncre docAncre;

    private final CompletionListView listeCompletion;
    private final CompletionDocView docCompletion;
    private final SignatureHelpView aideSignature;
    private final IntentionsMenuView menuIntentions;
    private final DiagnosticTooltipView infoBulle;
    private final SelectionBarView barreSelection;
    /** Le bord droit de la couche — bande d'erreurs, minimap, pouce
     *  (le chrome y vit seul). */
    private final BordDroit bordDroit;
    /** Le menu de débordement de la barre — sa carte propre sous la
     *  puce. */
    private final MenuDebordement menuDebordement;
    /** Le sélecteur de couleur — au double-tap d'un littéral. */
    private final SelecteurCouleur selecteurCouleur;
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

    /** Le thème courant — la palette des popups en dérive. */
    private EditorTheme theme;
    /** La taille de texte de l'éditeur, en sp — la typographie des popups s'y accorde. */
    private float tailleTexteSp;

    private List<DiagnosticHote> diagnostics = List.of();
    /** Le plan du fichier ouvert — la hiérarchie y lit sa forme. */
    private List<OutlineEntry> plan = List.of();
    private List<IntentionsMenuView.Item> intentions = List.of();
    private List<SelectionBarView.Item> actionsSelection = List.of();
    /** Les actions repliées derrière la puce de débordement. */
    private List<SelectionBarView.Item> debordementSelection = List.of();
    /** Le nom du fichier ouvert — le filtre des diagnostics s'y fait. */
    private String nomDuFichier = "";
    /** La session de langage branchée — null retombe sur le coach. */
    private @Nullable LanguageSession serveur;
    /** Le langage du fichier ouvert — la syntaxe des hiérarchies s'y
     *  lit, null sans langage. */
    private @Nullable LanguageSupport langue;
    /** L'uri du document ouvert auprès du serveur de langage. */
    private String uriDocument = "";
    /** La frappe en cours : la sélection qui suit ne rouvre pas l'info-bulle. */
    private boolean frappeEnCours;
    /** L'acceptation d'un candidat vient d'avoir lieu : l'édition ne rouvre pas la liste. */
    private boolean acceptationEnCours;

    /**
     * Construit l'orchestrateur : le manager naît sur la machine et la
     * couche de la scène, les vues se lient à leurs genres (jamais une
     * carte pour deux), les écouteurs y courtent.
     *
     * @param editeur      la vue d'éditeur ouverte
     * @param session      la session d'édition courante
     * @param couche       la couche porte-popups posée sur l'éditeur
     * @param theme        le thème d'éditeur courant
     * @param tailleTexteSp la taille de texte de l'éditeur, en sp
     * @param coach        le coach de l'hôte — {@link CoachPopups#muet()}
     */
    public PopupsEditeur(EditorView editeur, TextSession session,
            PopupLayer couche, EditorTheme theme, float tailleTexteSp,
            CoachPopups coach) {
        this.editeur = editeur;
        this.session = session;
        this.couche = couche;
        this.theme = theme;
        this.tailleTexteSp = tailleTexteSp;
        this.coach = coach;
        this.margePx = Math.round(MARGE_DP
                * couche.getResources().getDisplayMetrics().density);
        PopupController controleur = new PopupController();
        popups = new PopupManager(controleur, couche);
        PopupPalette palette = PalettesPopups.de(theme, tailleTexteSp);
        listeCompletion = new CompletionListView(couche.getContext(), palette);
        docCompletion = new CompletionDocView(couche.getContext(), palette);
        aideSignature = new SignatureHelpView(couche.getContext(), palette);
        menuIntentions = new IntentionsMenuView(couche.getContext(), palette);
        infoBulle = new DiagnosticTooltipView(couche.getContext(), palette);
        barreSelection = new SelectionBarView(couche.getContext(), palette);
        // le lien des genres : une carte par genre, un genre par carte
        // — le manager l'exige, le zombie d'état ne revient plus
        popups.lier(PopupController.PopupKind.COMPLETION_LIST, listeCompletion);
        popups.lier(PopupController.PopupKind.COMPLETION_DOC, docCompletion);
        popups.lier(PopupController.PopupKind.SIGNATURE_HELP, aideSignature);
        popups.lier(PopupController.PopupKind.INTENTIONS_MENU, menuIntentions);
        popups.lier(PopupController.PopupKind.DIAGNOSTIC_TOOLTIP, infoBulle);
        popups.lier(PopupController.PopupKind.SELECTION_BAR, barreSelection);
        // la section du débordement : le « MORE » du dépôt de départ,
        // l'hôte la surcharge à sa langue
        menuDebordement = new MenuDebordement(popups, palette, margePx,
                couche.getContext().getString(R.string.menu_plus));
        selecteurCouleur = new SelecteurCouleur(editeur, session, popups,
                margePx, palette);
        filAriane = new FilAriane(popups, session, margePx, palette);
        notifications = new Notifications(popups, margePx, palette);
        typeInfo = new TypeInfo(editeur, session, popups, margePx,
                palette);
        usages = new Usages(editeur, session, popups, margePx, palette);
        docExterne = new DocExterne(editeur, session, popups, margePx,
                palette, coach);
        hierarchies = new CarteHierarchies(editeur, session, popups,
                margePx, palette, this::planVif, this::nomVif,
                () -> langue);
        docAncre = new DocAncre(couche.getContext(), popups, editeur,
                session, margePx, palette, coach);
        bordDroit = new BordDroit(popups, editeur, session, margePx,
                palette, this::sauterDiagnostic);
        brancherEcouteurs();
    }

    /** Les écouteurs de la scène : les vues y préviennent, la session
     *  et l'éditeur y répondent — le montage du constructeur s'y
     *  déleste, la lecture y reste d'un tenant. */
    private void brancherEcouteurs() {
        listeCompletion.setOnPick(this::choisirCompletion);
        barreSelection.setOnOverflow(
                () -> menuDebordement.ouvrir(barreSelection,
                        debordementSelection));
        barreSelection.setOnDocs(this::ouvrirSurvol);
        session.addEditListener((debut, fin, retire, insere) -> surEdition());
        session.addSelectionListener(this::surSelection);
        editeur.setSurViewportChange(this::surFenetre);
    }


    /** Le thème tourne : la palette se redérive — le verre suit. */
    public void surTheme(EditorTheme nouveau) {
        theme = nouveau;
        rafraichirPalette();
    }

    /** La taille de texte tourne : la typographie des popups suit. */
    public void surTailleTexte(float tailleSp) {
        tailleTexteSp = tailleSp;
        rafraichirPalette();
    }

    private void rafraichirPalette() {
        PopupPalette palette = PalettesPopups.de(theme, tailleTexteSp);
        listeCompletion.setPalette(palette);
        docCompletion.setPalette(palette);
        aideSignature.setPalette(palette);
        menuIntentions.setPalette(palette);
        infoBulle.setPalette(palette);
        bordDroit.surPalette(palette);
        barreSelection.setPalette(palette);
        selecteurCouleur.surPalette(palette);
        filAriane.surPalette(palette);
        notifications.surPalette(palette);
        typeInfo.surPalette(palette);
        usages.surPalette(palette);
        docExterne.surPalette(palette);
        hierarchies.surPalette(palette);
        menuDebordement.surPalette(palette);
        docAncre.surPalette(palette);
    }

    /** Le manager du kit — l'activité y branche la toolbar et la
     *  délégation clavier. */
    public PopupManager manager() {
        return popups;
    }

    /** Les intentions du menu — l'hôte les fournit, libellés compris. */
    public void configurerIntentions(List<IntentionsMenuView.Item> items) {
        intentions = List.copyOf(items);
    }

    /**
     * Branche la session de langage : l'aide aux paramètres, le survol
     * et — quand le serveur les sert — les complétions passent par le
     * fil du protocole ; la langue se lit du fichier ouvert — null
     * retombe sur « text ».
     *
     * @param sessionLangage la session de langage démarrée
     * @param uri            l'uri du document ouvert
     * @param langue        le support du langage du document, null pour « text »
     */
    public void brancherServeur(LanguageSession sessionLangage, String uri,
            @Nullable LanguageSupport langueParam) {
        this.serveur = sessionLangage;
        this.uriDocument = uri;
        this.langue = langueParam;
        docAncre.brancherServeur(sessionLangage, uri);
        usages.brancherServeur(sessionLangage, uri);
        docExterne.brancherServeur(sessionLangage, uri);
        sessionLangage.ouvrir(uri,
                langueParam == null ? "text" : langueParam.id(),
                session.document().text());
    }

    /** Les actions de la barre de sélection — l'hôte les fournit :
     * les principales en pastilles, le débordement derrière la puce.
     *
     * @param principales les actions visibles de la barre
     * @param debordement les actions repliées, servies par le menu */
    public void configurerActionsSelection(
            List<SelectionBarView.Item> principales,
            List<SelectionBarView.Item> debordement) {
        actionsSelection = List.copyOf(principales);
        debordementSelection = List.copyOf(debordement);
    }

    /** Ouvre le sélecteur de couleur au double-tap d'un littéral. */
    public boolean ouvrirColorPicker(int offset) {
        return selecteurCouleur.ouvrirAuDoubleTap(offset);
    }

    /** Le plan du fichier ouvert — le fil d'Ariane s'y abreuvement,
     *  les hiérarchies y lisent la forme de l'arbre. */
    public void setPlan(List<OutlineEntry> entrees) {
        plan = List.copyOf(entrees);
        filAriane.setPlan(entrees);
        filAriane.rafraichir();
    }

    /** Les diagnostics de l'hôte — la bande et l'info-bulle s'y abreuvent. */
    public void setDiagnostics(List<DiagnosticHote> nouveaux) {
        diagnostics = List.copyOf(nouveaux);
        bordDroit.rafraichirBande(diagnostics, nomDuFichier,
                largeurCouche(), hauteurCouche());
    }

    /** Les diagnostics publiés — copie vive, l'hôte y superpose sa vue. */
    public List<DiagnosticHote> diagnostics() {
        return diagnostics;
    }

    /** La barre de recherche s'ouvre — l'état suit, le champ reste à l'hôte. */
    public void ouvrirRecherche() {
        popups.controleur().open(PopupController.PopupKind.SEARCH_BAR);
    }

    /** La barre de recherche se ferme — sur demande seule. */
    public void fermerRecherche() {
        popups.controleur().close(PopupController.PopupKind.SEARCH_BAR);
    }

    /**
     * La touche matérielle DÉLÉGUÉE par l'éditeur (PIEGE-27 : il garde
     * le focus) : la carte interactive ouverte reçoit flèches, validation
     * et échappement — le manager route, la touche déclinée retourne à
     * l'éditeur.
     *
     * @param keyCode le code de la touche déléguée
     * @return vrai quand la touche a servi un popup
     */
    public boolean surTouche(int keyCode) {
        return popups.surTouche(keyCode);
    }

    /** La feuille modale qui s'ouvre coupe l'intelligence de la couche :
     *  le manager ferme d'un seul tenant, le chrome demeure. */
    public void fermerSousVoile() {
        popups.fermerSousVoile();
    }

    /**
     * Ouvre ou filtre la complétion au curseur : le serveur de langage
     * qui SERT la complétion passe le premier — sa réponse brute
     * devient les candidats typés du kit et leur documentation ; sinon
     * les mots du code et les tables du coach — sans candidat, la
     * liste se ferme.
     */
    public void basculerCompletion() {
        if (serveur != null && serveur.sertCompletion()) {
            serveur.completion(uriDocument, session.document(), caret())
                    .whenComplete((reponse, echec) -> editeur.post(
                            () -> poserCompletion(
                                    ContenuPopups.candidats(reponse),
                                    ContenuPopups.docDe(reponse, 0))));
            return;
        }
        List<String> bruts = editeur.intelligence()
                .completionCandidates(coach.motsReserves());
        if (bruts.isEmpty()) {
            popups.fermer(PopupController.PopupKind.COMPLETION_LIST);
            return;
        }
        poserCompletion(coach.candidats(bruts),
                coach.docDe(bruts.get(0)));
    }

    /** Pose la complétion mesurée : les candidats typés gagnent la liste,
     * le préfixe tapé surligne ses runs, le premier candidat documenté
     * ouvre sa documentation au flanc. */
    private void poserCompletion(List<CompletionListView.Candidat> candidats,
            CompletionDocModel doc) {
        if (candidats.isEmpty()) {
            popups.fermer(PopupController.PopupKind.COMPLETION_LIST);
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
            popups.fermer(PopupController.PopupKind.COMPLETION_DOC);
        }
    }

    /** Ouvre le menu d'intentions au curseur — les actions de l'hôte. */
    public void ouvrirIntentions() {
        if (intentions.isEmpty()) {
            return;
        }
        // le menu d'intentions du caret ne porte AUCUNE section — la
        // carte la laisse au débordement des barres
        menuIntentions.setSection(null);
        menuIntentions.setActions(intentions);
        Rect curseur = editeur.caretBounds();
        PopupPlacer.Cadre placee = PopupAnchor.belowOrAboveCaret(
                curseur.left, curseur.bottom,
                popups.taille(PopupController.PopupKind.INTENTIONS_MENU),
                largeurCouche(), hauteurCouche(), margePx);
        popups.montrer(PopupController.PopupKind.INTENTIONS_MENU,
                placee.x(), placee.y());
    }

    /** Ouvre le survol du symbole au curseur — le « i » de la barre
     *  de sélection y vient (la résolution vit dans {@code DocAncre}). */
    public void ouvrirSurvol() {
        docAncre.survolAuCaret();
    }

    /** Le survol au DOUBLE-TAP — le geste seul et le « i » l'ouvrent. */
    public boolean ouvrirSurvolAuDoubleTap(int offset) {
        return docAncre.survolAuDoubleTap(offset);
    }

    /** Le poseur de documents ancrés — l'hôte y lit l'aperçu et le
     *  rendu @Preview. */
    public DocAncre docAncre() {
        return docAncre;
    }

    /**
     * Ouvre l'aide aux paramètres de l'appel ouvert sous le curseur —
     * le serveur de langage par le fil du protocole, le coach en
     * repli local sur réponse vide.
     */
    public void rafraichirSignature() {
        int caret = caret();
        if (serveur != null) {
            serveur.signature(uriDocument, session.document(), caret)
                    .whenComplete((modele, echec) -> editeur.post(
                            () -> montrerSignature(modele != null ? modele
                                    : coach.signatureDe(
                                            session.document(), caret))));
            return;
        }
        montrerSignature(coach.signatureDe(
                session.document(), caret()));
    }

    /** Pose l'aide aux paramètres — null ferme, le modèle sinon se place. */
    private void montrerSignature(SignatureAideModel modele) {
        if (modele == null) {
            popups.fermer(PopupController.PopupKind.SIGNATURE_HELP);
            return;
        }
        popups.controleur().open(PopupController.PopupKind.SIGNATURE_HELP);
        aideSignature.setModel(modele);
        placerCompletion();
    }

    private void surEdition() {
        frappeEnCours = true;
        popups.surFrappe();
        if (serveur != null) {
            serveur.synchroniser(uriDocument, session.document().text());
        }
        if (acceptationEnCours) {
            // l'édition de l'acceptation ne rouvre pas la liste : le
            // candidat retenu ne se repropose pas
            acceptationEnCours = false;
        } else if (ContenuPopups.doitOuvrirALaFrappe(session.document(),
                caret())) {
            // la complétion se propose à la FRAPPE, comme tout éditeur ;
            // déjà ouverte, la même voie filtre sans refermer
            basculerCompletion();
        } else {
            // le blanc et la ponctuation referment la liste — fermer
            // une liste close ne fait rien
            popups.fermer(PopupController.PopupKind.COMPLETION_LIST);
        }
        rafraichirSignature();
        filAriane.rafraichir();
        bordDroit.rafraichir(largeurCouche(), hauteurCouche());
    }

    private void surSelection(MultiSelection selection) {
        boolean apresFrappe = frappeEnCours;
        frappeEnCours = false;
        rafraichirBarreSelection(selection);
        filAriane.rafraichir();
        if (popups.estOuvert(PopupController.PopupKind.COMPLETION_LIST)) {
            basculerCompletion();
        }
        if (!apresFrappe) {
            rafraichirInfoBulle(selection);
        }
        if (popups.estOuvert(PopupController.PopupKind.SIGNATURE_HELP)) {
            rafraichirSignature();
        }
    }

    private void surFenetre() {
        popups.surDefilement();
        repositionnerTout();
    }

    private void rafraichirBarreSelection(MultiSelection selection) {
        List<SelRange> plages = selection.ranges();
        boolean visible = plages.size() == 1 && !plages.get(0).isEmpty();
        if (!visible) {
            popups.fermer(PopupController.PopupKind.SELECTION_BAR);
            return;
        }
        popups.controleur().open(PopupController.PopupKind.SELECTION_BAR);
        barreSelection.setActions(actionsSelection, debordementSelection);
        Rect sel = editeur.selectionBounds();
        PopupPlacer.Cadre placee = PopupAnchor.centeredAboveSelection(
                sel.left, sel.right, sel.top,
                popups.taille(PopupController.PopupKind.SELECTION_BAR),
                largeurCouche(), hauteurCouche(), margePx);
        popups.montrer(PopupController.PopupKind.SELECTION_BAR,
                placee.x(), placee.y());
        if (popups.estOuvert(PopupController.PopupKind.SELECTION_OVERFLOW)) {
            menuDebordement.ouvrir(barreSelection, debordementSelection);
        }
    }

    private void rafraichirInfoBulle(MultiSelection selection) {
        List<SelRange> plages = selection.ranges();
        if (plages.size() != 1) {
            return;
        }
        int ligne = session.document().lineForOffset(plages.get(0).caret());
        // l'audit Android Studio : la fiche empile TOUS les problèmes
        // du lieu — la décision pure vit chez ContenuPopups
        List<DiagnosticTooltipView.Probleme> problemes =
                ContenuPopups.problemesDe(diagnostics, nomDuFichier, ligne);
        if (problemes.isEmpty()) {
            return;
        }
        popups.controleur().open(
                PopupController.PopupKind.DIAGNOSTIC_TOOLTIP);
        infoBulle.setProblemes(problemes, indiceDActions());
        Rect curseur = editeur.caretBounds();
        PopupPlacer.Cadre placee = PopupAnchor.atPoint(
                curseur.left, curseur.top,
                popups.taille(PopupController.PopupKind.DIAGNOSTIC_TOOLTIP),
                largeurCouche(), hauteurCouche(), margePx);
        popups.montrer(PopupController.PopupKind.DIAGNOSTIC_TOOLTIP,
                placee.x(), placee.y());
    }

    /** L'indice d'actions qui clôt la fiche — le lien du bas de
     *  l'IDE de référence (« More actions… ») : il ne se pose que
     *  quand des intentions existent au caret (l'IDE ne promet pas
     *  de correctifs sans correctifs). */
    private @Nullable String indiceDActions() {
        return intentions.isEmpty() ? null
                : couche.getContext().getString(R.string.indice_actions);
    }

    private void placerCompletion() {
        Rect curseur = editeur.caretBounds();
        PopupPlacer.Placement placee = PopupPlacer.placer(
                new PopupPlacer.Cadre(curseur.left, curseur.top, 0,
                        curseur.height()),
                popups.taille(PopupController.PopupKind.COMPLETION_LIST),
                popups.taille(PopupController.PopupKind.COMPLETION_DOC),
                popups.taille(PopupController.PopupKind.SIGNATURE_HELP),
                largeurCouche(), hauteurCouche());
        popups.montrer(PopupController.PopupKind.COMPLETION_LIST,
                placee.completion().x(), placee.completion().y());
        if (popups.estOuvert(PopupController.PopupKind.COMPLETION_DOC)) {
            popups.montrer(PopupController.PopupKind.COMPLETION_DOC,
                    placee.documentation().x(), placee.documentation().y());
        }
        if (popups.estOuvert(PopupController.PopupKind.SIGNATURE_HELP)) {
            popups.montrer(PopupController.PopupKind.SIGNATURE_HELP,
                    placee.signature().x(), placee.signature().y());
        }
    }

    private void choisirCompletion(int index) {
        String candidat = listeCompletion.itemAt(index);
        if (candidat == null) {
            return;
        }
        int caret = caret();
        int debut = ContenuPopups.debutMot(session.document(), caret);
        // l'édition de l'acceptation ne rouvre pas la liste
        acceptationEnCours = true;
        if (!session.replaceRange(debut, caret, candidat)) {
            acceptationEnCours = false;
            return;
        }
        // la liste se ferme AVANT la sélection (sa documentation suit —
        // la cascade vit au manager, jamais codée deux fois)
        popups.fermer(PopupController.PopupKind.COMPLETION_LIST);
        session.setSelection(MultiSelection.of(
                SelRange.caretAt(debut + candidat.length())));
    }

    private void sauterDiagnostic(int offset) {
        session.setSelection(MultiSelection.of(SelRange.caretAt(offset)));
    }

    private void repositionnerTout() {
        if (popups.estOuvert(PopupController.PopupKind.COMPLETION_LIST)) {
            placerCompletion();
        }
        if (popups.estOuvert(PopupController.PopupKind.BREADCRUMB)) {
            filAriane.rafraichir();
        }
        if (popups.estOuvert(PopupController.PopupKind.ERROR_BAND)) {
            bordDroit.rafraichirBande(diagnostics, nomDuFichier,
                    largeurCouche(), hauteurCouche());
        }
        bordDroit.rafraichir(largeurCouche(), hauteurCouche());
        if (popups.estOuvert(PopupController.PopupKind.SELECTION_BAR)) {
            rafraichirBarreSelection(session.selection());
        }
        if (popups.estOuvert(PopupController.PopupKind.INTENTIONS_MENU)) {
            ouvrirIntentions();
        }
        // la consultation se REPOSITIONNE au défilement (le régime du
        // genre — le câblage manquait au pont, l'orchestrateur le porte)
        docExterne.repositionner();
    }

    /** Publie une notification au coin bas-droit — l'horloge la referme
     *  seule (4 000 ms) ; ce qui ne touche pas le texte s'y montre. */
    public void notifier(String titre, String message) {
        notifications.notifier(titre, message);
    }

    /** Publie une notification au coin bas-droit avec ses LIENS (l'audit
     *  Android Studio : les actions de la bulle de l'IDE de référence) —
     *  la prise d'un lien referme la bulle puis la fait courir. */
    public void notifier(String titre, String message,
            List<NotificationView.Action> actions) {
        notifications.notifier(titre, message, actions);
    }

    /** Ouvre la carte d'info de type du symbole au curseur — le type
     *  lu à la déclaration par le moteur du cœur, la carte sous le
     *  curseur, bornée. */
    public void ouvrirTypeInfo() {
        typeInfo.ouvrir();
    }

    /** Ouvre la carte des usages du symbole au curseur — le serveur de
     *  langage sert les lieux, l'occurrence ouverte saute, l'étrangère
     *  vit muette (l'audit Android Studio : le compte du serveur, tous
     *  fichiers ; sans serveur ni symbole, la carte ne se pose pas). */
    public void ouvrirUsages() {
        usages.ouvrir();
    }

    /** Ouvre la documentation externe du symbole au curseur — le
     *  serveur de langage par le fil du protocole, la table du coach
     *  en repli ; la consultation survit au défilement (elle se
     *  repositionne au curseur). */
    public void ouvrirDocExterne() {
        docExterne.ouvrir();
    }

    /** Ouvre la hiérarchie du symbole au curseur — de TYPES quand il
     *  nomme un type, d'APPELS quand il nomme une méthode (le plan
     *  décide) ; l'arbre de l'IDE de référence : la racine vit à
     *  l'en-tête, l'introuvable à la frontière. */
    public void ouvrirHierarchie() {
        hierarchies.ouvrir();
    }

    /** Le détachement de fin de vie : tout se ferme, l'avis du tap hors
     *  cartes se coupe, les liens se dissolvent — la scène peut recycler. */
    public void detacher() {
        popups.detacher();
    }

    /** Pose le nom du fichier ouvert — le filtre des diagnostics s'y fait. */
    public void setNomFichier(String nom) {
        nomDuFichier = nom;
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

    private int largeurCouche() {
        return couche.getWidth();
    }

    private int hauteurCouche() {
        return couche.getHeight();
    }

    /** Le caret courant — la première plage de sélection, zéro si vide. */
    private int caret() {
        List<SelRange> plages = session.selection().ranges();
        return plages.isEmpty() ? 0 : plages.get(0).caret();
    }
}
