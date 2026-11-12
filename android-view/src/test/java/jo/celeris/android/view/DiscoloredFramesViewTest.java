package jo.celeris.android.view;

import static org.junit.Assert.assertEquals;

import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.syntax.clike.JavaSyntax;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * Le compteur d'images décolorées (M7.5, §9 — la surimpression de
 * diagnostic s'y abreuve) : il ne compte QUE les images qui tracent
 * une rangée sans couleur qu'elle possédait — avant la première
 * publication rien ne se compte (rien n'avait de couleur à perdre),
 * et le rattrapage PIEGE-26 le garde à zéro à la frappe retenue.
 */
@RunWith(RobolectricTestRunner.class)
public class DiscoloredFramesViewTest {

    /** Avant la première publication, aucune image ne se compte. */
    @Test
    public void beforeFirstPublicationNothingCounts() {
        EditorTestRig banc = EditorTestRig.sur(
                RuntimeEnvironment.getApplication(), "int x = 1;");
        banc.vue().setLanguage(LanguageSupport.of(JavaSyntax.INSTANCE));
        // analyse retenue : aucune publication encore
        banc.retenirAnalyse();
        banc.image();
        banc.image();
        assertEquals("rien n'avait de couleur à perdre",
                0L, banc.vue().discoloredFrames());
    }

    /**
     * À la frappe retenue, le rattrapage sert les couleurs : le
     * compteur reste à zéro — la surimpression le prouve en direct
     * (PIEGE-26).
     */
    @Test
    public void retainedKeystrokesKeepTheCounterAtZero() {
        EditorTestRig banc = EditorTestRig.sur(
                RuntimeEnvironment.getApplication(), "int x = 1;");
        banc.vue().setLanguage(LanguageSupport.of(JavaSyntax.INSTANCE));
        banc.publierAnalyse();
        banc.image();
        for (int i = 0; i < 12; i++) {
            banc.vue().session().setSelection(MultiSelection.of(
                    SelRange.caretAt(banc.vue().session().document().length())));
            banc.validerTexte("a");
            banc.retenirAnalyse();
            banc.image();
        }
        assertEquals("le rattrapage ne laisse aucune image décolorée",
                0L, banc.vue().discoloredFrames());
    }
}
