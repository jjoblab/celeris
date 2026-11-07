package jo.celeris.android.widgets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import android.app.Activity;

import android.os.Looper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.RuntimeEnvironment;

/**
 * PIEGE-27 — un popup ne prend JAMAIS le focus d'entrée de l'éditeur :
 * la couche et ses popups restent non focalisables, la connexion
 * d'entrée et le focus d'une vue d'édition posée dessous ne bougent
 * pas d'un cran.
 */
@RunWith(RobolectricTestRunner.class)
public class PopupLayerFocusTest {

    /** La couche montre un popup : l'éditeur dessous garde le focus. */
    @Test
    public void popupNeverStealsInputFocus() {
        Activity activite = Robolectric.buildActivity(Activity.class).setup().get();
        Context contexte = activite;
        EditText editeur = new EditText(contexte);
        editeur.setText("int x = 1;");
        PopupLayer couche = new PopupLayer(contexte);
        android.widget.FrameLayout hote = new android.widget.FrameLayout(
                contexte);
        hote.addView(editeur, new android.widget.FrameLayout.LayoutParams(
                1080, 1920));
        hote.addView(couche, new android.widget.FrameLayout.LayoutParams(
                1080, 1920));
        activite.setContentView(hote);
        // L'attachement de la fenêtre se poste au looper principal :
        // il s'exécute avant de demander le focus.
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        hote.layout(0, 0, 1080, 1920);

        editeur.requestFocus();
        assertTrue("l'éditeur tient le focus avant le popup",
                editeur.hasFocus());
        assertEquals("l'éditeur sert une connexion d'entrée",
                true, editeur.onCreateInputConnection(
                        new EditorInfo()) != null);

        View popup = new View(contexte);
        couche.show(PopupController.PopupKind.COMPLETION_LIST, popup, 100, 200);
        assertTrue("le popup est montré",
                couche.isShowing(PopupController.PopupKind.COMPLETION_LIST));

        // PIEGE-27 : ni la couche ni le popup ne sont focalisables, le
        // focus n'a pas bougé d'un cran.
        assertFalse("la couche ne prend jamais le focus",
                couche.isFocusable());
        assertFalse("le popup montré ne prend jamais le focus",
                popup.isFocusable());
        assertTrue("l'éditeur garde le focus d'entrée",
                editeur.hasFocus());
        assertEquals("la connexion d'entrée reste à l'éditeur",
                true, editeur.onCreateInputConnection(
                        new EditorInfo()) != null);
    }

    /** Le repositionnement déplace le popup, la fermeture le retire. */
    @Test
    public void repositionMovesAndDismissRemoves() {
        Context contexte = RuntimeEnvironment.getApplication();
        PopupLayer couche = new PopupLayer(contexte);
        couche.layout(0, 0, 1080, 1920);
        View popup = new View(contexte);
        couche.show(PopupController.PopupKind.SIGNATURE_HELP, popup, 10, 20);
        couche.reposition(PopupController.PopupKind.SIGNATURE_HELP, 30, 40);
        assertEquals(30, popup.getLeft());
        assertEquals(40, popup.getTop());
        assertTrue(couche.dismiss(PopupController.PopupKind.SIGNATURE_HELP));
        assertFalse(couche.isShowing(PopupController.PopupKind.SIGNATURE_HELP));
    }
}
