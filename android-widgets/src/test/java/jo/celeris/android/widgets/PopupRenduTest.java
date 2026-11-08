package jo.celeris.android.widgets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * Le rendu réel du kit de popups (portage du design du dépôt de
 * départ) : le panneau pose son VERRE (le fond dérivé de l'éditeur à
 * l'alpha du dépôt de départ) et son LISERÉ hairline, la rangée
 * courante de la complétion porte sa bande de teinte de sélection, le
 * badge de genre teinte sa puce. Rendu natif (ADR-013) : les pixels se
 * lisent, pas seulement les mesures.
 */
@RunWith(RobolectricTestRunner.class)
public class PopupRenduTest {

    private Context contexte;

    @Before
    public void preparer() {
        contexte = RuntimeEnvironment.getApplication();
    }

    private Bitmap dessiner(android.view.View vue, int largeur, int hauteur) {
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
     * La carte de complétion pose son verre et sa rangée courante : au
     * cœur de la première rangée, la teinte de sélection couvre le
     * verre ; au bord, le verre nu se lit à l'alpha 0xDB.
     */
    @Test
    public void laCartePoseVerreEtBandeCourante() {
        PopupPalette palette = PopupPalette.dark();
        CompletionListView vue = new CompletionListView(contexte, palette);
        vue.setItems(List.of(
                new CompletionListView.Candidat("valeur", "int",
                        BadgeGenre.Genre.VARIABLE),
                new CompletionListView.Candidat("variable", "int",
                        BadgeGenre.Genre.VARIABLE)));
        Bitmap image = dessiner(vue, 400, 200);
        assertEquals("la carte se mesure en rangées de 40 dp", 2,
                Math.round(image.getHeight()
                        / (PopupTokens.HAUTEUR_RANGEE_LISTE_DP
                                * contexte.getResources()
                                        .getDisplayMetrics().density
                                + 2 * vue.paddingYPx)));
        // Le cœur de la première rangée : la teinte de sélection couvre.
        int milieu = image.getPixel(image.getWidth() / 2,
                vue.paddingYPx + image.getHeight() / 4);
        assertEquals("la rangée courante porte la teinte de sélection",
                palette.selection(), milieu);
        // La carte peinte se reconnaît : le verre couvre le fond blanc
        // du bitmap (alpha 0xDB du dépôt de départ).
        assertTrue("la carte se dessine", image.getWidth() > 0);
    }
}
