package jo.celeris.android.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Typeface;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * Le collaborateur des polices (PIEGE-05, PIEGE-23, PIEGE-28, D-14) :
 * le changement de police redérive métriques et viseur, vide le cache
 * par révision suivie ; la police posée vit dans la table de la règle,
 * la mesure et le tracé la lisent ensemble (PIEGE-22) ; la taille
 * changeante prévient l'écouteur (D-15).
 */
@RunWith(RobolectricTestRunner.class)
public class ViewFontsTest {

    private Context contexte;

    @Before
    public void preparer() {
        contexte = RuntimeEnvironment.getApplication();
    }

    /** Le changement de police redérive métriques et viseur (PIEGE-05). */
    @Test
    public void leChangementRederiveMetriquesEtViseur() {
        EditorTestRig banc = EditorTestRig.sur(contexte, "abc\n");
        EditorView vue = banc.vue();
        EditorMetrics avant = vue.metrics;
        HitTest viseurAvant = vue.viseur;
        vue.setFont(new FontSpec("mono", 22f));
        assertNotSame("les métriques se redérivent", avant, vue.metrics);
        assertNotSame("le viseur se redérive", viseurAvant, vue.viseur);
        assertEquals("la taille suit, en sp", 22f, vue.metrics.font().size(),
                0.01f);
    }

    /** La taille changeante prévient l'écouteur — la même taille se
     *  tait (D-15). */
    @Test
    public void laTailleChangeantePrevientLecouteur() {
        EditorTestRig banc = EditorTestRig.sur(contexte, "abc\n");
        EditorView vue = banc.vue();
        AtomicInteger avis = new AtomicInteger();
        vue.setOnFontSizeChanged(taille -> avis.incrementAndGet());
        vue.setFont(new FontSpec("mono", vue.metrics.font().size()));
        assertEquals("la même taille ne prévient pas", 0, avis.get());
        vue.setFont(new FontSpec("mono", 18f));
        assertEquals("la taille qui change prévient", 1, avis.get());
    }

    /** La police posée vit dans la table : la famille active se
     *  redérive aussitôt (D-14, PIEGE-05). */
    @Test
    public void laPolicePoseeSurLaFamilleActiveRederive() {
        EditorTestRig banc = EditorTestRig.sur(contexte, "abc\n");
        EditorView vue = banc.vue();
        EditorMetrics avant = vue.metrics;
        Typeface posee = Typeface.create("posee-mono", Typeface.NORMAL);
        vue.poserPolice("mono", posee);
        assertSame("la règle sert la police posée", posee,
                vue.reglePolices.policeDe("mono"));
        assertNotSame("la famille active se redérive", avant, vue.metrics);
    }

    /** La police posée sur une famille dormante attend son setFont —
     *  les métriques ne bougent pas (D-14). */
    @Test
    public void laPolicePoseeSurFamilleDormanteAttend() {
        EditorTestRig banc = EditorTestRig.sur(contexte, "abc\n");
        EditorView vue = banc.vue();
        EditorMetrics avant = vue.metrics;
        Typeface posee = Typeface.create("fira-test", Typeface.NORMAL);
        vue.poserPolice("fira", posee);
        assertSame("la règle sert la police posée", posee,
                vue.reglePolices.policeDe("fira"));
        assertSame("la famille dormante ne redérive rien", avant, vue.metrics);
        vue.setFont(new FontSpec("fira", 14f));
        assertNotSame("setFont réveille la famille", avant, vue.metrics);
    }

    /** La table de la règle : les défauts de la plateforme pour les
     *  familles connues, la police posée gagne, l'inconnue rend mono. */
    @Test
    public void laTableDeLaRegle() {
        AndroidFontRuler regle = new AndroidFontRuler();
        assertSame("sans", Typeface.SANS_SERIF, regle.policeDe("sans"));
        assertSame("serif", Typeface.SERIF, regle.policeDe("serif"));
        assertSame("mono", Typeface.MONOSPACE, regle.policeDe("mono"));
        assertSame("l'inconnue rend mono", Typeface.MONOSPACE,
                regle.policeDe("inconnue"));
        Typeface posee = Typeface.create("posee", Typeface.BOLD);
        regle.poserPolice("posee", posee);
        assertSame("la police posée gagne", posee, regle.policeDe("posee"));
    }

    /** La police posée se refuse au nul — familles et polices. */
    @Test(expected = NullPointerException.class)
    public void laPolicePoseeRefuseLeNul() {
        AndroidFontRuler regle = new AndroidFontRuler();
        regle.poserPolice("fira", null);
    }

    /** La règle mesure par la table : la police posée donne sa largeur
     *  de caractère (PIEGE-22 : ce qui mesure est ce qui trace). */
    @Test
    public void laRegleMesureParLaTable() {
        AndroidFontRuler regle = new AndroidFontRuler();
        FontSpec spec = new FontSpec("fira", 14f);
        Typeface posee = Typeface.create("fira-mesure", Typeface.NORMAL);
        regle.poserPolice("fira", posee);
        assertTrue("la largeur se mesure, positive",
                regle.charWidth(spec) > 0f);
        assertTrue("la hauteur se mesure, positive",
                regle.lineHeight(spec) > 0f);
    }
}
