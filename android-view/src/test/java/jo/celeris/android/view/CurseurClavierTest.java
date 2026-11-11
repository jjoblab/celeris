package jo.celeris.android.view;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowLooper;

/**
 * Le collaborateur du curseur et du clavier : l'appui qui prend le
 * focus demande le clavier et réveille le curseur plein ; l'appui long
 * de sélection le retire ; le curseur clignote au rythme de la
 * plateforme tant que la vue tient le focus — la perte de focus
 * l'arrête, le curseur redevient plein.
 */
@RunWith(RobolectricTestRunner.class)
public class CurseurClavierTest {

    private static final long PERIODE_MS = 530L;

    private Context contexte;
    private ShadowLooper boucle;

    @Before
    public void preparer() {
        contexte = RuntimeEnvironment.getApplication();
        boucle = Shadows.shadowOf(contexte.getMainLooper());
    }

    /**
     * Le banc attaché : le battement vit dans le looper de la fenêtre —
     * une vue détachée queue ses posts vers l'attachement (Robolectric).
     */
    private EditorTestRig bancAttache(String contenu) {
        EditorTestRig banc = EditorTestRig.sur(contexte, contenu);
        Activity hote = ActivityController.of(new Activity()).setup().get();
        hote.setContentView(banc.vue());
        return banc;
    }

    /** L'appui prend le focus et demande le clavier — le curseur se
     *  réveille plein. */
    @Test
    public void lappuiDemandeLeClavierEtReveillePlein() {
        EditorTestRig banc = bancAttache("abc\n");
        EditorView vue = banc.vue();
        assertFalse("aucun clavier demandé à l'ouverture",
                vue.curseurClavier.clavierDemande());
        vue.curseurClavier.surAppui();
        assertTrue("l'appui demande le clavier",
                vue.curseurClavier.clavierDemande());
        assertTrue("le curseur se réveille plein",
                vue.curseurClavier.visible());
    }

    /** L'appui long de sélection retire le clavier — le curseur
     *  clignote toujours. */
    @Test
    public void lappuiLongRetireLeClavier() {
        EditorTestRig banc = bancAttache("abc\n");
        EditorView vue = banc.vue();
        vue.curseurClavier.surAppui();
        vue.curseurClavier.masquerClavier();
        assertFalse("l'appui long retire le clavier",
                vue.curseurClavier.clavierDemande());
        assertTrue("le curseur reste plein jusqu'au battement",
                vue.curseurClavier.visible());
    }

    /** Le curseur clignote au rythme de la plateforme : une période
     *  l'efface, la suivante le rend. */
    @Test
    public void leCurseurClignoteAuRythme() {
        EditorTestRig banc = bancAttache("abc\n");
        EditorView vue = banc.vue();
        vue.curseurClavier.surFocus(true);
        assertTrue("réveillé plein", vue.curseurClavier.visible());
        boucle.idleFor(PERIODE_MS + 1L, java.util.concurrent.TimeUnit.MILLISECONDS);
        assertFalse("une période l'efface", vue.curseurClavier.visible());
        boucle.idleFor(PERIODE_MS + 1L, java.util.concurrent.TimeUnit.MILLISECONDS);
        assertTrue("la suivante le rend", vue.curseurClavier.visible());
    }

    /** La perte de focus arrête le battement — le curseur redevient
     *  plein et y reste. */
    @Test
    public void laPerteDeFocusArreteLeBattement() {
        EditorTestRig banc = bancAttache("abc\n");
        EditorView vue = banc.vue();
        vue.curseurClavier.surFocus(true);
        boucle.idleFor(PERIODE_MS + 1L, java.util.concurrent.TimeUnit.MILLISECONDS);
        assertFalse("le curseur clignote sous le focus",
                vue.curseurClavier.visible());
        vue.curseurClavier.surFocus(false);
        assertTrue("la perte de focus le rend plein",
                vue.curseurClavier.visible());
        boucle.idleFor(PERIODE_MS * 4L, java.util.concurrent.TimeUnit.MILLISECONDS);
        assertTrue("le battement est arrêté, le curseur y reste",
                vue.curseurClavier.visible());
    }
}
