package jo.celeris.android.view;

import android.content.Context;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.InputMethodManager;

import java.util.List;

import jo.celeris.core.text.SelRange;

/**
 * Le collaborateur du curseur et du clavier : l'appui qui prend le
 * focus ouvre l'IME — sans appel explicite, une vue focusable au
 * toucher n'ouvre jamais le clavier — et le curseur d'écriture
 * clignote au rythme de la plateforme tant que la vue tient le focus,
 * visible à l'écran.
 *
 * <p>Vue-collaborateur par délégation (même famille qu'
 * {@link AccessibilityHost}) : la vue reste la racine de composition,
 * les rappels de cycle de vie y restent minces, toute la mécanique
 * vit ici. Le curseur se réveille plein à chaque déplacement ou
 * édition — la vue le lui dit par {@link #surAppui()} et
 * {@link #reveiller()}.</p>
 */
final class CurseurClavier {

    /** La période de clignotement du curseur, en millisecondes. */
    private static final long PERIODE_CLIGNOTEMENT_MS = 530L;

    private final EditorView vue;
    private final Runnable battement = this::surBattement;
    private boolean visible = true;

    /**
     * Construit le collaborateur de la vue donnée — la vue n'est
     * consultée qu'après sa construction, jamais ici.
     *
     * @param vue la vue propriétaire du curseur et du clavier
     */
    CurseurClavier(EditorView vue) {
        this.vue = vue;
    }

    /** L'état courant : vrai quand le curseur se trace à cette image. */
    boolean visible() {
        return visible;
    }

    /**
     * L'appui : le focus se prend, le clavier se dévoile, le curseur
     * se réveille plein.
     */
    void surAppui() {
        vue.requestFocus();
        montrerClavier();
        reveiller();
    }

    /** Le gain ou la perte de focus démarre ou arrête le battement. */
    void surFocus(boolean gagne) {
        if (gagne) {
            reveiller();
        } else {
            arreter();
        }
    }

    /** L'attachement à la fenêtre : le cycle repart si le focus y est. */
    void surAttache() {
        if (vue.hasFocus()) {
            reveiller();
        }
    }

    /** Le détachement de la fenêtre : le battement s'arrête net. */
    void surDetache() {
        vue.removeCallbacks(battement);
    }

    /**
     * La visibilité de la fenêtre : cachée, le battement s'arrête —
     * rien ne se réveille en coulisse ; visible, il repart si le focus
     * y est.
     */
    void surVisibiliteFenetre(int visibilite) {
        if (visibilite == EditorView.VISIBLE && vue.hasFocus()) {
            reveiller();
        } else {
            vue.removeCallbacks(battement);
        }
    }

    /** Réveille le curseur : plein, cycle relancé. */
    void reveiller() {
        vue.removeCallbacks(battement);
        visible = true;
        vue.postDelayed(battement, PERIODE_CLIGNOTEMENT_MS);
        vue.invalidate();
    }

    /** Le battement : le curseur s'efface puis se montre, image après
     *  image. */
    private void surBattement() {
        visible = !visible;
        vue.invalidate();
        vue.postDelayed(battement, PERIODE_CLIGNOTEMENT_MS);
    }

    /** L'arrêt : le cycle meurt, le curseur redevient plein. */
    private void arreter() {
        vue.removeCallbacks(battement);
        visible = true;
    }

    /** Dévoile le clavier logiciel de la vue. */
    private void montrerClavier() {
        InputMethodManager clavier = (InputMethodManager)
                vue.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (clavier != null) {
            clavier.showSoftInput(vue, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    /**
     * Dévoile le curseur primaire : réajustement minimum du défilement
     * (CaretReveal), puis invalidation.
     */
    void revealCaret() {
        List<SelRange> plages = vue.session().selection().ranges();
        int curseur = plages.isEmpty() ? 0 : plages.get(0).caret();
        CaretReveal.Revealed devoile = CaretReveal.reveal(curseur,
                vue.session().document(), vue.projection, vue.metrics,
                vue.largeurGouttiere, vue.getWidth(), vue.getHeight(),
                vue.scrollX, vue.scrollY,
                Math.round(EditorView.MARGE_RETOUR_DP * vue.densite()));
        vue.scrollX = devoile.scrollX();
        vue.scrollY = devoile.scrollY();
        vue.invalidate();
    }

    /** Défile d'une page, bornée au contenu. */
    void defilerPage(boolean enAvant) {
        float hauteurContenu =
                vue.projection.rowCount() * vue.metrics.lineHeight();
        float pas = ScrollGeometry.visibleRows(vue.getHeight(),
                vue.metrics.lineHeight()) * vue.metrics.lineHeight();
        vue.scrollY = ScrollGeometry.clamp(
                enAvant ? vue.scrollY + pas : vue.scrollY - pas,
                hauteurContenu, vue.getHeight());
        vue.invalidate();
    }

    /** Annonce le changement de sélection au lecteur d'écran (G-05). */
    void annoncerSelection() {
        vue.sendAccessibilityEvent(
                AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED);
    }
}
