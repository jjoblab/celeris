package jo.celeris.demo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;
import android.graphics.Rect;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * PIEGE-25 sur la vitre : la gouttière ne bouge pas d'un pixel.
 * L'abscisse de la première encre de la bande des premières lignes
 * (le numéro, le premier caractère) ne bouge ni à la frappe qui allonge
 * le document, ni à la publication de l'analyse qui suit — le banc
 * JVM ({@code GutterStabilityTest}) a prouvé la largeur sur les
 * métriques ; celui-ci la prouve en pixels : le décalage minimal
 * entre les captures reste sous le pixel.
 */
@RunWith(AndroidJUnit4.class)
public class GutterStabilityOnDeviceTest {

    /**
     * Une ligne s'ajoute en fin de document, l'analyse se publie :
     * l'abscisse de la première encre ne bouge pas d'un pixel, le
     * contenu ne se décale pas d'un pixel.
     */
    @Test
    public void labscisseDeLaPremiereEncreNeBougePasDunPixel()
            throws Exception {
        try (ActivityScenario<EditorActivity> scenario = BancAppareil.ouvrirScenario()) {
            BancAppareil.attendreVue(scenario);
            BancAppareil.retenirAnalyse(scenario, BancAppareil.RETENUE_MS);
            Rect bande = BancAppareil.bandePremieresLignes(scenario);
            Bitmap avant = BancAppareil.capturerVue(scenario);
            System.out.println("APERCU AVANT\n" + BancAppareil.apercu(avant, bande));
            BancAppareil.frapper(scenario, "\ndix");
            Thread.sleep(250L);
            Bitmap apresFrappe = BancAppareil.capturerVue(scenario);
            Thread.sleep(BancAppareil.RETENUE_MS + 1500L);
            Bitmap apresAnalyse = BancAppareil.capturerVue(scenario);
            int encreAvant = ComparaisonImage.abscissePremiereEncre(avant, bande);
            int encreFrappe = ComparaisonImage.abscissePremiereEncre(apresFrappe, bande);
            int encreAnalyse = ComparaisonImage.abscissePremiereEncre(apresAnalyse, bande);
            assertTrue("la bande ne porte aucune encre — la capture ne mesure rien\n"
                    + BancAppareil.apercu(avant, bande)
                    + BancAppareil.etatSysteme(),
                    encreAvant > bande.left);
            assertEquals("l'abscisse de la première encre ne bouge pas à la frappe",
                    encreAvant, encreFrappe);
            assertEquals("l'abscisse de la première encre ne bouge pas à l'analyse",
                    encreAvant, encreAnalyse);
            int decalage = ComparaisonImage.decalageMinimal(
                    avant, apresAnalyse, bande, 12);
            assertTrue("le contenu ne se décale pas d'un pixel à la publication",
                    Math.abs(decalage) <= 1);
        }
    }
}
