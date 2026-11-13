package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests de la géométrie de la loupe (D-16 — la source unique des
 * bornes) : la bulle ne sort JAMAIS de la fenêtre — AU-DESSUS du
 * doigt si la place y suffit, SINON sous le doigt, SINON bornée dans
 * ce qui reste de la vue ; le centre x suit le doigt, retenu aux
 * bords. Pure et sans appareil : le contenu agrandi se trace dans
 * {@code LoupeTest}.
 */
class MagnifierGeometryTest {

    /** Le centre x suit le doigt, retenu aux bords — jamais coupée
     *  horizontalement ; fenêtre étroite : son centre. */
    @Test
    void magnifier_staysInsideViewport() {
        float demi = 40f;
        // Près du bord gauche : le centre se repousse à demi-diamètre.
        assertEquals(demi, MagnifierGeometry.centerX(10f, 240f, 80f), 1e-6f);
        // Près du bord droit : le centre se tire à largeur moins demi.
        assertEquals(240f - demi, MagnifierGeometry.centerX(239f, 240f, 80f),
                1e-6f);
        // Au milieu : le centre suit le doigt.
        assertEquals(120f, MagnifierGeometry.centerX(120f, 240f, 80f), 1e-6f);
        // Fenêtre plus étroite que la bulle (80 < 100) : son centre.
        assertEquals(40f, MagnifierGeometry.centerX(0f, 80f, 100f), 1e-6f);
    }

    /** Le centre y : AU-DESSUS si la place suffit, SINON sous le doigt,
     *  SINON borné — jamais coupée verticalement. */
    @Test
    void magnifier_resoudreCyGardeLaBulleVisible() {
        // au milieu de la vue : au-dessus (1,8 rayon au-dessus du doigt)
        assertEquals(400f - 180f,
                MagnifierGeometry.resoudreCy(400f, 2000f, 100f), 1e-6f);
        // doigt trop haut : la bulle passe SOUS le doigt
        assertEquals(50f + 180f,
                MagnifierGeometry.resoudreCy(50f, 2000f, 100f), 1e-6f);
        // vue serrée : ni au-dessus ni au-dessous — bornée dans ce qui reste
        assertEquals(100f, MagnifierGeometry.resoudreCy(50f, 240f, 100f),
                1e-6f);
        // vue plus petite que la bulle : son centre
        assertEquals(75f, MagnifierGeometry.resoudreCy(50f, 150f, 100f),
                1e-6f);
    }
}
