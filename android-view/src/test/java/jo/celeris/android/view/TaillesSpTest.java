package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests de la conversion des tailles d'interface (PIEGE-23) : les
 * tailles vivent en sp, converties une fois pour toutes à la densité de
 * l'appareil — jamais de pixel cru dans une mesure d'interface.
 *
 * <p>Le piège hérité : une police de 14 sp lue comme 14 pixels bruts
 * sur un écran xxhdpi (densité 3) rendait le texte minuscule — la vue
 * mesurait à la police de l'éditeur pendant que l'écran attendait
 * 42 pixels. La conversion vit désormais dans {@link TaillesSp}, pure,
 * et l'échelle de police de l'accessibilité s'y applique comme à la
 * densité.</p>
 */
class TaillesSpTest {

    /**
     * PIEGE-23 — taillesSpConvertiesALaDensite : 14 sp sur un écran de
     * densité 2,75 rendent 38,5 pixels — pas 14 ; l'échelle de police
     * de l'accessibilité (1,1) grossit pareil ; une densité nulle ne
     * divise jamais par zéro.
     */
    @Test
    void taillesSpConvertiesALaDensite() {
        assertEquals(38.5f, TaillesSp.enPixels(14f, 2.75f, 1f),
                "14 sp sur écran de densité 2,75 rendent 38,5 px");
        assertEquals(42f, TaillesSp.enPixels(14f, 3f, 1f),
                "14 sp sur écran xxhdpi rendent 42 px, jamais 14 px bruts");
        assertEquals(46.2f, TaillesSp.enPixels(14f, 3f, 1.1f),
                "l'échelle de police de l'accessibilité grossit la conversion");
        assertEquals(28f, TaillesSp.enPixels(14f, 2f, 1f),
                "14 sp sur écran xhdpi rendent 28 px");
    }
}
