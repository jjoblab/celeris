package jo.celeris.demo;

import android.graphics.Rect;

import java.util.List;

import jo.celeris.android.host.spi.DiagnosticHote;
import jo.celeris.android.host.spi.GraviteDiagnostic;
import jo.celeris.android.view.EditorTheme;
import jo.celeris.android.view.EditorView;
import jo.celeris.android.widgets.BreadcrumbView;
import jo.celeris.android.widgets.CompletionDocModel;
import jo.celeris.android.widgets.CompletionDocView;
import jo.celeris.android.widgets.CompletionListView;
import jo.celeris.android.widgets.DiagnosticTooltipView;
import jo.celeris.android.widgets.IntentionsMenuView;
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

/**
 * L'atelier des popups de la démonstration (M7.5, §7, reprise) : les
 * genres vivant dans la couche posée sur l'éditeur, le MANAGER du kit
 * tient la boucle état→vues d'un seul tenant — l'atelier déclenche, la
 * machine décide, la couche porte. PIEGE-27 : aucun popup ne prend le
 * focus, les touches matérielles lui sont DÉLÉGUÉES ({@link #surTouche}) ;
 * la barre de recherche reste l'affaire de l'activité — son champ doit
 * tenir le focus, la couche l'interdit — l'atelier n'en coordonne que l'ÉTAT.
 */
final class AtelierPopups {

    /** La respiration des ancres aux bords de la couche, en dp. */
    private static final float MARGE_DP = 8f;

    private final EditorView editeur;
    private final TextSession session;
    private final PopupManager popups;
    private final PopupLayer couche;
    private final int margePx;
    /** Le poseur de documents ancrés — survol, aperçu, rendu @Preview. */
    private final DocAncre docAncre;

    private final CompletionListView listeCompletion;
    private final CompletionDocView docCompletion;
    private final SignatureHelpView aideSignature;
    private final IntentionsMenuView menuIntentions;
    private final DiagnosticTooltipView infoBulle;
    private final BreadcrumbView filAriane;
    private final SelectionBarView barreSelection;
    /** Le bord droit de la couche — bande d'erreurs, minimap, pouce
     *  (chantier C9, plafond ADR-002 : le chrome y vit seul). */
    private final AtelierBordDroit bordDroit;
    /** Le menu de débordement de la barre — sa carte propre sous la
     *  puce (le chantier d'unification : plus de carte partagée). */
    private final MenuDebordement menuDebordement;
    /** L'atelier des couleurs — le sélecteur au double-tap d'un littéral. */
    private final AtelierCouleur atelierCouleur;
    /** L'atelier des usages — la carte des références du symbole au
     *  curseur (le signal du propriétaire du 2026-11-13). */
    private final AtelierUsages atelierUsages;
    /** L'atelier d'info de type — la carte du type du symbole au
     *  curseur (catégorie A, le fil rouge du guide du développeur). */
    private final AtelierTypeInfo atelierTypeInfo;
    /** L'atelier des hiérarchies — l'arbre de types ou d'appels du
     *  symbole au curseur (catégorie C, les moteurs se réveillent). */
    private final AtelierHierarchies atelierHierarchies;

    /** Le thème courant — la palette des popups en dérive, le
     *  rafraîchissement la suit. */
    private EditorTheme theme;
    /** La taille de texte de l'éditeur, en sp — la typographie des popups s'y accorde. */
    private float tailleTexteSp;

    private List<OutlineEntry> plan = List.of();
    private List<DiagnosticHote> diagnostics = List.of();
    private List<IntentionsMenuView.Item> intentions = List.of();
    private List<SelectionBarView.Item> actionsSelection = List.of();
    /** Les actions repliées derrière la puce de débordement. */
    private List<SelectionBarView.Item> debordementSelection = List.of();
    /** Le nom du fichier ouvert — le filtre des diagnostics s'y fait. */
    private String nomDuFichier = "";
    /** La session de langage branchée — null retombe sur le coach (M7.5, §8). */
    private LanguageSession serveur;
    /** L'uri du document ouvert auprès du serveur de langage. */
    private String uriDocument = "";
    /** La frappe en cours : la sélection qui la suit ne rouvre pas
     *  l'info-bulle — le contrôleur vient de la fermer (M7.5, §7). */
    private boolean frappeEnCours;
    /** L'acceptation d'un candidat vient d'avoir lieu : l'édition qui
     *  l'accompagne ne rouvre pas la liste (M7.5, redressement). */
    private boolean acceptationEnCours;

