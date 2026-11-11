package jo.celeris.android.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * Le collaborateur de diagnostic de scène (M7.5, §9) : la retenue
 * d'analyse se règle quand le démon la porte, l'exécuteur injecté par
 * l'hôte n'est JAMAIS détourné ; les lectures de vérité — taille de
 * police, largeur de gouttière — suivent la scène ; le compteur
 * d'images décolorées part de zéro (le rattrapage PIEGE-26 le garde
 * là).
 */
@RunWith(RobolectricTestRunner.class)
public class SceneDiagnosticsTest {

    private Context contexte;

    @Before
    public void preparer() {
        contexte = RuntimeEnvironment.getApplication();
    }

    /** La retenue par défaut se règle — la vue sans exécuteur injecté
     *  porte la retenue du démon. */
    @Test
    public void laRetenueParDefautSeRegle() {
        EditorView vue = new EditorView(contexte);
        assertEquals("le délai par défaut", EditorView.DELAI_RETENU_PAR_DEFAUT_MS,
                vue.retenueAnalyseMs());
        vue.setRetenueAnalyseMs(500L);
        assertEquals("le réglage se pose", 500L, vue.retenueAnalyseMs());
        vue.setRetenueAnalyseMs(0L);
        assertEquals("zéro rend la main immédiate", 0L, vue.retenueAnalyseMs());
    }

    /** L'exécuteur injecté par l'hôte n'est jamais détourné : le
     *  réglage s'ignore, la lecture rend zéro (M7.5, §9). */
    @Test
    public void lexecuteurInjecteNEstJamaisDetourne() {
        EditorTestRig banc = EditorTestRig.sur(contexte, "abc\n");
        EditorView vue = banc.vue();
        assertEquals("l'exécuteur du banc garde la main entière", 0L,
                vue.retenueAnalyseMs());
        vue.setRetenueAnalyseMs(500L);
        assertEquals("le réglage s'ignore sur l'exécuteur de l'hôte", 0L,
                vue.retenueAnalyseMs());
    }

    /** Les lectures de vérité : la taille de police suit la scène,
     *  la largeur de gouttière se lit après l'image. */
    @Test
    public void lesLecturesDeVerite() {
        EditorTestRig banc = EditorTestRig.sur(contexte, "abc\n");
        EditorView vue = banc.vue();
        assertEquals("la taille par défaut, en sp", 14f, vue.fontSizeSp(),
                0.01f);
        vue.setFont(new FontSpec("mono", 20f));
        assertEquals("la taille suit le changement", 20f, vue.fontSizeSp(),
                0.01f);
        banc.image();
        assertTrue("la largeur de gouttière se lit, positive",
                vue.gutterWidth() > 0f);
    }

    /** Le compteur d'images décolorées part de zéro : le rattrapage
     *  PIEGE-26 le garde là. */
    @Test
    public void leCompteurDecoloreesPartDeZero() {
        EditorTestRig banc = EditorTestRig.sur(contexte, "abc\n");
        EditorView vue = banc.vue();
        assertEquals("aucune image décolorée à l'ouverture", 0L,
                vue.discoloredFrames());
    }
}
