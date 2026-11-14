package jo.celeris.android.host.popups;

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
 * L'info de type (le fil rouge du guide du développeur — la première
 * surface portée de la démonstration à l'orchestrateur,
 * 2026-11-14) : le type du symbole au curseur se lit à la
 * DÉCLARATION par le moteur du cœur ({@link LecteurTypes}) —
 * « Type symbole », hors mots de conduite, JAMAIS un type deviné :
 * sans type lisible, la carte ne se pose pas (aucune fausse
 * oracle). La carte naît liée à son genre, se pose sous le curseur,
 * bornée ; les régimes suivent seuls (frappe, défilement, tap hors
 * cartes, échappement, voile — le manager tient tout, le satellite
 * déclenche).
 */
final class TypeInfo {

    private final EditorView editeur;
    private final TextSession session;
    private final PopupManager popups;
    private final int margePx;
    private final TypeInfoView carte;

    /**
     * Construit l'info de type sur le coordinateur des popups : la
     * carte naît liée à son genre, le patron des trois temps (lier,
     * déclencher, déléguer — USAGE §9).
     *
     * @param editeur la vue d'éditeur ouverte
     * @param session la session d'édition courante
     * @param popups  le coordinateur des popups de la scène
     * @param margePx la respiration des ancres aux bords, en pixels
     * @param palette la palette courante des popups
     */
    TypeInfo(EditorView editeur, TextSession session, PopupManager popups,
            int margePx, PopupPalette palette) {
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
     * Ouvre la carte d'info de type du symbole au curseur : le moteur
     * du cœur lit la déclaration, la carte se pose sous le curseur,
     * bornée. Sans symbole au curseur ou sans type lisible, la carte
     * ne se pose pas.
     */
    void ouvrir() {
        Document document = session.document();
        String symbole = ContenuPopups.symboleA(document, caret());
        String type = symbole == null ? null
                : LecteurTypes.typeDe(document, caret());
        if (type == null) {
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
