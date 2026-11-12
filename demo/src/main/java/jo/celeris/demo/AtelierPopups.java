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
 * DIX popups vivent dans la couche posée sur l'éditeur, la machine à
 * états décide qui reste ouvert — la frappe fait filtrer la complétion
 * sans refermer la liste, le défilement repositionne sans fermer, la
 * barre de sélection naît de la sélection et cède à la frappe, la
 * bande d'erreurs vit avec les diagnostics. PIEGE-27 : aucun popup ne
 * prend le focus, le clavier reste à l'éditeur ; la barre de recherche
 * reste l'affaire de l'activité — son champ doit tenir le focus, la
 * couche l'interdit — l'atelier n'en coordonne que l'ÉTAT.
 */
final class AtelierPopups {

    /** La respiration des ancres aux bords de la couche, en dp. */
    private static final float MARGE_DP = 8f;

    private final EditorView editeur;
    private final TextSession session;
    private final PopupController controleur = new PopupController();
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
    /** Le menu de débordement de la barre — la carte du kit sous la puce. */
    private final MenuDebordement menuDebordement;

    /** Le thème courant — la palette des popups en dérive, le rafraîchissement la suit. */
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
    /** La session de langage branchée — null retombe sur le coach (M7.5, §8) */
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
     * Construit l'atelier : les vues naissent avec la palette dérivée
     * du thème d'éditeur courant (le verre du dépôt de départ), les
     * écouteurs de la session et de la fenêtre y courtent.
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
        PopupPalette palette = paletteDe(theme, tailleTexteSp);
        listeCompletion = new CompletionListView(couche.getContext(), palette);
        docCompletion = new CompletionDocView(couche.getContext(), palette);
        aideSignature = new SignatureHelpView(couche.getContext(), palette);
        menuIntentions = new IntentionsMenuView(couche.getContext(), palette);
        infoBulle = new DiagnosticTooltipView(couche.getContext(), palette);
        filAriane = new BreadcrumbView(couche.getContext(), palette);
        barreSelection = new SelectionBarView(couche.getContext(), palette);
        menuDebordement = new MenuDebordement(controleur, menuIntentions,
                couche, margePx);
        docAncre = new DocAncre(editeur.getContext(), controleur,
                docCompletion, couche, editeur, session, margePx);
        bordDroit = new AtelierBordDroit(controleur, couche, editeur,
                session, margePx, palette, this::sauterDiagnostic);

