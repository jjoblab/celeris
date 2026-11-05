package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Le stylet, la souris et le pavé tactile (D-18, M5) : la molette
 * zoome avec Ctrl — facteur multiplicatif, borné — l'info-bulle du
 * survol paraît après un temps de station, le déplacement la retire.
 * Pure logique de geste, testable sans SDK.
 */
class PointerGesturesTest {

    /** La molette avec Ctrl zoome, sans Ctrl elle défile (le zoom tient). */
    @Test
    void moletteAvecCtrlZoome() {
        assertEquals(1.1f, PointerGestures.zoomApresMolette(1.0f, 1, true), 1e-6f);
        assertEquals(1.0f, PointerGestures.zoomApresMolette(1.0f, 3, false), 1e-6f);
        // zoom arrière : trois crans vers le bas
        assertEquals(1.0f / (1.1f * 1.1f * 1.1f),
                PointerGestures.zoomApresMolette(1.0f, -3, true), 1e-6f);
    }

    /** Le zoom est borné (D-15 hérité : 0,5 à 3,0). */
    @Test
    void zoomBorne() {
        assertEquals(3.0f, PointerGestures.zoomApresMolette(2.9f, 5, true), 1e-6f);
        assertEquals(0.5f, PointerGestures.zoomApresMolette(0.6f, -5, true), 1e-6f);
    }

    /** L'info-bulle paraît après le temps de station, pas avant. */
    @Test
    void bulleApresStation() {
        long delai = 500;
        assertFalse(PointerGestures.bulleVisible(499, 0, 3, 3, delai));
        assertTrue(PointerGestures.bulleVisible(500, 0, 3, 3, delai));
        assertTrue(PointerGestures.bulleVisible(900, 0, 3, 3, delai));
    }

    /** Le déplacement du pointeur retarde la bulle : la station repart. */
    @Test
    void deplacementRetardeLaBulle() {
        long delai = 500;
        // station de 400 ms puis déplacement : pas de bulle
        assertFalse(PointerGestures.bulleVisible(400, 0, 3, 3, delai));
        assertFalse(PointerGestures.bulleVisible(500, 100, 3, 5, delai));
        // la nouvelle station atteint le délai
        assertTrue(PointerGestures.bulleVisible(600, 100, 5, 5, delai));
    }

    /** Le retour à la position de station ne réarme rien : la fenêtre tient. */
    @Test
    void laFenetreDeStationTient() {
        long delai = 500;
        // station de 700 ms, un passage bref ailleurs, retour : la bulle tient
        assertTrue(PointerGestures.bulleVisible(700, 0, 3, 3, delai));
        assertTrue(PointerGestures.bulleVisible(701, 0, 3, 3, delai));
    }
}
