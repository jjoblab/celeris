package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * La géométrie stable de la gouttière (M7.5, §5, PIEGE-25) : la
 * largeur se calcule sur les LIGNES du document avec un plancher de
 * trois chiffres, des marges en dp converties à la densité, et une
 * coulisse de configuration — jamais de projection visuelle, jamais
 * d'état d'analyse.
 */
class GutterLayoutTest {

    private static final GutterLanes SANS_COULISSE = GutterLanes.AUCUNE;

    /** Le plancher : neuf lignes comme mille, trois chiffres au moins. */
    @Test
    void floorHoldsThreeDigits() {
        assertEquals(3, GutterLayout.digitsFor(1));
        assertEquals(3, GutterLayout.digitsFor(9));
        assertEquals(3, GutterLayout.digitsFor(10));
        assertEquals(3, GutterLayout.digitsFor(999));
        assertEquals(4, GutterLayout.digitsFor(1000));
    }

    /**
     * Même largeur pour un document de 800 lignes, rangées 800 ou
     * 1 200 : la gouttière ne connaît que les lignes, la projection ne
     * la touche pas.
     */
    @Test
    void widthIgnoresVisualRows() {
        int parLignes = GutterLayout.width(GutterLayout.digitsFor(800),
                8f, 2.625f, 8f, SANS_COULISSE);
        // 1 200 rangées du même document : les lignes ne bougent pas,
        // la largeur non plus — le calcul n'a même pas d'entrée rangées.
        int parLignesToujours = GutterLayout.width(GutterLayout.digitsFor(800),
                8f, 2.625f, 8f, SANS_COULISSE);
        assertEquals(parLignes, parLignesToujours);
        assertEquals(3, GutterLayout.digitsFor(800),
                "800 lignes : trois chiffres, le retour à la ligne n'y change rien");
    }

    /** Le passage de 999 à 1 000 lignes élargit une fois, d'un cran exact. */
    @Test
    void thousandthLineWidensByExactlyOneDigit() {
        int avant = GutterLayout.width(GutterLayout.digitsFor(999),
                8f, 1f, 8f, SANS_COULISSE);
        int apres = GutterLayout.width(GutterLayout.digitsFor(1000),
                8f, 1f, 8f, SANS_COULISSE);
        assertEquals(8f, apres - avant, 1e-3f,
                "un chiffre s'ajoute, pas deux");
    }

    /** Trois densités, la même respiration en dp : la largeur suit la densité. */
    @Test
    void sameDpBreathingAcrossDensities() {
        float[] densites = {1.0f, 2.625f, 3.5f};
        float dpAttendu = -1f;
        for (float densite : densites) {
            int largeur = GutterLayout.width(3, 8f * densite, densite, 8f,
                    SANS_COULISSE);
            float enDp = largeur / densite;
            if (dpAttendu < 0f) {
                dpAttendu = enDp;
            }
            assertEquals(dpAttendu, enDp, 0.6f,
                    "la respiration en dp ne dépend pas de la densité");
        }
    }

    /** La coulisse : la configuration réserve, les données n'élargissent rien. */
    @Test
    void laneComesFromConfigurationOnly() {
        assertEquals(0f, GutterLanes.AUCUNE.laneWidth(8f, 8f), 1e-6f);
        assertEquals(16f, new GutterLanes(true, false, false, false)
                .laneWidth(8f, 6f), 1e-6f,
                "la syntaxe réserve deux caractères — le chevron y vit à l'aise");
        assertEquals(16f, new GutterLanes(false, true, false, false)
                .laneWidth(8f, 6f), 1e-6f, "l'intégration réserve deux caractères");
        assertEquals(16f, new GutterLanes(false, true, true, true)
                .laneWidth(8f, 6f), 1e-6f, "les coulisses activées se superposent");
        assertTrue(GutterLayout.laneOnly(1f, 8f, 8f, GutterLanes.AUCUNE) > 0,
                "sans numéros, la respiration reste");
    }
}
