package jo.celeris.core.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

/**
 * L'horloge du cœur de texte est en NANOSECONDES (System::nanoTime, la
 * convention de TextSession.of et de la vue) : la coalescence des frappes
 * et le budget regex doivent TENIR sur cette horloge.
 *
 * <p>L'ancien code comparait des millisecondes nues contre l'horloge nano :
 * deux frappes à 100 ms d'écart voyaient un écart de 100 000 000 « ≥ 500 »
 * — jamais coalescées, chaque caractère tapé redevenait une étape
 * d'annulation séparée ; et le budget regex de 250 ms expirait en 250 ns :
 * le remplacement regex par la session n'aboutissait jamais.
 */
final class HorlogeNanoCoalescenceTest {

    /** Horloge nano simulée : chaque lecture avance d'exactement 1 ms. */
    private static final class HorlogeFrappe implements java.util.function.LongSupplier {
        private long nano = 1_000_000_000L;

        @Override
        public long getAsLong() {
            nano += 1_000_000L;
            return nano;
        }
    }

    @Test
    void frappesSimultaneesSeCoalescentSurHorlogeNano() {
        HorlogeFrappe horloge = new HorlogeFrappe();
        TextSession session = TextSession.of("ab", horloge);
        session.type("c");
        session.type("d");
        session.type("e");
        session.undo();
        // Les trois frappes à ~1 ms d'écart forment UNE étape : un seul
        // undo revient à « ab » (l'ancien code en laissait deux).
        assertEquals("ab", session.text(),
                "des frappes rapprochées sur l'horloge nano forment UNE étape d'annulation");
        assertFalse(session.undo(), "la chaîne ne compte qu'une étape");
    }

    @Test
    void leBudgetRegexTientSurHorlogeNano() {
        HorlogeFrappe horloge = new HorlogeFrappe();
        // Texte assez long pour que la garde échantillonnée (tous les 1024
        // accès) relève l'horloge : sans conversion à la frontière,
        // l'échéance expirait à la première relève.
        StringBuilder texte = new StringBuilder("x1");
        for (int i = 0; i < 5_000; i++) {
            texte.append("y2");
        }
        TextSession session = TextSession.of(texte.toString(), horloge);
        int remplaces = session.replaceAll(
                "[0-9]", "N",
                new FindOptions(false, false, true));
        assertEquals(5_001, remplaces,
                "le remplacement regex aboutit sur l'horloge nano (budget en ms converti à la frontière)");
        assertEquals('N', session.text().charAt(1), "les chiffres sont remplacés");
        assertFalse(session.text().contains("2"), "aucun chiffre ne survit");
    }
}
