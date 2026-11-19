package jo.celeris.android.view;

import static org.junit.Assert.assertTrue;

import android.view.KeyEvent;

import jo.celeris.core.text.MultiSelection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * La régression du propriétaire (retour de terrain, session 21) : des
 * DELETE enchaînés au clavier font remonter le caret à la ligne
 * précédente — et les mots de cette ligne disparaissent de l'image
 * jusqu'à la prochaine insertion (Enter les rend visibles). Le
 * document reste juste : c'est le RENDU qui perd la rangée fusionnée.
 *
 * <p>Le banc reproduit la frappe exacte : caret au début d'une ligne,
 * DEL répétés jusqu'à manger le saut de ligne précédent, puis une
 * image — les mots de la ligne fusionnée doivent y rester tracés.</p>
 */
@RunWith(RobolectricTestRunner.class)
public class FusionDeleteRenduTest {

    private static final String DOCUMENT =
            "ligne zero\nligne un\nligne deux\nligne trois";

    /** La concaténation des tranches de texte tracées dans l'image. */
    private static String textes(EditorTestRig.RecordingCanvas image) {
        StringBuilder text = new StringBuilder();
        for (EditorTestRig.RecordingCanvas.TextDraw trace : image.traces()) {
            text.append(trace.text());
        }
        return text.toString();
    }

    private static KeyEvent del() {
        long temps = System.currentTimeMillis();
        return new KeyEvent(temps, temps, KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_DEL, 0, 0);
    }

    /** Les mots de la ligne fusionnée restent dans l'image après la
     *  remontée du caret par DEL enchaînés. */
    @Test
    public void lesMotsSurviventALaFusionParDel() {
        EditorTestRig banc = EditorTestRig.on(
                RuntimeEnvironment.getApplication(), DOCUMENT);
        EditorView vue = banc.vue();
        // caret au début de la ligne 1 (offset 11) : DEL retire le
        // saut de ligne de « ligne zero » puis remonte en mangeant
        // ses derniers caractères — la frappe exacte du propriétaire.
        vue.session().setSelection(MultiSelection.caretsAt(11));
        for (int i = 0; i < 8; i++) {
            vue.onKeyDown(KeyEvent.KEYCODE_DEL, del());
        }
        // le document attendu : « lig » + « ligne un » fusionnés
        String attendu = vue.session().document().text();
        assertTrue("le document a fusionné les lignes",
                attendu.startsWith("ligligne un"));

        // l'image après la frappe : les mots de la ligne fusionnée
        // (le « ligne un » qui a remonté) doivent rester tracés.
        String trace = textes(banc.image());
        assertTrue("« ligne un » reste visible après la fusion (tracé : "
                + trace + ")",
                trace.contains("ligne un"));
    }

    /** Variante sans retour à la ligne : la même remontée garde
     *  l'image complète même quand l'analyse est retenue (la couleur
     *  en retard ne doit jamais cacher le texte). */
    @Test
    public void laFusionResteVisibleSansCouleur() {
        EditorTestRig banc = EditorTestRig.on(
                RuntimeEnvironment.getApplication(), DOCUMENT);
        EditorView vue = banc.vue();
        banc.retenirAnalyse();
        vue.session().setSelection(MultiSelection.caretsAt(11));
        for (int i = 0; i < 8; i++) {
            vue.onKeyDown(KeyEvent.KEYCODE_DEL, del());
        }
        String trace = textes(banc.image());
        assertTrue("« ligne un » reste visible sans publication (tracé : "
                + trace + ")",
                trace.contains("ligne un"));
    }
}
