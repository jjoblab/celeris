package jo.celeris.android.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Typeface;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * Les ligatures (chantier C13, D-14) : « liga » ouvre les ligatures de
 * la police, « -liga » les ferme — fermées par défaut, activables par
 * {@code setLigatures} ; la police posée par l'hôte sous un nom de
 * famille vit dans le tracé, la mesure et le tracé lisent la même
 * table (PIEGE-22). Test de capture : le canvas du banc capte les
 * attributs de ligature et la police de chaque tracé de texte.
 */
@RunWith(RobolectricTestRunner.class)
public class LigaturesTest {

    private Context contexte;

    @Before
    public void preparer() {
        contexte = RuntimeEnvironment.getApplication();
    }

    /** Vrai quand chaque tracé porte l'attribut de ligature donné. */
    private static boolean toutTrace(EditorTestRig.RecordingCanvas image,
            String attribut) {
        assertTrue("au moins un tracé de texte", !image.traces().isEmpty());
        for (var trace : image.traces()) {
            if (!attribut.equals(trace.ligatures())) {
                return false;
            }
        }
        return true;
    }

    /** Les ligatures fermées par défaut : « -liga » sur chaque pinceau
     *  du texte, le texte se trace lettre à lettre. */
    @Test
    public void fermeesParDefaut() {
        EditorTestRig banc = EditorTestRig.sur(contexte, "a => b != c\n");
        EditorTestRig.RecordingCanvas image = banc.image();
        assertTrue("les ligatures se ferment par défaut",
                toutTrace(image, "-liga"));
    }

    /** La bascule ouvre puis referme : « liga » puis « -liga » sur
     *  chaque pinceau du texte. */
    @Test
    public void laBasculeOuvrePuisReferme() {
        EditorTestRig banc = EditorTestRig.sur(contexte, "a => b != c\n");
        EditorView vue = banc.vue();
        vue.setLigatures(true);
        assertTrue("les ligatures s'ouvrent par la bascule",
                toutTrace(banc.image(), "liga"));
        vue.setLigatures(false);
        assertTrue("les ligatures se referment par la bascule",
                toutTrace(banc.image(), "-liga"));
    }

    /** La police posée vit dans le tracé : la famille nommée rend la
     *  police posée par l'hôte à la mesure comme au tracé (PIEGE-22) —
     *  la voie de la police à ligatures. */
    @Test
    public void laPolicePoseeVitDansLeTrace() {
        EditorTestRig banc = EditorTestRig.sur(contexte, "a => b\n");
        EditorView vue = banc.vue();
        Typeface fira = Typeface.create("fira-code", Typeface.NORMAL);
        vue.poserPolice("fira", fira);
        vue.setFont(new FontSpec("fira", 14f));
        assertSame("la règle sert la police posée (la table de la mesure)",
                fira, vue.reglePolices.policeDe("fira"));
        EditorTestRig.RecordingCanvas image = banc.image();
        assertTrue("au moins un tracé de texte", !image.traces().isEmpty());
        for (var trace : image.traces()) {
            assertSame("le texte se trace dans la police posée",
                    fira, trace.police());
        }
    }

    /** La famille active se redérive quand sa police se repose : les
     *  métriques suivent la nouvelle police, la famille dormante attend
     *  son setFont. */
    @Test
    public void laFamilleActiveSeRederiveAuRepos() {
        EditorTestRig banc = EditorTestRig.sur(contexte, "a => b\n");
        EditorView vue = banc.vue();
        Typeface autre = Typeface.create("autre-mono", Typeface.BOLD);
        // la famille active (« mono » par défaut) se repose : le tracé
        // suit aussitôt, sans setFont
        vue.poserPolice("mono", autre);
        EditorTestRig.RecordingCanvas image = banc.image();
        assertTrue("au moins un tracé de texte", !image.traces().isEmpty());
        for (var trace : image.traces()) {
            assertSame("le texte suit la police reposée sur sa famille",
                    autre, trace.police());
        }
    }
}
