package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests de la géométrie de gouttière (D-01 — largeur du champ, alignement).
 */
class GutterGeometryTest {

    /** Le nombre de chiffres suit les puissances de dix. */
    @Test
    void nombresDeChiffres() {
        assertEquals(1, GutterGeometry.digits(1));
        assertEquals(1, GutterGeometry.digits(9));
        assertEquals(2, GutterGeometry.digits(10));
        assertEquals(2, GutterGeometry.digits(99));
        assertEquals(3, GutterGeometry.digits(100));
        assertEquals(6, GutterGeometry.digits(100000));
    }

    /** La largeur du champ couvre le plus grand numéro plus la respiration. */
    @Test
    void largeurDuChamp() {
        assertEquals(26f, GutterGeometry.width(99, 10f, 3f), 1e-6f);
        assertEquals(46f, GutterGeometry.width(1000, 10f, 3f), 1e-6f);
        assertEquals(13f, GutterGeometry.width(9, 10f, 1.5f), 1e-6f);
    }

    /** Le numéro s'aligne à droite : son abscisse dépend de sa largeur propre. */
    @Test
    void alignementADroite() {
        // gouttière de 46 px, caractère de 10 px, marge de 3 : « 7 » finit à 43.
        assertEquals(33f, GutterGeometry.numberX(1, 46f, 10f, 3f), 1e-6f);
        // « 42 » finit à 43.
        assertEquals(23f, GutterGeometry.numberX(2, 46f, 10f, 3f), 1e-6f);
        // « 1000 » finit à 43.
        assertEquals(3f, GutterGeometry.numberX(4, 46f, 10f, 3f), 1e-6f);
    }
}
