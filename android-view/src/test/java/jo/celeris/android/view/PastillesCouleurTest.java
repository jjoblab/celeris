package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Les littéraux de couleur de la gouttière (C-05) : chaque forme
 * Android se résout — la courte étend ses composantes, la forme sans
 * alpha prend l'alpha plein — et une pastille par ligne, la première.
 */
class PastillesCouleurTest {

    @Test
    void lesFormesLonguesSeLisentTellesQuelles() {
        assertEquals(0xFFFF5722, ColorChips.resoudre("FF5722"));
        assertEquals(0x80FF5722, ColorChips.resoudre("80FF5722"));
        assertEquals(0xFFFFFFFF, ColorChips.resoudre("FFFFFF"));
    }

    @Test
    void lesFormesCourtesEtendentLeursComposantes() {
        assertEquals(0xFFFF0000, ColorChips.resoudre("F00"));
        assertEquals(0x00FFFFFF, ColorChips.resoudre("0FFF"));
        assertEquals(0xFF00FF00, ColorChips.resoudre("0F0"));
    }

    @Test
    void laPastilleSePoseSurLaLignePorteuse() {
        ColorChip chip = ColorChips.chip(4,
                "    <color name=\"accent\">#FF5722</color>");
        assertTrue(chip != null);
        assertEquals(4, chip.line());
        assertEquals(26, chip.column());
        assertEquals(0xFFFF5722, chip.argb());
        assertEquals("#FF5722".length(), chip.length());
    }

    @Test
    void laPremiereCouleurSeuleCompteParLigne() {
        ColorChip chip = ColorChips.chip(0,
                "de #F00 puis #0F0 sur la meme ligne");
        assertTrue(chip != null);
        assertEquals(0xFFFF0000, chip.argb());
    }

    @Test
    void laLigneSansCouleurNePortePasDePastille() {
        assertNull(ColorChips.chip(0, "android:text=\"sans couleur\""));
        assertNull(ColorChips.chip(0, "#12G45 n'est pas hexa"));
        assertNull(ColorChips.chip(0, "#FF572 trop court"));
    }

    @Test
    void leLitteralProlongeNeComptePas() {
        assertNull(ColorChips.chip(0, "#FF57223"));
    }

    @Test
    void tousLesLitterauxDuneLigneSEncadrent() {
        List<ColorChip> chips =
                ColorChips.tous(3, "a #fff b #0000CD c");
        assertEquals(2, chips.size());
        assertEquals(3, chips.get(0).line());
        assertEquals(3, chips.get(0).column());
        assertEquals(0xFFFFFFFF, chips.get(0).argb());
        assertEquals(4, chips.get(0).length());
        assertEquals(10, chips.get(1).column());
        assertEquals(0xFF0000CD, chips.get(1).argb());
        assertEquals(7, chips.get(1).length());
        assertTrue(ColorChips.tous(0, "sans couleur").isEmpty());
    }

    @Test
    void leFormatTientLaLargeurDuLitteral() {
        assertEquals("#e43", ColorChips.formater(0xFFF44336, 3));
        assertEquals("#822196f3", ColorChips.formater(0x822196F3, 8));
        assertEquals("#2196f3", ColorChips.formater(0xFF2196F3, 6));
        assertEquals("#8f00", ColorChips.formater(0x80FF0000, 4));
    }

    @Test
    void lesLignesRendentLeursPastillesDansLOrdre() {
        List<ColorChip> chips = ColorChips.ofLines(List.of(
                "<resources>",
                "    <color name=\"a\">#F00</color>",
                "    <color name=\"b\">#00FF00</color>",
                "</resources>"));
        assertEquals(2, chips.size());
        assertEquals(1, chips.get(0).line());
        assertEquals(0xFFFF0000, chips.get(0).argb());
        assertEquals(2, chips.get(1).line());
        assertEquals(0xFF00FF00, chips.get(1).argb());
    }

    @Test
    void lesPastillesViventDansLesMarquesDeGouttiere() {
        GutterIntegrations marks = new GutterIntegrations();
        assertEquals(0, marks.chip(1));
        marks.setChips(ColorChips.ofLines(List.of(
                "<color name=\"a\">#F00</color>")));
        assertEquals(0xFFFF0000, marks.chip(0));
        assertEquals(0, marks.chip(1));
        marks.setChips(List.of());
        assertEquals(0, marks.chip(0));
    }
}
