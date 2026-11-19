package jo.celeris.android.host.popups;

import android.graphics.Rect;

import jo.celeris.android.view.EditorView;
import jo.celeris.android.view.ColorChip;
import jo.celeris.android.view.ColorChips;
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
public final class ColorPickerLogic {

    private final EditorView editor;
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
    private int digits;

    /**
     * Construit le sélecteur sur le coordinateur des popups.
     *
     * @param editor la vue d'éditeur ouverte
     * @param session la session d'édition courante
     * @param popups  le coordinateur des popups de la scène
     * @param margePx la respiration des ancres aux bords, en pixels
     * @param palette la palette courante des popups
     */
    ColorPickerLogic(EditorView editor, TextSession session,
            PopupManager popups, int margePx, PopupPalette palette) {
        this.editor = editor;
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
        int line = document.lineForOffset(offset);
        int column = offset - document.lineStart(line);
        for (ColorChip chip : ColorChips.tous(line,
                document.lineText(line))) {
            int start = chip.column() - 1;
            if (column < start || column >= start + chip.length()) {
                continue;
            }
            debutLitteral = document.lineStart(line) + start;
            finLitteral = debutLitteral + chip.length();
            alphaOrigine = chip.argb() >>> 24;
            digits = chip.length() - 1;
            selecteur.open(chip.argb(), digits);
            Rect cursor = editor.caretBounds();
            PopupPlacer.Cadre placee = PopupAnchor.belowOrAboveCaret(
                    cursor.left, cursor.bottom, size(),
                    popups.couche().getWidth(),
                    popups.couche().getHeight(), margePx);
            popups.show(PopupController.PopupKind.COLOR_PICKER,
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
        String litteral = ColorChips.formater(compose, digits);
        popups.close(PopupController.PopupKind.COLOR_PICKER);
        if (session.replaceRange(debutLitteral, finLitteral, litteral)) {
            session.setSelection(MultiSelection.of(
                    SelRange.caretAt(debutLitteral + litteral.length())));
        }
    }

    /** La taille mesurée de la carte — l'ancre y prend sa taille. */
    private PopupPlacer.Cadre size() {
        return popups.size(PopupController.PopupKind.COLOR_PICKER);
    }
}
