package jo.celeris.android.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import jo.celeris.core.ai.SuggestionFantome;
import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.syntax.clike.JavaSyntax;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * PIEGE-12 — le chemin de rendu façonné ne tisse pas les indices en
 * ligne : les lignes qui en portent retombent sur le chemin tissé, au
 * prix des ligatures sur ces lignes.
 *
 * <p>Le texte fantôme (F-01) est l'indice en ligne éphémère du dépôt —
 * PIEGE-11 en a posé la discipline d'encre. Ici, la ligne qui le porte
 * se trace par le MÊME chemin tissé que sans lui : chaque tracé du
 * code revient IDENTIQUE — texte, encre, position — et l'indice se
 * tisse PAR-DESSUS, à l'encre fanée, APRÈS le code, jamais fusionné à
 * la mise en page de la rangée.</p>
 */
@RunWith(RobolectricTestRunner.class)
public class InlayWovenPathViewTest {

    /**
     * La ligne qui porte l'indice retombe au chemin tissé : le code se
     * trace exactement comme sans indice, l'indice s'y ajoute dessus,
     * fané, après lui.
     */
    @Test
    public void linesWithInlaysFallBackToWovenPath() {
        EditorTestRig banc = EditorTestRig.sur(
                RuntimeEnvironment.getApplication(), "int nombre = 1;");
        EditorView vue = banc.vue();
        vue.setLanguage(LanguageSupport.of(JavaSyntax.INSTANCE));
        banc.publierAnalyse();
        // l'image sans indice : le chemin tissé du code
        EditorTestRig.RecordingCanvas sansIndice = banc.image();

        // l'indice en ligne se pose à la fin de la ligne (F-01)
        vue.setFantome(new SuggestionFantome("Nombre",
                vue.session().document().length(),
                vue.session().document().revision()));
        EditorTestRig.RecordingCanvas avecIndice = banc.image();

        List<EditorTestRig.RecordingCanvas.TextDraw> tissage = sansIndice.traces();
        List<EditorTestRig.RecordingCanvas.TextDraw> tissageIndice =
                avecIndice.traces();
        assertTrue("l'image avec indice trace davantage",
                tissageIndice.size() > tissage.size());
        // chaque tracé du code revient IDENTIQUE — texte, encre,
        // position : la rangée ne se remet pas en page pour l'indice
        for (EditorTestRig.RecordingCanvas.TextDraw trace : tissage) {
            assertTrue("le tracé tissé revient identique : " + trace.text(),
                    tissageIndice.indexOf(trace) >= 0);
        }
        // les tracés SUPPLÉMENTAIRES sont l'indice, à l'encre fanée
        // (PIEGE-11) — ils occupent la QUEUE : l'indice se tisse
        // PAR-DESSUS le code, après lui, jamais fusionné
        int fanes = 0;
        int premierFane = -1;
        for (int i = 0; i < tissageIndice.size(); i++) {
            EditorTestRig.RecordingCanvas.TextDraw trace = tissageIndice.get(i);
            if ((trace.color() >>> 24) == 0x99) {
                if (premierFane < 0) {
                    premierFane = i;
                }
                fanes++;
            }
        }
        assertTrue("l'indice se trace à l'encre fanée (PIEGE-11)", fanes > 0);
        assertEquals("les tracés supplémentaires sont exactement l'indice",
                tissageIndice.size() - tissage.size(), fanes);
        assertEquals("l'indice vient après le code tissé",
                tissageIndice.size() - fanes, premierFane);
        assertTrue("l'indice porte le texte suggéré",
                tissageIndice.get(premierFane).text().toString()
                        .startsWith("Nombre"));
    }
}
