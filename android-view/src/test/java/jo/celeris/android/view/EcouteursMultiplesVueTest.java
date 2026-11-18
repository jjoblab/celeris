package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * Les écouteurs multi-abonnés de la vue (ADR-0022, phase 2, T3 — le
 * point 13 de la revue du 2026-11-17 : huit poseurs à emplacement
 * unique s'écrasaient l'un l'autre). Les paires add…Listener /
 * remove…Listener notifient CHACUN leurs abonnés : le kit et l'hôte
 * s'abonnent ensemble, le retrait de l'un ne coupe jamais l'autre ;
 * la voie 1.x set… vit en compatibilité.
 */
@RunWith(RobolectricTestRunner.class)
public class EcouteursMultiplesVueTest {

    /** Document long : le défilement y survit au bornage de
     *  l'assemblage d'image (comme le banc des ancres). */
    private static final String DOCUMENT = ("int a = 1;\n").repeat(200);

    private EditorTestRig banc() {
        return EditorTestRig.sur(RuntimeEnvironment.getApplication(), DOCUMENT);
    }

    /** Deux abonnés du changement de fenêtre notifiés ENSEMBLE — le
     *  repositionneur du kit et l'écouteur de l'hôte ne s'écrasent
     *  plus (le défaut du point 13 : le second poseur écrasait le
     *  premier en silence). */
    @Test
    public void deuxAbonnesViewportNotifiesEnsemble() {
        EditorTestRig banc = banc();
        EditorView vue = banc.vue();
        int[] avisKit = { 0 };
        int[] avisHote = { 0 };
        vue.addViewportChangeListener(() -> avisKit[0]++);
        vue.addViewportChangeListener(() -> avisHote[0]++);
        banc.image();
        assertEquals(1, avisKit[0], "le kit reçoit le premier avis");
        assertEquals(1, avisHote[0], "l'hôte reçoit le même avis");

        vue.scrollY = 120f;
        banc.image();
        assertEquals(2, avisKit[0], "le défilement atteint le kit");
        assertEquals(2, avisHote[0], "le défilement atteint l'hôte");
    }

    /** Le retrait d'un abonné ne coupe jamais l'autre — la voie
     *  remove…Listener détache sans casse. */
    @Test
    public void leRetraitDunAbonneNeCoupePasLAutre() {
        EditorTestRig banc = banc();
        EditorView vue = banc.vue();
        int[] avisKit = { 0 };
        int[] avisHote = { 0 };
        Runnable abonneKit = () -> avisKit[0]++;
        vue.addViewportChangeListener(abonneKit);
        vue.addViewportChangeListener(() -> avisHote[0]++);
        banc.image();
        assertEquals(1, avisKit[0]);
        assertEquals(1, avisHote[0]);

        vue.removeViewportChangeListener(abonneKit);
        vue.scrollY = 240f;
        banc.image();
        assertEquals(1, avisKit[0], "l'abonné retiré ne reçoit plus rien");
        assertEquals(2, avisHote[0], "l'abonné vivant continue de recevoir");
    }

    /** Deux abonnés du changement de fenêtre notifiés ensemble — le
     *  kit et l'hôte s'abonnent sans s'écraser (la voie 2.0 seule
     *  vit désormais, les poseurs 1.x sont retirés). */
    @Test
    public void deuxAbonnesFenetreNotifiesEnsemble() {
        EditorTestRig banc = banc();
        EditorView vue = banc.vue();
        int[] avisKit = { 0 };
        int[] avisHote = { 0 };
        vue.addViewportChangeListener(() -> avisKit[0]++);
        vue.addViewportChangeListener(() -> avisHote[0]++);
        banc.image();
        assertEquals(1, avisKit[0], "l'abonné du kit reçoit l'avis");
        assertEquals(1, avisHote[0], "l'abonné de l'hôte reçoit le même avis");
    }

    /** Deux abonnés du changement de taille de police notifiés
     *  ensemble — la persistance de l'hôte et la façade du kit
     *  vivent sur le même événement (D-15). */
    @Test
    public void deuxAbonnesTaillePoliceNotifiesEnsemble() {
        EditorTestRig banc = banc();
        EditorView vue = banc.vue();
        float[] taillesKit = { 0f };
        float[] taillesHote = { 0f };
        vue.addFontSizeChangeListener(taille -> taillesKit[0] = taille);
        vue.addFontSizeChangeListener(taille -> taillesHote[0] = taille);
        vue.setFont(new FontSpec("mono", 17f));
        assertEquals(17f, taillesKit[0], 0.001f,
                "le kit lit la nouvelle taille");
        assertEquals(17f, taillesHote[0], 0.001f,
                "l'hôte lit la même taille");
    }

    /** Le verdict du double-tap se CUMULE : l'abonné qui consomme ne
     *  prive pas l'autre de son verdict — chacun rend le sien, la
     *  cascade s'arrête dès qu'un consomme. */
    @Test
    public void leVerdictDoubleTapAtteintChaqueAbonne() {
        EditorView vue = new EditorView(RuntimeEnvironment.getApplication());
        int[] offsetsRecus = { -1, -1 };
        vue.addDoubleTapListener(offset -> {
            offsetsRecus[0] = offset;
            return false;
        });
        vue.addDoubleTapListener(offset -> {
            offsetsRecus[1] = offset;
            return true;
        });
        boolean consomme = vue.rappels.notifierDoubleTap(42);
        assertEquals(true, consomme, "un abonné qui consomme rend vrai");
        assertEquals(42, offsetsRecus[0], "le premier abonné a rendu son verdict");
        assertEquals(42, offsetsRecus[1], "le second abonné a rendu son verdict");
    }

    /** La bascule de point d'arrêt atteint CHAQUE abonné (D-04) —
     *  le kit et l'hôte ensemble, la voie 2.0 seule. */
    @Test
    public void laBasculeArretAtteintChaqueAbonne() {
        EditorView vue = new EditorView(RuntimeEnvironment.getApplication());
        int[] lignesKit = { -1 };
        int[] lignesHote = { -1 };
        vue.addBreakpointToggleListener(ligne -> lignesKit[0] = ligne);
        vue.addBreakpointToggleListener(ligne -> lignesHote[0] = ligne);
        vue.rappels.notifierBasculeArret(7);
        assertEquals(7, lignesKit[0], "l'abonné du kit rend la ligne");
        assertEquals(7, lignesHote[0], "l'abonné de l'hôte rend la même ligne");
    }
}
