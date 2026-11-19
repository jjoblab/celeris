package jo.celeris.android.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import jo.celeris.core.text.TextSession;

/**
 * L'épissure du cache de rangées à l'édition (B-01/B-02, l'analyse
 * croisée) : une frappe ne rejette PAS tout le cache vivant — les
 * rangées AVANT la ligne touchée survivent par IDENTITÉ (re-timbrées
 * à la nouvelle révision), celles de la ligne touchée se re-mesurent,
 * celles d'après sont décalées. Le dépôt de départ documente pourquoi
 * son LineRenderCache porte une révision PAR LIGNE : « un compteur de
 * révision global invaliderait toutes les lignes en cache à chaque
 * édition, ruinant le cache ». Le cache de celeris portait la
 * révision GLOBALE dans sa clé et invalidait tout à chaque frappe.
 */
@RunWith(RobolectricTestRunner.class)
public class EpissureCacheTest {

    /** Le montage du banc : une vue sur son harnais, la vue mesurée. */
    private EditorTestRig banc(String text) {
        EditorTestRig rig = EditorTestRig.on(
                RuntimeEnvironment.getApplication(), text);
        rig.vue().layout(0, 0, 1080, 1920);
        return rig;
    }

    private RowLayout rangee(EditorView vue, long revision, int rangee) {
        return vue.cache.find(revision, rangee, vue.theme.id(),
                vue.metrics.font(), vue.projection.columns());
    }

    /** Une frappe au MILIEU du document ne rejette pas les rangées
     *  AU-DESSUS : la première rangée survit par identité (la même
     *  instance de RowLayout, re-timbrée à la nouvelle révision) —
     *  l'ancien chemin la re-mesurait (invalidation totale à chaque
     *  frappe, B-01). */
    @Test
    public void uneFrappeAuMilieuNeRejettePasLesRangeesAuDessus() {
        EditorTestRig rig = banc(
                "ligne une\nligne deux\nligne trois\nligne quatre\n");
        EditorView vue = rig.vue();
        TextSession session = vue.session();
        rig.image();
        long revisionAvant = session.document().revision();
        RowLayout hautAvant = rangee(vue, revisionAvant, 0);
        RowLayout basAvant = rangee(vue, revisionAvant, 3);
        assertNotNull("la frame a rempli le cache", hautAvant);
        assertNotNull(basAvant);

        // Une frappe au début de la ligne quatre (le bas du document).
        int debutLigneQuatre = session.document().lineStart(3);
        assertTrue(session.replaceRange(debutLigneQuatre,
                debutLigneQuatre, "X"));
        rig.image();
        long revisionApres = session.document().revision();

        // La rangée du HAUT survit par IDENTITÉ : même instance.
        RowLayout hautApres = rangee(vue, revisionApres, 0);
        assertNotNull("la rangée haute vit toujours au cache",
                hautApres);
        assertSame("la rangée haute survit par identité (pas de re-mesure)",
                hautAvant, hautApres);
        // La rangée du bas, elle, a bien été re-mesurée (texte neuf).
        RowLayout basApres = rangee(vue, revisionApres, 3);
        assertNotNull(basApres);
        assertTrue("la rangée touchée porte le texte neuf",
                basApres.text().startsWith("X"));
        assertNotSame("la rangée touchée est une nouvelle mesure",
                basAvant, basApres);
    }

    /** Une frappe qui AJOUTE une ligne décale les rangées d'après :
     *  la ligne d'après change de NUMÉRO de ligne portée — la
     *  re-mesure y est obligatoire, la survie y est interdite (une
     *  rangée décalée qui survirait porterait un mauvais numéro de
     *  ligne). */
    @Test
    public void uneFrappeQuiAjouteUneLigneRemesureCorrectement() {
        EditorTestRig rig = banc("alpha\nbeta\ngamma\n");
        EditorView vue = rig.vue();
        TextSession session = vue.session();
        rig.image();
        assertEquals("quatre lignes au départ (la vide finale comprise)",
                4, session.document().lineCount());

        // Entrée au milieu de « beta » : la ligne se coupe en deux.
        int milieuBeta = session.document().lineStart(1) + 2;
        assertTrue(session.replaceRange(milieuBeta, milieuBeta, "\n"));
        rig.image();
        // La ligne vide finale compte : cinq lignes après la coupe.
        assertEquals(5, session.document().lineCount());
        long revisionApres = session.document().revision();

        // La rangée 0 survit par identité ; la rangée 2 porte la
        // NOUVELLE ligne 2 (« ta », suite de beta), pas un résidu.
        RowLayout rangee0 = rangee(vue, revisionApres, 0);
        assertNotNull(rangee0);
        RowLayout rangee2 = rangee(vue, revisionApres, 2);
        assertEquals("la rangée 2 porte la bonne ligne",
                2, rangee2.line());
        assertTrue("la rangée 2 porte « ta »",
                rangee2.text().startsWith("ta"));
    }

    /** Le cache est BORNÉ (B-02) : 512 entrées au plus, les moins
     *  récemment servies quittent la table — un document déroulé ne
     *  l'emplifait sans plafond à vie. */
    @Test
    public void leCacheEstBorne() {
        EditorTestRig rig = banc("alpha\nbeta\ngamma\n");
        EditorView vue = rig.vue();
        int plafond = vue.cache.capacity();
        assertTrue("le cache a un plafond (512 attendu)",
                plafond <= 512);
        // La voie de dépôt elle-même borne : 2000 dépôts ne font
        // jamais plus de 512 entrées.
        FontSpec font = vue.metrics.font();
        RowLayout gabarit = new RowLayout(0, 0, 5, "alpha",
                50f, java.util.List.of(), java.util.List.of());
        for (int i = 0; i < 2000; i++) {
            vue.cache.store(new RowKey(i, i, 1, font), gabarit);
        }
        assertTrue("le plafond tient : " + vue.cache.size(),
                vue.cache.size() <= 512);
    }
}
