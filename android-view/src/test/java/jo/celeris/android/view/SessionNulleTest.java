package jo.celeris.android.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import jo.celeris.core.text.TextSession;

/**
 * D11 (l'analyse croisée) : {@code setSession(null)} ne crashe plus —
 * le départ le supporte comme « vider la vue » (l'onglet fermé) ; la
 * vue celeris y reçoit une session vide inerte, jamais un NPE au
 * premier écouteur.
 */
@RunWith(RobolectricTestRunner.class)
public class SessionNulleTest {

    /** null vide la vue : le document devient vide, la vue vit. */
    @Test
    public void laSessionNulleVideLaVue() {
        EditorView vue = new EditorView(RuntimeEnvironment.getApplication());
        vue.setSession(TextSession.of("du texte\n"));
        assertEquals("du texte\n", vue.session().document().text());
        vue.setSession(null); // avant : NPE à l'écouteur
        assertEquals("la vue vide vit, document vide",
                "", vue.session().document().text());
        // et la vue reçoit un vrai document juste après
        vue.setSession(TextSession.of("apres\n"));
        assertEquals("apres\n", vue.session().document().text());
    }

    /** La session vide de la vue nulle : la vue mesure et se
     *  dessine sans crasher (le pipeline tombe sur un document vide,
     *  jamais sur null). */
    @Test
    public void laVueVideDessineSansCrasher() {
        EditorView vue = new EditorView(RuntimeEnvironment.getApplication());
        vue.setSession(null);
        vue.measure(1080, 1920);
        vue.layout(0, 0, 1080, 1920);
        android.graphics.Bitmap image = android.graphics.Bitmap.createBitmap(
                1080, 1920, android.graphics.Bitmap.Config.ARGB_8888);
        vue.draw(new android.graphics.Canvas(image)); // le chemin de peinture
        assertTrue("la vue vide vit", vue.session() != null);
    }
}
