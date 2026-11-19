package jo.celeris.android.host.popups;

import android.graphics.Rect;

import java.util.List;

import jo.celeris.android.view.EditorView;
import jo.celeris.android.widgets.CompletionDocModel;
import jo.celeris.android.widgets.CompletionDocView;
import jo.celeris.android.widgets.PopupAnchor;
import jo.celeris.android.widgets.PopupController;
import jo.celeris.android.widgets.PopupManager;
import jo.celeris.android.widgets.PopupPalette;
import jo.celeris.android.widgets.PopupPlacer;

import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextSession;

import org.jspecify.annotations.Nullable;

/**
 * La documentation externe (la troisième surface portée de la
 * exemple d'intégration à l'orchestrateur, 2026-11-14) : la documentation du
 * symbole au curseur, hors contexte de complétion — le serveur de
 * langage par le fil du protocole, la table du coach en repli, le
 * MÊME chemin de données que le survol. La carte vit le régime de la
 * CONSULTATION : ouverte, elle se lit — la frappe ne la chasse pas, le
 * défilement la REPOSITIONNE ({@link #repositionner} — la consultation
 * suit le curseur), le tap hors cartes la referme, l'échappement
 * aussi.
 *
 * <p>La frontière tient : la carte DÉRIVÉE ne connaît NI le serveur,
 * NI l'éditeur — le satellite sert d'intermédiaire ; SANS symbole au
 * curseur, la carte ne se pose pas.</p>
 */
final class DocExterne {

    private final EditorView editor;
    private final TextSession session;
    private final PopupManager popups;
    private final int margePx;
    private final CompletionDocView carte;
    /** Le coach de l'hôte — la table du repli sans serveur. */
    private final CoachPopups coach;
    /** La session de langage branchée — la documentation y passe. */
    private @Nullable LanguageSession server;
    /** L'uri du document ouvert auprès du serveur de langage. */
    private String uriDocument = "";

    /**
     * Construit la documentation externe sur le coordinateur des
     * popups : la carte DÉRIVÉE naît liée à son genre — sa propre
     * instance, jamais la carte du survol (une carte par genre).
     *
     * @param editor la vue d'éditeur ouverte
     * @param session la session d'édition courante
     * @param popups  le coordinateur des popups de la scène
     * @param margePx la respiration des ancres aux bords, en pixels
     * @param palette la palette courante des popups
     * @param coach   le coach de l'hôte — le repli sans serveur
     */
    DocExterne(EditorView editor, TextSession session, PopupManager popups,
            int margePx, PopupPalette palette, CoachPopups coach) {
        this.editor = editor;
        this.session = session;
        this.popups = popups;
        this.margePx = margePx;
        this.coach = coach;
        this.carte = new CompletionDocView(popups.couche().getContext(),
                palette);
        popups.lier(PopupController.PopupKind.EXTERNAL_DOC, carte);
    }

    /**
     * Branche la session de langage — la documentation du symbole y
     * passe au fil du protocole.
     *
     * @param server la session démarrée, null pour le repli seul
     * @param uri     l'uri du document ouvert
     */
    void connectServer(@Nullable LanguageSession server, String uri) {
        this.server = server;
        this.uriDocument = uri;
    }

    /** La palette tourne : la carte de documentation la suit. */
    void surPalette(PopupPalette palette) {
        carte.setPalette(palette);
    }

    /**
     * Ouvre la documentation du symbole au curseur : le serveur sert le
     * survol par le fil du protocole, la table du coach en repli. Sans
     * symbole au curseur, la carte ne se pose pas.
     */
    void open() {
        String symbole = ContenuPopups.symboleA(session.document(), caret());
        if (symbole == null) {
            return;
        }
        if (server != null) {
            server.hover(uriDocument, session.document(), symbole, caret())
                    .whenComplete((doc, echec) -> editor.post(
                            () -> place(doc)));
            return;
        }
        place(coach.docDe(symbole));
    }

    /** Le défilement a déplacé le curseur : la consultation se
     *  REPOSITIONNE — la carte ouverte se re-pose au curseur, elle ne
     *  se ferme pas (le régime de la consultation, le contenu ne
     *  re-paraît pas : la même carte se re-place). */
    void repositionner() {
        if (popups.estOuvert(PopupController.PopupKind.EXTERNAL_DOC)) {
            show();
        }
    }

    /** Pose la documentation de symbole — null ne pose rien, le modèle
     *  sinon gagne la carte et s'ancre sous le curseur, borné. */
    private void place(@Nullable CompletionDocModel doc) {
        if (doc == null) {
            return;
        }
        carte.setModel(doc);
        show();
    }

    /** Monte la carte mesurée sous le curseur, bornée — la pose ET la
     *  re-pose y passent (la même carte se re-place, jamais le contenu
     *  ne re-naît). */
    private void show() {
        Rect cursor = editor.caretBounds();
        PopupPlacer.Cadre placee = PopupAnchor.belowOrAboveCaret(
                cursor.left, cursor.bottom, size(),
                popups.couche().getWidth(),
                popups.couche().getHeight(), margePx);
        popups.show(PopupController.PopupKind.EXTERNAL_DOC,
                placee.x(), placee.y());
    }

    /** La taille mesurée de la carte — l'ancre y anticipe la pose. */
    private PopupPlacer.Cadre size() {
        return popups.size(PopupController.PopupKind.EXTERNAL_DOC);
    }

    /** La position du curseur — la tête de la sélection courante. */
    private int caret() {
        List<SelRange> ranges = session.selection().ranges();
        return ranges.isEmpty() ? 0 : ranges.get(0).caret();
    }
}
