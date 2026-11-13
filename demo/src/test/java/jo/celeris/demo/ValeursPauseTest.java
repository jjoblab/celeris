package jo.celeris.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import jo.celeris.android.host.spi.ValeurEnLigne;

import org.junit.jupiter.api.Test;

/**
 * Les valeurs de débogage en ligne de la pause (E-03, catégorie E de
 * l'inventaire — la SPI était servie, la surface dormait) : la pause
 * simulée y lit les affectations ÉCRITES de sa ligne — « ident =
 * valeur » — déterministe, jamais deviné (la pause ne devine pas ce
 * que la ligne ne dit pas). Pur, sans appareil — le rendu en fin de
 * ligne vit au banc de la vue ({@code ValeursPainterTest}).
 */
class ValeursPauseTest {

    /** Les affectations écrites se lisent : la ligne de pause sert
     *  ses valeurs — jusqu'à trois, jamais plus. */
    @Test
    void lesAffectationsLitteralesSeLisent() {
        List<ValeurEnLigne> valeurs = EtatDebogage.valeursDePause(5,
                "total = 0; copie = total;");
        assertEquals(2, valeurs.size());
        assertEquals(new ValeurEnLigne(5, "total", "0"), valeurs.get(0));
        assertEquals(new ValeurEnLigne(5, "copie", "total"), valeurs.get(1));
    }

    /** La ligne sans affectation ne sert rien — jamais de valeur
     *  devinée ; les littéraux composés se lisent entiers. */
    @Test
    void laLigneSansAffectationNeSertRien() {
        assertTrue(EtatDebogage.valeursDePause(5, "return total;")
                .isEmpty(), "le retour ne déclare pas de valeur");
        List<ValeurEnLigne> negatif = EtatDebogage.valeursDePause(3,
                "solde = -12;");
        assertEquals("solde", negatif.get(0).expression());
        assertEquals("-12", negatif.get(0).valeur(),
                "le littéral négatif se lit entier");
    }
}
