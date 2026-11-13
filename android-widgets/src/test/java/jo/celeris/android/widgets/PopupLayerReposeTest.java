package jo.celeris.android.widgets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Canvas;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * La couche re-pose sans tuer (M8.6, reprise) : la frappe suivante
 * RE-POSE la même vue de complétion — elle se re-mesure et se
 * re-place, jamais elle ne re-naît ni ne meurt (le filtre local,
 * sans refermer) ; la carte de documentation PARTAGÉE (la même vue
 * sous le genre doc de complétion puis sous le survol) se détache de
 * son ancien créneau — une vue ne vit jamais sous deux genres.
 */
@RunWith(RobolectricTestRunner.class)
public class PopupLayerReposeTest {

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
            // rien à tracer — la possession seule intéresse le banc
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

    @Before
    public void preparer() {
        contexte = RuntimeEnvironment.getApplication();
    }

    private PanneauEssai panneau() {
        return new PanneauEssai(contexte, PopupPalette.dark());
    }

    /** La frappe suivante re-pose la MÊME vue (le filtre de
     *  complétion, sans refermer) : elle reste portée, attachée —
     *  exactement une fois — et le re-pose n'arme jamais sa mort. */
    @Test
    public void reposerLaMemeVueNeLaTuePas() {
        PopupLayer couche = new PopupLayer(contexte);
        PanneauEssai vue = panneau();
        couche.show(PopupController.PopupKind.COMPLETION_LIST, vue, 10, 10);
        assertSame("la première pose porte la vue",
                vue, couche.popupOf(PopupController.PopupKind.COMPLETION_LIST));
        // le banc fige la naissance : la carte se tient
        vue.finirEntree();
        assertFalse("la carte figée se tient, sans voile",
                vue.voileEnCours());
        // la frappe suivante re-pose la MÊME vue, à la position neuve
        couche.show(PopupController.PopupKind.COMPLETION_LIST, vue, 20, 20);
        assertSame("le re-pose garde la vue à son genre",
                vue, couche.popupOf(PopupController.PopupKind.COMPLETION_LIST));
        assertSame("la vue reste attachée à sa couche",
                couche, vue.getParent());
        assertEquals("la vue n'est attachée qu'UNE fois",
                1, couche.getChildCount());
        assertFalse("le re-pose de la même vue n'arme pas sa mort",
                vue.voileEnCours());
    }

    /** La carte PARTAGÉE (la documentation de complétion puis le
     *  survol : la même instance, le motif de l'atelier) se détache
     *  de son ancien créneau au changement de genre — jamais deux
     *  créneaux ne portent la même vue, jamais la vue ne se
     *  re-attache par-dessus elle-même. */
    @Test
    public void laMemeVueNeVitJamaisSousDeuxGenres() {
        PopupLayer couche = new PopupLayer(contexte);
        PanneauEssai vue = panneau();
        couche.show(PopupController.PopupKind.COMPLETION_DOC, vue, 10, 10);
        assertSame("la doc vit sous son genre", vue,
                couche.popupOf(PopupController.PopupKind.COMPLETION_DOC));
        // le banc fige la naissance : la carte se tient
        vue.finirEntree();
        assertFalse("la carte figée se tient, sans voile",
                vue.voileEnCours());
        // le survol prend la MÊME carte — l'ancien créneau se vide
        couche.show(PopupController.PopupKind.HOVER, vue, 30, 30);
        assertSame("la carte vit sous le survol", vue,
                couche.popupOf(PopupController.PopupKind.HOVER));
        assertNull("l'ancien créneau se vide",
                couche.popupOf(PopupController.PopupKind.COMPLETION_DOC));
        assertSame("la carte reste attachée, une seule fois",
                couche, vue.getParent());
        assertEquals("une seule attache pour une carte",
                1, couche.getChildCount());
        assertFalse("le changement de genre n'arme pas la mort",
                vue.voileEnCours());
        // le renvoi du genre courant emporte la carte
        assertTrue("le renvoi du survol ferme",
                couche.dismiss(PopupController.PopupKind.HOVER));
        assertNull("la carte partie, le créneau se vide",
                couche.popupOf(PopupController.PopupKind.HOVER));
        assertFalse("le créneau vide ne montre plus",
                couche.isShowing(PopupController.PopupKind.HOVER));
    }
}
