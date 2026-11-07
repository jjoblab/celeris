package jo.celeris.android.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * Le test de fumée du banc de vue (ADR-013) : la vue réelle se pose,
 * se mesure, se dessine — le canvas enregistreur capte le texte, la
 * densité de l'appareil se lit, l'exécuteur manuel tient le travail
 * d'analyse. Ce test prouve l'étage avant tout correctif de M7.5 : un
 * banc qui ne tourne pas n'abrite aucun test rouge.
 */
@RunWith(RobolectricTestRunner.class)
public class EditorTestRigSmokeTest {

    /** La vue se dessine et le texte du document arrive au canvas. */
    @Test
    public void rigDrawsDocumentText() {
        EditorTestRig banc = EditorTestRig.sur(
                RuntimeEnvironment.getApplication(), "int x = 1;");
        EditorTestRig.RecordingCanvas image = banc.image();
        assertFalse("l'image doit tracer du texte", image.traces().isEmpty());
        assertTrue("le mot-clé doit être tracé (peu importe l'encre ici)",
                image.couleurDe("int") != 0);
    }

    /** La densité de l'appareil se lit : les sp s'y convertissent (PIEGE-23). */
    @Test
    public void rigExposesDeviceDensity() {
        EditorTestRig.sur(RuntimeEnvironment.getApplication(), "hello");
        float densite = RuntimeEnvironment.getApplication()
                .getResources().getDisplayMetrics().density;
        assertTrue("la densité doit être positive", densite > 0f);
        assertEquals("la conversion sp vers pixels suit la densité (PIEGE-23)",
                14f * densite, TaillesSp.enPixels(14f, densite, 1f), 1e-3f);
    }
}
