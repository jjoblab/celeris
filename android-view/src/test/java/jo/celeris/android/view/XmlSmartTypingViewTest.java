package jo.celeris.android.view;

import static org.junit.Assert.assertEquals;

import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * La frappe XML branchée à la vue (C5/C6, l'analyse croisée) : le
 * chevron qui clôt une balise ouvrante insère sa fermante appariée,
 * la frappe dans un nom de balise lié se reflète dans la fermante,
 * le retour arrière y retire son miroir. Les moteurs vivaient PORTÉS
 * sans jamais être branchés (XmlLinkedTags), la fermeture de balise
 * n'existait pas.
 */
@RunWith(RobolectricTestRunner.class)
public class XmlSmartTypingViewTest {

    private EditorTestRig bancBranche(String contenu) {
        EditorTestRig banc = EditorTestRig.on(
                RuntimeEnvironment.getApplication(), contenu);
        banc.vue().intelligence().setProvider(IntelligenceHub.builtins());
        return banc;
    }

    private void curseurA(EditorTestRig banc, int offset) {
        banc.vue().session().setSelection(
                MultiSelection.of(SelRange.caretAt(offset)));
    }

    /** C5 : « > » clôt la balise ouvrante — la fermante suit, le
     *  caret vit entre les deux. */
    @Test
    public void leChevronFermeLaBalise() {
        EditorTestRig banc = bancBranche("<div");
        curseurA(banc, 4);
        banc.validerTexte(">");
        assertEquals("<div></div>",
                banc.vue().session().document().text());
        assertEquals(5, banc.vue().session().selection()
                .ranges().get(0).caret());
    }

    /** C6 : la frappe dans le nom ouvrant se reflète dans la
     *  fermante — le renommage lié. */
    @Test
    public void laFrappeDansLeNomSeReflete() {
        EditorTestRig banc = bancBranche("<div></div>");
        curseurA(banc, 4);
        banc.validerTexte("2");
        assertEquals("<div2></div2>",
                banc.vue().session().document().text());
    }
}
