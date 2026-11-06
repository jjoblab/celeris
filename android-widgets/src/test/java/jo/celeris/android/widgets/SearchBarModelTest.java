package jo.celeris.android.widgets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.Match;
import jo.celeris.core.text.SelRange;

/**
 * La barre de recherche (A-20, facette interface M5) : l'état de la
 * barre au-dessus du moteur M2 — les huit combinaisons d'options
 * (casse, mot entier, regex), la navigation circulaire des occurrences,
 * l'occurrence courante la plus proche du curseur. Le remplacement vit
 * au cœur (replaceAll par la voie de transaction, livré M2). En mode
 * regex, le mot entier appartient au motif (documenté au M2).
 */
class SearchBarModelTest {

    private final Document doc = Document.of("mot motus Mot\nle mot et mot-mot\n");

    /** Les huit combinaisons d'options donnent les occurrences exactes (A-20). */
    @Test
    void lesHuitCombinaisons() {
        boolean[] cas = {false, true};
        for (boolean casse : cas) {
            for (boolean motEntier : cas) {
                for (boolean regex : cas) {
                    SearchBarModel barre = new SearchBarModel(doc, () -> 0L);
                    barre.setQuery("mot");
                    barre.setCaseSensitive(casse);
                    barre.setWholeWord(motEntier);
                    barre.setRegex(regex);
                    assertEquals(occurrencesAttendues(casse, motEntier, regex),
                            barre.matches().size(),
                            "casse=" + casse + " motEntier=" + motEntier
                                    + " regex=" + regex);
                }
            }
        }
    }

    /**
     * Le décompte attendu de chaque combinaison, vérifié contre le
     * moteur : « mot motus Mot / le mot et mot-mot ».
     */
    private static int occurrencesAttendues(boolean casse, boolean motEntier,
            boolean regex) {
        if (regex) {
            // en mode regex, le mot entier appartient au motif
            return casse ? 5 : 6;
        }
        if (motEntier) {
            // « motus » ne compte pas, les deux « mot » du trait d'union comptent
            return casse ? 4 : 5;
        }
        return casse ? 5 : 6;
    }

    /** La navigation est circulaire : après la dernière, la première revient. */
    @Test
    void navigationCirculaire() {
        SearchBarModel barre = new SearchBarModel(doc, () -> 0L);
        barre.setQuery("mot");
        assertEquals(6, barre.matches().size());
        assertEquals(0, barre.currentIndex());
        for (int i = 0; i < 5; i++) {
            assertTrue(barre.next());
        }
        assertEquals(5, barre.currentIndex());
        assertTrue(barre.next());
        assertEquals(0, barre.currentIndex());
        assertTrue(barre.previous());
        assertEquals(5, barre.currentIndex());
    }

    /** L'occurrence courante se choisit la plus proche du curseur à l'ouverture. */
    @Test
    void occurrenceLaPlusProcheDuCurseur() {
        SearchBarModel barre = new SearchBarModel(doc, () -> 0L);
        barre.setQuery("mot");
        barre.setCurrentNear(20);
        assertEquals(4, barre.currentIndex());
        SelRange courante = barre.current();
        assertEquals(doc.text().indexOf("mot", 20), courante.start());
    }

    /** La requête vide ne trouve rien, l'index est vide. */
    @Test
    void requeteVideNeTrouveRien() {
        SearchBarModel barre = new SearchBarModel(doc, () -> 0L);
        barre.setQuery("");
        assertTrue(barre.matches().isEmpty());
        assertEquals(-1, barre.currentIndex());
        assertFalse(barre.next());
    }

    /** Changer une option recalcule les occurrences, l'index reste dans les bornes. */
    @Test
    void changerUneOptionRecalcule() {
        SearchBarModel barre = new SearchBarModel(doc, () -> 0L);
        barre.setQuery("mot");
        barre.next();
        barre.setWholeWord(true);
        assertEquals(5, barre.matches().size());
        assertTrue(barre.currentIndex() >= 0);
        assertTrue(barre.currentIndex() < 5);
    }

    /** Le document remplacé recalcule — le suivi de session. */
    @Test
    void documentRemplaceRecalcule() {
        SearchBarModel barre = new SearchBarModel(doc, () -> 0L);
        barre.setQuery("mot");
        assertEquals(6, barre.matches().size());
        barre.setDocument(Document.of("rien ici\n"));
        assertTrue(barre.matches().isEmpty());
    }

    /** L'occurrence courante expose ses bornes, la sélection suit. */
    @Test
    void occurrenceCouranteExposeSesBornes() {
        SearchBarModel barre = new SearchBarModel(doc, () -> 0L);
        barre.setQuery("motus");
        List<Match> occurrences = barre.matches();
        assertEquals(1, occurrences.size());
        assertEquals(occurrences.get(0).start(), barre.current().start());
        assertEquals(occurrences.get(0).end(), barre.current().end());
    }
}
