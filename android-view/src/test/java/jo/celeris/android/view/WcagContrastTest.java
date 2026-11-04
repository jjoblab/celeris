package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests du rapport de contraste WCAG (utilitaire du critère D-13).
 */
class WcagContrastTest {

    /** Noir sur blanc : le maximum absolu, 21:1. */
    @Test
    void noirSurBlancDonneLeMaximum() {
        assertEquals(21.0, WcagContrast.ratio(0xFF000000, 0xFFFFFFFF), 0.01);
        assertEquals(21.0, WcagContrast.ratio(0xFFFFFFFF, 0xFF000000), 0.01,
                "le rapport est symétrique");
    }

    /** Deux couleurs identiques : le minimum absolu, 1:1. */
    @Test
    void couleursIdentiquesDonnentUn() {
        assertEquals(1.0, WcagContrast.ratio(0xFF336699, 0xFF336699), 0.000001);
    }

    /** Un gris moyen sur blanc : quelque part entre les extrêmes. */
    @Test
    void grisMoyenEntreLesExtremes() {
        double ratio = WcagContrast.ratio(0xFF808080, 0xFFFFFFFF);
        assertTrue(ratio > 1.0 && ratio < 21.0, "ratio: " + ratio);
    }
}
