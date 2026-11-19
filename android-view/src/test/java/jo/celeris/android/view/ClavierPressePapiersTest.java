package jo.celeris.android.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.KeyEvent;

import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextSession;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * Les actions presse-papiers du CLAVIER (C-12, l'analyse croisée) :
 * Ctrl+C copie, Ctrl+X coupe, Ctrl+V colle — comme tout éditeur de
 * code, sans ouvrir la barre de sélection. Le dépôt de départ sert
 * ces trois raccourcis ; celeris n'en servait AUCUN (et liait même
 * REDO à Ctrl+X : le code 52 est X, pas Y).
 */
@RunWith(RobolectricTestRunner.class)
public class ClavierPressePapiersTest {

    private static final String DOCUMENT = "public class Salut {\n}\n";

    private EditorTestRig banc(String texte) {
        return EditorTestRig.sur(
                RuntimeEnvironment.getApplication(), texte);
    }

    private static ClipboardManager pressePapiers() {
        return (ClipboardManager) RuntimeEnvironment.getApplication()
                .getSystemService(Context.CLIPBOARD_SERVICE);
    }

    private static KeyEvent ctrl(int code) {
        long temps = System.currentTimeMillis();
        return new KeyEvent(temps, temps, KeyEvent.ACTION_DOWN, code, 0,
                KeyEvent.META_CTRL_ON);
    }

    /** Ctrl+C copie la sélection au presse-papiers système, le
     *  document reste intact. */
    @Test
    public void ctrlCCopieLaSelection() {
        EditorTestRig banc = banc(DOCUMENT);
        EditorView vue = banc.vue();
        TextSession session = vue.session();
        session.setSelection(MultiSelection.of(new SelRange(0, 6)));

        assertTrue("Ctrl+C est consommé",
                vue.onKeyDown(KeyEvent.KEYCODE_C, ctrl(KeyEvent.KEYCODE_C)));
        assertEquals("public",
                pressePapiers().getPrimaryClip().getItemAt(0)
                        .coerceToText(RuntimeEnvironment.getApplication())
                        .toString());
        assertEquals("le document est intact", DOCUMENT,
                session.document().text());
    }

    /** Ctrl+X coupe : la sélection part au presse-papiers ET au vide. */
    @Test
    public void ctrlXCoupeLaSelection() {
        EditorTestRig banc = banc(DOCUMENT);
        EditorView vue = banc.vue();
        TextSession session = vue.session();
        session.setSelection(MultiSelection.of(new SelRange(0, 6)));

        assertTrue("Ctrl+X est consommé",
                vue.onKeyDown(KeyEvent.KEYCODE_X, ctrl(KeyEvent.KEYCODE_X)));
        assertEquals("public",
                pressePapiers().getPrimaryClip().getItemAt(0)
                        .coerceToText(RuntimeEnvironment.getApplication())
                        .toString());
        assertEquals("la sélection est retirée du document",
                " class Salut {\n}\n", session.document().text());
    }

    /** Ctrl+V colle le presse-papiers sur la sélection. */
    @Test
    public void ctrlVColleLePressePapiers() {
        EditorTestRig banc = banc(DOCUMENT);
        EditorView vue = banc.vue();
        TextSession session = vue.session();
        pressePapiers().setPrimaryClip(
                ClipData.newPlainText("banc", "[colle]"));
        session.setSelection(MultiSelection.of(new SelRange(0, 6)));

        assertTrue("Ctrl+V est consommé",
                vue.onKeyDown(KeyEvent.KEYCODE_V, ctrl(KeyEvent.KEYCODE_V)));
        assertEquals("[colle] class Salut {\n}\n",
                session.document().text());
    }

    /** Ctrl+Y refait (le bug latent : REDO était lié au code 52,
     *  qui est X — Ctrl+Y ne répondait pas et Ctrl+X entrait en
     *  conflit avec la coupe). */
    @Test
    public void ctrlYRefait() {
        EditorTestRig banc = banc("ab");
        EditorView vue = banc.vue();
        TextSession session = vue.session();
        session.replaceRange(0, 0, "x");
        assertTrue(session.undo());

        assertTrue("Ctrl+Y est consommé",
                vue.onKeyDown(KeyEvent.KEYCODE_Y, ctrl(KeyEvent.KEYCODE_Y)));
        assertEquals("xab", session.document().text());
    }
}