        listeCompletion.setOnPick(this::choisirCompletion);
        barreSelection.setOnOverflow(
                () -> menuDebordement.ouvrir(barreSelection,
                        debordementSelection));
        session.addEditListener((debut, fin, retire, insere) -> surEdition());
        session.addSelectionListener(this::surSelection);
        editeur.setSurViewportChange(this::surFenetre);
    }

    /**
     * Le thème tourne : la palette des popups se redérive — le verre
     * suit le fond de l'éditeur, les encres suivent le thème, les
     * panneaux ouverts se redessinent.
     *
     * @param nouveau le thème d'éditeur appliqué
     */
    void surTheme(EditorTheme nouveau) {
        theme = nouveau;
        rafraichirPalette();
    }

    /**
     * La taille de texte de l'éditeur change : la typographie des
     * popups s'y accorde (l'échelle du kit dérive de la base).
     *
     * @param tailleSp la nouvelle taille, en sp
     */
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
     * et — quand le serveur les sert — les complétions passent par le
     * fil du protocole, le coach ne garde que le repli. La langue se
     * lit du fichier ouvert.
     *
     * @param sessionLangage la session de langage démarrée
     * @param uri            l'uri du document ouvert
     */
    void brancherServeur(LanguageSession sessionLangage, String uri) {
        this.serveur = sessionLangage;
        this.uriDocument = uri;
        LanguageSupport langue = LangagesDemo.langageDe(nomDuFichier,
                null, session.document().text());
        sessionLangage.ouvrir(uri, langue == null ? "text" : langue.id(),
                session.document().text());
    }

    /**
     * Les actions de la barre de sélection — l'activité les fournit :
     * les principales en pastilles, le débordement derrière la puce.
     *
     * @param principales les actions visibles de la barre
     * @param debordement les actions repliées, servies par le menu
     */
    void configurerActionsSelection(List<SelectionBarView.Item> principales,
            List<SelectionBarView.Item> debordement) {
        actionsSelection = List.copyOf(principales);
        debordementSelection = List.copyOf(debordement);
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
        controleur.open(PopupController.PopupKind.SEARCH_BAR);
    }

    /** La barre de recherche se ferme — sur demande seule (M7.5, §7). */
    void fermerRecherche() {
        controleur.close(PopupController.PopupKind.SEARCH_BAR);
    }

    /**
     * Ouvre ou filtre la complétion au curseur (M8.6) : le serveur de
     * langage qui SERT la complétion passe le premier — sa réponse
     * brute devient les candidats typés du kit, leur documentation
     * suit la liste ; sinon les mots du code et la table du coach.
     * Sans candidat, la liste se ferme.
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
            fermer(PopupController.PopupKind.COMPLETION_LIST);
            return;
        }
        poserCompletion(PopupCoach.candidats(bruts),
                PopupCoach.docFor(bruts.get(0)));
    }

    /**
     * Pose la complétion mesurée : les candidats typés gagnent la
     * liste, le préfixe tapé surligne ses runs, le premier candidat
     * documenté ouvre sa documentation au flanc.
     */
    private void poserCompletion(List<CompletionListView.Candidat> candidats,
            CompletionDocModel doc) {
        if (candidats.isEmpty()) {
            fermer(PopupController.PopupKind.COMPLETION_LIST);
            return;
        }
        controleur.open(PopupController.PopupKind.COMPLETION_LIST);
        listeCompletion.setItems(candidats);
        listeCompletion.setPrefix(PopupCoach.prefixeCourant(
                session.document(), caret()));
        placerCompletion();
        if (doc != null) {
            controleur.open(PopupController.PopupKind.COMPLETION_DOC);
            docCompletion.setModel(doc);
            placerCompletion();
        } else {
            fermer(PopupController.PopupKind.COMPLETION_DOC);
        }
    }

    /** Ouvre le menu d'intentions au curseur — les actions de l'activité. */
    void ouvrirIntentions() {
        if (intentions.isEmpty()) {
            return;
        }
        controleur.open(PopupController.PopupKind.INTENTIONS_MENU);
        menuIntentions.setActions(intentions);
        Rect curseur = editeur.caretBounds();
        ancrer(PopupController.PopupKind.INTENTIONS_MENU, menuIntentions,
                PopupAnchor.belowOrAboveCaret(curseur.left, curseur.bottom,
                        taille(menuIntentions), largeurCouche(),
                        hauteurCouche(), margePx));
    }

    /**
     * Ouvre le survol du symbole au curseur — la documentation du
     * serveur de langage par le fil du protocole, la table du coach
     * en repli.
     */
    void ouvrirSurvol() {
        String symbole = symboleAuCaret();
        if (symbole == null) {
            return;
        }
        if (serveur != null) {
            int caret = caret();
            serveur.survol(uriDocument, session.document(), symbole, caret)
                    .whenComplete((doc, echec) -> editeur.post(
                            () -> montrerSurvol(doc)));
            return;
        }
        montrerSurvol(PopupCoach.docFor(symbole));
    }

    /** Pose le survol — null ferme, le modèle sinon s'ancre au point. */
    private void montrerSurvol(CompletionDocModel doc) {
        docAncre.survol(doc);
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
            fermer(PopupController.PopupKind.SIGNATURE_HELP);
            return;
        }
        controleur.open(PopupController.PopupKind.SIGNATURE_HELP);
        aideSignature.setModel(modele);
        placerCompletion();
    }

    private void surEdition() {
        frappeEnCours = true;
        controleur.onKeystroke();
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
            fermer(PopupController.PopupKind.COMPLETION_LIST);
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
        if (controleur.isOpen(PopupController.PopupKind.COMPLETION_LIST)) {
            basculerCompletion();
        }
        if (!apresFrappe) {
            rafraichirInfoBulle(selection);
        }
        if (controleur.isOpen(PopupController.PopupKind.SIGNATURE_HELP)) {
            rafraichirSignature();
        }
    }

    private void surFenetre() {
        controleur.onScroll();
        repositionnerTout();
    }

    private void rafraichirBarreSelection(MultiSelection selection) {
        List<SelRange> plages = selection.ranges();
        boolean visible = plages.size() == 1 && !plages.get(0).isEmpty();
        if (!visible) {
            fermer(PopupController.PopupKind.SELECTION_BAR);
            return;
        }
        controleur.open(PopupController.PopupKind.SELECTION_BAR);
        barreSelection.setActions(actionsSelection, debordementSelection);
        Rect sel = editeur.selectionBounds();
        ancrer(PopupController.PopupKind.SELECTION_BAR, barreSelection,
                PopupAnchor.centeredAboveSelection(sel.left, sel.right,
                        sel.top, taille(barreSelection), largeurCouche(),
                        hauteurCouche(), margePx));
        if (controleur.isOpen(PopupController.PopupKind.SELECTION_OVERFLOW)) {
            menuDebordement.ouvrir(barreSelection, debordementSelection);
        }
    }

    /**
     * Le fil d'Ariane du curseur : les entrées du plan qui
     * l'englobent, du dehors au dedans.
     */
    private void rafraichirFilAriane() {
        List<String> chemin = PopupCoach.pathFor(plan, caret());
        if (chemin.isEmpty()) {
            fermer(PopupController.PopupKind.BREADCRUMB);
            return;
        }
        controleur.open(PopupController.PopupKind.BREADCRUMB);
        filAriane.setPath(chemin, largeurCouche());
        ancrer(PopupController.PopupKind.BREADCRUMB, filAriane,
                PopupAnchor.topLeftCorner(taille(filAriane),
                        largeurCouche(), hauteurCouche(), margePx));
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
                controleur.open(PopupController.PopupKind.DIAGNOSTIC_TOOLTIP);
                infoBulle.setMessage(diagnostic.message(),
                        diagnostic.gravite() == GraviteDiagnostic.ERREUR
                                ? DiagnosticTooltipView.Severity.ERROR
                                : DiagnosticTooltipView.Severity.WARNING);
                Rect curseur = editeur.caretBounds();
                ancrer(PopupController.PopupKind.DIAGNOSTIC_TOOLTIP, infoBulle,
                        PopupAnchor.atPoint(curseur.left, curseur.top,
                                taille(infoBulle), largeurCouche(),
                                hauteurCouche(), margePx));
                return;
            }
        }
    }

    private void placerCompletion() {
        Rect curseur = editeur.caretBounds();
        PopupPlacer.Placement placee = PopupPlacer.placer(
                new PopupPlacer.Cadre(curseur.left, curseur.top, 0,
                        curseur.height()),
                taille(listeCompletion), taille(docCompletion),
                taille(aideSignature), largeurCouche(), hauteurCouche());
        couche.show(PopupController.PopupKind.COMPLETION_LIST, listeCompletion,
                placee.completion().x(), placee.completion().y());
        if (controleur.isOpen(PopupController.PopupKind.COMPLETION_DOC)) {
            couche.show(PopupController.PopupKind.COMPLETION_DOC, docCompletion,
                    placee.documentation().x(), placee.documentation().y());
        }
        if (controleur.isOpen(PopupController.PopupKind.SIGNATURE_HELP)) {
            couche.show(PopupController.PopupKind.SIGNATURE_HELP, aideSignature,
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
        // la liste se ferme AVANT la sélection : l'écouteur de
        // sélection ne la rouvre pas sur le curseur d'après
        fermer(PopupController.PopupKind.COMPLETION_LIST);
        session.setSelection(MultiSelection.of(
                SelRange.caretAt(debut + candidat.length())));
    }

    private void sauterDiagnostic(int offset) {
        session.setSelection(MultiSelection.of(SelRange.caretAt(offset)));
    }

    private void repositionnerTout() {
        if (controleur.isOpen(PopupController.PopupKind.COMPLETION_LIST)) {
            placerCompletion();
        }
        if (controleur.isOpen(PopupController.PopupKind.BREADCRUMB)) {
            rafraichirFilAriane();
        }
        if (controleur.isOpen(PopupController.PopupKind.ERROR_BAND)) {
            bordDroit.rafraichirBande(diagnostics, nomDuFichier,
                    largeurCouche(), hauteurCouche());
        }
        bordDroit.rafraichir(largeurCouche(), hauteurCouche());
        if (controleur.isOpen(PopupController.PopupKind.SELECTION_BAR)) {
            rafraichirBarreSelection(session.selection());
        }
        if (controleur.isOpen(PopupController.PopupKind.INTENTIONS_MENU)) {
            ouvrirIntentions();
        }
    }

    private void fermer(PopupController.PopupKind kind) {
        if (controleur.close(kind)) {
            couche.dismiss(kind);
            if (kind == PopupController.PopupKind.COMPLETION_LIST) {
                couche.dismiss(PopupController.PopupKind.COMPLETION_DOC);
            } else if (kind == PopupController.PopupKind.SELECTION_BAR) {
                couche.dismiss(PopupController.PopupKind.SELECTION_OVERFLOW);
            }
        }
    }

    private void ancrer(PopupController.PopupKind kind,
            android.view.View vue, PopupPlacer.Cadre placee) {
        // déjà montré, MÊME vue : le popup se RE-MONTE sans jamais
        // quitter la couche — le dragage de sélection y déplace la
        // barre d'un seul tenant, identique à l'appui long qui l'a
        // posée (aucune re-naissance d'image en image)
        if (couche.popupOf(kind) == vue) {
            couche.remonter(kind, placee.x(), placee.y());
            return;
        }
        couche.show(kind, vue, placee.x(), placee.y());
    }

    private PopupPlacer.Cadre taille(android.view.View vue) {
        vue.measure(android.view.View.MeasureSpec.makeMeasureSpec(0,
                        android.view.View.MeasureSpec.UNSPECIFIED),
                android.view.View.MeasureSpec.makeMeasureSpec(0,
                        android.view.View.MeasureSpec.UNSPECIFIED));
        return new PopupPlacer.Cadre(0, 0, vue.getMeasuredWidth(),
                vue.getMeasuredHeight());
    }

    private int caret() {
        List<SelRange> plages = session.selection().ranges();
        return plages.isEmpty() ? 0 : plages.get(0).caret();
    }

    private String symboleAuCaret() {
        return PopupCoach.symboleA(session.document(), caret());
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

    /** La machine à états courante — les bancs de la démo y lisent. */
    PopupController controleur() {
        return controleur;
    }

    /** La carte des menus du kit — l'overflow de la toolbar la partage
     *  (le motif du dépôt de départ : une carte, tous les menus). */
    IntentionsMenuView carteMenus() {
        return menuIntentions;
    }
}
