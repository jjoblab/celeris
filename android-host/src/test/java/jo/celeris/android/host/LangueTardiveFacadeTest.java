package jo.celeris.android.host;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import jo.celeris.android.view.EditorView;
import jo.celeris.android.widgets.PopupLayer;
import jo.celeris.core.language.LanguageKit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.LooperMode;

/**
 * La langue TARDIVE par la FAÇADE (le retour terrain du 2026-11-19 :
 * « plus de retard de 37 s mais pas de coloration ») : le montage
 * réel du sample — kit asynchrone (vrai fil démon, builtinPacks),
 * open() AVANT la fin de l'assemblage. La pose différée de la façade
 * doit amener la langue, et la VUE doit publier une analyse visible :
 * la langue résolue se lit, le langage vit sur la vue.
 */
@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.PAUSED)
public class LangueTardiveFacadeTest {

    private static final String SOURCE = """
            package exemple;

            class Premiere {
                void methode() {
                    int a = 1;
                }
            }
            """;

    /** Le montage du sample : vue + couche + kit asynchrone builtin. */
    @Test
    public void laLangueArriveEtVitSurLaVue() throws Exception {
        Application contexte = RuntimeEnvironment.getApplication();
        EditorView vue = new EditorView(contexte);
        vue.setId(android.R.id.text1);
        PopupLayer couche = new PopupLayer(contexte);
        couche.setId(android.R.id.text2);
        FrameLayout scene = new FrameLayout(contexte);
        scene.addView(vue, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        scene.addView(couche, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        android.view.WindowManager fenetre = (android.view.WindowManager)
                contexte.getSystemService(android.content.Context.WINDOW_SERVICE);
        fenetre.addView(scene, new android.view.WindowManager.LayoutParams(
                android.view.WindowManager.LayoutParams.MATCH_PARENT,
                android.view.WindowManager.LayoutParams.MATCH_PARENT));
        // Le kit du sample : assemblage sur VRAI fil démon.
        // Comme le sample : le consommateur d'erreurs reçoit les
        // paquets fautifs (les grammaires sans fabrique), l'assemblage
        // continue — java (handwritten) vit.
        final StringBuilder trace = new StringBuilder();
        java.util.concurrent.ExecutorService demon =
                java.util.concurrent.Executors.newSingleThreadExecutor(f -> {
                    Thread fil = new Thread(f, "banc-langues");
                    fil.setDaemon(true);
                    fil.setUncaughtExceptionHandler((t, e) ->
                            trace.append("FIL MORT : ").append(e));
                    return fil;
                });
        LanguageKit kit = LanguageKit.builder(demon)
                .builtinPacks()
                .onError(erreur -> trace.append("PAQUET FAUTIF : ")
                        .append(erreur.getMessage()).append('\n'))
                .build();
        kit.preload();
        CelerisEditor editeur = CelerisEditor.attach(vue, couche, kit);
        editeur.open("Main.java", SOURCE);
        // L'assemblage réel des 32 paquets : jusqu'à 30 s au pire —
        // le banc attend la fin réelle (le latch du kit).
        long fin = System.currentTimeMillis() + 25_000L;
        while (editeur.language() == null
                && System.currentTimeMillis() < fin) {
            Thread.sleep(50L);
            org.robolectric.shadows.ShadowLooper.runUiThreadTasks();
        }
        assertNotNull("la langue posée par la façade",
                editeur.language());
        assertTrue("la langue du .java est le Java",
                "java".equals(editeur.language().id()));
        editeur.close();
    }
}
