package jo.celeris.android.host.popups;

import android.graphics.Rect;

import jo.celeris.android.view.EditorView;
import jo.celeris.android.view.PastilleCouleur;
import jo.celeris.android.view.PastillesCouleur;
import jo.celeris.android.widgets.ColorPickerView;
import jo.celeris.android.widgets.PopupAnchor;
import jo.celeris.android.widgets.PopupController;
import jo.celeris.android.widgets.PopupManager;
import jo.celeris.android.widgets.PopupPalette;
import jo.celeris.android.widgets.PopupPlacer;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextSession;

/**
 * Le sélecteur de couleur de l'éditeur : le double-tap sur un littéral
 * de couleur ouvre la carte du kit — champ de saturation, bande de
 * teinte, palette Material — et le pick APPLIQUE implicite : l'alpha
 * d'origine survit, le littéral reformaté remplace l'ancien EN PLACE,
 * à SA largeur — le code environnant ne bouge pas. Le coordinateur
 * tient la carte liée à son genre, le tap HORS CARTES la referme
 * (l'annulation enfin tenue, comme la documentation le promettait).
 */
public final class SelecteurCouleur {

    private final EditorView editeur;
    private final TextSession session;
    private final PopupManager popups;
    private final int margePx;
    private final ColorPickerView selecteur;

    /** Le littéral en édition : sa borne de début, sa fin. */
    private int debutLitteral;
    private int finLitteral;
    /** L'alpha du littéral d'origine — il survit au pick. */
    private int alphaOrigine;
    /** La largeur du littéral d'origine, en chiffres sans le dièse. */
    private int chiffres;

    /**
     * Construit le sélecteur sur le coordinateur des popups.
     *
     * @param editeur la vue d'éditeur ouverte
     * @param session la session d'édition courante
     * @param popups  le coordinateur des popups de la scène
     * @param margePx la respiration des ancres aux bords, en pixels
     * @param palette la palette courante des popups
     */
    SelecteurCouleur(EditorView editeur, TextSession session,
            PopupManager popups, int margePx, PopupPalette palette) {
        this.editeur = editeur;
        this.session = session;
        this.popups = popups;
        this.margePx = margePx;
        this.selecteur = new ColorPickerView(
                popups.couche().getContext(), palette);
        popups.lier(PopupController.PopupKind.COLOR_PICKER, selecteur);
        selecteur.setOnAppliquer(this::appliquer);
    }

    /** La palette tourne : le verre du sélecteur suit le thème. */
    void surPalette(PopupPalette palette) {
        selecteur.setPalette(palette);
    }

    /**
     * Ouvre le sélecteur au double-tap : vrai quand l'offset vise un
     * littéral de couleur (le geste est consommé, la carte s'ouvre sous
     * la ligne), faux quand il n'en vise aucun (le tap retombe dans la
     * cascade du simple).
     *
     * @param offset l'offset visé par le double-tap
     * @return vrai quand le sélecteur s'est ouvert
     */
    boolean ouvrirAuDoubleTap(int offset) {
        Document document = session.document();
        int ligne = document.lineForOffset(offset);
        int colonne = offset - document.lineStart(ligne);
        for (PastilleCouleur pastille : PastillesCouleur.tous(ligne,
                document.lineText(ligne))) {
            int debut = pastille.colonne() - 1;
            if (colonne < debut || colonne >= debut + pastille.longueur()) {
                continue;
            }
            debutLitteral = document.lineStart(ligne) + debut;
            finLitteral = debutLitteral + pastille.longueur();
            alphaOrigine = pastille.argb() >>> 24;
            chiffres = pastille.longueur() - 1;
            selecteur.ouvrir(pastille.argb(), chiffres);
            Rect curseur = editeur.caretBounds();
            PopupPlacer.Cadre placee = PopupAnchor.belowOrAboveCaret(
                    curseur.left, curseur.bottom, taille(),
                    popups.couche().getWidth(),
                    popups.couche().getHeight(), margePx);
            popups.montrer(PopupController.PopupKind.COLOR_PICKER,
                    placee.x(), placee.y());
            return true;
        }
        return false;
    }

    /**
     * Le pick s'applique : l'alpha d'origine compose la teinte choisie,
     * le littéral reformaté remplace l'ancien en place — la largeur
     * tient, le curseur finit au bout du littéral neuf.
     */
    private void appliquer(int teinte) {
        int compose = (alphaOrigine << 24) | (teinte & 0xFFFFFF);
        String litteral = PastillesCouleur.formater(compose, chiffres);
        popups.fermer(PopupController.PopupKind.COLOR_PICKER);
        if (session.replaceRange(debutLitteral, finLitteral, litteral)) {
            session.setSelection(MultiSelection.of(
                    SelRange.caretAt(debutLitteral + litteral.length())));
        }
    }

    /** La taille mesurée de la carte — l'ancre y prend sa taille. */
    private PopupPlacer.Cadre taille() {
        return popups.taille(PopupController.PopupKind.COLOR_PICKER);
    }
}
