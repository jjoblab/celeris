package jo.celeris.core.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Les squelettes de méthode (B-06) : implémenter ou surcharger une
 * méthode — le squelette naît de la signature, les trous y reçoivent
 * les curseurs dans l'ordre de tabulation. Le moteur de gabarits M5
 * fait le reste : valeurs par défaut posées, miroirs liés.
 */
class SquelettesMethodeTest {

    /** Le squelette de surcharge : types seuls, trous aux paramètres et au corps. */
    @Test
    void leSqueletteDeSurcharge() {
        String corps = SquelettesMethode.squelette("toString",
                List.of("String", "int"), "String");
        assertEquals("@Override\npublic String toString("
                + "${p1:String arg0}, ${p2:int arg1}) {\n    ${corps}\n}", corps);
        // le moteur de gabarits ordonne les trous : p1, p2 puis le corps
        Gabarit gabarit = Gabarit.of(corps);
        assertEquals(List.of("p1", "p2", "corps"),
                gabarit.trous().stream().map(Trou::variable).toList());
    }

    /** Le squelette sans paramètre : un seul trou, le corps. */
    @Test
    void leSqueletteSansParametre() {
        String corps = SquelettesMethode.squelette("fermer", List.of(), "void");
        assertEquals("@Override\npublic void fermer() {\n    ${corps}\n}", corps);
        Gabarit gabarit = Gabarit.of(corps);
        assertEquals(List.of("corps"),
                gabarit.trous().stream().map(Trou::variable).toList());
    }

    /** Le rendu pose les défauts : chaque paramètre reçoit son argN. */
    @Test
    void leRenduPoseLesDefauts() {
        String corps = SquelettesMethode.squelette("ajouter",
                List.of("java.util.List"), "boolean");
        Gabarit gabarit = Gabarit.of(corps);
        String rendu = gabarit.rendu();
        assertTrue(rendu.startsWith("@Override\npublic boolean ajouter("));
        assertTrue(rendu.contains("java.util.List arg0"));
        assertTrue(rendu.endsWith("\n}"));
    }
}
