package jo.celeris.demo;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import jo.celeris.android.widgets.PopupController;
import jo.celeris.android.widgets.PopupLayer;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * PIEGE-27 sur la vitre : le premier popup réel de complétion ne vole
 * JAMAIS le focus d'entrée de l'éditeur. La frappe d'un identifiant
 * ouvre la liste par la voie de l'IME — le serveur de langage Java
 * répond — l'éditeur garde le focus pendant que la liste vit ; le
 * blanc referme la liste, le focus ne quitte toujours pas l'éditeur.
 * Le banc JVM ({@code PopupLayerFocusTest}) a prouvé la couche
 * focalisable-never ; celui-ci la prouve avec le premier popup réel
 * de la démonstration.
 */
@RunWith(AndroidJUnit4.class)
public class CompletionPopupOnDeviceTest {

    /** L'attente maximale de la réponse du serveur de complétion. */
    static final long ATTENTE_SERVEUR_MS = 8000L;

    /**
     * Le premier popup réel s'ouvre à la frappe, le focus reste à
     * l'éditeur ; le blanc referme la liste, le focus y reste encore.
     */
    @Test
    public void lePremierPopupReelNeVolePasLeFocusDeLediteur()
            throws InterruptedException {
        try (ActivityScenario<EditorActivity> scenario = BancAppareil.ouvrirScenario()) {
            BancAppareil.attendreVue(scenario);
            BancAppareil.focaliser(scenario);
            BancAppareil.frapper(scenario, "q");
            assertTrue("le popup de complétion s'ouvre à la frappe",
                    attendrePopupOuvert(scenario));
            scenario.onActivity(ecran -> {
                PopupLayer couche = BancAppareil.couchePopups(ecran);
                assertFalse("la couche des popups reste non focalisable (PIEGE-27)",
                        couche.isFocusable());
                assertTrue("l'éditeur garde le focus d'entrée pendant le popup",
                        BancAppareil.editeur(ecran).hasFocus());
            });
            BancAppareil.frapper(scenario, " ");
            scenario.onActivity(ecran -> {
                PopupLayer couche = BancAppareil.couchePopups(ecran);
                assertFalse("le blanc referme la liste de complétion",
                        couche.isShowing(PopupController.PopupKind.COMPLETION_LIST));
                assertTrue("le focus ne quitte jamais l'éditeur",
                        BancAppareil.editeur(ecran).hasFocus());
            });
        }
    }

    /**
     * Attend que le popup de complétion vive : la réponse du serveur de
     * langage arrive par la file de l'éditeur — le banc sonde la
     * couche jusqu'à ce que la liste se montre.
     */
    private static boolean attendrePopupOuvert(
            ActivityScenario<EditorActivity> scenario)
            throws InterruptedException {
        boolean[] ouvert = new boolean[1];
        long fin = System.currentTimeMillis() + ATTENTE_SERVEUR_MS;
        while (System.currentTimeMillis() < fin) {
            scenario.onActivity(ecran -> ouvert[0] = BancAppareil
                    .couchePopups(ecran)
                    .isShowing(PopupController.PopupKind.COMPLETION_LIST));
            if (ouvert[0]) {
                return true;
            }
            Thread.sleep(150L);
        }
        return ouvert[0];
    }
}
