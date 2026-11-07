package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests du zoom au pincement (D-15) : la taille de police monte par
 * PALIERS sur le facteur CUMULÉ du geste — le facteur du détecteur est
 * incrémental, il se cumule depuis le départ avant de choisir le
 * palier (PIEGE-24) — et le point du document sous le doigt y reste :
 * le défilement se réajuste pour que la colonne et la rangée sous le
 * point focal gardent leur position à l'écran.
 */
class PinchZoomTest {

    /** Le facteur cumulé mène au palier, cran après cran sur l'échelle. */
    @Test
    void pinchZoom_stepsInDiscreteSizes() {
        // 14 × 1,2 = 16,8 : le palier 16 s'entre (à 15,7 près), le 18
        // ne s'entre pas (16,8 < 17,7) ; 14 × 0,8 = 11,2 : le palier
        // 12 reste le plus proche entré.
        assertEquals(16f, PinchZoom.steppedSize(14f, PinchZoom.targetSize(14f, 1.2f)),
                1e-6f);
        assertEquals(12f, PinchZoom.steppedSize(14f, PinchZoom.targetSize(14f, 0.8f)),
                1e-6f);
        // Le palier 20 s'entre franchement (14 × 1,5 = 21 ≥ 19,7).
        assertEquals(20f, PinchZoom.steppedSize(14f, PinchZoom.targetSize(14f, 1.5f)),
                1e-6f);
        // Sans pincement net, le palier courant tient.
        assertEquals(14f, PinchZoom.steppedSize(14f, PinchZoom.targetSize(14f, 1.03f)),
                1e-6f);
    }

    /**
     * PIEGE-24 — la trajectoire documentée : le facteur cumulé
     * 1,0 → 1,25 → 1,5 depuis 14 sp mène aux paliers 14 → 16 → 20.
     */
    @Test
    void pinchZoom_cumulativeFactorWalksTheLadder() {
        float palier = 14f;
        palier = PinchZoom.steppedSize(palier, PinchZoom.targetSize(14f, 1.0f));
        assertEquals(14f, palier, 1e-6f);
        // 14 × 1,25 = 17,5 : le 16 s'entre, le 18 pas encore (17,5 < 17,7).
        palier = PinchZoom.steppedSize(palier, PinchZoom.targetSize(14f, 1.25f));
        assertEquals(16f, palier, 1e-6f);
        // 14 × 1,5 = 21 : la marche grimpe jusqu'à 20 (le 24 n'entre
        // qu'à 23,4).
        palier = PinchZoom.steppedSize(palier, PinchZoom.targetSize(14f, 1.5f));
        assertEquals(20f, palier, 1e-6f);
    }

    /**
     * L'hystérésis tient le tremblement : ±3 % autour d'un palier ne
     * fait jamais vibrer la taille entre deux crans.
     */
    @Test
    void pinchZoom_hysteresisHoldsThroughThreePercentFlutter() {
        // Autour de 14 : 13,58 à 14,42 — ni le 12 (sortie à 12,3) ni
        // le 16 (entrée à 15,7).
        assertEquals(14f, PinchZoom.steppedSize(14f, 14f * 0.97f), 1e-6f);
        assertEquals(14f, PinchZoom.steppedSize(14f, 14f * 1.03f), 1e-6f);
        // Autour de 16 : 15,52 à 16,48 — ni le 14 (sortie à 14,3) ni
        // le 18 (entrée à 17,7).
        assertEquals(16f, PinchZoom.steppedSize(16f, 16f * 0.97f), 1e-6f);
        assertEquals(16f, PinchZoom.steppedSize(16f, 16f * 1.03f), 1e-6f);
        // À la frontière même des paliers, le tremblement ne bascule
        // pas : la cible 15,4 depuis 14 reste au 14 (le 16 entre à 15,7).
        assertEquals(14f, PinchZoom.steppedSize(14f, 15.4f), 1e-6f);
        assertEquals(16f, PinchZoom.steppedSize(16f, 15.6f), 1e-6f);
    }

    /** Les paliers sont bornés : la taille ne sort jamais de l'échelle. */
    @Test
    void pinchZoom_clampsToLadderBounds() {
        // Le facteur se borne d'abord : 4 reste 4, 0,1 se relève à 0,25.
        assertEquals(112f, PinchZoom.targetSize(28f, 4f), 1e-6f);
        assertEquals(2.5f, PinchZoom.targetSize(10f, 0.1f), 1e-6f);
        // Puis la marche se borne aux extrémités de l'échelle.
        assertEquals(28f, PinchZoom.steppedSize(28f, PinchZoom.targetSize(28f, 4f)),
                1e-6f);
        assertEquals(10f, PinchZoom.steppedSize(10f, PinchZoom.targetSize(10f, 0.1f)),
                1e-6f);
    }

    /** D-15 — le point du document sous le doigt y reste, en X comme en Y. */
    @Test
    void pinchZoom_keepsColumnUnderFinger() {
        // Gouttière 40 px, focal à 120 px, caractère de 10, défilement 30 :
        // colonne (120 − 40 + 30) / 10 = 11. Après zoom, caractère de 16 :
        // la colonne 11 doit rester à 120 px de la fenêtre.
        float nouveauScroll = PinchZoom.scrollXKeepingColumn(120f, 40f, 40f,
                30f, 10f, 16f);
        float colonne = (120f - 40f + nouveauScroll) / 16f;
        assertEquals(11f, colonne, 1e-4f,
                "la colonne sous le doigt ne bouge pas d'un cran");

        // La gouttière qui change entre les deux images ne dérive pas
        // le point focal : gouttière 40 → 34, la colonne lue à 40 se
        // pose à 34.
        float avecGouttiere = PinchZoom.scrollXKeepingColumn(120f, 40f, 34f,
                30f, 10f, 16f);
        float colonneGouttiere = (120f - 34f + avecGouttiere) / 16f;
        assertEquals(11f, colonneGouttiere, 1e-4f,
                "la colonne tient aussi quand la gouttière change de palier");

        // Idem en Y : la rangée sous le doigt y reste ((50 + 70) / 20 = 6).
        float nouveauScrollY = PinchZoom.scrollYKeepingRow(50f, 70f, 20f, 32f);
        float rangee = (50f + nouveauScrollY) / 32f;
        assertEquals(6f, rangee, 1e-4f,
                "la rangée sous le doigt ne bouge pas d'un cran");
    }
}
