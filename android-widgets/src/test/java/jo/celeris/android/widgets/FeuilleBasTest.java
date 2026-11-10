package jo.celeris.android.widgets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * La feuille du bas de l'ADR-018 (C12) : le voile et la feuille se
 * posent sur le cadre hôte, le titre et le libellé de fermeture se
 * posent, la largeur et la hauteur se cantonnent aux jetons de la
 * maquette, et les QUATRE fermetures tiennent — bouton, voile,
 * glissement au-delà du seuil, retour qui se consomme. Le mouvement
 * réduit coupe les glissements : les bancs l'imposent, la fermeture
 * logique ne dépend d'aucune horloge.
 */
@RunWith(RobolectricTestRunner.class)
public class FeuilleBasTest {

    private Context contexte;

    @Before
    public void preparer() {
        contexte = RuntimeEnvironment.getApplication();
        // Mouvement réduit : aucune animation ne diffère un retrait —
        // les bancs restent déterministes (ADR-008 : le réglage se
        // respecte, la feuille n'a pas besoin de bouger).
        Settings.Global.putFloat(contexte.getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE, 0f);
    }

    private FrameLayout hotePose(int largeur, int hauteur) {
        FrameLayout hote = new FrameLayout(contexte);
        hote.measure(
                View.MeasureSpec.makeMeasureSpec(largeur,
                        View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(hauteur,
                        View.MeasureSpec.EXACTLY));
        hote.layout(0, 0, largeur, hauteur);
        return hote;
    }

    private FeuilleBas feuilleGarnie() {
        FeuilleBas feuille = new FeuilleBas(contexte, PopupPalette.dark());
        feuille.titre("Reglages");
        feuille.libelleFermeture("Fermer");
        TextView contenu = new TextView(contexte);
        contenu.setText("Un contenu dense");
        feuille.contenu(contenu);
        return feuille;
    }

    private void trainer(View poignee, float versLeBas) {
        long t = SystemClock.uptimeMillis();
        poignee.dispatchTouchEvent(MotionEvent.obtain(
                t, t, MotionEvent.ACTION_DOWN, 50f, 10f, 0));
        poignee.dispatchTouchEvent(MotionEvent.obtain(
                t, t + 50, MotionEvent.ACTION_MOVE, 50f, 10f + versLeBas, 0));
        poignee.dispatchTouchEvent(MotionEvent.obtain(
                t, t + 100, MotionEvent.ACTION_UP, 50f, 10f + versLeBas, 0));
    }

    /** Le voile se pose sous la feuille, le titre et le contenu vivent. */
    @Test
    public void leVoileEtLaFeuilleSePosent() {
        FrameLayout hote = hotePose(1000, 1800);
        FeuilleBas feuille = feuilleGarnie();
        feuille.montrer(hote);
        assertTrue("la feuille est ouverte", feuille.estOuverte());
        assertEquals("l'hote porte le voile puis la feuille",
                2, hote.getChildCount());
        assertTrue("le voile se pose SOUS la feuille",
                hote.getChildAt(0) == feuille.voileVue());
        assertEquals("le titre se pose", "Reglages",
                feuille.titreVue().getText().toString());
        assertEquals("le libelle de fermeture se pose", "Fermer",
                feuille.boutonFermeture().getContentDescription());
        assertTrue("le contenu vit dans la feuille",
                feuille.getChildCount() >= 3);
    }

    /** La fermeture logique est immédiate : l'etat, le voile et le
     *  rappel ne dependent d'aucune horloge. */
    @Test
    public void leRetourFermeEtSeConsommeUneSeuleFois() {
        FrameLayout hote = hotePose(1000, 1800);
        FeuilleBas feuille = feuilleGarnie();
        AtomicInteger fermetures = new AtomicInteger();
        feuille.setSurFermeture(fermetures::incrementAndGet);
        feuille.montrer(hote);
        assertTrue("le retour se consomme par la feuille ouverte",
                feuille.surRetour());
        assertFalse("fermee, la feuille laisse le retour passer",
                feuille.surRetour());
        assertFalse(feuille.estOuverte());
        assertNull("le voile s'en va", feuille.voileVue());
        assertEquals("le rappel court une fois", 1, fermetures.get());
    }

    /** Le voile clique ferme ; le bouton ferme — ses deux cibles
     *  tiennent la promesse de la maquette. */
    @Test
    public void leVoileEtLeBoutonFerment() {
        FrameLayout hote = hotePose(1000, 1800);
        FeuilleBas feuille = feuilleGarnie();
        feuille.montrer(hote);
        feuille.voileVue().performClick();
        assertFalse("le voile clique ferme", feuille.estOuverte());

        FrameLayout autreHote = hotePose(1000, 1800);
        FeuilleBas autre = feuilleGarnie();
        autre.montrer(autreHote);
        autre.boutonFermeture().performClick();
        assertFalse("le bouton ferme", autre.estOuverte());
    }

    /** La largeur se cantonne a celle de la maquette, la hauteur a sa
     *  part — le corps defile, il ne deborde pas. */
    @Test
    public void lesCotesSeCantonnentAuxJetons() {
        FrameLayout hote = hotePose(1000, 1800);
        FeuilleBas feuille = new FeuilleBas(contexte, PopupPalette.dark());
        LinearLayout dense = new LinearLayout(contexte);
        dense.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < 80; i++) {
            TextView ligne = new TextView(contexte);
            ligne.setText("ligne " + i);
            dense.addView(ligne);
        }
        feuille.contenu(dense);
        feuille.montrer(hote);
        hote.measure(
                View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1800, View.MeasureSpec.EXACTLY));
        hote.layout(0, 0, 1000, 1800);
        assertTrue("la largeur se cantonne (densite 1 : 660 px)",
                feuille.getWidth() <= 660);
        assertTrue("la hauteur se cantonne a la part de la maquette",
                feuille.getHeight() <= Math.round(1800 * 0.76f));
        assertTrue("la feuille garde une hauteur lisible",
                feuille.getHeight() > 100);
    }

    /** Le glissement au-dela du seuil emporte la feuille ; en deca,
     *  elle revient — jamais plus haut que son repos. */
    @Test
    public void leGlissementSuitSonSeuil() {
        FrameLayout hote = hotePose(1000, 1800);
        FeuilleBas feuille = feuilleGarnie();
        feuille.montrer(hote);
        trainer(feuille.poigneeVue(), 40f);
        assertTrue("un glissement court laisse la feuille ouverte",
                feuille.estOuverte());
        assertEquals("la feuille revient a son repos", 0f,
                feuille.getTranslationY(), 0f);

        trainer(feuille.poigneeVue(), 300f);
        assertFalse("un glissement au-dela du seuil emporte la feuille",
                feuille.estOuverte());
    }

    /** La cible du bouton fait au moins celle du kit ; la poignee est
     *  muette a l'accessibilite (maquette : aria-hidden). */
    @Test
    public void lesCiblesTactilesEtLAccessibiliteTiennent() {
        FrameLayout hote = hotePose(1000, 1800);
        FeuilleBas feuille = feuilleGarnie();
        feuille.montrer(hote);
        hote.measure(
                View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1800, View.MeasureSpec.EXACTLY));
        hote.layout(0, 0, 1000, 1800);
        assertTrue("la cible du bouton fait au moins 48 px",
                feuille.boutonFermeture().getWidth() >= 48
                        && feuille.boutonFermeture().getHeight() >= 48);
        assertEquals("la poignee est muette au lecteur d'ecran",
                View.IMPORTANT_FOR_ACCESSIBILITY_NO,
                feuille.poigneeVue().getImportantForAccessibility());
    }

    /** Le verre suit la palette posee — le titre change d'encre. */
    @Test
    public void laPaletteRetematise() {
        FrameLayout hote = hotePose(1000, 1800);
        FeuilleBas feuille = feuilleGarnie();
        feuille.montrer(hote);
        feuille.setPalette(PopupPalette.light());
        assertEquals("l'encre du titre suit la palette",
                PopupPalette.light().text(),
                feuille.titreVue().getCurrentTextColor());
    }

    /** L'hote doit etre un cadre superpose — jamais une pile lineaire. */
    @Test
    public void lhoteDoitEtreUnFrameLayout() {
        FeuilleBas feuille = feuilleGarnie();
        LinearLayout pile = new LinearLayout(contexte);
        assertThrows(IllegalStateException.class, () -> feuille.montrer(pile));
        assertFalse("rien ne s'est ouvert", feuille.estOuverte());
    }
}
