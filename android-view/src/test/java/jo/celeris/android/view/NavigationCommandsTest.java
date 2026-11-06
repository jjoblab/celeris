package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;

import org.junit.jupiter.api.Test;

/**
 * Tests des constructions de sélection des commandes de navigation :
 * le bornage au document, l'ordre ancre-avant-curseur, les entiers
 * hostiles (négatifs, au-delà de la fin) — la partie commande sur vue
 * est mince et suit la même voie que KeyActions.
 */
class NavigationCommandsTest {

    /** Le curseur se borne au document : jamais négatif, jamais au-delà. */
    @Test
    void curseurBorneAuDocument() {
        MultiSelection selection = NavigationCommands.caretAt(5, 10);
        assertEquals(1, selection.ranges().size());
        assertEquals(5, selection.ranges().get(0).caret());
        assertEquals(5, selection.ranges().get(0).start());

        assertEquals(0, NavigationCommands.caretAt(-3, 10).ranges().get(0).caret(),
                "négatif : borne au début");
        assertEquals(0, NavigationCommands.caretAt(Integer.MIN_VALUE, 10)
                .ranges().get(0).caret(), "MIN_VALUE : borne au début");
        assertEquals(10, NavigationCommands.caretAt(11, 10).ranges().get(0).caret(),
                "au-delà : borne à la fin");
        assertEquals(10, NavigationCommands.caretAt(Integer.MAX_VALUE, 10)
                .ranges().get(0).caret(), "MAX_VALUE : borne à la fin");
        assertEquals(0, NavigationCommands.caretAt(0, 0).ranges().get(0).caret(),
                "document vide : offset 0");
    }

    /** La plage se borne et s'ordonne : l'ancre précède le curseur. */
    @Test
    void plageBorneeEtOrdonnee() {
        MultiSelection selection = NavigationCommands.rangeBetween(2, 7, 10);
        SelRange plage = selection.ranges().get(0);
        assertEquals(2, plage.start());
        assertEquals(7, plage.end());

        assertEquals(3, NavigationCommands.rangeBetween(3, 3, 10)
                .ranges().get(0).start(), "plage vide admise (recherche sans sélection)");
        assertEquals(0, NavigationCommands.rangeBetween(-5, 4, 10)
                .ranges().get(0).start(), "début négatif : borne au début");
        assertEquals(10, NavigationCommands.rangeBetween(8, 99, 10)
                .ranges().get(0).end(), "fin démesurée : borne à la fin du document");
        assertEquals(4, NavigationCommands.rangeBetween(8, 4, 10)
                .ranges().get(0).start(), "fin avant début : ordonnée");
        assertEquals(8, NavigationCommands.rangeBetween(8, 4, 10)
                .ranges().get(0).end(), "fin avant début : la plus grande devient fin");
    }
}
