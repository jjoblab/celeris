package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests du zoom au pincement (D-15) : la taille de police monte par
 * PALIERS — jamais une valeur libre qui reclassifierait le texte à
 * chaque pixel — et le point du document sous le doigt y reste : le
 * défilement se réajuste pour que la colonne et la rangée sous le
 * point focal gardent leur position à l'écran.
 */
class PinchZoomTest {

    /** Les paliers montent par crans, jamais par valeurs libres. */
    @Test
    void pinchZoom_stepsInDiscreteSizes() {
        // 14 × 1,2 ≈ 16,8 → palier 16 ; 14 × 0,8 ≈ 11,2 → palier 12.
        assertEquals(16f, PinchZoom.steppedSize(14f, 1.2f), 1e-6f);
        assertEquals(12f, PinchZoom.steppedSize(14f, 0.8f), 1e-6f);
        // Le plus proche palier l'emporte.
        assertEquals(18f, PinchZoom.steppedSize(14f, 1.5f), 1e-6f);
        // Sans pincement net, le palier courant tient.
        assertEquals(14f, PinchZoom.steppedSize(14f, 1.03f), 1e-6f);
    }

    /** Les paliers sont bornés : la taille ne sort jamais de l'échelle. */
    @Test
    void pinchZoom_clampsToLadderBounds() {
        assertEquals(28f, PinchZoom.steppedSize(28f, 4f), 1e-6f);
        assertEquals(10f, PinchZoom.steppedSize(10f, 0.1f), 1e-6f);
    }

    /** D-15 — le point du document sous le doigt y reste, en X comme en Y. */
    @Test
    void pinchZoom_keepsColumnUnderFinger() {
        // Gouttière 40 px, focal à 120 px, caractère de 10 → colonne 8
        // (9,0 avec le défilement 30). Après zoom, caractère de 16 :
        // la colonne 9 doit rester à 120 px de la fenêtre.
        float nouveauScroll = PinchZoom.scrollXKeepingColumn(120f, 40f, 30f,
                10f, 16f);
        float colonne = (120f - 40f + nouveauScroll) / 16f;
        assertEquals(9f, colonne, 1e-4f,
                "la colonne sous le doigt ne bouge pas d'un cran");

        // Idem en Y : la rangée sous le doigt y reste.
        float nouveauScrollY = PinchZoom.scrollYKeepingRow(50f, 70f, 20f, 32f);
        float rangee = (50f + nouveauScrollY) / 32f;
        assertEquals(6f, rangee, 1e-4f,
                "la rangée sous le doigt ne bouge pas d'un cran");
    }
}
