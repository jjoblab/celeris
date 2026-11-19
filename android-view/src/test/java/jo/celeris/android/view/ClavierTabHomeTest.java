package jo.celeris.android.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.view.KeyEvent;

import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextSession;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * Tab, Shift+Tab et le Home intelligent au CLAVIER (C1/C2, l'analyse
 * croisée) : Tab indente les lignes de la sélection (Shift+Tab les
 * désindente), Home bascule entre le premier non-blanc et la colonne
 * zéro. Le dépôt de départ sert les trois ; celeris n'en servait
 * aucun.
 */
@RunWith(RobolectricTestRunner.class)
public class ClavierTabHomeTest {

    /** Le banc branché : le fournisseur standard posé (la frappe
     *  intelligente vit au clavier comme dans CelerisEditor). */
    private EditorTestRig bancBranche(String contenu) {
        EditorTestRig banc = EditorTestRig.sur(
                RuntimeEnvironment.getApplication(), contenu);
        banc.vue().intelligence().setProvider(IntelligenceHub.builtins());
        return banc;
    }

    private static KeyEvent touche(int code, int meta) {
        long temps = System.currentTimeMillis();
        return new KeyEvent(temps, temps, KeyEvent.ACTION_DOWN, code, 0,
                meta);
    }

    /** Tab sur une sélection multi-lignes : chaque ligne gagne son
     *  cran, la sélection survit décalée. */
    @Test
    public void tabIndenteLesLignesSelectionnees() {
        EditorTestRig banc = bancBranche("alpha\nbeta\ngamma\n");
        EditorView vue = banc.vue();
        TextSession session = vue.session();
        // sélection des lignes 1-2 (beta..gamma\n)
        session.setSelection(MultiSelection.of(new SelRange(6, 17)));
        assertTrue("Tab est consommé",
                vue.onKeyDown(KeyEvent.KEYCODE_TAB,
                        touche(KeyEvent.KEYCODE_TAB, 0)));
        assertEquals("alpha\n    beta\n    gamma\n",
                session.document().text());
    }

    /** Shift+Tab retire un cran aux lignes sélectionnées. */
    @Test
    public void shiftTabRetireUnCran() {
        EditorTestRig banc = bancBranche(
                "alpha\n        beta\n        gamma\n");
        EditorView vue = banc.vue();
        TextSession session = vue.session();
        session.setSelection(MultiSelection.of(new SelRange(10, 29)));
        assertTrue("Shift+Tab est consommé",
                vue.onKeyDown(KeyEvent.KEYCODE_TAB,
                        touche(KeyEvent.KEYCODE_TAB, KeyEvent.META_SHIFT_ON)));
        assertEquals("alpha\n    beta\n    gamma\n",
                session.document().text());
    }

    /** Home bascule : le premier appel va au premier non-blanc, le
     *  second revient à la colonne zéro. */
    @Test
    public void homeBasculePremierNonBlancPuisColonneZero() {
        EditorTestRig banc = bancBranche("    alpha beta\n");
        EditorView vue = banc.vue();
        TextSession session = vue.session();
        // la frame se prépare d'abord : les mouvements lisent la
        // projection courante de l'image (leçon des bancs de défilement).
        banc.image();
        // caret à la fin de « beta » (colonne 14, ligne 0 — la fin
        // du document vivrait sur la ligne vide finale)
        session.setSelection(MultiSelection.caretsAt(14));
        assertTrue(vue.onKeyDown(KeyEvent.KEYCODE_MOVE_HOME,
                touche(KeyEvent.KEYCODE_MOVE_HOME, 0)));
        assertEquals("le premier Home va au premier non-blanc",
                4, session.selection().carets().get(0).intValue());
        assertTrue(vue.onKeyDown(KeyEvent.KEYCODE_MOVE_HOME,
                touche(KeyEvent.KEYCODE_MOVE_HOME, 0)));
        assertEquals("le second Home revient à la colonne zéro",
                0, session.selection().carets().get(0).intValue());
    }
}
