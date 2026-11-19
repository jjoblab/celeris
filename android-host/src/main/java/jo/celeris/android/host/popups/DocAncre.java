package jo.celeris.android.host.popups;

import android.content.Context;
import android.graphics.Rect;

import java.util.List;

import jo.celeris.android.host.R;
import jo.celeris.android.view.EditorView;
import jo.celeris.android.view.PreviewMarks;
import jo.celeris.android.widgets.DefinitionPreview;
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
    private final EditorView editor;
    private final TextSession session;
    private final CoachPopups coach;
    private final int margePx;
    /** La session de langage branchée — le survol y passe au protocole. */
    private @Nullable LanguageSession server;
    /** L'uri du document ouvert auprès du serveur de langage. */
    private String uriDocument = "";

    /**
     * Construit le poseur de documents ancrés : la carte naît liée à son
     * genre — le survol — par le manager, la palette courante la vêt.
     *
     * @param contexte  le contexte — les libellés s'y lisent
     * @param popups    le coordinateur des popups de la scène
     * @param editor   la vue d'éditeur ouverte
     * @param session   la session d'édition courante
     * @param margePx   la respiration des ancres aux bords de la couche
     * @param palette   la palette courante des popups
     * @param coach     le coach de l'hôte — le repli du survol sans serveur
     */
    DocAncre(Context contexte, PopupManager popups, EditorView editor,
            TextSession session, int margePx, PopupPalette palette,
            CoachPopups coach) {
        this.contexte = contexte;
        this.popups = popups;
        this.vue = new CompletionDocView(popups.couche().getContext(),
                palette);
        popups.lier(PopupController.PopupKind.HOVER, vue);
        this.editor = editor;
        this.session = session;
        this.coach = coach;
        this.margePx = margePx;
    }

    /**
     * Branche la session de langage — le survol du symbole au curseur y
     * passe au fil du protocole, la table du coach reste le repli.
     *
     * @param server la session démarrée, null pour le repli seul
     * @param uri     l'uri du document ouvert
     */
    void connectServer(@Nullable LanguageSession server, String uri) {
        this.server = server;
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
        if (server != null) {
            server.hover(uriDocument, session.document(), symbole, caret())
                    .whenComplete((doc, echec) -> editor.post(
                            () -> hover(doc)));
            return;
        }
        hover(coach.docDe(symbole));
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
    public void hover(@Nullable CompletionDocModel doc) {
        if (doc == null) {
            return;
        }
        atCaret(doc);
    }

    /**
     * L'aperçu rapide de la définition du symbole au curseur : la
     * déclaration se lit dans la carte ancrée au curseur — sans
     * quitter le fichier. Aucun symbole, aucun aperçu.
     *
     * @param plan   les entrées du plan du fichier ouvert
     * @param lines les lignes du document ouvert
     */
    public void preview(List<OutlineEntry> plan, List<String> lines) {
        int cursor = caret();
        DefinitionPreview preview = DefinitionPreview.of(plan, lines,
                DefinitionPreview.symboleAu(session.document().text(), cursor),
                cursor);
        if (preview == null) {
            return;
        }
        atCaret(CompletionDocModel.of(
                contexte.getString(R.string.celeris_titre_apercu, preview.nom()),
                String.join("\n", preview.extrait())));
    }

    /**
     * Le rendu délégué d'un repère {@code @Preview} : la scène reste à
     * l'hôte, la réponse se lit dans la carte ancrée au coin de la
     * gouttière — la colonne des repères porte l'ancre.
     *
     * @param line   la ligne du repère cliqué dans la gouttière
     * @param marks les repères du fichier ouvert
     */
    public void renduPreview(int line,
            List<PreviewMarks.PreviewMark> marks) {
        String composable = "";
        for (PreviewMarks.PreviewMark repere : marks) {
            if (repere.line() == line) {
                composable = repere.label();
                break;
            }
        }
        auCoin(CompletionDocModel.of(
                contexte.getString(R.string.celeris_titre_rendu_preview),
                contexte.getString(R.string.celeris_message_rendu_preview,
                        composable, line + 1)));
    }

    /** Ouvre le document au CURSEUR — survol, aperçu de définition. */
    private void atCaret(CompletionDocModel doc) {
        Rect cursor = editor.caretBounds();
        place(doc, PopupAnchor.atPoint(cursor.left, cursor.top,
                size(), layerWidth(), layerHeight(), margePx));
    }

    /** Ouvre le document au COIN de la gouttière — rendu @Preview. */
    private void auCoin(CompletionDocModel doc) {
        place(doc, PopupAnchor.topLeftCorner(size(),
                layerWidth(), layerHeight(), margePx));
    }

    /** Le document posé : la carte HOVER porte le modèle à l'ancre — le
     *  manager tient l'état ET la vue d'un seul tenant. */
    private void place(CompletionDocModel doc, PopupPlacer.Cadre placee) {
        vue.setModel(doc);
        popups.show(PopupController.PopupKind.HOVER,
                placee.x(), placee.y());
    }

    /** La taille mesurée de la carte — l'ancre y anticipe la pose. */
    private PopupPlacer.Cadre size() {
        return popups.size(PopupController.PopupKind.HOVER);
    }

    private int layerWidth() {
        return popups.couche().getWidth();
    }

    private int layerHeight() {
        return popups.couche().getHeight();
    }

    private int caret() {
        List<SelRange> ranges = session.selection().ranges();
        return ranges.isEmpty() ? 0 : ranges.get(0).caret();
    }
}
