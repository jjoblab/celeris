package jo.celeris.android.view;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.syntax.clike.JavaSyntax;
import jo.celeris.core.text.TextSession;

/**
 * D-07 (l'analyse croisée) : les plis manuels SURVIVENT au changement
 * d'onglet — l'état replié appartient AU DOCUMENT, pas à la vue : le
 * retour d'un document connu y retrouve ses plis, un document inconnu
 * repart déplié.
 */
@RunWith(RobolectricTestRunner.class)
public class PlisParDocumentTest {

    private static final String SOURCE = """
            class Premiere {
                void methode() {
                    int x = 1;
                }
            }
            """;

    /** Le banc posé : la vue du rig, la session A, la langue, la
     *  publication drainée — les régions pliables vivent. */
    private EditorView monter(TextSession sessionA) {
        EditorTestRig banc = EditorTestRig.sur(
                RuntimeEnvironment.getApplication(), "x\n");
        EditorView vue = banc.vue();
        vue.setSession(sessionA);
        vue.setLanguage(LanguageSupport.of(JavaSyntax.INSTANCE));
        banc.publierAnalyse();
        banc.image(); // l'habillage du dessin porte les régions pliables
        return vue;
    }

    @Test
    public void lePliSurvitAuChangementDonglet() {
        TextSession sessionA = TextSession.of(SOURCE);
        EditorView vue = monter(sessionA);

        // la méthode (ligne 1) se replie
        assertTrue("la ligne de méthode est pliable",
                vue.syntaxe.marques().pliable(1));
        assertTrue("le replie consomme", vue.syntaxe.basculerPli(1));
        assertTrue("la méthode est repliée",
                vue.plis.isFolded(1));

        // changement d'onglet : l'état replié se MÉMORISE pour A
        vue.setSession(TextSession.of("class Autre {\n}\n"));
        assertFalse("le document B repart déplié",
                vue.plis.isFolded(1));

        // retour au document A : SON pli revient
        vue.setSession(sessionA);
        assertTrue("le pli du document A revient au retour",
                vue.plis.isFolded(1));
        vue.release();
    }

    @Test
    public void leDepliagePersisteAussi() {
        TextSession sessionA = TextSession.of(SOURCE);
        EditorView vue = monter(sessionA);

        // replier puis DÉPLIER : l'état déplié se retient
        vue.syntaxe.basculerPli(1);
        assertTrue(vue.plis.isFolded(1));
        vue.syntaxe.basculerPli(1);
        assertFalse("la méthode est dépliée", vue.plis.isFolded(1));

        vue.setSession(TextSession.of("class B {\n}\n"));
        vue.setSession(sessionA);
        assertFalse("le dépliage explicite persiste au retour",
                vue.plis.isFolded(1));
        vue.release();
    }
}
