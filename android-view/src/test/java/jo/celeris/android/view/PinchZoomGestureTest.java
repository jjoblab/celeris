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
 * <p>Écrits ROUGES sur le code courant : le pincement réel (deux
 * pointeurs, quinze déplacements) doit faire passer la police de 14 à
 * 28 sp par paliers, la colonne sous le point focal doit tenir, et la
 * mise en page servie après un changement de police doit porter les
 * NOUVELLES métriques.</p>
 */
@RunWith(RobolectricTestRunner.class)
public class PinchZoomGestureTest {

    private static final String LONGUE_LIGNE =
            "int valeur = calculIntermediaire(un, deux, trois); return valeur;";

    /** PIEGE-24 — un pincement réel qui double l'écartement double la police (14 → 28 sp). */
    @Test
    public void pinchOutIncreasesFontSize() {
        EditorTestRig banc = EditorTestRig.sur(
                RuntimeEnvironment.getApplication(), "int x = 1;");
        banc.pincer(540f, 960f, 200f, 400f, 15);
        assertEquals("l'écartement doublé doit mener au palier 28 sp",
                28f, banc.vue().policeLogique.size(), 1e-6f);
    }

    /** PIEGE-24 — un pincement qui réduit l'écartement de moitié ramène 28 → 14 sp. */
    @Test
    public void pinchInDecreasesFontSize() {
        EditorTestRig banc = EditorTestRig.sur(
                RuntimeEnvironment.getApplication(), LONGUE_LIGNE);
        banc.vue().setFont(new FontSpec("mono", 28f));
        banc.pincer(540f, 960f, 400f, 200f, 15);
        assertEquals("l'écartement réduit de moitié doit ramener au palier 14 sp",
                14f, banc.vue().policeLogique.size(), 1e-6f);
    }

    /**
     * PIEGE-24 — la colonne sous le point focal reste en place au zoom :
     * la colonne visée avant le palier se retrouve à la même abscisse
     * après, gouttière recalculée comprise.
     */
    @Test
    public void pinchInKeepsColumnUnderFocalPoint() {
        EditorTestRig banc = EditorTestRig.sur(
                RuntimeEnvironment.getApplication(), LONGUE_LIGNE);
        EditorView vue = banc.vue();
        vue.setFont(new FontSpec("mono", 28f));
        banc.image();
        float focalX = 540f;
        float colonneAvant = (focalX - vue.largeurGouttiere + vue.scrollX)
                / vue.metrics.charWidth();
        banc.pincer(focalX, 960f, 400f, 200f, 15);
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
