package jo.celeris.demo;

import android.graphics.Rect;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.android.host.spi.DiagnosticHote;
import jo.celeris.android.host.spi.GraviteDiagnostic;
import jo.celeris.android.view.EditorView;
import jo.celeris.android.widgets.BreadcrumbView;
import jo.celeris.android.widgets.CompletionDocModel;
import jo.celeris.android.widgets.CompletionDocView;
import jo.celeris.android.widgets.CompletionListView;
import jo.celeris.android.widgets.DiagnosticTooltipView;
import jo.celeris.android.widgets.ErrorBandModel;
import jo.celeris.android.widgets.ErrorBandView;
import jo.celeris.android.widgets.IntentionsMenuView;
import jo.celeris.android.widgets.PopupAnchor;
import jo.celeris.android.widgets.PopupController;
import jo.celeris.android.widgets.PopupLayer;
import jo.celeris.android.widgets.PopupPalette;
import jo.celeris.android.widgets.PopupPlacer;
import jo.celeris.android.widgets.SelectionBarView;
import jo.celeris.android.widgets.SignatureAideModel;
import jo.celeris.android.widgets.SignatureHelpView;
import jo.celeris.core.syntax.LanguageSyntax;
import jo.celeris.core.syntax.OutlineEntry;
import jo.celeris.core.syntax.SyntaxIssue;
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
    /** Le nombre de crans de la bande d'erreurs. */
    private static final int CRANS_BANDE = 200;

    private final EditorView editeur;
    private final TextSession session;
    private final PopupController controleur = new PopupController();
    private final PopupLayer couche;
    private final int margePx;

    private final CompletionListView listeCompletion;
    private final CompletionDocView docCompletion;
    private final SignatureHelpView aideSignature;
    private final IntentionsMenuView menuIntentions;
    private final DiagnosticTooltipView infoBulle;
    private final BreadcrumbView filAriane;
    private final ErrorBandView bandeErreurs;
    private final SelectionBarView barreSelection;

    private List<OutlineEntry> plan = List.of();
    private List<DiagnosticHote> diagnostics = List.of();
    private List<IntentionsMenuView.Item> intentions = List.of();
    private List<SelectionBarView.Item> actionsSelection = List.of();
    /** Le nom du fichier ouvert — le filtre des diagnostics s'y fait. */
    private String nomDuFichier = "";
    /** Le serveur de langage branché — null retombe sur le coach (M7.5, §8). */
    private ServeurLangage serveur;
    /** L'uri du document ouvert auprès du serveur de langage. */
    private String uriDocument = "";
    /** La frappe en cours : la sélection qui la suit ne rouvre pas
     *  l'info-bulle — le contrôleur vient de la fermer (M7.5, §7). */
    private boolean frappeEnCours;
    /** L'acceptation d'un candidat vient d'avoir lieu : l'édition qui
     *  l'accompagne ne rouvre pas la liste (M7.5, redressement). */
    private boolean acceptationEnCours;

    /**
     * Construit l'atelier : les vues naissent avec la palette du thème
     * courant, les écouteurs de la session et de la fenêtre y courtent.
     *
     * @param editeur la vue d'éditeur ouverte
     * @param session la session d'édition courante
     * @param couche  la couche porte-popups posée sur l'éditeur
     * @param sombre  vrai pour la palette sombre du thème courant
     */
    AtelierPopups(EditorView editeur, TextSession session, PopupLayer couche,
            boolean sombre) {
        this.editeur = editeur;
        this.session = session;
        this.couche = couche;
        this.margePx = Math.round(MARGE_DP
                * couche.getResources().getDisplayMetrics().density);
        PopupPalette palette = sombre ? PopupPalette.dark()
                : PopupPalette.light();
        listeCompletion = new CompletionListView(couche.getContext(), palette);
        docCompletion = new CompletionDocView(couche.getContext(), palette);
        aideSignature = new SignatureHelpView(couche.getContext(), palette);
        menuIntentions = new IntentionsMenuView(couche.getContext(), palette);
        infoBulle = new DiagnosticTooltipView(couche.getContext(), palette);
        filAriane = new BreadcrumbView(couche.getContext(), palette);
        bandeErreurs = new ErrorBandView(couche.getContext(), palette);
        barreSelection = new SelectionBarView(couche.getContext(), palette);

        listeCompletion.setOnPick(this::choisirCompletion);
        bandeErreurs.setOnJump(this::sauterDiagnostic);
        session.addEditListener((debut, fin, retire, insere) -> surEdition());
        session.addSelectionListener(this::surSelection);
        editeur.setSurViewportChange(this::surFenetre);
    }

    /** Les intentions du menu — l'activité les fournit, libellés compris. */
    void configurerIntentions(List<IntentionsMenuView.Item> items) {
        intentions = List.copyOf(items);
    }

    /**
     * Branche le serveur de langage (M7.5, §8) : l'aide aux
     * paramètres et le survol passent par le fil du protocole — le
     * coach ne garde que le repli. La langue se lit du fichier ouvert.
     *
     * @param serveurLangage la session de langage démarrée
     * @param uri            l'uri du document ouvert
     */
    void brancherServeur(ServeurLangage serveurLangage, String uri) {
        this.serveur = serveurLangage;
        this.uriDocument = uri;
        LanguageSyntax langue = FichiersExemple.languePour(nomDuFichier);
        serveurLangage.ouvrir(uri, langue == null ? "text" : langue.id(),
                session.document().text());
    }

    /** Les actions de la barre de sélection — l'activité les fournit. */
    void configurerActionsSelection(List<SelectionBarView.Item> items) {
        actionsSelection = List.copyOf(items);
    }

    /** Le plan du fichier ouvert — le fil d'Ariane s'y abreuvement. */
    void setPlan(List<OutlineEntry> entrees) {
        plan = List.copyOf(entrees);
        rafraichirFilAriane();
    }

    /** Les diagnostics de l'hôte — la bande et l'info-bulle s'y abreuvent. */
    void setDiagnostics(List<DiagnosticHote> nouveaux) {
        diagnostics = List.copyOf(nouveaux);
        rafraichirBandeErreurs();
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
     * Ouvre ou filtre la complétion au curseur : les candidats du
     * CONCENTRATEUR — les mots du code par l'analyseur, le coach en
     * repli —, le premier surligné ; la documentation du premier
     * candidat connu suit sa liste. Sans candidat, la liste se ferme.
     */
    void basculerCompletion() {
        List<String> candidats = editeur.intelligence()
                .completionCandidates(PopupCoach.motsReserves());
        if (candidats.isEmpty()) {
            fermer(PopupController.PopupKind.COMPLETION_LIST);
            return;
        }
        controleur.open(PopupController.PopupKind.COMPLETION_LIST);
        listeCompletion.setItems(candidats);
        placerCompletion();
        CompletionDocModel doc = PopupCoach.docFor(candidats.get(0));
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
        if (doc == null) {
            return;
        }
        controleur.open(PopupController.PopupKind.HOVER);
        docCompletion.setModel(doc);
        Rect curseur = editeur.caretBounds();
        ancrer(PopupController.PopupKind.HOVER, docCompletion,
                PopupAnchor.atPoint(curseur.left, curseur.top,
                        taille(docCompletion), largeurCouche(),
                        hauteurCouche(), margePx));
    }

    /** Ouvre l'aide aux paramètres de l'appel ouvert sous le curseur —
     *  le serveur de langage par le fil du protocole, le coach en repli. */
    void rafraichirSignature() {
        if (serveur != null) {
            int caret = caret();
            serveur.signature(uriDocument, session.document(), caret)
                    .whenComplete((modele, echec) -> editeur.post(
                            () -> montrerSignature(modele)));
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
        barreSelection.setActions(actionsSelection);
        Rect sel = editeur.selectionBounds();
        ancrer(PopupController.PopupKind.SELECTION_BAR, barreSelection,
                PopupAnchor.centeredAboveSelection(sel.left, sel.right,
                        sel.top, taille(barreSelection), largeurCouche(),
                        hauteurCouche(), margePx));
    }

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

    private void rafraichirBandeErreurs() {
        if (diagnostics.isEmpty()) {
            fermer(PopupController.PopupKind.ERROR_BAND);
            return;
        }
        controleur.open(PopupController.PopupKind.ERROR_BAND);
        bandeErreurs.setModel(ErrorBandModel.of(issues(), session.document(),
                CRANS_BANDE), CRANS_BANDE);
        bandeErreurs.setSpanPx(Math.max(1, hauteurCouche() - 2 * margePx));
        PopupPlacer.Cadre placee = PopupAnchor.rightEdge(taille(bandeErreurs),
                largeurCouche(), hauteurCouche(), margePx);
        couche.show(PopupController.PopupKind.ERROR_BAND, bandeErreurs,
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
        String texte = session.document().text();
        int debut = caret;
        while (debut > 0 && estIdentifiant(texte.charAt(debut - 1))) {
            debut--;
        }
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
            rafraichirBandeErreurs();
        }
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
            }
        }
    }

    private void ancrer(PopupController.PopupKind kind,
            android.view.View vue, PopupPlacer.Cadre placee) {
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
        String texte = session.document().text();
        int caret = caret();
        int debut = caret;
        while (debut > 0 && estIdentifiant(texte.charAt(debut - 1))) {
            debut--;
        }
        return debut == caret ? null : texte.substring(debut, caret);
    }

    /** Pose le nom du fichier ouvert — le filtre des diagnostics s'y fait. */
    void setNomFichier(String nom) {
        nomDuFichier = nom;
    }

    private List<SyntaxIssue> issues() {
        List<SyntaxIssue> issues = new ArrayList<>();
        for (DiagnosticHote diagnostic : diagnostics) {
            if (diagnostic.chemin().nom().equals(nomDuFichier)) {
                int ligne = Math.max(0, diagnostic.ligne() - 1);
                int debut = session.document().lineStart(ligne);
                issues.add(new SyntaxIssue(debut, 1, diagnostic.message()));
            }
        }
        return issues;
    }

    private int largeurCouche() {
        return couche.getWidth();
    }

    private int hauteurCouche() {
        return couche.getHeight();
    }

    private static boolean estIdentifiant(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    /** La machine à états courante — les bancs de la démo y lisent. */
    PopupController controleur() {
        return controleur;
    }
}
