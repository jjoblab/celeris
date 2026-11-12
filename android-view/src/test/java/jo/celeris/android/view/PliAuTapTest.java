package jo.celeris.android.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.os.SystemClock;
import android.view.MotionEvent;

import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.syntax.clike.JavaSyntax;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * L'icône de pli de la gouttière répond au doigt (D-07, le constat du
 * propriétaire) : l'appui du chevron replie — le texte sous le pli
 * disparaît de l'image et se réhabille au dépliage, les rangées sous le
 * pli resservent le texte d'APRÈS le repli (PIEGE-02 : le cache des
 * mises en page s'oublie entier, sinon la rangée 4 repliée resservait
 * encore le texte d'avant). L'appui hors coulisse ne plie rien.
 *
 * <p>La garde du pli vit pour l'analyse, qu'elle soit servie par le
 * coordinateur synchrone OU par le moteur hors fil (le banc embarque
 * le moteur — la voie de production de la démonstration) : l'ancienne
 * garde {@code coordinateur == null} y tuait l'appui avant même le
 * test d'appartenance à la coulisse.</p>
 */
@RunWith(RobolectricTestRunner.class)
public class PliAuTapTest {

    private static final String CLASSE = "class A {\n"
            + "    void m() {\n"
            + "        int b = 1;\n"
            + "    }\n"
            + "}\n";

    /** Le chevron de pli répond au tap : la région sous la ligne 0 se replie. */
    @Test
    public void leChevronDePliRepondAuTap() {
        EditorTestRig banc = EditorTestRig.sur(
                RuntimeEnvironment.getApplication(), CLASSE);
        EditorView vue = banc.vue();
        vue.setLanguage(LanguageSupport.of(JavaSyntax.INSTANCE));
        banc.publierAnalyse();
        banc.image(); // l'habillage du dessin porte les régions pliables
        // la ligne 0 porte une région pliable (l'accordade de la classe)
        assertTrue("la ligne 0 est pliable",
                vue.syntaxe.marques().pliable(0));
        assertFalse("rien n'est encore replié",
                vue.syntaxe.marques().replie(0));
        tapCoulisse(banc, 0);
        assertTrue("le tap sur le chevron replie la région (D-07)",
                vue.syntaxe.marques().replie(0));
        // le dépliage suit le même chemin
        tapCoulisse(banc, 0);
        assertFalse("le second tap déplie",
                vue.syntaxe.marques().replie(0));
    }

    /**
     * Le texte sous le pli se réhabille au repli (PIEGE-02) : après le
     * repli de la région de la ligne 0, la rangée 1 ne trace plus
     * « b » — le corps replié vit sous la ligne repliée ; au dépliage
     * le corps revient.
     */
    @Test
    public void leTexteSousLePliSeRehabiliteAuReplie() {
        EditorTestRig banc = EditorTestRig.sur(
                RuntimeEnvironment.getApplication(), CLASSE);
        EditorView vue = banc.vue();
        vue.setLanguage(LanguageSupport.of(JavaSyntax.INSTANCE));
        banc.publierAnalyse();
        banc.image(); // l'habillage du dessin porte les régions pliables
        EditorTestRig.RecordingCanvas avant = banc.image();
        assertNotEquals("« b » est tracé avant le repli",
                0, avant.couleurDe("b"));
        tapCoulisse(banc, 0);
        EditorTestRig.RecordingCanvas replie = banc.image();
        assertEquals("le corps replié ne trace plus « b » (PIEGE-02)",
                0, replie.couleurDe("b"));
        tapCoulisse(banc, 0);
        EditorTestRig.RecordingCanvas deplie = banc.image();
        assertNotEquals("le corps revient au dépliage",
                0, deplie.couleurDe("b"));
    }

    /** Le tap hors coulisse ne plie rien — le curseur s'y pose. */
    @Test
    public void leTapHorsCoulisseNePliePas() {
        EditorTestRig banc = EditorTestRig.sur(
                RuntimeEnvironment.getApplication(), CLASSE);
        EditorView vue = banc.vue();
        vue.setLanguage(LanguageSupport.of(JavaSyntax.INSTANCE));
        banc.publierAnalyse();
        banc.image(); // l'habillage du dessin porte les régions pliables
        // l'appui dans le TEXTE de la rangée 0, loin de la coulisse
        float y = vue.metrics.lineHeight() / 2f;
        float x = vue.largeurGouttiere + 20f * vue.metrics.charWidth();
        long t = SystemClock.uptimeMillis();
        vue.onTouchEvent(EditorTestRig.obtenir(t, t,
                MotionEvent.ACTION_DOWN, x, y));
        vue.onTouchEvent(EditorTestRig.obtenir(t, t + 16L,
                MotionEvent.ACTION_UP, x, y));
        assertFalse("le tap dans le texte ne replie rien",
                vue.syntaxe.marques().replie(0));
    }

    /** Le tap dans la coulisse, rangée donnée — DOWN puis UP au chevron. */
    private static void tapCoulisse(EditorTestRig banc, int rangee) {
        EditorView vue = banc.vue();
        float y = (rangee + 0.5f) * vue.metrics.lineHeight();
        // la coulisse : la moitié gauche de la gouttière, où vit le chevron
        float x = vue.gouttiereEtat.coulisseCourante() / 2f;
        long t = SystemClock.uptimeMillis();
        vue.onTouchEvent(EditorTestRig.obtenir(t, t,
                MotionEvent.ACTION_DOWN, x, y));
        vue.onTouchEvent(EditorTestRig.obtenir(t, t + 16L,
                MotionEvent.ACTION_UP, x, y));
    }
}
