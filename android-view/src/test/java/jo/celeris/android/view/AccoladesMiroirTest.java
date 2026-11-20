package jo.celeris.android.view;

import jo.celeris.android.view.internal.model.BracePair;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextSession;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * Les accolades appariées se surlignent (D-23, la reprise du
 * propriétaire — le petit rectangle qui encadre la paire) : la paire
 * sous le curseur se trouve (borne avant, borne sur), l'imbrication
 * s'y respecte, l'orpheline n'y répond pas et le plafond tient.
 *
 * <p>À l'image : les DEUX rectangles se tracent à l'encre du curseur
 * atténuée — quatre traits chacun —, et la paire étalée sur des
 * rangées porte sa JONCTION : une jambe verticale au bord gauche de
 * l'ouvrante, un coude horizontal vers la fermante. Une sélection
 * non vide coupe le surlignage — la plage porte déjà l'attention.</p>
 */
@RunWith(RobolectricTestRunner.class)
public class AccoladesMiroirTest {

    private static final String CLASSE = "class A {\n"
            + "    int b = 1;\n"
            + "}\n";

    /** La paire se trouve : le curseur juste après la fermante voit l'ouvrante. */
    @Test
    public void laPaireApresLaFermanteSeTrouve() {
        TextSession session = TextSession.of(CLASSE, System::nanoTime);
        int ouvrante = CLASSE.indexOf('{');
        int fermante = CLASSE.indexOf('}');
        int[] pair = new int[2];
        assertTrue("la paire se trouve après la fermante",
                BracePair.appariee(session.document(), fermante + 1,
                        pair));
        assertEquals("l'ouvrante d'abord", ouvrante, pair[0]);
        assertEquals("la fermante ensuite", fermante, pair[1]);
    }

    /** Le curseur posé SUR l'ouvrante voit sa fermante. */
    @Test
    public void laPaireSurLouvranteSeTrouve() {
        TextSession session = TextSession.of(CLASSE, System::nanoTime);
        int ouvrante = CLASSE.indexOf('{');
        int[] pair = new int[2];
        assertTrue("la paire se trouve sur l'ouvrante",
                BracePair.appariee(session.document(), ouvrante, pair));
        assertEquals("l'ouvrante d'abord", ouvrante, pair[0]);
        assertEquals("la fermante ensuite", CLASSE.indexOf('}'), pair[1]);
    }

    /** L'imbrication se respecte : la fermante interne épouse son ouvrante. */
    @Test
    public void laPaireImbriqueeEpouseSaBorne() {
        String text = "if (a) { f( { } ); }";
        TextSession session = TextSession.of(text, System::nanoTime);
        int interneOuvrante = text.indexOf("{ }");
        int interneFermante = interneOuvrante + 2;
        int[] pair = new int[2];
        assertTrue(BracePair.appariee(session.document(),
                interneFermante + 1, pair));
        assertEquals("la fermante interne épouse son ouvrante",
                interneOuvrante, pair[0]);
        assertEquals(interneFermante, pair[1]);
    }

    /** L'orpheline ne répond pas — ni les parenthèses, ni les crochets. */
    @Test
    public void lOrphelineNeRepondPas() {
        TextSession session = TextSession.of("foo (bar\n", System::nanoTime);
        int[] pair = new int[2];
        assertFalse("une ouvrante orpheline ne se pare pas",
                BracePair.appariee(session.document(), 6, pair));
        assertFalse("le blanc ne se pare pas",
                BracePair.appariee(session.document(), 2, pair));
    }

    /** Le plafond tient : une paire au-delà de la borne ne se trouve pas. */
    @Test
    public void lePlafondDuBalayageTient() {
        StringBuilder body = new StringBuilder(BracePair.CEILING + 16);
        body.append('{');
        for (int i = 0; i < BracePair.CEILING; i++) {
            body.append('x');
        }
        body.append('}');
        TextSession session = TextSession.of(body.toString(), System::nanoTime);
        int[] pair = new int[2];
        assertFalse("une paire au-delà du plafond ne se trouve pas",
                BracePair.appariee(session.document(),
                        body.length(), pair));
        // sous le plafond, la même paire se trouve
        body.setLength(BracePair.CEILING / 2);
        body.append('}');
        TextSession proche = TextSession.of(body.toString(), System::nanoTime);
        assertTrue("sous le plafond, la paire se trouve",
                BracePair.appariee(proche.document(),
                        body.length(), pair));
    }

