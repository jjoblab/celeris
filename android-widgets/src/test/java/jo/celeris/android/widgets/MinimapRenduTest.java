package jo.celeris.android.widgets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;

import jo.celeris.core.text.TextSession;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * Le rendu réel de la minimap (reprise du design) : la silhouette se
 * tisse SERRÉE — chaque rangée remplit son cran, aucun trou ne
 * sépare deux briques ; le document court garde sa hauteur honnête,
 * une rangée par ligne, sans s'étirer sur l'étendue posée. Les encres
 * se posent ADOUCIES — la chaîne se lit à l'accent atténué, le code
 * nu à l'encre assombrie — et la fenêtre visible teinte sa
 * CARTOUCHE : le remplissage tenu se voit contre la brique qui ne le
 * porte pas. Rendu natif (ADR-013) : les pixels se lisent, pas
 * seulement les mesures.
 */
@RunWith(RobolectricTestRunner.class)
public class MinimapRenduTest {

    private Context contexte;

    @Before
    public void preparer() {
        contexte = RuntimeEnvironment.getApplication();
    }

    /** Le document de tant de lignes portant le texte donné. */
    private static String document(String ligne, int nombre) {
        StringBuilder contenu = new StringBuilder();
        for (int i = 0; i < nombre; i++) {
            if (i > 0) {
                contenu.append('\n');
            }
            contenu.append(ligne);
        }
        return contenu.toString();
    }

    private Bitmap dessiner(MinimapView vue) {
        vue.measure(android.view.View.MeasureSpec.makeMeasureSpec(0,
                        android.view.View.MeasureSpec.UNSPECIFIED),
                android.view.View.MeasureSpec.makeMeasureSpec(0,
                        android.view.View.MeasureSpec.UNSPECIFIED));
        vue.layout(0, 0, vue.getMeasuredWidth(), vue.getMeasuredHeight());
        Bitmap image = Bitmap.createBitmap(
                Math.max(1, vue.getWidth()), Math.max(1, vue.getHeight()),
                Bitmap.Config.ARGB_8888);
        vue.draw(new Canvas(image));
        return image;
    }

    /**
     * La silhouette se tisse SERRÉE : vingt rangées de code, vingt
     * briques — au cœur horizontal de chaque brique, le pixel ne
     * rend JAMAIS le fond de bande : aucun trou ne sépare deux
     * rangées.
     */
    @Test
    public void laSilhouetteSeTisseSerreeSansTrou() {
        int lignes = 20;
        var session = TextSession.of(document("aaaaaaaaaaaaaaaaaaaa", lignes),
                System::nanoTime);
        MinimapView vue = new MinimapView(contexte, PopupPalette.dark());
        vue.setModel(MinimapModel.of(session.document(), lignes), lignes);
        vue.setSpanPx(1000);
        Bitmap image = dessiner(vue);
        int hauteurBrique = image.getHeight() / lignes;
        assertEquals("vingt briques honnêtes à 2,5 dp (densité 1)",
                2, hauteurBrique);
        // le cœur horizontal de chaque brique : le code rend son encre
        // assombrie, jamais le fond de bande (25, 25, 25)
        int x = 8;
        for (int brique = 0; brique < lignes; brique++) {
            int y = Math.min(image.getHeight() - 1, brique * hauteurBrique + 1);
            int pixel = image.getPixel(x, y);
            assertTrue("la brique " + brique + " se tisse sans trou",
                    pixel != 0xFF191919);
        }
    }

    /**
     * L'encre se lit ADOUCIE : la ligne qui porte une chaîne rend la
     * teinte chaude de l'accent atténué — la ligne de code nu rend
     * l'encre de texte assombrie, grise ; les deux se distinguent au
     * pixel.
     */
    @Test
    public void lEncreSeLitAdoucie() {
        String contenu = "aaaaaaaaaaaaaaaaaaaa\n\"bbbbbbbbbbbb\"";
        var session = TextSession.of(contenu, System::nanoTime);
        MinimapView vue = new MinimapView(contexte, PopupPalette.dark());
        vue.setModel(MinimapModel.of(session.document(), 2), 2);
        vue.setSpanPx(1000);
        Bitmap image = dessiner(vue);
        int code = image.getPixel(8, 1);
        int chaine = image.getPixel(8, 4);
        // la chaîne (accent 220, 220, 170 à 0,8) garde ses canaux
        // rouges et verts AU-DESSUS du code assombri (116, 116, 116)
        assertTrue("la chaîne se lit à l'accent adouci",
                ((chaine >> 16) & 0xFF) > ((code >> 16) & 0xFF));
        assertTrue("le code nu se lit à l'encre assombrie",
                ((code >> 16) & 0xFF) < 0x80);
    }

    /**
     * La fenêtre visible teinte sa cartouche : la brique sous la
     * fenêtre porte le remplissage tenu — le pixel s'en trouve
     * déplacé contre la même brique hors fenêtre.
     */
    @Test
    public void laFenetreTeinteSaCartouche() {
        int lignes = 20;
        var session = TextSession.of(document("aaaaaaaaaaaaaaaaaaaa", lignes),
                System::nanoTime);
        MinimapView vue = new MinimapView(contexte, PopupPalette.dark());
        vue.setModel(MinimapModel.of(session.document(), lignes), lignes);
        vue.setViewport(0, 5, lignes);
        vue.setSpanPx(1000);
        Bitmap image = dessiner(vue);
        int hauteurBrique = image.getHeight() / lignes;
        int dedans = image.getPixel(8, hauteurBrique + 1);
        int dehors = image.getPixel(8, 15 * hauteurBrique + 1);
        assertNotEquals("la cartouche teinte la brique qu'elle couvre",
                dedans, dehors);
    }
}
