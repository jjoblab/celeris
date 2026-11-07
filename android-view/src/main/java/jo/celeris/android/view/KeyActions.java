package jo.celeris.android.view;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextSession;

/**
 * L'application des actions clavier sur la session (D-17) : les
 * suppressions, l'annulation et le rétablissement passent au pilote de
 * session, les mouvements au moteur de déplacement ({@link CaretsMotion}),
 * tout sélectionner sur le document entier. Classe extraite de la vue :
 * la complexité du grand aiguillage quitte la classe d'assemblage
 * (ADR-002 — plafonds de complexité).
 */
final class KeyActions {

    private KeyActions() {
    }

    /**
     * Applique l'action sur la session de la vue.
     *
     * @param vue    la vue propriétaire de la session
     * @param action l'action clavier demandée
     * @return vrai si l'action est consommée
     */
    static boolean appliquer(EditorView vue, EditorAction action) {
        TextSession session = vue.session();
        Document document = session.document();
        switch (action) {
            case DELETE_BACKWARD -> session.deleteBackward();
            case DELETE_FORWARD -> session.deleteForward();
            case UNDO -> session.undo();
            case REDO -> session.redo();
            case SELECT_ALL -> session.setSelection(MultiSelection.of(
                    new SelRange(0, document.length())));
            case ACCEPT_FANTOME -> {
                if (!vue.fantome.surTab()) {
                    return false;
                }
            }
            default -> mouvements(vue, action);
        }
        vue.invalidate();
        return true;
    }

    /** Les mouvements et sélections : la projection et la fenêtre courantes. */
    private static void mouvements(EditorView vue, EditorAction action) {
        FrameState etat = vue.etatImageCourante();
        int pageRows = ScrollGeometry.visibleRows(vue.getHeight(),
                etat.metrics().lineHeight());
        vue.session().setSelection(CaretsMotion.move(etat.document(),
                etat.projection(), vue.session().selection(), action, pageRows));
    }
}
