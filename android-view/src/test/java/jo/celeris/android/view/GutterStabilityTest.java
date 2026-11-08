package jo.celeris.android.view;

import static org.junit.Assert.assertEquals;

import java.util.Map;
import java.util.Set;

import jo.celeris.core.language.LanguageSupport;
import jo.celeris.core.syntax.JavaSyntax;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * Les défauts de gouttière constatés dans la démonstration (M7.5, §5) :
 * sa largeur dépend de grandeurs qui bougent sans rapport avec le
 * nombre de lignes — les RANGÉES visuelles au lieu des lignes (retour
 * à la ligne, plis), l'arrivée d'un résultat d'intégration, le dixième
 * caractère de numéro (PIEGE-25).
 *
 * <p>Écrits ROUGES sur le code courant : la largeur ne doit JAMAIS
 * bouger à la dixième ligne, à l'arrivée de marques, au retour à la
 * ligne ni au repli — le champ de numéros se calcule sur les LIGNES du
 * document, avec plancher de chiffres, marges en dp et coulisse de
 * configuration.</p>
 */
@RunWith(RobolectricTestRunner.class)
public class GutterStabilityTest {

    /** Neuf lignes puis la dixième : la largeur ne bouge pas d'un cran. */
    @Test
    public void gutterWidthDoesNotJumpWhenTypingTenthLine() {
        EditorTestRig banc = EditorTestRig.sur(
                RuntimeEnvironment.getApplication(), "un\ndeux\ntrois\n"
                + "quatre\ncinq\nsix\nsept\nhuit\nneuf");
        banc.image();
        float avant = banc.vue().largeurGouttiere;
        banc.validerTexte("\ndix");
        banc.image();
        assertEquals("la dixième ligne n'élargit pas la gouttière "
                        + "(plancher de trois chiffres)",
                avant, banc.vue().largeurGouttiere, 1e-3f);
    }

    /** L'arrivée de marques d'intégration ne change pas la largeur. */
    @Test
    public void gutterWidthIsStableWhenIntegrationMarksArrive() {
        EditorTestRig banc = EditorTestRig.sur(
                RuntimeEnvironment.getApplication(), "un\ndeux\ntrois");
        banc.image();
        float avant = banc.vue().largeurGouttiere;
        banc.vue().marquesGouttiere().setIntegration(
                MarquesIntegration.of(Map.of(1, MarqueLigne.MODIFIE),
                        Map.of(), null, Set.of()));
        banc.image();
        assertEquals("l'arrivée de marques ne décale pas le texte",
                avant, banc.vue().largeurGouttiere, 1e-3f);
    }

    /** Le retour à la ligne change les rangées, jamais la gouttière. */
    @Test
    public void gutterWidthIsStableAcrossWrapToggle() {
        StringBuilder texte = new StringBuilder();
        for (int i = 0; i < 800; i++) {
            texte.append("ligne ").append(i).append(" : ")
                    .append("contenu assez long pour deborder largement "
                            + "la fenêtre sans retour a la ligne, encore "
                            + "un peu plus de texte, encore un peu, et "
                            + "voila enfin la fin de cette ligne tres "
                            + "longue qui va s'enrouler plusieurs fois.\n");
        }
        EditorTestRig banc = EditorTestRig.sur(
                RuntimeEnvironment.getApplication(), texte.toString());
        banc.image();
        float sansRetour = banc.vue().largeurGouttiere;
        banc.vue().setSoftWrap(true);
        banc.image();
        assertEquals("le retour a la ligne ne touche pas la gouttière",
                sansRetour, banc.vue().largeurGouttiere, 1e-3f);
    }

    /** Replier un bloc change les rangées, jamais la gouttière. */
    @Test
    public void gutterWidthDoesNotShrinkWhenFolding() {
        StringBuilder texte = new StringBuilder("class A {\n");
        texte.append("    void m() {\n");
        for (int i = 0; i < 1000; i++) {
            texte.append("        int valeur").append(i).append(" = 1;\n");
        }
        texte.append("    }\n}\n");
        EditorTestRig banc = EditorTestRig.sur(
                RuntimeEnvironment.getApplication(), texte.toString());
        EditorView vue = banc.vue();
        vue.setLanguage(LanguageSupport.of(JavaSyntax.INSTANCE));
        banc.publierAnalyse();
        banc.image();
        float deplie = vue.largeurGouttiere;
        assertEquals("le pli de la méthode se pose", true,
                vue.syntaxe.basculerPli(1));
        banc.image();
        assertEquals("replier un bloc ne rétrécit pas la gouttière",
                deplie, vue.largeurGouttiere, 1e-3f);
    }

    /** Deux images sans édition : le texte ne bouge pas d'un pixel. */
    @Test
    public void textXDoesNotChangeBetweenConsecutiveFrames() {
        EditorTestRig banc = EditorTestRig.sur(
                RuntimeEnvironment.getApplication(), "int x = 1;\nint y = 2;");
        EditorTestRig.RecordingCanvas premiere = banc.image();
        EditorTestRig.RecordingCanvas seconde = banc.image();
        assertEquals("aucun tracé ne bouge entre deux images stables",
                premiere.traces().toString(), seconde.traces().toString());
    }
}
