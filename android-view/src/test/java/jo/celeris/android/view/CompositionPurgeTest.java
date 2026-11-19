package jo.celeris.android.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import jo.celeris.core.text.TextSession;

/**
 * La composition IME appartient AU document : changer de session (le
 * retour terrain des onglets) clôt la composition en vol — ses offsets
 * ne s'appliquent JAMAIS au document qui arrive (A-19, l'analyse
 * croisée : le dépôt de départ purge explicitement, celeris laissait la
 * composition collée à l'onglet mort).
 */
@RunWith(RobolectricTestRunner.class)
public class CompositionPurgeTest {

    /** Le montage du banc : une vue sur son harnais. */
    private EditorTestRig banc(String text) {
        return EditorTestRig.on(
                org.robolectric.RuntimeEnvironment.getApplication(), text);
    }

    /** Le backspace IME juste après un changement d'onglet agit sur le
     *  document NEUF : la composition morte de l'onglet quitté ne
     *  prête plus ses offsets (l'ancien chemin détruisait les
     *  premiers caractères du document arrivé). */
    @Test
    public void leBackspaceImeApresChangementAgitSurLeDocumentNeuf() {
        EditorTestRig banc = banc("hello world\n");
        EditorView vue = banc.vue();
        TextSession sessionA = vue.session();
        // Composition IME vivante sur le document A (voie publique).
        banc.composerTexte("XX");
        assertEquals("XXhello world\n",
                sessionA.document().text());
        // Changement d'onglet : B arrive avec SA sélection (le curseur
        // au 'f', comme un onglet rouvert), la composition de A meurt.
        TextSession sessionB = TextSession.of("ef\n", System::nanoTime);
        sessionB.setSelection(jo.celeris.core.text.MultiSelection.caretsAt(2));
        vue.setSession(sessionB);
        // Backspace IME (deleteSurroundingText du composeur) : il ne
        // retire que selon le document NEUF — le 'f', pas un bloc aux
        // offsets de la composition morte.
        assertTrue(banc.connexionPublique().deleteSurroundingText(1, 0));
        assertEquals("e\n", sessionB.document().text());
        assertEquals("XXhello world\n", sessionA.document().text());
    }

    /** Une composition relancée après le changement compose bien sur
     *  le document neuf — la purge ne casse pas la saisie suivante. */
    @Test
    public void laCompositionReprendSurLeDocumentNeuf() {
        EditorTestRig banc = banc("un\n");
        EditorView vue = banc.vue();
        banc.composerTexte("Z");
        vue.setSession(TextSession.of("deux\n", System::nanoTime));
        banc.composerTexte("Y");
        assertEquals("Ydeux\n", vue.session().document().text());
    }
}
