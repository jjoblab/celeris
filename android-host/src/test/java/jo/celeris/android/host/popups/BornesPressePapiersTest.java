package jo.celeris.android.host.popups;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import jo.celeris.android.view.EditorView;
import jo.celeris.android.widgets.PopupLayer;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextSession;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * Les bornes Binder du presse-papiers (A-20, l'analyse croisée) : la
 * transaction IPC d'Android plafonne ~1 Mo — copier un document monstre
 * tuait l'application (TransactionTooLargeException). Le dépôt de
 * départ borne à 200 000 caractères la copie et à 4 096 le texte de
 * sélection servi à l'IME ; celeris ne bornait RIEN.
 */
@RunWith(RobolectricTestRunner.class)
public class BornesPressePapiersTest {

    /** Le montage du banc : vue, session, couche, orchestrateur. */
    private static EditorPopups start(TextSession session) {
        Application contexte = RuntimeEnvironment.getApplication();
        EditorView editor = new EditorView(contexte);
        PopupLayer couche = new PopupLayer(contexte);
        editor.setSession(session);
        return new EditorPopups(editor, session, couche,
                jo.celeris.android.view.EditorThemes.DARK, 14f,
                CoachPopups.muet());
    }

    private static ClipboardManager pressePapiers() {
        return (ClipboardManager) RuntimeEnvironment.getApplication()
                .getSystemService(Context.CLIPBOARD_SERVICE);
    }

    /** « Tout copier » sur un document monstre est BORNÉ : le
     *  presse-papiers reçoit les 200 000 premiers caractères, pas la
     *  transaction monstre qui tue le processus (le dépôt de départ
     *  borne pareil, EditorClipboard). */
    @Test
    public void toutCopierEstBorneSurUnDocumentMonstre() {
        String monstre = "a".repeat(500_000);
        TextSession session = TextSession.of(monstre, System::nanoTime);
        EditorPopups popups = start(session);
        session.setSelection(MultiSelection.of(new SelRange(0, 1)));

        popups.barActions().stream()
                .filter(i -> "Tout copier".equals(i.label()))
                .findFirst().orElseThrow().action().run();

        ClipData clip = pressePapiers().getPrimaryClip();
        assertNotNull(clip);
        CharSequence colle = clip.getItemAt(0)
                .coerceToText(RuntimeEnvironment.getApplication());
        assertTrue("la copie est bornée (200 000), jamais la taille du document",
                colle.length() <= 200_000);
        assertEquals("le début du document est copié",
                "a".repeat(200_000), colle.toString());
        popups.detach();
    }

    /** Copier une sélection monstre est borné pareil — le plafond
     *  protège la transaction, pas seulement le « tout copier ». */
    @Test
    public void copierUneSelectionMonstreEstBorne() {
        String monstre = "b".repeat(400_000);
        TextSession session = TextSession.of(monstre, System::nanoTime);
        EditorPopups popups = start(session);
        session.setSelection(MultiSelection.of(new SelRange(0, 400_000)));

        popups.barActions().stream()
                .filter(i -> "Copier".equals(i.label()))
                .findFirst().orElseThrow().action().run();

        CharSequence colle = pressePapiers().getPrimaryClip()
                .getItemAt(0).coerceToText(RuntimeEnvironment.getApplication());
        assertTrue("la sélection copiée est bornée",
                colle.length() <= 200_000);
        popups.detach();
    }

    /** Le texte de sélection servi à l'IME est borné à 4 096 : le
     *  protocole IME n'a pas à recevoir un document entier (le dépôt
     *  de départ borne pareil son getSelectedText). */
    @Test
    public void leTexteDeSelectionServiALImeEstBorne() {
        String monstre = "c".repeat(100_000);
        Application contexte = RuntimeEnvironment.getApplication();
        EditorView vue = new EditorView(contexte);
        TextSession session = TextSession.of(monstre, System::nanoTime);
        vue.setSession(session);
        session.setSelection(MultiSelection.of(new SelRange(0, 100_000)));

        InputConnection connexion = vue.onCreateInputConnection(
                new EditorInfo());
        assertNotNull(connexion);
        CharSequence servi = connexion.getSelectedText(0);
        assertNotNull(servi);
        assertTrue("la sélection servie à l'IME est bornée (4 096)",
                servi.length() <= 4_096);
        assertEquals("le début de la sélection est servi",
                "c".repeat(4_096), servi.toString());
    }
}
