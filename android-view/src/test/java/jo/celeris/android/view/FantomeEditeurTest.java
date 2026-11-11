package jo.celeris.android.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.core.ai.SuggestionFantome;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * Le collaborateur du texte fantôme (F-01, ADR-012) : la vue affiche,
 * l'hôte décide — la suggestion se dépose et se lit, le geste
 * d'acceptation (Tab, l'appui sur la première ligne) part au rappel de
 * l'hôte, le geste sans suggestion ou sans rappel se refuse.
 */
@RunWith(RobolectricTestRunner.class)
public class FantomeEditeurTest {

    private Context contexte;

    @Before
    public void preparer() {
        contexte = RuntimeEnvironment.getApplication();
    }

    /** La suggestion se dépose et se retire — null la ferme. */
    @Test
    public void laSuggestionSeDeposeEtSeRetire() {
        EditorTestRig banc = EditorTestRig.sur(contexte, "abc\n");
        EditorView vue = banc.vue();
        assertNull("aucune suggestion à l'ouverture", vue.fantome.suggestion());
        SuggestionFantome suggestion = new SuggestionFantome("def", 3,
                vue.session().document().revision());
        vue.setFantome(suggestion);
        assertSame("la suggestion posée se lit", suggestion,
                vue.fantome.suggestion());
        vue.setFantome(null);
        assertNull("null la ferme", vue.fantome.suggestion());
    }

    /** Tab accepte le premier mot — le geste part au rappel de
     *  l'hôte, la vue ne touche à aucune session. */
    @Test
    public void tabAccepteLePremierMot() {
        EditorTestRig banc = EditorTestRig.sur(contexte, "abc\n");
        EditorView vue = banc.vue();
        List<GesteFantome> gestes = new ArrayList<>();
        vue.setSurGesteFantome(gestes::add);
        vue.setFantome(new SuggestionFantome("def", 3,
                vue.session().document().revision()));
        assertTrue("Tab se consomme", vue.fantome.surTab());
        assertEquals("le geste d'acceptation part à l'hôte",
                List.of(GesteFantome.ACCEPTER_MOT), gestes);
        assertEquals("la vue ne touche à aucune session", "abc\n",
                vue.session().document().text());
    }

    /** Tab refuse sans suggestion — le rappel ne part pas. */
    @Test
    public void tabRefuseSansSuggestion() {
        EditorTestRig banc = EditorTestRig.sur(contexte, "abc\n");
        EditorView vue = banc.vue();
        List<GesteFantome> gestes = new ArrayList<>();
        vue.setSurGesteFantome(gestes::add);
        assertFalse("sans suggestion, Tab se refuse", vue.fantome.surTab());
        assertTrue("aucun geste n'est parti", gestes.isEmpty());
    }

    /** Tab refuse sans rappel — le geste se tait. */
    @Test
    public void tabRefuseSansRappel() {
        EditorTestRig banc = EditorTestRig.sur(contexte, "abc\n");
        EditorView vue = banc.vue();
        vue.setFantome(new SuggestionFantome("def", 3,
                vue.session().document().revision()));
        assertFalse("sans rappel, Tab se refuse", vue.fantome.surTab());
    }

    /** L'appui sur la première ligne du fantôme accepte — le point
     *  visé vit à droite de l'ancre, dans la rangée de l'ancre. */
    @Test
    public void lappuiSurLaPremiereLigneAccepte() {
        EditorTestRig banc = EditorTestRig.sur(contexte, "abc\n");
        EditorView vue = banc.vue();
        List<GesteFantome> gestes = new ArrayList<>();
        vue.setSurGesteFantome(gestes::add);
        vue.setFantome(new SuggestionFantome("def", 3,
                vue.session().document().revision()));
        banc.image();
        // la première ligne du fantôme : la rangée de l'ancre (3 →
        // rangée 0), à droite du texte « abc »
        float x = vue.largeurGouttiere + 4f * vue.metrics.charWidth();
        float y = vue.metrics.lineHeight() / 2f;
        assertTrue("l'appui sur le fantôme se consomme",
                vue.fantome.touche(x, y));
        assertEquals("le geste part à l'hôte",
                List.of(GesteFantome.ACCEPTER_MOT), gestes);
    }

    /** L'appui hors du fantôme se refuse — ni la rangée au-dessus,
     *  ni la gauche de l'ancre. */
    @Test
    public void lappuiHorsDuFantomeSeRefuse() {
        EditorTestRig banc = EditorTestRig.sur(contexte, "abc\n");
        EditorView vue = banc.vue();
        List<GesteFantome> gestes = new ArrayList<>();
        vue.setSurGesteFantome(gestes::add);
        vue.setFantome(new SuggestionFantome("def", 3,
                vue.session().document().revision()));
        banc.image();
        float x = vue.largeurGouttiere + 4f * vue.metrics.charWidth();
        // la rangée au-dessus de l'ancre
        assertFalse("l'appui au-dessus se refuse",
                vue.fantome.touche(x, vue.metrics.lineHeight() * -1.5f));
        // à gauche de l'ancre, dans le texte
        assertFalse("l'appui dans le texte se refuse",
                vue.fantome.touche(vue.largeurGouttiere,
                        vue.metrics.lineHeight() / 2f));
        assertTrue("aucun geste n'est parti", gestes.isEmpty());
    }
}
