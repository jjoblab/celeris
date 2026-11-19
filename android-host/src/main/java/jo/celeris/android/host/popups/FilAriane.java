package jo.celeris.android.host.popups;

import java.util.List;

import jo.celeris.android.widgets.BreadcrumbView;
import jo.celeris.android.widgets.PopupAnchor;
import jo.celeris.android.widgets.PopupController;
import jo.celeris.android.widgets.PopupManager;
import jo.celeris.android.widgets.PopupPalette;
import jo.celeris.android.widgets.PopupPlacer;
import jo.celeris.core.syntax.structure.OutlineEntry;

import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextSession;

/**
 * Le fil d'Ariane de l'éditeur : le chemin du plan qui englobe le
 * curseur, du dehors au dedans, posé au coin haut-gauche de la
 * couche. Le plan s'y abreuvement ({@link #setOutline}), le
 * rafraîchissement suit le curseur ({@link #refresh}) — vide hors
 * de tout symbole, le fil se referme.
 */
final class FilAriane {

    private final PopupManager popups;
    private final TextSession session;
    private final int margePx;
    private final BreadcrumbView vue;

    private List<OutlineEntry> plan = List.of();

    /**
     * Construit le fil d'Ariane sur le coordinateur des popups : la
     * carte naît liée à son genre, la palette courante la vêt.
     *
     * @param popups  le coordinateur des popups de la scène
     * @param session la session d'édition courante
     * @param margePx la respiration des ancres aux bords de la couche
     * @param palette la palette courante des popups
     */
    FilAriane(PopupManager popups, TextSession session, int margePx,
            PopupPalette palette) {
        this.popups = popups;
        this.session = session;
        this.margePx = margePx;
        this.vue = new BreadcrumbView(popups.couche().getContext(),
                palette);
        popups.lier(PopupController.PopupKind.BREADCRUMB, vue);
    }

    /** La palette tourne : la carte du fil la suit. */
    void surPalette(PopupPalette palette) {
        vue.setPalette(palette);
    }

    /** Le plan du fichier ouvert — le fil s'y abreuvement. */
    void setOutline(List<OutlineEntry> entrees) {
        plan = List.copyOf(entrees);
    }

    /** Le fil du curseur : les entrées du plan qui l'englobent, du
     *  dehors au dedans — vide, le fil se referme. */
    void refresh() {
        List<String> chemin = ContenuPopups.pathFor(plan, caret());
        if (chemin.isEmpty()) {
            popups.close(PopupController.PopupKind.BREADCRUMB);
            return;
        }
        popups.controleur().open(PopupController.PopupKind.BREADCRUMB);
        vue.setPath(chemin, popups.couche().getWidth());
        PopupPlacer.Cadre placee = PopupAnchor.topLeftCorner(
                popups.size(PopupController.PopupKind.BREADCRUMB),
                popups.couche().getWidth(),
                popups.couche().getHeight(), margePx);
        popups.show(PopupController.PopupKind.BREADCRUMB,
                placee.x(), placee.y());
    }

    /** Le caret courant — la première plage de sélection, zéro si vide. */
    private int caret() {
        List<SelRange> ranges = session.selection().ranges();
        return ranges.isEmpty() ? 0 : ranges.get(0).caret();
    }
}
