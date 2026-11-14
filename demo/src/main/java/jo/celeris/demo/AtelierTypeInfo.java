package jo.celeris.demo;

import android.graphics.Rect;

import java.util.List;

import jo.celeris.android.view.EditorView;
import jo.celeris.android.widgets.PopupAnchor;
import jo.celeris.android.widgets.PopupController;
import jo.celeris.android.widgets.PopupManager;
import jo.celeris.android.widgets.PopupPalette;
import jo.celeris.android.widgets.PopupPlacer;
import jo.celeris.android.widgets.TypeInfoView;
import jo.celeris.core.intelligence.LecteurTypes;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextSession;

/**
 * L'atelier d'info de type de la démonstration (catégorie A de
 * l'inventaire — le fil rouge du guide du développeur, la seconde
 * surface réveillée du signal du propriétaire) : la carte du type du
 * symbole au curseur. Le lecteur local lit la DÉCLARATION dans le
 * document — « Type symbole », hors mots de conduite — la carte se
 * pose sous le curseur, bornée ; sans symbole ou sans type lisible,
 * la carte ne se pose pas (aucune fausse oracle). Les régimes suivent
 * seuls (frappe, défilement, tap hors cartes, échappement, voile — le
 * manager tient tout, l'atelier déclenche).
 *
 * <p>La frontière tient : la carte ne connaît NI le lecteur, NI
 * l'éditeur — l'atelier sert d'intermédiaire, le type arrive par
 * {@code setType(...)}, le Runnable du saut n'existe pas (la carte
 * décrit, elle ne choisit pas).</p>
 */
final class AtelierTypeInfo {

    private final EditorView editeur;
    private final TextSession session;
    private final PopupManager popups;
    private final int margePx;
    private final TypeInfoView carte;

    /**
     * Construit l'atelier d'info de type sur le coordinateur des
     * popups : la carte naît liée à son genre, le patron des trois
     * temps (lier, déclencher, déléguer — USAGE §9).
     *
     * @param editeur la vue d'éditeur ouverte
     * @param session la session d'édition courante
     * @param popups  le coordinateur des popups de la scène
     * @param margePx la respiration des ancres aux bords, en pixels
     * @param palette la palette courante des popups
     */
    AtelierTypeInfo(EditorView editeur, TextSession session,
            PopupManager popups, int margePx, PopupPalette palette) {
        this.editeur = editeur;
        this.session = session;
        this.popups = popups;
        this.margePx = margePx;
        this.carte = new TypeInfoView(popups.couche().getContext(),
                palette);
        popups.lier(PopupController.PopupKind.TYPE_INFO, carte);
    }

    /** La palette tourne : la carte d'info de type la suit. */
    void surPalette(PopupPalette palette) {
        carte.setPalette(palette);
    }

    /**
     * Ouvre la carte d'info de type du symbole au curseur : le lecteur
     * lit la déclaration, la carte se pose sous le curseur, bornée.
     * Sans symbole au curseur ou sans type lisible, la carte ne se
     * pose pas.
     */
    void ouvrir() {
        Document document = session.document();
        String symbole = PopupCoach.symboleA(document, caret());
        String type = symbole == null ? null
                : LecteurTypes.typeDe(document, caret());
        if (symbole == null || type == null) {
            return;
        }
        carte.setType(type);
        Rect curseur = editeur.caretBounds();
        PopupPlacer.Cadre placee = PopupAnchor.belowOrAboveCaret(
                curseur.left, curseur.bottom, taille(),
                popups.couche().getWidth(),
                popups.couche().getHeight(), margePx);
        popups.montrer(PopupController.PopupKind.TYPE_INFO,
                placee.x(), placee.y());
    }

    /** La taille mesurée de la carte — l'ancre y anticipe la pose. */
    private PopupPlacer.Cadre taille() {
        return popups.taille(PopupController.PopupKind.TYPE_INFO);
    }

    /** La position du curseur — la tête de la sélection courante. */
    private int caret() {
        List<SelRange> plages = session.selection().ranges();
        return plages.isEmpty() ? 0 : plages.get(0).caret();
    }
}
