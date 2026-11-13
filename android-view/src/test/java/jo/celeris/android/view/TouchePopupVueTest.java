package jo.celeris.android.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.view.KeyEvent;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * La délégation clavier des popups (chantier d'unification) : la touche
 * matérielle remonte à l'hôte AVANT la table de raccourcis — le verdict
 * CONSOMMÉ rend la main sans bouger le curseur (PIEGE-27 : la vue garde
 * le focus, la touche part à l'hôte), le verdict décliné laisse la
 * flèche à sa vie normale.
 */
@RunWith(RobolectricTestRunner.class)
public class TouchePopupVueTest {

    private static KeyEvent touche(int code) {
        return new KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, code, 0, 0);
    }

    private EditorView vue() {
        EditorTestRig banc = EditorTestRig.sur(
                RuntimeEnvironment.getApplication(), "int x = 1;\nint y = 2;");
        return banc.vue();
    }

    private int caret(EditorView vue) {
        return vue.session().selection().ranges().get(0).caret();
    }

    /** Le verdict CONSOMMÉ : la flèche sert le popup de l'hôte, le
     *  curseur ne bouge PAS — la délégation précède la table de
     *  raccourcis. */
    @Test
    public void leVerdictConsommeSertLePopupSansBougerLeCurseur() {
        EditorView vue = vue();
        List<Integer> deleguees = new ArrayList<>();
        vue.setSurTouchePopup(code -> {
            deleguees.add(code);
            return code == KeyEvent.KEYCODE_DPAD_DOWN;
        });
        assertTrue("la touche consommée se rend à l'hôte",
                vue.onKeyDown(KeyEvent.KEYCODE_DPAD_DOWN,
                        touche(KeyEvent.KEYCODE_DPAD_DOWN)));
        assertEquals("la touche déléguée se rend une fois",
                List.of(KeyEvent.KEYCODE_DPAD_DOWN), deleguees);
        assertEquals("le curseur ne bouge pas — la flèche a servi le popup",
                0, caret(vue));
    }

    /** Le verdict DÉCLINÉ : la flèche retourne à sa vie normale — le
     *  curseur descend à la rangée suivante, la table de raccourcis
     *  décide. Le crochet coupé (null) vit la même vie. */
    @Test
    public void leVerditDeclineLaisseLaFlecheASaVieNormale() {
        EditorView vue = vue();
        vue.setSurTouchePopup(code -> code == KeyEvent.KEYCODE_ESCAPE);
        assertTrue("la touche déclinée tombe dans la vie normale",
                vue.onKeyDown(KeyEvent.KEYCODE_DPAD_DOWN,
                        touche(KeyEvent.KEYCODE_DPAD_DOWN)));
        assertEquals("la flèche déclinée descend le curseur",
                11, caret(vue));
        vue.setSurTouchePopup(null);
        vue.onKeyDown(KeyEvent.KEYCODE_DPAD_UP, touche(KeyEvent.KEYCODE_DPAD_UP));
        assertEquals("le crochet coupé laisse la flèche remonter",
                0, caret(vue));
    }
}
