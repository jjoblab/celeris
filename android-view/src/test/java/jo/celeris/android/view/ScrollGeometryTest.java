package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests de la géométrie de défilement : bornage et fenêtre de rangées.
 */
class ScrollGeometryTest {

    /** Le décalage ne sort jamais du contenu. */
    @Test
    void bornageDuDecalage() {
        assertEquals(0f, ScrollGeometry.clamp(-30f, 1000f, 200f), 1e-6f);
        assertEquals(300f, ScrollGeometry.clamp(300f, 1000f, 200f), 1e-6f);
        assertEquals(800f, ScrollGeometry.clamp(950f, 1000f, 200f), 1e-6f);
    }

    /** Un contenu plus court que la fenêtre ne défile pas. */
    @Test
    void contenuPlusCourtNeDefilePas() {
        assertEquals(0f, ScrollGeometry.clamp(150f, 100f, 200f), 1e-6f);
        assertEquals(0f, ScrollGeometry.maxScroll(100f, 200f), 1e-6f);
        assertEquals(0f, ScrollGeometry.clamp(50f, 200f, 200f), 1e-6f,
                "contenu égal à la fenêtre : décalage nul");
    }

    /** Le décalage maximal est contenu moins fenêtre. */
    @Test
    void decalageMaximal() {
        assertEquals(800f, ScrollGeometry.maxScroll(1000f, 200f), 1e-6f);
    }

    /** La première rangée visible suit le décalage, à la hauteur de rangée près. */
    @Test
    void premiereRangeeVisible() {
        assertEquals(0, ScrollGeometry.firstRow(0f, 20f));
        assertEquals(0, ScrollGeometry.firstRow(19.9f, 20f));
        assertEquals(1, ScrollGeometry.firstRow(20f, 20f));
        assertEquals(1, ScrollGeometry.firstRow(39.9f, 20f));
        assertEquals(7, ScrollGeometry.firstRow(140f, 20f));
        assertEquals(0, ScrollGeometry.firstRow(-5f, 20f), "décalage négatif borné à zéro");
    }

    /** La fenêtre couvre les rangées entières plus l'entamée, une de marge. */
    @Test
    void nombreDeRangeesVisibles() {
        assertEquals(6, ScrollGeometry.visibleRows(100f, 20f), "5 entières plus l'entamée");
        assertEquals(5, ScrollGeometry.visibleRows(61f, 20f), "4 entières plus l'entamée");
        assertEquals(11, ScrollGeometry.visibleRows(200f, 20f));
    }
}
