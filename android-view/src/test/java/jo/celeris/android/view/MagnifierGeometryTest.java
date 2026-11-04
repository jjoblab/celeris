package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests de la loupe (tranche tactile) : la loupe flotte AU-DESSUS du
 * doigt, jamais sous lui, et reste dans la fenêtre — la géométrie est
 * pure, le contenu agrandi se trace sur l'appareil.
 */
class MagnifierGeometryTest {

    /** La loupe flotte au-dessus du doigt, à distance fixe. */
    @Test
    void magnifier_floatsAboveFinger() {
        // Centre à 60 px au-dessus du doigt, diamètre 80.
        assertEquals(140f, MagnifierGeometry.centerY(200f, 80f), 1e-6f);
        // Jamais sous le doigt.
        assertTrue(MagnifierGeometry.centerY(200f, 80f) < 200f);
    }

    /** La loupe reste dans la fenêtre : le bord la retient. */
    @Test
    void magnifier_staysInsideViewport() {
        float demi = 40f;
        // Près du bord gauche : le centre se repousse à demi-diamètre.
        assertEquals(demi, MagnifierGeometry.centerX(10f, 240f, 80f), 1e-6f);
        // Près du bord droit : le centre se tire à largeur moins demi.
        assertEquals(240f - demi, MagnifierGeometry.centerX(239f, 240f, 80f), 1e-6f);
        // Au milieu : le centre suit le doigt.
        assertEquals(120f, MagnifierGeometry.centerX(120f, 240f, 80f), 1e-6f);
    }
}
