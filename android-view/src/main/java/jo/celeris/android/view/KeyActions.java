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
        if (action == EditorAction.ZOOM_IN || action == EditorAction.ZOOM_OUT
                || action == EditorAction.ZOOM_RESET) {
            zoom(vue, action);
            vue.invalidate();
            return true;
        }
        if (action == EditorAction.ACCEPT_FANTOME) {
            boolean consomme = servirTab(vue);
            vue.invalidate();
            return consomme || true;
        }
        if (servirPressePapiers(vue, action)) {
            vue.invalidate();
            return true;
        }
        switch (action) {
            case DELETE_BACKWARD, DELETE_FORWARD, ENTER, TAB, UNTAB, UNDO,
                    REDO, SELECT_ALL -> edition(vue, action);
            default -> mouvements(vue, action);
        }
        vue.invalidate();
        return true;
    }

    /**
     * Tab (C1) : le fantôme visible se sert d'abord — sinon il
     * indente les lignes de la sélection (la sémantique du dépôt de
     * départ : la complétion d'abord, l'indentation en repli).
     * L'appel direct à l'édition évite la boucle — jamais appliquer()
     * ne se rappelle.
     */
    private static boolean servirTab(EditorView vue) {
        if (vue.fantome.surTab()) {
            return true;
        }
        edition(vue, EditorAction.TAB);
        return true;
    }

    /**
     * Le presse-papiers clavier (C-12) : COPY, CUT, PASTE —
     * consécutifs dans l'enum — se sert avant la dispatch
     * principale : jamais la géométrie n'y perd son chemin.
     *
     * @return vrai si l'action est une action presse-papiers servie
     */
    private static boolean servirPressePapiers(EditorView vue, EditorAction action) {
        if (action.ordinal() < EditorAction.COPY.ordinal()
                || action.ordinal() > EditorAction.PASTE.ordinal()) {
            return false;
        }
        PressePapiersClavier.servir(vue, action);
        return true;
    }

    /** Le zoom clavier (A-18) : la taille suit la table de la vue. */
    private static void zoom(EditorView vue, EditorAction action) {
        switch (action) {
            case ZOOM_IN -> vue.zoom.agrandir();
            case ZOOM_OUT -> vue.zoom.retrecir();
            default -> vue.zoom.retablir(EditorView.POLICE_PAR_DEFAUT.size());
        }
    }

    /** Les actions d'édition : suppression, entrée, historique,
     *  sélection entière et presse-papiers (C-12) — le cœur de la
     *  session, jamais la géométrie. Toute édition qui bouge le
     *  caret le DÉVOILE ensuite (le retour de terrain du
     *  2026-11-20 : les DEL enchaînés au clavier faisaient remonter
     *  le caret à la ligne fusionnée SANS suivre au défilement — la
     *  ligne disparaissait du viewport jusqu'à la prochaine touche
     *  servie par l'intelligence, qui elle révèle ; la voie clavier
     *  révèle désormais comme toutes les autres). */
    private static void edition(EditorView vue, EditorAction action) {
        TextSession session = vue.session();
        Document document = session.document();
        boolean caretABouge = true;
        switch (action) {
            case DELETE_BACKWARD -> {
                // M7.5 §8 — la paire vide et le cran d'indentation
                // passent par le fournisseur, la main au cœur sinon.
                if (!vue.intelligence.backspaceTyped()) {
                    session.deleteBackward();
                }
            }
            case DELETE_FORWARD -> {
                // C8 : la paire vide passe par le fournisseur, la
                // main au cœur sinon.
                if (!vue.intelligence.deleteForwardTyped()) {
                    session.deleteForward();
                }
            }
            case ENTER -> vue.intelligence.enterTyped();
            case TAB -> vue.intelligence.tabTyped();
            case UNTAB -> vue.intelligence.untabTyped();
            case UNDO -> session.undo();
            case REDO -> session.redo();
            case SELECT_ALL -> {
                session.setSelection(MultiSelection.of(
                        new SelRange(0, document.length())));
                caretABouge = false;
            }
            default -> caretABouge = false;
        }
        if (caretABouge) {
            vue.curseurClavier.revealCaret();
        }
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
