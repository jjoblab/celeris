package jo.celeris.android.view;

import static org.junit.Assert.assertTrue;

import android.view.KeyEvent;

import jo.celeris.core.text.MultiSelection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * La régression du propriétaire (retour de terrain du 2026-11-20, le
 * exemple EXACT) : le caret à la fin de «&nbsp;&nbsp;&nbsp;&nbsp;}&nbsp;»,
 * les DEL enchaînés effacent l'accolade, l'indentation puis remontent à
 * la ligne précédente — et TOUTE la ligne « public class MaClasse { »
 * devient invisible (seul « { » reste vers le bord droit), jusqu'à
 * Entrée qui la rend visible.
 */
@RunWith(RobolectricTestRunner.class)
public class EffacementLignePrecedenteTest {

    /** L'exemple exact du terrain. */
    private static final String DOCUMENT =
            "public class MaClasse {\n"
            + "    }\n";

    private static KeyEvent del() {
        long t = System.currentTimeMillis();
        return new KeyEvent(t, t, KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_DEL, 0, 0);
    }

    /** La jointure du texte tracé, avec chaque tracé et sa position. */
    private static String diagnostic(EditorTestRig.RecordingCanvas image) {
        StringBuilder sb = new StringBuilder();
        for (EditorTestRig.RecordingCanvas.TextDraw trace : image.traces()) {
            sb.append('[').append(trace.text()).append('@')
                    .append(Math.round(trace.x())).append(',')
                    .append(Math.round(trace.y())).append(']');
        }
        return sb.toString();
    }

    /** L'exemple du terrain : après la remontée, « public class
     *  MaClasse { » reste tracé — et À SA PLACE (bord gauche). */
    @Test
    public void laLigneEntiereResteTracetApresRemontee() {
        EditorTestRig banc = EditorTestRig.sur(
                RuntimeEnvironment.getApplication(), DOCUMENT);
        EditorView vue = banc.vue();
        banc.publierAnalyse();
        // le caret à la fin de «     } » (fin de ligne 1) : la frappe
        // exacte du terrain — DEL efface }, puis les blancs, puis le
        // saut : le caret remonte à la fin de la ligne 0
        int finLigne1 = vue.session().document().lineEnd(1);
        vue.session().setSelection(MultiSelection.caretsAt(finLigne1));
        for (int i = 0; i < 6; i++) {
            vue.onKeyDown(KeyEvent.KEYCODE_DEL, del());
            banc.image(); // la frame de chaque frappe, comme le terrain
        }
        String doc = vue.session().document().text();
        String attendu = "public class MaClasse {";
        assertTrue("le document attendu (doc = '" + doc.replace("\n", "\\n")
                + "')", attendu.equals(doc.replace("\n", "")) || doc.contains(attendu));

        EditorTestRig.RecordingCanvas image = banc.image();
        String diag = diagnostic(image);
        assertTrue("« public class MaClasse { » reste tracé au bord GAUCHE "
                + "après la remontée (tracés : " + diag + ")",
                image.traces().stream().anyMatch(t ->
                        t.text().toString().contains("public class")
                                && t.x() < vue.getWidth() / 2f));
    }

    /** Variante : la publication (couleur) en retard ne change rien —
     *  le doute du propriétaire (« peut-être la coloration ») se tranche
     *  au banc : le texte reste tracé noir même sans couleur. */
    @Test
    public void laLigneResteTracetSansCouleur() {
        EditorTestRig banc = EditorTestRig.sur(
                RuntimeEnvironment.getApplication(), DOCUMENT);
        EditorView vue = banc.vue();
        banc.retenirAnalyse();
        int finLigne1 = vue.session().document().lineEnd(1);
        vue.session().setSelection(MultiSelection.caretsAt(finLigne1));
        for (int i = 0; i < 6; i++) {
            vue.onKeyDown(KeyEvent.KEYCODE_DEL, del());
            banc.image();
        }
        EditorTestRig.RecordingCanvas image = banc.image();
        String diag = diagnostic(image);
        assertTrue("« public class MaClasse { » reste tracé sans couleur "
                + "(tracés : " + diag + ")",
                image.traces().stream().anyMatch(t ->
                        t.text().toString().contains("public class")));
    }
}
