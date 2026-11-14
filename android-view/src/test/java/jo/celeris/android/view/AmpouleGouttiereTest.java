package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jo.celeris.core.text.Document;

import org.junit.jupiter.api.Test;

/**
 * L'ampoule d'intentions de la gouttière (l'audit Android Studio du
 * 2026-11-14 — le propriétaire : « le petit icône ampoule jaune comme
 * dans Android Studio, je ne l'ai pas vue dans notre éditeur ») : les
 * lignes porteuses se déposent — jamais devinées, l'hôte seul décide
 * (la ligne du caret qui porte un diagnostic) —, le clic dans la
 * coulisse rend la ligne visée à l'hôte, qui y ouvre le menu
 * d'intentions ; le clic hors ampoule ne se consomme pas.
 */
class AmpouleGouttiereTest {

    @Test
    void lesAmpoulesSePosentEtLeClicDelegue() {
        MarquesGouttiere marques = new MarquesGouttiere();
        assertFalse(marques.ampoule(2), "sans pose, aucune ampoule");
        marques.setAmpoules(new int[] {2, 7});
        assertTrue(marques.ampoule(2));
        assertTrue(marques.ampoule(7));
        assertFalse(marques.ampoule(3), "la voisine reste nue");
        // le retrait vide la coulisse
        marques.setAmpoules(new int[0]);
        assertFalse(marques.ampoule(2));
        marques.setAmpoules(new int[] {2});

        int[] ouvertes = {-1};
        marques.setSurAmpoule(ligne -> ouvertes[0] = ligne);
        Document document = Document.of("l0\nl1\nl2\nl3\nl4\nl5\nl6\nl7");
        VisualProjection projection = new VisualProjection(document,
                new FoldModel(), 0);
        // le clic dans la coulisse sur la ligne de l'ampoule délègue
        assertTrue(marques.surAppuiAmpoule(5f, 2 * 16f + 8f, 16f, 0f, 16f,
                projection));
        assertEquals(2, ouvertes[0]);
        // le clic hors ampoule ne se consomme pas
        assertFalse(marques.surAppuiAmpoule(5f, 3 * 16f + 8f, 16f, 0f, 16f,
                projection));
        assertEquals(2, ouvertes[0]);
        // le clic hors coulisse ne se consomme pas
        assertFalse(marques.surAppuiAmpoule(30f, 2 * 16f + 8f, 16f, 0f, 16f,
                projection));
        assertEquals(2, ouvertes[0]);
    }

    @Test
    void sansEcouteurLampouleNeSePrendPas() {
        MarquesGouttiere marques = new MarquesGouttiere();
        marques.setAmpoules(new int[] {2});
        Document document = Document.of("l0\nl1\nl2");
        VisualProjection projection = new VisualProjection(document,
                new FoldModel(), 0);
        // sans écouteur, le clic traverse — le texte garde son geste
        assertFalse(marques.surAppuiAmpoule(5f, 2 * 16f + 8f, 16f, 0f, 16f,
                projection));
        // la pose reste lisible quand même
        assertTrue(marques.ampoule(2));
    }
}
