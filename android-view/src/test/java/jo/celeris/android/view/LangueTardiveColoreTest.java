package jo.celeris.android.view;

import static org.junit.Assert.assertTrue;

import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.syntax.clike.JavaSyntax;

import org.junit.Test;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;

import org.junit.runner.RunWith;

/**
 * La langue TARDIVE colore quand même (le retour terrain du
 * 2026-11-19 : « plus de retard de 37 s mais pas de coloration ») :
 * l'ouverture ne patiente plus l'assemblage du registre — la session
 * se pose D'ABORD, la langue arrive APRÈS par la pose différée de la
 * façade. La vue doit alors rebrancher son moteur et publier une
 * analyse qui se voit : l'image se pare de couleurs, jamais l'écran
 * ne reste à l'encre de base.
 */
@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.PAUSED)
public class LangueTardiveColoreTest {

    private static final String SOURCE = """
            package exemple;

            class Premiere {
                void methode() {
                    int a = 1;
                }
            }
            """;

    /** La séquence de la façade : session d'abord, langue en retard —
     *  l'image doit montrer de la couleur après publication. */
    @Test
    public void laLanguePoseeApresLaSessionColoreLEcran() {
        EditorTestRig banc = EditorTestRig.on(
                org.robolectric.RuntimeEnvironment.getApplication(), SOURCE);
        // La langue arrive APRÈS la session (pose différée) :
        banc.vue().setLanguage(LanguageSupport.of(JavaSyntax.INSTANCE));
        banc.publierAnalyse();
        // L'image doit se parer : « class » (mot réservé) prend son
        // encre, distincte de l'identifiant « methode ».
        EditorTestRig.RecordingCanvas image = banc.image();
        int encreReserve = image.couleurDe("class");
        int encreIdentifiant = image.couleurDe("methode");
        assertTrue("le mot réservé est tracé", encreReserve != 0);
        assertTrue("l'identifiant est tracé", encreIdentifiant != 0);
        assertTrue("la langue tardive colore : le mot réservé « class »"
                + " doit se distinguer de l'identifiant ("
                + String.format("0x%08X", encreReserve)
                + " vs "
                + String.format("0x%08X", encreIdentifiant) + ")",
                encreReserve != encreIdentifiant);
    }
}
