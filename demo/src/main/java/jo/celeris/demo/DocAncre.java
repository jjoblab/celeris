package jo.celeris.demo;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;

import java.util.List;

import jo.celeris.android.view.EditorView;
import jo.celeris.android.widgets.ApercuDefinition;
import jo.celeris.android.widgets.CompletionDocModel;
import jo.celeris.android.widgets.CompletionDocView;
import jo.celeris.android.widgets.PopupAnchor;
import jo.celeris.android.widgets.PopupController;
import jo.celeris.android.widgets.PopupLayer;
import jo.celeris.android.widgets.PopupPlacer;
import jo.celeris.android.view.ReperesPreview;
import jo.celeris.core.syntax.OutlineEntry;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextSession;

import org.jspecify.annotations.Nullable;

/**
 * Le document ancré de l'atelier des popups (ADR-018, chantier C12) :
 * le survol de symbole, l'aperçu rapide de définition (D-11) et le
 * rendu délégué d'un repère {@code @Preview} (C-11) partagent la même
 * carte de documentation, posée sur la couche à l'ancre donnée — au
 * curseur ou au coin de la gouttière, la colonne des repères. Popup
 * ancrée, au point d'intérêt : la déclaration se lit sans quitter le
 * fichier.
 */
final class DocAncre {

    private final Context contexte;
    private final PopupController controleur;
    private final CompletionDocView vue;
    private final PopupLayer couche;
    private final EditorView editeur;
    private final TextSession session;
    private final int margePx;

    /**
     * Construit le poseur de documents ancrés.
     *
     * @param contexte  le contexte — les libellés s'y lisent
     * @param controleur la machine à états des popups
     * @param vue        la carte de documentation partagée
     * @param couche     la couche porte-popups
     * @param editeur    la vue d'éditeur ouverte
     * @param session    la session d'édition courante
     * @param margePx    la respiration des ancres aux bords de la couche
     */
    DocAncre(Context contexte, PopupController controleur,
            CompletionDocView vue, PopupLayer couche, EditorView editeur,
            TextSession session, int margePx) {
        this.contexte = contexte;
        this.controleur = controleur;
        this.vue = vue;
        this.couche = couche;
        this.editeur = editeur;
        this.session = session;
        this.margePx = margePx;
    }

    /**
     * Pose le survol de symbole — null ne pose rien, le modèle sinon
     * s'ancre au curseur.
     *
     * @param doc le modèle du survol, null pour fermer
     */
    void survol(@Nullable CompletionDocModel doc) {
        if (doc == null) {
            return;
        }
        auCurseur(doc);
    }

    /**
     * L'aperçu rapide de la définition du symbole au curseur (D-11) :
     * la déclaration se lit dans la carte ancrée au curseur — sans
     * quitter le fichier. Aucun symbole, aucun aperçu.
     *
     * @param plan  les entrées du plan du fichier ouvert
     * @param lignes les lignes du document ouvert
     */
    void apercu(List<OutlineEntry> plan, List<String> lignes) {
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
     * Le rendu délégué d'un repère {@code @Preview} (C-11) : la scène
     * reste à l'hôte, la réponse se lit dans la carte ancrée au coin de
     * la gouttière — la colonne des repères porte l'ancre.
     *
     * @param ligne   la ligne du repère cliqué dans la gouttière
     * @param reperes les repères du fichier ouvert
     */
    void renduPreview(int ligne,
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
                taille(), couche.getWidth(), couche.getHeight(), margePx));
    }

    /** Ouvre le document au COIN de la gouttière — rendu @Preview. */
    private void auCoin(CompletionDocModel doc) {
        poser(doc, PopupAnchor.topLeftCorner(taille(),
                couche.getWidth(), couche.getHeight(), margePx));
    }

    /** Le document posé : la carte HOVER porte le modèle à l'ancre. */
    private void poser(CompletionDocModel doc, PopupPlacer.Cadre placee) {
        controleur.open(PopupController.PopupKind.HOVER);
        vue.setModel(doc);
        couche.show(PopupController.PopupKind.HOVER, vue,
                placee.x(), placee.y());
    }

    /** La taille mesurée de la carte — l'ancre y anticipe la pose. */
    private PopupPlacer.Cadre taille() {
        vue.measure(View.MeasureSpec.makeMeasureSpec(0,
                        View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0,
                        View.MeasureSpec.UNSPECIFIED));
        return new PopupPlacer.Cadre(0, 0, vue.getMeasuredWidth(),
                vue.getMeasuredHeight());
    }

    private int caret() {
        List<SelRange> plages = session.selection().ranges();
        return plages.isEmpty() ? 0 : plages.get(0).caret();
    }
}
