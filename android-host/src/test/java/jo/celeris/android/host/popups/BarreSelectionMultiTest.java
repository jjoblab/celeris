package jo.celeris.android.host.popups;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import jo.celeris.android.view.EditorView;
import jo.celeris.android.widgets.PopupLayer;
import jo.celeris.android.widgets.SelectionBarView;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextSession;

/**
 * C13 (l'analyse croisée) : le presse-papiers multi-curseurs de la
 * VOIE TACTILE — la barre de sélection copie tous les segments
 * (joints par saut de ligne) et colle en distribution, la même
 * sémantique que le clavier (une seule vérité, deux voies).
 */
@RunWith(RobolectricTestRunner.class)
public class BarreSelectionMultiTest {

    private static final String DOCUMENT = "alpha beta\nalpha beta\n";

    /** Le montage du banc : orchestrateur sur la session donnée. */
    private static EditorPopups start(TextSession session) {
        Application contexte = RuntimeEnvironment.getApplication();
        EditorView editor = new EditorView(contexte);
        PopupLayer couche = new PopupLayer(contexte);
        editor.setSession(session);
        return new EditorPopups(editor, session, couche,
                jo.celeris.android.view.EditorThemes.DARK, 14f,
                CoachPopups.muet());
    }

    private static SelectionBarView.Item action(
            EditorPopups popups, String label) {
        return popups.barActions().stream()
                .filter(i -> label.equals(i.label())).findFirst()
                .orElseThrow();
    }

    private static ClipboardManager pressePapiers() {
        return (ClipboardManager) RuntimeEnvironment.getApplication()
                .getSystemService(Context.CLIPBOARD_SERVICE);
    }

    /** Copier multi : les segments des DEUX plages se joignent. */
    @Test
    public void copierMultiJointLesSegments() {
        TextSession session = TextSession.of(DOCUMENT, System::nanoTime);
        EditorPopups popups = start(session);
        // « alpha » (0..5) et « beta » (17..21)
        session.setSelection(MultiSelection.of(
                new SelRange(0, 5), new SelRange(17, 21)));
        action(popups, "Copier").action().run();
        assertEquals("alpha\nbeta",
                pressePapiers().getPrimaryClip().getItemAt(0)
                        .coerceToText(RuntimeEnvironment.getApplication())
                        .toString());
        popups.detach();
    }

    /** Coller multi : le clip à deux lignes se distribue sur les
     *  deux plages — la même sémantique que Ctrl+V. */
    @Test
    public void collerMultiDistribueAuxPlages() {
        TextSession session = TextSession.of("x\ny\n", System::nanoTime);
        EditorPopups popups = start(session);
        pressePapiers().setPrimaryClip(ClipData.newPlainText("celeris",
                "un\ndeux"));
        session.setSelection(MultiSelection.of(
                new SelRange(0, 1), new SelRange(2, 3)));
        action(popups, "Coller").action().run();
        assertEquals("un\ndeux\n", session.document().text());
        popups.detach();
    }

    /** Un seul curseur : rien ne change — les curseurs seuls ne
     *  copient rien (le clip reste intact). */
    @Test
    public void lesCurseursSeulsNeCopientRien() {
        TextSession session = TextSession.of(DOCUMENT, System::nanoTime);
        EditorPopups popups = start(session);
        pressePapiers().setPrimaryClip(ClipData.newPlainText("celeris",
                "avant"));
        session.setSelection(MultiSelection.caretsAt(0, 6));
        action(popups, "Copier").action().run();
        assertEquals("le clip n'a pas bougé", "avant",
                pressePapiers().getPrimaryClip().getItemAt(0)
                        .coerceToText(RuntimeEnvironment.getApplication())
                        .toString());
        assertTrue("le document est intact",
                session.document().text().equals(DOCUMENT));
        popups.detach();
    }
}
