package jo.celeris.demo;

import static org.junit.Assert.assertEquals;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import jo.celeris.android.view.EditorView;
import jo.celeris.android.view.FontSpec;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * PIEGE-24 sur la vitre : le pincement réel zoome par paliers — un
 * pincement ample porte la police au palier maximal 28 sp, un
 * pincement resserré ramène au palier minimal 14 sp. Le banc JVM
 * ({@code PinchZoomGestureTest}) a prouvé la logique du détecteur sur
 * de vrais {@code MotionEvent} ; celui-ci prouve la voie d'entrée
 * réelle — deux pointeurs vivants injectés par la voie du système
 * (la même qu'UiAutomator), l'écartement exact du banc JVM (120 à 520
 * pour l'ample, 400 à 160 pour le resserré).
 */
@RunWith(AndroidJUnit4.class)
public class PinchZoomOnDeviceTest {

    /** Un pincement ample porte la police de 14 sp au palier 28 sp. */
    @Test
    public void lePincementAmpleZoomeAuPalierMaximal()
            throws Exception {
        try (ActivityScenario<EditorActivity> scenario = BancAppareil.ouvrirScenario()) {
            BancAppareil.attendreVue(scenario);
            float[] taille = new float[1];
            scenario.onActivity(ecran ->
                    taille[0] = BancAppareil.editeur(ecran).fontSizeSp());
            assertEquals("la police de départ vit au palier 14 sp",
                    14f, taille[0], 1e-6f);
            BancAppareil.pincer(scenario, 120f, 520f);
            scenario.onActivity(ecran ->
                    taille[0] = BancAppareil.editeur(ecran).fontSizeSp());
            assertEquals("un pincement ample doit mener au palier maximal 28 sp",
                    28f, taille[0], 1e-6f);
        }
    }

    /** Un pincement resserré ramène 28 sp au palier minimal 14 sp. */
    @Test
    public void lePincementResserreRameneAuPalierMinimal()
            throws Exception {
        try (ActivityScenario<EditorActivity> scenario = BancAppareil.ouvrirScenario()) {
            BancAppareil.attendreVue(scenario);
            float[] taille = new float[1];
            scenario.onActivity(ecran -> {
                EditorView editeur = BancAppareil.editeur(ecran);
                editeur.setFont(new FontSpec("mono", 28f));
                taille[0] = editeur.fontSizeSp();
            });
            assertEquals("la police posée vit au palier 28 sp",
                    28f, taille[0], 1e-6f);
            BancAppareil.pincer(scenario, 400f, 160f);
            scenario.onActivity(ecran ->
                    taille[0] = BancAppareil.editeur(ecran).fontSizeSp());
            assertEquals("un pincement resserré doit ramener au palier 14 sp",
                    14f, taille[0], 1e-6f);
        }
    }
}