    /**
     * Construit l'atelier : le manager naît sur la machine et la couche
     * de la scène, les vues se lient à leurs genres (jamais une carte
     * pour deux), les écouteurs y courtent.
     *
     * @param editeur      la vue d'éditeur ouverte
     * @param session      la session d'édition courante
     * @param couche       la couche porte-popups posée sur l'éditeur
     * @param theme        le thème d'éditeur courant
     * @param tailleTexteSp la taille de texte de l'éditeur, en sp
     */
    AtelierPopups(EditorView editeur, TextSession session, PopupLayer couche,
            EditorTheme theme, float tailleTexteSp) {
        this.editeur = editeur;
        this.session = session;
        this.couche = couche;
        this.theme = theme;
        this.tailleTexteSp = tailleTexteSp;
        this.margePx = Math.round(MARGE_DP
                * couche.getResources().getDisplayMetrics().density);
        PopupController controleur = new PopupController();
        popups = new PopupManager(controleur, couche);
        PopupPalette palette = paletteDe(theme, tailleTexteSp);
        listeCompletion = new CompletionListView(couche.getContext(), palette);
        docCompletion = new CompletionDocView(couche.getContext(), palette);
        aideSignature = new SignatureHelpView(couche.getContext(), palette);
        menuIntentions = new IntentionsMenuView(couche.getContext(), palette);
        infoBulle = new DiagnosticTooltipView(couche.getContext(), palette);
        filAriane = new BreadcrumbView(couche.getContext(), palette);
        barreSelection = new SelectionBarView(couche.getContext(), palette);
        // le lien des genres : une carte par genre, un genre par carte
        // — le manager l'exige, le zombie d'état ne revient plus
        popups.lier(PopupController.PopupKind.COMPLETION_LIST, listeCompletion);
        popups.lier(PopupController.PopupKind.COMPLETION_DOC, docCompletion);
        popups.lier(PopupController.PopupKind.SIGNATURE_HELP, aideSignature);
        popups.lier(PopupController.PopupKind.INTENTIONS_MENU, menuIntentions);
        popups.lier(PopupController.PopupKind.DIAGNOSTIC_TOOLTIP, infoBulle);
        popups.lier(PopupController.PopupKind.BREADCRUMB, filAriane);
        popups.lier(PopupController.PopupKind.SELECTION_BAR, barreSelection);
        // la section du débordement : le « MORE » du dépôt de départ,
        // traduit par la démonstration
        menuDebordement = new MenuDebordement(popups, palette, margePx,
                couche.getContext().getString(R.string.menu_plus));
        atelierCouleur = new AtelierCouleur(editeur, session, popups, margePx, palette);
        atelierUsages = new AtelierUsages(editeur, session, popups, margePx, palette);
        atelierTypeInfo = new AtelierTypeInfo(editeur, session, popups, margePx, palette);
        atelierHierarchies = new AtelierHierarchies(editeur, session, popups,
                margePx, palette, () -> plan, () -> nomDuFichier);
        docAncre = new DocAncre(couche.getContext(), popups, editeur, session, margePx, palette);
        bordDroit = new AtelierBordDroit(popups, editeur, session, margePx,
                palette, this::sauterDiagnostic);

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
    void surTheme(EditorTheme nouveau) {
        theme = nouveau;
        rafraichirPalette();
    }

    /** La taille de texte tourne : la typographie des popups suit. */
    void surTailleTexte(float tailleSp) {
        tailleTexteSp = tailleSp;
        rafraichirPalette();
    }

    private void rafraichirPalette() {
        PopupPalette palette = paletteDe(theme, tailleTexteSp);
        listeCompletion.setPalette(palette);
        docCompletion.setPalette(palette);
        aideSignature.setPalette(palette);
        menuIntentions.setPalette(palette);
        infoBulle.setPalette(palette);
        filAriane.setPalette(palette);
        bordDroit.surPalette(palette);
        barreSelection.setPalette(palette);
        atelierCouleur.surPalette(palette);
        atelierUsages.surPalette(palette);
        atelierTypeInfo.surPalette(palette);
        atelierHierarchies.surPalette(palette);
        menuDebordement.surPalette(palette);
        docAncre.surPalette(palette);
    }

    /** Le manager du kit — l'activité y branche la toolbar et la
     *  délégation clavier. */
    PopupManager manager() {
        return popups;
    }

    /** La palette dérivée du thème d'éditeur — le verre du dépôt de
     *  départ, par la voie commune de la démonstration. */
    private static PopupPalette paletteDe(EditorTheme theme,
            float tailleTexteSp) {
        return PalettesDemo.de(theme, tailleTexteSp);
    }

    /** Les intentions du menu — l'activité les fournit, libellés compris. */
    void configurerIntentions(List<IntentionsMenuView.Item> items) {
        intentions = List.copyOf(items);
    }

    /**
     * Branche la session de langage (M7.5, §8 — M8.6 : tout serveur
     * déclaré entre par la même porte) : l'aide aux paramètres, le survol
     * et — quand le serveur les sert — les complétions passent par le fil
     * du protocole ; la langue se lit du fichier ouvert.
     *
     * @param sessionLangage la session de langage démarrée
     * @param uri            l'uri du document ouvert
     */
    void brancherServeur(LanguageSession sessionLangage, String uri) {
        this.serveur = sessionLangage;
        this.uriDocument = uri;
        docAncre.brancherServeur(sessionLangage, uri);
        atelierUsages.brancherServeur(sessionLangage, uri);
        LanguageSupport langue = LangagesDemo.langageDe(nomDuFichier,
                null, session.document().text());
        sessionLangage.ouvrir(uri, langue == null ? "text" : langue.id(),
                session.document().text());
    }

    /** Les actions de la barre de sélection — l'activité les fournit :
     * les principales en pastilles, le débordement derrière la puce.
     *
     * @param principales les actions visibles de la barre
     * @param debordement les actions repliées, servies par le menu */
    void configurerActionsSelection(List<SelectionBarView.Item> principales,
            List<SelectionBarView.Item> debordement) {
        actionsSelection = List.copyOf(principales);
        debordementSelection = List.copyOf(debordement);
    }

    /** Ouvre le sélecteur de couleur au double-tap d'un littéral — le
     *  verdict dit si le geste est consommé. */
    boolean ouvrirColorPicker(int offset) {
        return atelierCouleur.ouvrirAuDoubleTap(offset);
    }

    /** Ouvre la carte des usages du symbole au curseur — les
     *  références servies par le serveur de langage (le signal du
     *  propriétaire du 2026-11-13, la première surface réveillée). */
    void ouvrirUsages() {
        atelierUsages.ouvrir();
    }

    /** Ouvre la carte d'info de type du symbole au curseur — le type
     *  lu à la déclaration par le coach local (catégorie A, le fil
     *  rouge du guide du développeur, la seconde surface réveillée). */
    void ouvrirTypeInfo() {
        atelierTypeInfo.ouvrir();
    }

    /** Ouvre la hiérarchie du symbole au curseur — de types s'il nomme
     *  un type, d'appels s'il nomme une méthode (catégorie C, les
     *  moteurs de core-intelligence se réveillent). */
    void ouvrirHierarchie() {
        atelierHierarchies.ouvrir();
    }

    /** Le plan du fichier ouvert — le fil d'Ariane s'y abreuvement. */
    void setPlan(List<OutlineEntry> entrees) {
        plan = List.copyOf(entrees);
        rafraichirFilAriane();
    }

    /** Les diagnostics de l'hôte — la bande et l'info-bulle s'y abreuvent. */
    void setDiagnostics(List<DiagnosticHote> nouveaux) {
        diagnostics = List.copyOf(nouveaux);
        bordDroit.rafraichirBande(diagnostics, nomDuFichier,
                largeurCouche(), hauteurCouche());
    }

    /** La barre de recherche s'ouvre — l'état suit, le champ reste à l'activité. */
    void ouvrirRecherche() {
        popups.controleur().open(PopupController.PopupKind.SEARCH_BAR);
    }

    /** La barre de recherche se ferme — sur demande seule (M7.5, §7). */
    void fermerRecherche() {
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
    boolean surTouche(int keyCode) {
        return popups.surTouche(keyCode);
    }

    /** La feuille modale qui s'ouvre coupe l'intelligence de la couche :
     *  le manager ferme d'un seul tenant, le chrome demeure. */
    void fermerSousVoile() {
        popups.fermerSousVoile();
    }

    /**
     * Ouvre ou filtre la complétion au curseur (M8.6) : le serveur de
     * langage qui SERT la complétion passe le premier — sa réponse brute
     * devient les candidats typés du kit et leur documentation ; sinon
     * les mots du code et la table du coach — sans candidat, la liste se
     * ferme.
     */
    void basculerCompletion() {
        if (serveur != null && serveur.sertCompletion()) {
            serveur.completion(uriDocument, session.document(), caret())
                    .whenComplete((reponse, echec) -> editeur.post(
                            () -> poserCompletion(
                                    PopupCoach.candidats(reponse),
                                    PopupCoach.docDe(reponse, 0))));
            return;
        }
        List<String> bruts = editeur.intelligence()
                .completionCandidates(PopupCoach.motsReserves());
        if (bruts.isEmpty()) {
            popups.fermer(PopupController.PopupKind.COMPLETION_LIST);
            return;
        }
        poserCompletion(PopupCoach.candidats(bruts),
                PopupCoach.docFor(bruts.get(0)));
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
        listeCompletion.setPrefix(PopupCoach.prefixeCourant(
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

    /** Ouvre le menu d'intentions au curseur — les actions de l'activité. */
    void ouvrirIntentions() {
        if (intentions.isEmpty()) {
            return;
        }
        // le menu d'intentions du caret ne porte AUCUNE section — la
        // carte partagée la laisse au débordement des barres
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
    void ouvrirSurvol() {
        docAncre.survolAuCaret();
    }

    /** Le survol au DOUBLE-TAP — le geste seul et le « i » l'ouvrent
     *  (le redressement du 2026-11-13). */
    boolean ouvrirSurvolAuDoubleTap(int offset) {
        return docAncre.survolAuDoubleTap(offset);
    }

    /** Le poseur de documents ancrés — l'activité y lit (C12). */
    DocAncre docAncre() {
        return docAncre;
    }

    /** Ouvre l'aide aux paramètres de l'appel ouvert sous le curseur —
     *  le serveur de langage par le fil du protocole, le coach en
     *  repli local sur réponse vide (C7). */
    void rafraichirSignature() {
        int caret = caret();
        if (serveur != null) {
            serveur.signature(uriDocument, session.document(), caret)
                    .whenComplete((modele, echec) -> editeur.post(
                            () -> montrerSignature(modele != null ? modele
                                    : PopupCoach.signatureFor(
                                            session.document(), caret))));
            return;
        }
        montrerSignature(PopupCoach.signatureFor(
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
            // candidat retenu ne se repropose pas (M7.5, redressement)
            acceptationEnCours = false;
        } else if (PopupCoach.doitOuvrirALaFrappe(session.document(),
                caret())) {
            // M7.5, redressement — le constat du propriétaire : la
            // complétion se propose à la FRAPPE, comme tout éditeur ;
            // déjà ouverte, la même voie filtre sans refermer
            basculerCompletion();
        } else {
            // le blanc et la ponctuation referment la liste — fermer
            // une liste close ne fait rien, le garde isOpen est de trop
            popups.fermer(PopupController.PopupKind.COMPLETION_LIST);
        }
        rafraichirSignature();
        rafraichirFilAriane();
        bordDroit.rafraichir(largeurCouche(), hauteurCouche());
    }

    private void surSelection(MultiSelection selection) {
        boolean apresFrappe = frappeEnCours;
        frappeEnCours = false;
        rafraichirBarreSelection(selection);
        rafraichirFilAriane();
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

    /** Le fil d'Ariane du curseur : les entrées du plan qui
     * l'englobent, du dehors au dedans. */
    private void rafraichirFilAriane() {
        List<String> chemin = PopupCoach.pathFor(plan, caret());
        if (chemin.isEmpty()) {
            popups.fermer(PopupController.PopupKind.BREADCRUMB);
            return;
        }
        popups.controleur().open(PopupController.PopupKind.BREADCRUMB);
        filAriane.setPath(chemin, largeurCouche());
        PopupPlacer.Cadre placee = PopupAnchor.topLeftCorner(
                popups.taille(PopupController.PopupKind.BREADCRUMB),
                largeurCouche(), hauteurCouche(), margePx);
        popups.montrer(PopupController.PopupKind.BREADCRUMB,
                placee.x(), placee.y());
    }

    private void rafraichirInfoBulle(MultiSelection selection) {
        List<SelRange> plages = selection.ranges();
        if (plages.size() != 1) {
            return;
        }
        int ligne = session.document().lineForOffset(plages.get(0).caret());
        for (DiagnosticHote diagnostic : diagnostics) {
            if (diagnostic.ligne() == ligne + 1
                    && diagnostic.chemin().nom().equals(nomDuFichier)) {
                popups.controleur().open(
                        PopupController.PopupKind.DIAGNOSTIC_TOOLTIP);
                infoBulle.setMessage(diagnostic.message(),
                        diagnostic.gravite() == GraviteDiagnostic.ERREUR
                                ? DiagnosticTooltipView.Severity.ERROR
                                : DiagnosticTooltipView.Severity.WARNING);
                Rect curseur = editeur.caretBounds();
                PopupPlacer.Cadre placee = PopupAnchor.atPoint(
                        curseur.left, curseur.top,
                        popups.taille(PopupController.PopupKind.DIAGNOSTIC_TOOLTIP),
                        largeurCouche(), hauteurCouche(), margePx);
                popups.montrer(PopupController.PopupKind.DIAGNOSTIC_TOOLTIP,
                        placee.x(), placee.y());
                return;
            }
        }
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
        int debut = PopupCoach.debutMot(session.document(), caret);
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
            rafraichirFilAriane();
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
    }

    /** Le détachement de fin de vie : tout se ferme, l'avis du tap hors
     * cartes se coupe, les liens se dissolvent — la scène peut recycler. */
    void detacher() {
        popups.detacher();
    }

    /** Pose le nom du fichier ouvert — le filtre des diagnostics s'y fait. */
    void setNomFichier(String nom) {
        nomDuFichier = nom;
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

    /** La machine à états courante — les bancs de la démo y lisent. */
    PopupController controleur() {
        return popups.controleur();
    }
}
