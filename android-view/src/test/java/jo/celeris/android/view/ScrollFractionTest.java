package jo.celeris.android.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * Le défilement par fraction (chantier C9) : la fraction verticale du
 * défilement se lit (0 en tête, 1 au pied), la part visible suit la
 * fenêtre, le défilement à une fraction se borne à l'image suivante —
 * l'avis de fenêtre y part, la minimap et le pouce s'y abreuvent.
 */
@RunWith(RobolectricTestRunner.class)
public class ScrollFractionTest {

    private Context contexte;

    @Before
    public void preparer() {
        contexte = RuntimeEnvironment.getApplication();
    }

    /** Un texte de {@code lines} lignes courtes. */
    private static String text(int lines) {
        StringBuilder contenu = new StringBuilder(lines * 3);
        for (int i = 0; i < lines; i++) {
            if (i > 0) {
                contenu.append('\n');
            }
            contenu.append("ab");
        }
        return contenu.toString();
    }

    /**
     * La fraction se lit : 0 en tête, 1 au pied — le défilement à une
     * fraction borne, l'avis de fenêtre part, la premiere ligne suit.
     */
    @Test
    public void fractionSeLitEtLeDefilementBorne() {
        EditorTestRig banc = EditorTestRig.on(contexte, text(200));
        EditorView vue = banc.vue();
        // la première image pose les métriques : la course se mesure après
        banc.image();
        assertEquals("la tête est à la fraction zéro", 0f,
                vue.scrollFractionY(), 0.001f);
        assertTrue("la part visible se borne", vue.visibleFractionY() >= 0f
                && vue.visibleFractionY() <= 1f);
        // le défilement à la moitié : la fraction suit, la fenêtre descend
        vue.scrollToFractionY(0.5f);
        banc.image();
        assertEquals("la moitié défile à la moitié", 0.5f,
                vue.scrollFractionY(), 0.15f);
        assertTrue("la premiere ligne visible a suivi",
                vue.firstVisibleLine() > 0);
        // hors des bornes : la fraction se borne aux extrêmes
        vue.scrollToFractionY(-5f);
        banc.image();
        assertEquals("négatif retombe en tête", 0f,
                vue.scrollFractionY(), 0.001f);
        vue.scrollToFractionY(5f);
        banc.image();
        assertEquals("au-delà retombe au pied", 1f,
                vue.scrollFractionY(), 0.01f);
    }

    /** Les lignes visibles se lisent — au moins une, la première suit le défilement. */
    @Test
    public void lignesVisiblesSeLivsent() {
        EditorTestRig banc = EditorTestRig.on(contexte, text(200));
        EditorView vue = banc.vue();
        banc.image();
        assertTrue("au moins une ligne visible", vue.visibleLines() >= 1);
        int avant = vue.firstVisibleLine();
        vue.scrollToFractionY(1f);
        banc.image();
        assertTrue("le pied descend la premiere ligne visible",
                vue.firstVisibleLine() >= avant);
    }

    /** Un document qui tient : la fraction reste nulle, la part visible entière. */
    @Test
    public void documentQuiTientResteNul() {
        EditorTestRig banc = EditorTestRig.on(contexte, "seule\nligne");
        EditorView vue = banc.vue();
        banc.image();
        assertEquals("tout tient : fraction nulle", 0f,
                vue.scrollFractionY(), 0.001f);
        assertEquals("tout tient : part visible entière", 1f,
                vue.visibleFractionY(), 0.001f);
        // le défilement ne casse rien : la fraction reste nulle
        vue.scrollToFractionY(1f);
        assertEquals("rien à dérouler : la fraction reste nulle", 0f,
                vue.scrollFractionY(), 0.001f);
    }
}
