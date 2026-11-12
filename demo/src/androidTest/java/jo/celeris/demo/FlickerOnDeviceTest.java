package jo.celeris.demo;

import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;
import android.graphics.Rect;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * PIEGE-26 sur la vitre : pendant une frappe à analyse retenue, les
 * mots non touchés gardent leurs pixels. La frappe vit en fin de
 * document (le crochet de débogage retient l'analyse), trois captures
 * prennent la vitre PENDANT la retenue, une quatrième après la
 * publication — la bande des premières lignes, jamais touchée, garde
 * ses pixels d'une capture à l'autre. Le banc JVM
 * ({@code FlickerStabilityTest}) a prouvé la mesure conservée ; celui-ci
 * la prouve en pixels sur le vrai chemin de composition.
 */
@RunWith(AndroidJUnit4.class)
public class FlickerOnDeviceTest {

    /** La part maximale de pixels fortement différents : un centième. */
    static final int FRACTION_MAXIMALE_MILLIEMES = 10;

    /**
     * Trois captures pendant la frappe retenue, une après la
     * publication : les mots non touchés gardent leurs pixels — la
     * porte d'encre garde le vert honnête, un banc en vide ne
     * prouverait rien.
     */
    @Test
    public void lesMotsNonTouchesGardentLeursPixelsPendantLaFrappeRetenue()
            throws Exception {
        try (ActivityScenario<EditorActivity> scenario = BancAppareil.ouvrirScenario()) {
            BancAppareil.attendreVue(scenario);
            BancAppareil.retenirAnalyse(scenario, BancAppareil.RETENUE_MS);
            Rect bande = BancAppareil.bandePremieresLignes(scenario);
            BancAppareil.frapper(scenario, "x");
            Bitmap[] captures = new Bitmap[4];
            captures[0] = BancAppareil.capturerVue(scenario);
            Thread.sleep(700L);
            captures[1] = BancAppareil.capturerVue(scenario);
            Thread.sleep(700L);
            captures[2] = BancAppareil.capturerVue(scenario);
            Thread.sleep(BancAppareil.RETENUE_MS + 500L);
            captures[3] = BancAppareil.capturerVue(scenario);
            int encre = ComparaisonImage.abscissePremiereEncre(captures[0], bande);
            assertTrue("la bande ne porte aucune encre — la capture ne mesure rien\n"
                    + BancAppareil.apercu(captures[0], bande),
                    encre > bande.left);
            for (int premiere = 0; premiere < captures.length; premiere++) {
                for (int seconde = premiere + 1; seconde < captures.length; seconde++) {
                    int fraction = ComparaisonImage.fractionForte(
                            captures[premiere], captures[seconde], bande);
                    assertTrue("les mots non touchés gardent leurs pixels "
                            + "(captures " + (premiere + 1) + " et " + (seconde + 1)
                            + ", fraction " + fraction + "‰)\n"
                            + BancAppareil.apercu(captures[premiere], bande),
                            fraction <= FRACTION_MAXIMALE_MILLIEMES);
                }
            }
        }
    }
}
