package jo.celeris.android.host.popups;

import android.content.Context;
import android.graphics.Rect;

import java.util.List;

import jo.celeris.android.host.R;
import jo.celeris.android.view.EditorView;
import jo.celeris.android.view.ReperesPreview;
import jo.celeris.android.widgets.ApercuDefinition;
import jo.celeris.android.widgets.CompletionDocModel;
import jo.celeris.android.widgets.CompletionDocView;
import jo.celeris.android.widgets.PopupAnchor;
import jo.celeris.android.widgets.PopupController;
import jo.celeris.android.widgets.PopupManager;
import jo.celeris.android.widgets.PopupPalette;
import jo.celeris.android.widgets.PopupPlacer;
import jo.celeris.core.syntax.structure.OutlineEntry;

import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextSession;

import org.jspecify.annotations.Nullable;

/**
 * Le document ancré de l'éditeur (ADR-018) : le survol de symbole,
 * l'aperçu rapide de définition et le rendu délégué d'un repère
 * {@code @Preview} partagent le même GENRE de carte de documentation,
 * posée sur la couche à l'ancre donnée — au curseur ou au coin de la
 * gouttière, la colonne des repères. Popup ancrée, au point
 * d'intérêt : la déclaration se lit sans quitter le fichier. La
 * carte vit sous son PROPRE genre survol — la documentation de
 * complétion ne se vole plus entre deux créneaux, l'état ne ment
 * plus.
 */
public final class DocAncre {

    private final Context contexte;
    private final PopupManager popups;
    private final CompletionDocView vue;
    private final EditorView editeur;
    private final TextSession session;
    private final CoachPopups coach;
    private final int margePx;
    /** La session de langage branchée — le survol y passe au protocole. */
    private @Nullable LanguageSession serveur;
    /** L'uri du document ouvert auprès du serveur de langage. */
    private String uriDocument = "";

    /**
     * Construit le poseur de documents ancrés : la carte naît liée à son
     * genre — le survol — par le manager, la palette courante la vêt.
     *
     * @param contexte  le contexte — les libellés s'y lisent
     * @param popups    le coordinateur des popups de la scène
     * @param editeur   la vue d'éditeur ouverte
     * @param session   la session d'édition courante
     * @param margePx   la respiration des ancres aux bords de la couche
     * @param palette   la palette courante des popups
     * @param coach     le coach de l'hôte — le repli du survol sans serveur
     */
    DocAncre(Context contexte, PopupManager popups, EditorView editeur,
            TextSession session, int margePx, PopupPalette palette,
            CoachPopups coach) {
        this.contexte = contexte;
        this.popups = popups;
        this.vue = new CompletionDocView(popups.couche().getContext(),
                palette);
        popups.lier(PopupController.PopupKind.HOVER, vue);
        this.editeur = editeur;
        this.session = session;
        this.coach = coach;
        this.margePx = margePx;
    }

    /**
     * Branche la session de langage — le survol du symbole au curseur y
     * passe au fil du protocole, la table du coach reste le repli.
     *
     * @param serveur la session démarrée, null pour le repli seul
     * @param uri     l'uri du document ouvert
     */
    void brancherServeur(@Nullable LanguageSession serveur, String uri) {
        this.serveur = serveur;
        this.uriDocument = uri;
    }

    /** La palette tourne : la carte du survol la suit. */
    void surPalette(PopupPalette palette) {
        vue.setPalette(palette);
    }

    /**
     * Ouvre le survol du symbole au curseur — le « i » de la barre de
     * sélection y vient : la documentation du serveur de langage par le
     * fil du protocole, la table du coach en repli.
     */
    public void survolAuCaret() {
        String symbole = ContenuPopups.symboleA(session.document(), caret());
        if (symbole == null) {
            return;
        }
        if (serveur != null) {
            serveur.survol(uriDocument, session.document(), symbole, caret())
                    .whenComplete((doc, echec) -> editeur.post(
                            () -> survol(doc)));
            return;
        }
        survol(coach.docDe(symbole));
    }

