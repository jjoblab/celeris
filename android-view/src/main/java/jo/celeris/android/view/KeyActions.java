package jo.celeris.android.view;

import android.view.KeyEvent;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextSession;

import org.jspecify.annotations.Nullable;

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
     * L'action d'une frappe matérielle (D-17) : toute frappe clôt la
     * composition d'abord — la composition est une interaction de plage
     * primaire — puis la table de raccourcis de la vue décide ; la
     * frappe inconnue rend null, la vue rend la main à sa classe mère.
     *
     * @param vue     la vue propriétaire du clavier
     * @param liaison la table de raccourcis courante
     * @param clavier l'état de composition IME
     * @param keyCode le code de la touche
     * @param event   l'événement de frappe
     * @return l'action liée, ou null quand la frappe est libre
     */
    static @Nullable EditorAction action(EditorView vue, KeyBindings liaison,
            CompositionState clavier, int keyCode, KeyEvent event) {
        if (clavier.isComposing()) {
            clavier.finish();
            vue.invalidate();
        }
        KeyBinding touche = new KeyBinding(keyCode, event.isShiftPressed(),
                event.isCtrlPressed(), event.isAltPressed());
        return liaison.actionOf(touche);
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
            case DELETE_BACKWARD -> {
                // M7.5 §8 — la paire vide et le cran d'indentation
                // passent par le fournisseur, la main au cœur sinon.
                if (!vue.intelligence.backspaceTyped()) {
                    session.deleteBackward();
                }
            }
            case DELETE_FORWARD -> session.deleteForward();
            case ENTER -> vue.intelligence.enterTyped();
            case UNDO -> session.undo();
            case REDO -> session.redo();
            case SELECT_ALL -> session.setSelection(MultiSelection.of(
                    new SelRange(0, document.length())));
            case ACCEPT_FANTOME -> {
                if (!vue.fantome.surTab()) {
                    return false;
                }
            }
            case ZOOM_IN -> vue.zoom.agrandir();
            case ZOOM_OUT -> vue.zoom.retrecir();
            case ZOOM_RESET -> vue.zoom.retablir(EditorView.POLICE_PAR_DEFAUT.size());
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
