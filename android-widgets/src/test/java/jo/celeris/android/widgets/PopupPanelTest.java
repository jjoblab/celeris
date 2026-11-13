package jo.celeris.android.widgets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Canvas;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * Le panneau de base des popups (portage de la carte du dépôt de
 * départ) : les respirations et la hauteur de rangée dérivent de la
 * densité et de la palette, la palette qui se remplace redérive tout,
 * la mesure réserve les respirations des deux côtés — le contenu vit
 * chez la sous-classe (PIEGE-27 : le panneau ne demande jamais le
 * focus).
 */
@RunWith(RobolectricTestRunner.class)
public class PopupPanelTest {

    private Context contexte;

    /** Le panneau d'essai : un contenu carré de 100 × 50. */
    private static final class PanneauEssai extends PopupPanel {

        static final int LARGEUR = 100;
        static final int HAUTEUR = 50;

        PanneauEssai(Context context, PopupPalette palette) {
            super(context, palette);
        }

        @Override
        void drawContent(Canvas canvas) {
            // rien à tracer — la géométrie seule intéresse le banc
        }

        @Override
        int contentWidth() {
            return LARGEUR;
        }

        @Override
        int contentHeight() {
            return HAUTEUR;
        }
    }

    /** Mesure et dessine le panneau sur un bitmap — l'image rend. */
    private static void dessiner(PopupPanel panneau) {
        int largeur = android.view.View.MeasureSpec.makeMeasureSpec(0,
                android.view.View.MeasureSpec.UNSPECIFIED);
        panneau.measure(largeur, largeur);
        panneau.layout(0, 0, panneau.getMeasuredWidth(),
                panneau.getMeasuredHeight());
        android.graphics.Bitmap image = android.graphics.Bitmap.createBitmap(
                Math.max(1, panneau.getWidth()),
                Math.max(1, panneau.getHeight()),
                android.graphics.Bitmap.Config.ARGB_8888);
        panneau.draw(new Canvas(image));
    }

    private static PopupPalette palette() {
        return new PopupPalette(0xFFF0F0F0, 0xFFDDDDDD, 0xFF222222,
                0xFF888888, 0xFFDDDDDD, 0xFF6666FF, 0xFF8866FF,
                0xFFDD4444, 0xFFDDAA33, 0xFF4488DD, 14f);
    }

    @Before
    public void preparer() {
        contexte = RuntimeEnvironment.getApplication();
    }

    /** Les respirations dérivent de la densité — en pixels entiers. */
    @Test
    public void lesRespirationsDeriventDeLaDensite() {
        PanneauEssai panneau = new PanneauEssai(contexte, palette());
        float densite = contexte.getResources().getDisplayMetrics().density;
        assertEquals("la respiration horizontale suit la densité",
                Math.round(PopupTokens.RESPIRATION_X_DP * densite),
                panneau.paddingPx);
        assertEquals("la respiration verticale suit la densité",
                Math.round(PopupTokens.RESPIRATION_Y_DP * densite),
                panneau.paddingYPx);
    }

    /** La hauteur de rangée dérive de la palette — la typographie du
     *  corps, en pixels. */
    @Test
    public void laHauteurDeRangeeDeriveDeLaPalette() {
        PanneauEssai panneau = new PanneauEssai(contexte, palette());
        assertTrue("la hauteur de rangée se dérive, positive",
                panneau.rowHeightPx > 0);
    }

    /** La mesure réserve les respirations des deux côtés : le contenu
     *  plus deux fois chaque respiration. */
    @Test
    public void laMesureReserveLesRespirations() {
        PanneauEssai panneau = new PanneauEssai(contexte, palette());
        int largeur = android.view.View.MeasureSpec.makeMeasureSpec(0,
                android.view.View.MeasureSpec.UNSPECIFIED);
        int hauteur = android.view.View.MeasureSpec.makeMeasureSpec(0,
                android.view.View.MeasureSpec.UNSPECIFIED);
        panneau.measure(largeur, hauteur);
        assertEquals("la largeur mesure le contenu et deux respirations",
                PanneauEssai.LARGEUR + 2 * panneau.paddingPx,
                panneau.getMeasuredWidth());
        assertEquals("la hauteur mesure le contenu et deux respirations",
                PanneauEssai.HAUTEUR + 2 * panneau.paddingYPx,
                panneau.getMeasuredHeight());
    }

    /** La fluidité du propriétaire (2026-11-13 : « ajoute une petite
     *  animation pour les popups ») : la carte NAÎT — la couche arme à
     *  sa pose, le voile s'ouvre à la première image ; le banc fige
     *  la naissance, la carte dessinée hors couche reste STEADY. */
    @Test
    public void laCarteNaitEnVoileEtLeBancLaFige() {
        PanneauEssai panneau = new PanneauEssai(contexte, palette());
        assertFalse("hors naissance, la carte reste steady",
                panneau.voileEnCours());
        // la couche arme la naissance à la pose
        panneau.naissance();
        assertTrue("la naissance armée ouvre à la première image",
                panneau.voileEnCours());
        // la première image ouvre l'horloge — le voile vit
        dessiner(panneau);
        assertTrue("l'horloge ouverte, le voile de naissance vit",
                panneau.voileEnCours());
        // le banc fige la naissance : la carte se tient
        panneau.finirEntree();
        assertFalse("la carte figée se tient, sans voile",
                panneau.voileEnCours());
    }

    /** La carte MEURT en voile : la couche arme la mort au renvoi, le
     *  retrait différé part à la fin du voile — jamais un retrait
     *  brusque. */
    @Test
    public void laCarteMeurtEnVoileAvantSonRetrait() {
        PanneauEssai panneau = new PanneauEssai(contexte, palette());
        final boolean[] retire = {false};
        panneau.mourir(() -> retire[0] = true);
        assertTrue("la mort armée, le voile vit", panneau.voileEnCours());
        // le retrait ne part PAS à l'armement — le voile d'abord
        dessiner(panneau);
        assertFalse("le retrait différé ne part pas à l'armement",
                retire[0]);
        assertTrue("l'horloge ouverte, le voile de mort vit",
                panneau.voileEnCours());
    }

    /** La palette qui se remplace redérive tout : respirations et
     *  hauteur de rangée suivent. */
    @Test
    public void laPaletteRemplaceeRederiveTout() {
        PanneauEssai panneau = new PanneauEssai(contexte, palette());
        int rangeeAvant = panneau.rowHeightPx;
        // la même base typographique : la redérivation tient, la
        // demande de replan suit (requestLayout ne jette jamais ici)
        panneau.setPalette(palette());
        assertEquals("la hauteur de rangée se redérive", rangeeAvant,
                panneau.rowHeightPx);
        assertEquals("la palette posée se lit", 0xFF222222,
                panneau.palette.text());
    }
}
