package jo.celeris.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * La surimpression de diagnostic (M7.5, §9 — livrée) : la ligne de
 * vérité porte les quatre valeurs du rendu — taille, gouttière,
 * images décolorées, retenue — en chiffres arrondis, unités
 * universelles. Pur, sans appareil.
 */
class DiagnosticOverlayTest {

    /** La ligne porte les quatre valeurs, arrondies au plus proche. */
    @Test
    void lineCarriesTheFourValuesRounded() {
        assertEquals("14 sp · 61 px · 0 img · 250 ms",
                DiagnosticOverlay.line(14f, 60.8f, 0L, 250L));
        assertEquals("28 sp · 100 px · 3 img · 0 ms",
                DiagnosticOverlay.line(28.4f, 100.2f, 3L, 0L));
    }
}
