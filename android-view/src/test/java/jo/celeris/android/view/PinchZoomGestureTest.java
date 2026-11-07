package jo.celeris.android.view;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * Les défauts du pincement constatés dans la démonstration (M7.5, §4) :
 * le zoom ne fait RIEN — le facteur du détecteur est incrémental et le
 * choix par palier le lit comme cumulé (PIEGE-24) — et le changement de
 * police ne re-mesure pas un document jamais édité : la source de
 * rangées garde les métriques de l'ancienne police (PIEGE-28).
 *
 * <p>Les gestes partent d'un écartement plus bas que l'arrivée visée :
 * le détecteur n'engage le pincement qu'après son seuil de slop
 * (environ 40 px sous Robolectric) — le facteur cumulé se mesure
 * depuis l'ENGAGEMENT, pas depuis le premier contact.</p>
 */
@RunWith(RobolectricTestRunner.class)
public class PinchZoomGestureTest {

    private static final String LONGUE_LIGNE =
            "int valeur = calculIntermediaire(un, deux, trois, quatre); "
            + "int autre = moteur(valeur, valeur, valeur, valeur); "
            + "return valeur + autre + valeur + autre + valeur;";

    /** PIEGE-24 — un pincement ample fait passer la police de 14 à 28 sp. */
    @Test
    public void pinchOutIncreasesFontSize() {
        EditorTestRig banc = EditorTestRig.sur(
                RuntimeEnvironment.getApplication(), "int x = 1;");
        banc.pincer(540f, 960f, 120f, 520f, 15);
        assertEquals("un pincement ample doit mener au palier maximal 28 sp",
                28f, banc.vue().policeLogique.size(), 1e-6f);
    }

    /** PIEGE-24 — un pincement qui resserre ramène 28 sp à 14 sp. */
    @Test
    public void pinchInDecreasesFontSize() {
        EditorTestRig banc = EditorTestRig.sur(
                RuntimeEnvironment.getApplication(), LONGUE_LIGNE);
        banc.vue().setFont(new FontSpec("mono", 28f));
        banc.pincer(540f, 960f, 400f, 160f, 15);
        assertEquals("un pincement resserré doit ramener au palier 14 sp",
                14f, banc.vue().policeLogique.size(), 1e-6f);
    }

    /**
     * PIEGE-24 — la colonne sous le point focal reste en place au zoom :
     * la colonne visée avant le palier se retrouve à la même abscisse
     * après, gouttière recalculée comprise — sur une ligne plus large
     * que la fenêtre, le défilement garde de la marge de part et
     * d'autre.
     */
    @Test
    public void pinchInKeepsColumnUnderFocalPoint() {
        EditorTestRig banc = EditorTestRig.sur(
                RuntimeEnvironment.getApplication(), LONGUE_LIGNE);
        EditorView vue = banc.vue();
        vue.setFont(new FontSpec("mono", 28f));
        banc.image();
        vue.scrollX = 400f;
        float focalX = 540f;
        float colonneAvant = (focalX - vue.largeurGouttiere + vue.scrollX)
                / vue.metrics.charWidth();
        banc.pincer(focalX, 960f, 400f, 240f, 15);
        banc.image();
        float colonneApres = (focalX - vue.largeurGouttiere + vue.scrollX)
                / vue.metrics.charWidth();
        assertEquals("la colonne sous le point focal ne bouge pas d'un cran",
                colonneAvant, colonneApres, 0.25f);
    }

    /**
     * PIEGE-28 — un changement de police re-mesure un document jamais
     * édité : la mise en page servie porte les métriques de la NOUVELLE
     * police, pas celles que la source avait capturées à la construction.
     */
    @Test
    public void zoomOnUneditedDocumentRemeasuresText() {
        EditorTestRig banc = EditorTestRig.sur(
                RuntimeEnvironment.getApplication(), "hello");
        EditorView vue = banc.vue();
        assertEquals("le document n'a jamais été édité, la révision est nulle",
                0L, vue.session().document().revision());
        vue.setFont(new FontSpec("mono", 28f));
        banc.image();
        FrameState etat = vue.etatImageCourante();
        RowLayout servie = vue.cache.find(etat.revision(), 0,
                etat.theme().id(), etat.metrics().font(),
                etat.projection().columns());
        assertEquals("la mise en page se mesure à la nouvelle police",
                5 * etat.metrics().charWidth(), servie.width(), 1e-3f);
    }
}