    /**
     * Le survol AU DOUBLE-TAP : le curseur prend le point du geste —
     * l'ancre du popup de documentation y vit ; vrai quand un symbole
     * s'y trouve, faux quand la cascade du tap suit.
     *
     * @param offset l'offset du double-tap
     * @return vrai quand le survol s'ouvre
     */
    public boolean survolAuDoubleTap(int offset) {
        if (ContenuPopups.symboleA(session.document(), offset) == null) {
            return false;
        }
        session.setSelection(MultiSelection.of(SelRange.caretAt(offset)));
        survolAuCaret();
        return true;
    }

    /**
     * Pose le survol de symbole — null ne pose rien, le modèle sinon
     * s'ancre au curseur.
     *
     * @param doc le modèle du survol, null pour fermer
     */
    public void survol(@Nullable CompletionDocModel doc) {
        if (doc == null) {
            return;
        }
        auCurseur(doc);
    }

    /**
     * L'aperçu rapide de la définition du symbole au curseur : la
     * déclaration se lit dans la carte ancrée au curseur — sans
     * quitter le fichier. Aucun symbole, aucun aperçu.
     *
     * @param plan   les entrées du plan du fichier ouvert
     * @param lignes les lignes du document ouvert
     */
    public void apercu(List<OutlineEntry> plan, List<String> lignes) {
        int curseur = caret();
        ApercuDefinition apercu = ApercuDefinition.de(plan, lignes,
                ApercuDefinition.symboleAu(session.document().text(), curseur),
                curseur);
        if (apercu == null) {
            return;
        }
        auCurseur(CompletionDocModel.of(
                contexte.getString(R.string.titre_apercu, apercu.nom()),
                String.join("\n", apercu.extrait())));
    }

    /**
     * Le rendu délégué d'un repère {@code @Preview} : la scène reste à
     * l'hôte, la réponse se lit dans la carte ancrée au coin de la
     * gouttière — la colonne des repères porte l'ancre.
     *
     * @param ligne   la ligne du repère cliqué dans la gouttière
     * @param reperes les repères du fichier ouvert
     */
    public void renduPreview(int ligne,
            List<ReperesPreview.ReperePreview> reperes) {
        String composable = "";
        for (ReperesPreview.ReperePreview repere : reperes) {
            if (repere.ligne() == ligne) {
                composable = repere.libelle();
                break;
            }
        }
        auCoin(CompletionDocModel.of(
                contexte.getString(R.string.titre_rendu_preview),
                contexte.getString(R.string.message_rendu_preview,
                        composable, ligne + 1)));
    }

    /** Ouvre le document au CURSEUR — survol, aperçu de définition. */
    private void auCurseur(CompletionDocModel doc) {
        Rect curseur = editeur.caretBounds();
        poser(doc, PopupAnchor.atPoint(curseur.left, curseur.top,
                taille(), coucheLargeur(), coucheHauteur(), margePx));
    }

    /** Ouvre le document au COIN de la gouttière — rendu @Preview. */
    private void auCoin(CompletionDocModel doc) {
        poser(doc, PopupAnchor.topLeftCorner(taille(),
                coucheLargeur(), coucheHauteur(), margePx));
    }

    /** Le document posé : la carte HOVER porte le modèle à l'ancre — le
     *  manager tient l'état ET la vue d'un seul tenant. */
    private void poser(CompletionDocModel doc, PopupPlacer.Cadre placee) {
        vue.setModel(doc);
        popups.montrer(PopupController.PopupKind.HOVER,
                placee.x(), placee.y());
    }

    /** La taille mesurée de la carte — l'ancre y anticipe la pose. */
    private PopupPlacer.Cadre taille() {
        return popups.taille(PopupController.PopupKind.HOVER);
    }

    private int coucheLargeur() {
        return popups.couche().getWidth();
    }

    private int coucheHauteur() {
        return popups.couche().getHeight();
    }

    private int caret() {
        List<SelRange> plages = session.selection().ranges();
        return plages.isEmpty() ? 0 : plages.get(0).caret();
    }
}
