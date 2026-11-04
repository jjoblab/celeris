package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests de la pilule « ajouter un curseur » (A-01, tactile) : l'appui
 * long l'arme à la position du doigt, un appui dessus la désarme, un
 * appui ailleurs ajoute un curseur — la géométrie et les transitions
 * vivent ici, pures et sans appareil.
 */
class CursorPillTest {

    /** L'appui long arme la pilule à la position donnée. */
    @Test
    void pill_longPressArmsAtPosition() {
        CursorPill pilule = new CursorPill();
        assertFalse(pilule.isArmed());

        pilule.show(100f, 200f);

        assertTrue(pilule.isArmed());
        assertEquals(100f, pilule.x(), 1e-6f);
        assertEquals(200f, pilule.y(), 1e-6f);
        assertTrue(pilule.contains(100f, 200f), "le centre appartient à la pilule");
        assertTrue(pilule.contains(100f + 20f, 200f - 10f));
        assertFalse(pilule.contains(100f + 60f, 200f), "trop à droite : hors pilule");
        assertFalse(pilule.contains(100f, 200f + 60f), "trop bas : hors pilule");
    }

    /** Les bornes de la pilule : un rectangle posé au-dessus du doigt. */
    @Test
    void pill_boundsFloatAboveTouchPoint() {
        CursorPill pilule = new CursorPill();
        pilule.show(100f, 200f);

        // La pilule chevauche le point d'appui (le doigt la tient) et
        // s'étend vers le haut : son bord bas descend juste sous le
        // doigt, son bord haut le dépasse d'une hauteur de pilule.
        assertEquals(100f - CursorPill.HALF_WIDTH, pilule.left(), 1e-6f);
        assertEquals(100f + CursorPill.HALF_WIDTH, pilule.right(), 1e-6f);
        assertEquals(200f + CursorPill.GRIP_BELOW, pilule.bottom(), 1e-6f);
        assertEquals(pilule.bottom() - CursorPill.HEIGHT, pilule.top(), 1e-6f);
    }

    /** La désarme : plus de pilule, plus de zone d'appui. */
    @Test
    void pill_hideDisarms() {
        CursorPill pilule = new CursorPill();
        pilule.show(100f, 200f);
        pilule.hide();

        assertFalse(pilule.isArmed());
        assertFalse(pilule.contains(100f, 200f));
    }
}
