package jo.celeris.android.view;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.os.Looper;

import java.util.StringJoiner;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLooper;

/**
 * Le préfetch de viewport (B-03, l'analyse croisée) : après un calme
 * de 150 ms sans défilement, les rangées AU-DESSUS et EN-DESSOUS du
 * viewport se préchauffent au cache — le fling ne paie plus la mesure
 * dans la frame. Le dépôt de départ réchauffe pareil (morceaux de 8,
 * pauses entre morceaux) ; celeris ne mesurait QUE dans la boucle de
 * dessin.
 */
@RunWith(RobolectricTestRunner.class)
public class ViewportPrefetchTest {

    private static final long CALME_MS = 200;

    /** Le montage : 300 lignes, la vue pleine taille, scrollée au
     *  tiers du document. Le banc ATTACHÉ : une vue détachée queue
     *  ses posts vers l'attachement (leçon de KeyboardCursorTest),
     *  le préfetch y vivrait mort. */
    private EditorTestRig bancLong() {
        StringJoiner joiner = new StringJoiner("\n");
        for (int i = 0; i < 300; i++) {
            joiner.add("ligne numero " + i);
        }
        EditorTestRig rig = EditorTestRig.on(
                RuntimeEnvironment.getApplication(), joiner + "\n");
        android.app.Activity host = org.robolectric.android.controller.ActivityController
                .of(new android.app.Activity()).setup().get();
        host.setContentView(rig.vue());
        rig.vue().layout(0, 0, 1080, 1920);
        // la première image pose les métriques : la course se mesure
        // après (leçon de ScrollFractionTest), PUIS le défilement.
        rig.image();
        rig.vue().scrollToFractionY(0.33f);
        rig.image();
        return rig;
    }

    private RowLayout rangee(EditorView vue, int number) {
        return vue.cache.find(vue.session.document().revision(), number,
                vue.theme.id(), vue.metrics.font(),
                vue.projection.columns());
    }

    /** Après le calme, les rangées SOUS le viewport vivent au cache :
     *  le fling descendant ne paiera pas la mesure. */
    @Test
    public void leCalmePrechauffeLesRangeesSousLeViewport() {
        EditorTestRig rig = bancLong();
        EditorView vue = rig.vue();
        int premiere = vue.firstVisibleLine();
        int visibles = vue.visibleLines();
        int dessous = premiere + visibles + 4;

        // Avant le calme : rien au-delà du viewport.
        assertTrue("la rangée lointaine n'est pas encore mesurée",
                rangee(vue, dessous) == null);

        // Le calme de 200 ms : le préfetch s'exécute.
        ShadowLooper shadow = org.robolectric.Shadows.shadowOf(Looper.getMainLooper());
        shadow.idleFor(java.time.Duration.ofMillis(CALME_MS));

        assertNotNull("la rangée sous le viewport est préchauffée (B-03)",
                rangee(vue, dessous));
    }

    /** Le défilement REPRIS annule le préfetch en attente : jamais de
     *  préchauffage périmé après un nouveau scroll (la tâche se
     *  reprogramme au nouveau viewport). */
    @Test
    public void leScrollReprisReprogrammeLePrechauffage() {
        EditorTestRig rig = bancLong();
        EditorView vue = rig.vue();

        // Un nouveau scroll AVANT le calme : le préfetch suit le
        // NOUVEAU viewport, pas l'ancien.
        vue.scrollToFractionY(0.66f);
        rig.image();
        int nouvellePremiere = vue.firstVisibleLine();
        int visibles = vue.visibleLines();

        ShadowLooper shadow = org.robolectric.Shadows.shadowOf(Looper.getMainLooper());
        shadow.idleFor(java.time.Duration.ofMillis(CALME_MS));

        // La rangée sous le NOUVEAU viewport est préchauffée.
        assertNotNull("le préfetch suit le viewport courant",
                rangee(vue, nouvellePremiere + visibles + 4));
    }
}
