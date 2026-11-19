package jo.celeris.android.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.view.KeyEvent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextSession;

/**
 * C13 (l'analyse croisée) : le presse-papiers multi-curseurs — tous
 * les curseurs copient (segments joints par saut de ligne) et collent
 * (distribués si le clip a autant de lignes que de curseurs, sinon le
 * clip entier sur chaque curseur) — la parité avec le dépôt de
 * départ, la même sémantique que les éditeurs modernes.
 */
@RunWith(RobolectricTestRunner.class)
public class PressePapiersMultiTest {

    private static final String DOCUMENT = "alpha beta\nalpha beta\nalpha beta\n";

    private EditorTestRig banc(String text) {
        EditorTestRig banc = EditorTestRig.on(RuntimeEnvironment.getApplication(), text);
        return banc;
    }

    private String clip() {
        return pressePapiers().getPrimaryClip().getItemAt(0)
                .coerceToText(RuntimeEnvironment.getApplication()).toString();
    }

    private android.content.ClipboardManager pressePapiers() {
        return (android.content.ClipboardManager) RuntimeEnvironment.getApplication()
                .getSystemService(android.content.Context.CLIPBOARD_SERVICE);
    }

    private KeyEvent ctrl(int code) {
        return new KeyEvent(0, 0, KeyEvent.ACTION_DOWN, code, 0,
                KeyEvent.META_CTRL_ON);
    }

    /** Ctrl+C multi : les DEUX plages copient, jointes par \n. */
    @Test
    public void ctrlCMultiCopieToutesLesPlages() {
        EditorTestRig banc = banc(DOCUMENT);
        EditorView vue = banc.vue();
        TextSession session = vue.session();
        // deux plages : « alpha » (0..5) ligne 1 et « beta » (17..21) ligne 2
        session.setSelection(MultiSelection.of(
                new SelRange(0, 5), new SelRange(17, 21)));
        assertTrue(vue.onKeyDown(KeyEvent.KEYCODE_C, ctrl(KeyEvent.KEYCODE_C)));
        assertEquals("les segments se joignent par saut de ligne",
                "alpha\nbeta", clip());
    }

    /** Ctrl+X multi : les DEUX plages partent au presse-papiers (jointes)
     *  ET au vide. */
    @Test
    public void ctrlXMultiCoupeToutesLesPlages() {
        EditorTestRig banc = banc(DOCUMENT);
        EditorView vue = banc.vue();
        TextSession session = vue.session();
        session.setSelection(MultiSelection.of(
                new SelRange(0, 5), new SelRange(17, 21)));
        assertTrue(vue.onKeyDown(KeyEvent.KEYCODE_X, ctrl(KeyEvent.KEYCODE_X)));
        assertEquals("alpha\nbeta", clip());
        // « alpha » et « beta » partis : les deux lignes raccourcissent
        assertEquals("les deux segments partent",
                " beta\nalpha \nalpha beta\n", session.document().text());
    }

    /** Ctrl+V distribué : un clip à autant de LIGNES que de curseurs
     *  se distribue — ligne i sur curseur i. */
    @Test
    public void ctrlVDistribueAuxCurseurs() {
        EditorTestRig banc = banc("x\ny\n");
        EditorView vue = banc.vue();
        TextSession session = vue.session();
        pressePapiers().setPrimaryClip(android.content.ClipData.newPlainText(
                "celeris", "un\ndeux"));
        // deux curseurs réduits, sur les « x » et « y »
        session.setSelection(MultiSelection.of(
                new SelRange(0, 1), new SelRange(2, 3)));
        assertTrue(vue.onKeyDown(KeyEvent.KEYCODE_V, ctrl(KeyEvent.KEYCODE_V)));
        assertEquals("la ligne i va au curseur i",
                "un\ndeux\n", session.document().text());
    }

    /** Ctrl+V en masse : un clip SANS distribution (une seule ligne
     *  pour deux curseurs) se colle ENTIER sur chaque curseur. */
    @Test
    public void ctrlVEnMasseSurChaqueCurseur() {
        EditorTestRig banc = banc("x\ny\n");
        EditorView vue = banc.vue();
        TextSession session = vue.session();
        pressePapiers().setPrimaryClip(android.content.ClipData.newPlainText(
                "celeris", "ab"));
        session.setSelection(MultiSelection.of(
                new SelRange(0, 1), new SelRange(2, 3)));
        assertTrue(vue.onKeyDown(KeyEvent.KEYCODE_V, ctrl(KeyEvent.KEYCODE_V)));
        assertEquals("le clip entier sur chaque curseur",
                "ab\nab\n", session.document().text());
    }

    /** Un seul curseur, un clip multi-lignes : comportement classique,
     *  tout se colle sur LE curseur (pas de distribution). */
    @Test
    public void unCurseurColleToutLeClip() {
        EditorTestRig banc = banc("x\n");
        EditorView vue = banc.vue();
        TextSession session = vue.session();
        pressePapiers().setPrimaryClip(android.content.ClipData.newPlainText(
                "celeris", "un\ndeux"));
        session.setSelection(MultiSelection.of(new SelRange(0, 1)));
        assertTrue(vue.onKeyDown(KeyEvent.KEYCODE_V, ctrl(KeyEvent.KEYCODE_V)));
        assertEquals("un seul curseur reçoit tout",
                "un\ndeux\n", session.document().text());
    }

    /** Les curseurs SEULS ne copient rien (pas de plage vide au clip). */
    @Test
    public void lesCurseursSeulsNeCopientRien() {
        EditorTestRig banc = banc(DOCUMENT);
        EditorView vue = banc.vue();
        vue.session().setSelection(MultiSelection.caretsAt(0, 6));
        pressePapiers().setPrimaryClip(android.content.ClipData.newPlainText(
                "celeris", "avant"));
        vue.onKeyDown(KeyEvent.KEYCODE_C, ctrl(KeyEvent.KEYCODE_C));
        assertEquals("rien n'a changé", "avant", clip());
    }
}
