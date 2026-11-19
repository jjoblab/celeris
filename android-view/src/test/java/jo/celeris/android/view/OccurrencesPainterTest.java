package jo.celeris.android.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import jo.celeris.core.text.MultiSelection;

/**
 * C11 (l'analyse croisée) : le mot au caret s'allume — les
 * occurrences ENTières du mot courant se voilent sous le texte, la
 * couche se tait sur les non-identifiants (ponctuation, nombres) et
 * pendant les sélections étendues.
 *
 * <p>Le compteur du banc : les rectangles pleins de l'image dont la
 * couleur porte l'alpha du voile d'occurrences (0x33) — la convention
 * de {@link BandeEtPorteeActiveTest} (filtrer les rects par teinte),
 * l'alpha distingue le voile d'occurrence de la sélection (0x99) et
 * de la bande de ligne (0x18).</p>
 */
@RunWith(RobolectricTestRunner.class)
public class OccurrencesPainterTest {

    /** L'alpha du voile d'occurrence — trois fois plus discret que
     *  la sélection : le mot s'illumine sans crier. */
    private static final int ALPHA_OCCURRENCES = 0x33;

    private Context contexte;

    @Before
    public void preparer() {
        contexte = RuntimeEnvironment.getApplication();
    }

    /** Le banc posé, la couche armée, une image dessinée. */
    private EditorTestRig.RecordingCanvas banc(String contenu, int caret) {
        EditorTestRig banc = EditorTestRig.on(contexte, contenu);
        banc.vue().setOccurrencesVisibles(true);
        banc.vue().session().setSelection(MultiSelection.caretsAt(caret));
        banc.vue().invalidate();
        return banc.image();
    }

    /** Les rects voilés à l'alpha d'occurrence dans l'image. */
    private int voiles(EditorTestRig.RecordingCanvas image) {
        int n = 0;
        for (EditorTestRig.RecordingCanvas.RectDraw rect : image.rectangles()) {
            if ((rect.color() >>> 24) == ALPHA_OCCURRENCES) {
                n++;
            }
        }
        return n;
    }

    /** Le mot au caret allume ses occurrences : « mot » en trois
     *  exemplaires se voilent, le pluriel « mots » reste nu — la
     *  frontière est le MOT ENTIER, casse exacte. */
    @Test
    public void leMotAuCaretAllumeSesOccurrences() {
        EditorTestRig.RecordingCanvas image = banc(
                "mot un\nmot deux\nmots trois\nmot\n", 0);
        assertEquals("trois occurrences exactes du mot courant",
                3, voiles(image));
    }

    /** La ponctuation n'allume rien : le caret APRÈS le « ; » ne
     *  touche aucun identifiant (ni avant ni après) — wordAt rend
     *  null, la couche reste muette. */
    @Test
    public void laPonctuationNAllumeRien() {
        String text = "a; b; c;\n";
        int apresPointVirgule = text.indexOf(';') + 1;
        EditorTestRig.RecordingCanvas image = banc(text, apresPointVirgule);
        assertEquals("aucune occurrence peinte", 0, voiles(image));
    }

    /** Une sélection étendue coupe la couche : le mot courant parle
     *  par sa sélection, pas par ses occurrences. */
    @Test
    public void laSelectionEtendueCoupeLaCouche() {
        EditorTestRig banc = EditorTestRig.on(contexte, "mot mot mot\n");
        banc.vue().setOccurrencesVisibles(true);
        banc.vue().session().setSelection(
                MultiSelection.of(new jo.celeris.core.text.SelRange(0, 3)));
        banc.vue().invalidate();
        EditorTestRig.RecordingCanvas image = banc.image();
        assertEquals("sélection étendue : couche muette", 0, voiles(image));
    }

    /** Le voile suit le caret : le même document, un autre mot, ses
     *  propres occurrences. */
    @Test
    public void leVoileSuitLeCaret() {
        // caret sur le « beta » de la ligne 1 (offset 6)
        EditorTestRig.RecordingCanvas image = banc(
                "alpha beta\nbeta alpha\n", 6);
        assertEquals("beta allume ses deux exemplaires", 2, voiles(image));
    }

    /** Le mot au caret au MILIEU du mot y touche aussi (wordAt va
     *  dans les deux sens). */
    @Test
    public void leCaretAuMilieuDuMotYTouche() {
        // offset 8 = milieu de « beta » ligne 1
        EditorTestRig.RecordingCanvas image = banc(
                "alpha beta\nbeta alpha\n", 8);
        assertEquals("le caret au milieu allume quand même", 2, voiles(image));
    }

    /** L'ordre du montage : la bande de ligne courante (0x18) se
     *  trace AVANT le voile d'occurrence (0x33) — la bande éclaire,
     *  le voile colore, la sélection (0x99) viendrait par-dessus.
     *      */
    @Test
    public void leVoileVitSousLaSelection() {
        EditorTestRig.RecordingCanvas image = banc(
                "alpha beta\nbeta alpha\n", 0);
        int premiereBande = -1;
        int premierVoile = -1;
        int i = 0;
        for (EditorTestRig.RecordingCanvas.RectDraw rect : image.rectangles()) {
            int alpha = rect.color() >>> 24;
            if (alpha == 0x18 && premiereBande < 0) {
                premiereBande = i;
            }
            if (alpha == ALPHA_OCCURRENCES && premierVoile < 0) {
                premierVoile = i;
            }
            i++;
        }
        assertTrue("la bande de ligne courante précède le voile",
                premiereBande >= 0 && premierVoile > premiereBande);
    }
}
