package jo.celeris.android.host.popups;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import jo.celeris.android.view.EditorView;
import jo.celeris.android.widgets.PopupLayer;
import jo.celeris.android.widgets.SelectionBarView;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextSession;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * La barre de sélection au DÉFAUT du montage (la revue du propriétaire
 * du 2026-11-19 : « il donne seulement l'option i, alors qu'il devait
 * donner copier, couper, coller (s'il existe quelque chose à coller),
 * copier tout, le i et les ... ») : l'hôte qui ne pose AUCUNE action
 * sert les actions d'édition STANDARD — couper, copier, coller au
 * presse-papiers système près, tout copier — sur le PRESSE-PAPIERS
 * SYSTÈME d'Android, comme tout éditeur de code.
 *
 * <p>Banc réel (ADR-013) : la scène se monte comme chez l'hôte, le
 * presse-papiers de Robolectric joue le système, la sélection se pose
 * à la main sur la session.</p>
 */
@RunWith(RobolectricTestRunner.class)
public class BarreSelectionDefautTest {

    private static final String DOCUMENT = "public class Salut {\n}\n";

    /** Le montage du banc : vue, session, couche, orchestrateur au
     *  coach muet — l'hôte n'a rien posé d'autre. */
    private static EditorPopups start(TextSession session) {
        Application contexte = RuntimeEnvironment.getApplication();
        EditorView editor = new EditorView(contexte);
        PopupLayer couche = new PopupLayer(contexte);
        editor.setSession(session);
        return new EditorPopups(editor, session, couche,
                jo.celeris.android.view.EditorThemes.DARK, 14f,
                CoachPopups.muet());
    }

    /** Sans action posée, la barre porte les QUATRE actions
     *  d'édition standard (Coller absent : le presse-papiers est
     *  vide). */
    @Test
    public void lesActionsEditionStandardViventSansHote() {
        TextSession session = TextSession.of(DOCUMENT, System::nanoTime);
        EditorPopups popups = start(session);
        session.setSelection(MultiSelection.of(new SelRange(0, 6)));

        // la voie publique de la barre : les actions lues par la pose
        java.util.List<SelectionBarView.Item> actions =
                popups.barActions();
        assertTrue("la barre a des actions sans hôte",
                actions.size() >= 3);
        assertTrue("Couper vit",
                actions.stream().anyMatch(i -> "Couper".equals(i.label())));
        assertTrue("Copier vit",
                actions.stream().anyMatch(i -> "Copier".equals(i.label())));
        assertTrue("Tout copier vit",
                actions.stream().anyMatch(i -> "Tout copier".equals(i.label())));
        assertFalse("Coller ne vit pas : le presse-papiers est vide",
                actions.stream().anyMatch(i -> "Coller".equals(i.label())));
        popups.detach();
    }

    /** Le presse-papiers plein fait vivre Coller — la pastille suit le
     *  système, comme l'exige la revue (« s'il existe quelque chose
     *  à coller »). */
    @Test
    public void collerVitQuandLePressePapiersEstPlein() {
        Application contexte = RuntimeEnvironment.getApplication();
        ClipboardManager pressePapiers = (ClipboardManager) contexte
                .getSystemService(Context.CLIPBOARD_SERVICE);
        pressePapiers.setPrimaryClip(
                ClipData.newPlainText("banc", "du texte"));

        TextSession session = TextSession.of(DOCUMENT, System::nanoTime);
        EditorPopups popups = start(session);
        session.setSelection(MultiSelection.of(new SelRange(0, 6)));

        assertTrue("Coller vit : le presse-papiers est plein",
                popups.barActions().stream()
                        .anyMatch(i -> "Coller".equals(i.label())));
        popups.detach();
    }

    /** Copier pose la sélection au presse-papiers SYSTÈME : le geste
     *  d'édition par défaut vit vraiment, pas seulement l'affichage. */
    @Test
    public void copierPoseLePressePapiersSysteme() {
        TextSession session = TextSession.of(DOCUMENT, System::nanoTime);
        EditorPopups popups = start(session);
        session.setSelection(MultiSelection.of(new SelRange(0, 6)));

        popups.barActions().stream()
                .filter(i -> "Copier".equals(i.label()))
                .findFirst().orElseThrow().action().run();

        Application contexte = RuntimeEnvironment.getApplication();
        ClipboardManager pressePapiers = (ClipboardManager) contexte
                .getSystemService(Context.CLIPBOARD_SERVICE);
        assertEquals("la sélection copiée vit au presse-papiers",
                "public",
                pressePapiers.getPrimaryClip()
                        .getItemAt(0).coerceToText(contexte).toString());
        popups.detach();
    }
}
