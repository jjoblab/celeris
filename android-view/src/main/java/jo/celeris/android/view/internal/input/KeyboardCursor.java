package jo.celeris.android.view.internal.input;

import android.content.Context;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.InputMethodManager;
import jo.celeris.android.view.CaretReveal;
import jo.celeris.android.view.ScrollGeometry;
import jo.celeris.android.view.internal.access.EditorAccess;
import jo.celeris.android.view.internal.accessibility.AccessibilityHost;

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
public final class KeyboardCursor  {

    /** La période de clignotement du curseur, en millisecondes. */
    private static final long PERIODE_CLIGNOTEMENT_MS = 530L;

    private final EditorAccess acces;
    private final Runnable battement = this::surBattement;
    private boolean visible = true;
    /** Vrai tant que le clavier s'est vu demander de se montrer —
     *  l'appui long de sélection le retire (M7.5). Accès de paquet :
     *  le banc y lit l'état sans fenêtre réelle. */
    private boolean keyboardRequested;

    /**
     * Construit le collaborateur de la vue donnée — la vue n'est
     * consultée qu'après sa construction, jamais ici.
     *
     * @param vue la vue propriétaire du curseur et du clavier
     */
    public KeyboardCursor(EditorAccess acces) {
        this.acces = acces;
    }

    /** L'état courant : vrai quand le curseur se trace à cette image. */
    public boolean visible() {
        return visible;
    }

    /**
     * Le TAP (redressement du propriétaire) : le focus se prend, le
     * clavier se dévoile, le curseur se réveille plein. Le doigt POSÉ
     * n'appelle plus ici — le geste qui glisse devient défilement et
     * n'arme rien ; le relâchement sans mouvement, lui seul, ouvre
     * l'IME.
     */
    public void surAppui() {
        acces.requestFocus();
        showKeyboard();
        keyboardRequested = true;
        reveiller();
    }

    /**
     * L'appui long de sélection (M7.5) : le clavier se RETIRE — la
     * sélection se continue au doigt, le clavier n'a rien à faire
     * là ; le curseur clignote toujours.
     */
    public void masquerClavier() {
        InputMethodManager clavier = (InputMethodManager)
                acces.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (clavier != null && acces.getWindowToken() != null) {
            clavier.hideSoftInputFromWindow(acces.getWindowToken(), 0);
        }
        keyboardRequested = false;
    }

    /** Vrai tant que le clavier est demandé montré — accès de paquet, bancs. */
    public boolean keyboardRequested() {
        return keyboardRequested;
    }

    /** Le gain ou la perte de focus démarre ou arrête le battement. */
    public void surFocus(boolean gagne) {
        if (gagne) {
            reveiller();
        } else {
            stop();
        }
    }

    /** L'attachement à la fenêtre : le cycle repart si le focus y est. */
    public void surAttache() {
        if (acces.hasFocus()) {
            reveiller();
        }
    }

    /** Le détachement de la fenêtre : le battement s'arrête net. */
    public void surDetache() {
        acces.removeCallbacks(battement);
    }

    /**
     * La visibilité de la fenêtre : cachée, le battement s'arrête —
     * rien ne se réveille en coulisse ; visible, il repart si le focus
     * y est.
     */
    public void surVisibiliteFenetre(int visibilite) {
        if (visibilite == EditorAccess.VISIBLE && acces.hasFocus()) {
            reveiller();
        } else {
            acces.removeCallbacks(battement);
        }
    }

    /** Réveille le curseur : plein, cycle relancé. */
    public void reveiller() {
        acces.removeCallbacks(battement);
        visible = true;
        acces.postDelayed(battement, PERIODE_CLIGNOTEMENT_MS);
        acces.invalidate();
    }

    /** Le battement : le curseur s'efface puis se montre, image après
     *  image. */
    private void surBattement() {
        visible = !visible;
        acces.invalidate();
        acces.postDelayed(battement, PERIODE_CLIGNOTEMENT_MS);
    }

    /** L'arrêt : le cycle meurt, le curseur redevient plein. */
    private void stop() {
        acces.removeCallbacks(battement);
        visible = true;
    }

    /** Dévoile le clavier logiciel de la vue. */
    private void showKeyboard() {
        InputMethodManager clavier = (InputMethodManager)
                acces.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (clavier != null) {
            clavier.showSoftInput(acces.vue(), InputMethodManager.SHOW_IMPLICIT);
        }
    }

    /**
     * Dévoile le curseur primaire : réajustement minimum du défilement
     * (CaretReveal), puis invalidation.
     */
    public void revealCaret() {
        List<SelRange> ranges = acces.session().selection().ranges();
        int cursor = ranges.isEmpty() ? 0 : ranges.get(0).caret();
        CaretReveal.Revealed devoile = CaretReveal.reveal(cursor,
                acces.session().document(), acces.projection(), acces.metrics(),
                acces.largeurGouttiere(), acces.getWidth(), acces.getHeight(),
                acces.scrollX(), acces.scrollY(),
                Math.round(EditorAccess.MARGE_RETOUR_DP * acces.densite()));
        acces.setScrollX(devoile.scrollX());
        acces.setScrollY(devoile.scrollY());
        acces.invalidate();
    }

    /** Défile d'une page, bornée au contenu. */
    public void defilerPage(boolean enAvant) {
        float contentHeight =
                acces.projection().rowCount() * acces.metrics().lineHeight();
        float pas = ScrollGeometry.visibleRows(acces.getHeight(),
                acces.metrics().lineHeight()) * acces.metrics().lineHeight();
        acces.setScrollY(ScrollGeometry.clamp(
                enAvant ? acces.scrollY() + pas : acces.scrollY() - pas,
                contentHeight, acces.getHeight()));
        acces.invalidate();
    }

    /** Annonce le changement de sélection au lecteur d'écran (G-05). */
    public void annoncerSelection() {
        acces.sendAccessibilityEvent(
                AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED);
    }
}