    /**
     * L'image trace les DEUX rectangles ET la jonction : quatre traits
     * par rectangle à l'encre du curseur atténuée, une jambe verticale
     * au bord gauche de l'ouvrante et un coude horizontal vers la
     * fermante — la colonne de la fermante diffère, le coude s'y voit.
     */
    @Test
    public void lImageTraceLesRectanglesEtLaJonction() {
        EditorTestRig banc = EditorTestRig.on(
                RuntimeEnvironment.getApplication(), CLASSE);
        EditorView vue = banc.vue();
        int ouvrante = CLASSE.indexOf('{');
        int fermante = CLASSE.indexOf('}');
        vue.session.setSelection(MultiSelection.of(
                SelRange.caretAt(fermante + 1)));
        EditorTestRig.RecordingCanvas image = banc.image();
        int encreRectangle = avecAlpha(vue.theme.cursorColor(), 0.55f);
        int encreTrait = avecAlpha(vue.theme.cursorColor(), 0.4f);
        assertEquals("quatre traits par rectangle, deux rectangles",
                8, traitsA(image, encreRectangle));
        assertEquals("la jambe verticale et le coude horizontal",
                2, traitsA(image, encreTrait));
        float xOuvrante = vue.largeurGouttiere
                + ouvrante * vue.metrics.charWidth();
        assertTrue("la jambe verticale vit au bord gauche de l'ouvrante",
                jambeVerticaleA(image, xOuvrante, encreTrait));
    }

    /** Une sélection non vide coupe le surlignage — le curseur seul le porte. */
    @Test
    public void laSelectionNonVideCoupeLeSurlignage() {
        EditorTestRig banc = EditorTestRig.on(
                RuntimeEnvironment.getApplication(), CLASSE);
        EditorView vue = banc.vue();
        int ouvrante = CLASSE.indexOf('{');
        int fermante = CLASSE.indexOf('}');
        vue.session.setSelection(MultiSelection.of(
                new SelRange(ouvrante, fermante)));
        EditorTestRig.RecordingCanvas image = banc.image();
        int encreRectangle = avecAlpha(vue.theme.cursorColor(), 0.55f);
        int encreTrait = avecAlpha(vue.theme.cursorColor(), 0.4f);
        assertEquals("une plage ne porte pas les rectangles", 0,
                traitsA(image, encreRectangle));
        assertEquals("une plage ne porte pas la jonction", 0,
                traitsA(image, encreTrait));
    }

    /** Le nombre de traits de l'image à la couleur donnée. */
    private static int traitsA(EditorTestRig.RecordingCanvas image, int couleur) {
        int total = 0;
        for (var trait : image.lines()) {
            if (trait.color() == couleur) {
                total++;
            }
        }
        return total;
    }

    /** Vrai quand une jambe verticale vit à l'abscisse donnée (±1 px). */
    private static boolean jambeVerticaleA(EditorTestRig.RecordingCanvas image,
            float x, int couleur) {
        for (var trait : image.lines()) {
            if (trait.color() == couleur
                    && Math.abs(trait.x1() - x) <= 1f
                    && Math.abs(trait.x2() - x) <= 1f) {
                return true;
            }
        }
        return false;
    }

    /** L'alpha d'une couleur ARGB portée à son canal — comme la couche. */
    private static int avecAlpha(int couleur, float alpha) {
        int a = (int) (((couleur >>> 24) & 0xFF)
                * Math.max(0f, Math.min(1f, alpha)));
        return (a << 24) | (couleur & 0x00FFFFFF);
    }
}
