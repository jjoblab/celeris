package jo.celeris.demo;

import android.graphics.Rect;

import jo.celeris.android.view.EditorView;
import jo.celeris.android.view.PastilleCouleur;
import jo.celeris.android.view.PastillesCouleur;
import jo.celeris.android.widgets.ColorPickerView;
import jo.celeris.android.widgets.PopupAnchor;
import jo.celeris.android.widgets.PopupController;
import jo.celeris.android.widgets.PopupLayer;
import jo.celeris.android.widgets.PopupPalette;
import jo.celeris.android.widgets.PopupPlacer;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextSession;

/**
 * L'atelier des couleurs de la démonstration (le signal du
 * propriétaire du 2026-11-13 : « ajouter aussi le double clic dessus
 * pour afficher le popups couleur picker ») : le double-tap sur un
 * littéral de couleur ouvre le sélecteur du kit — la carte du dépôt de
 * départ, champ de saturation, bande de teinte, palette Material — et
 * le pick APPLIQUE implicite : l'alpha d'origine survit, le littéral
 * reformaté remplace l'ancien EN PLACE, à SA largeur — le code
 * environnant ne bouge pas.
 */
final class AtelierCouleur {

    private final EditorView editeur;
    private final TextSession session;
    private final PopupController controleur;
    private final PopupLayer couche;
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
     * Construit l'atelier des couleurs.
     *
     * @param editeur   la vue d'éditeur ouverte
     * @param session   la session d'édition courante
     * @param controleur la machine à états des popups
     * @param couche    la couche porte-popups posée sur l'éditeur
     * @param margePx   la respiration des ancres aux bords, en pixels
     * @param palette   la palette courante des popups
     */
    AtelierCouleur(EditorView editeur, TextSession session,
            PopupController controleur, PopupLayer couche, int margePx,
            PopupPalette palette) {
        this.editeur = editeur;
        this.session = session;
        this.controleur = controleur;
        this.couche = couche;
        this.margePx = margePx;
        this.selecteur = new ColorPickerView(couche.getContext(), palette);
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
            controleur.open(PopupController.PopupKind.COLOR_PICKER);
            selecteur.ouvrir(pastille.argb(), chiffres);
            Rect curseur = editeur.caretBounds();
            PopupPlacer.Cadre placee = PopupAnchor.belowOrAboveCaret(
                    curseur.left, curseur.bottom, taille(selecteur),
                    couche.getWidth(), couche.getHeight(), margePx);
            couche.show(PopupController.PopupKind.COLOR_PICKER, selecteur,
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
        controleur.close(PopupController.PopupKind.COLOR_PICKER);
        couche.dismiss(PopupController.PopupKind.COLOR_PICKER);
        if (session.replaceRange(debutLitteral, finLitteral, litteral)) {
            session.setSelection(MultiSelection.of(
                    SelRange.caretAt(debutLitteral + litteral.length())));
        }
    }

    /** La carte mesurée sans contrainte — son ancre y prend sa taille. */
    private static PopupPlacer.Cadre taille(ColorPickerView vue) {
        vue.measure(android.view.View.MeasureSpec.makeMeasureSpec(0,
                        android.view.View.MeasureSpec.UNSPECIFIED),
                android.view.View.MeasureSpec.makeMeasureSpec(0,
                        android.view.View.MeasureSpec.UNSPECIFIED));
        return new PopupPlacer.Cadre(0, 0, vue.getMeasuredWidth(),
                vue.getMeasuredHeight());
    }
}
