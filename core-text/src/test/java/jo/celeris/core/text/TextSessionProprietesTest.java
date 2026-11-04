package jo.celeris.core.text;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.arbitraries.IntegerArbitrary;
import net.jqwik.api.arbitraries.StringArbitrary;

/**
 * Propriétés jqwik de la session de texte : l'oracle naïf est un
 * {@link StringBuilder} miroir (ADR-003, ADR-010) — défaire toutes les
 * étapes restaure le texte initial, les refaire toutes restaure le texte
 * final, quelle que soit la suite d'éditions (coalescence comprise).
 */
class TextSessionProprietesTest {

    /** Gène d'édition : position brute, portée, texte inséré. */
    record Edition(int departBrut, int portee, String texte) {
    }

    @Provide
    Arbitrary<List<Edition>> sequences() {
        IntegerArbitrary departs = net.jqwik.api.Arbitraries.integers().between(0, 400);
        IntegerArbitrary portees = net.jqwik.api.Arbitraries.integers().between(0, 6);
        StringArbitrary textes = net.jqwik.api.Arbitraries.strings()
                .withChars('a', 'b', 'c', '\n', ' ', 'é')
                .ofMinLength(0)
                .ofMaxLength(9);
        Arbitrary<Edition> edition = Combinators.combine(departs, portees, textes)
                .as(Edition::new);
        return edition.list().ofMinSize(1).ofMaxSize(25);
    }

    /**
     * Défaire puis refaire toutes les étapes restaure les textes : suite
     * aléatoire d'éditions appliquées à la session et à un miroir
     * {@code StringBuilder} ; l'annulation intégrale revient au texte
     * initial, le refaire intégral revient au texte final.
     */
    @Property(tries = 250)
    void defairePuisRefaireToutesLesEtapes_restaureLesTextes(
            @ForAll("sequences") List<Edition> editions) {
        String texteInitial = "ligne initiale\nseconde ligne\n";
        TextSession session = TextSession.of(texteInitial, () -> 0L);
        StringBuilder miroir = new StringBuilder(texteInitial);
        for (Edition edition : editions) {
            int depart = Math.min(edition.departBrut(), session.length());
            int fin = Math.min(depart + edition.portee(), session.length());
            session.replaceRange(depart, fin, edition.texte());
            miroir.replace(depart, fin, edition.texte());
        }
        String texteFinal = miroir.toString();
        assertEquals(texteFinal, session.text(), "l'oracle naïf suit chaque édition");

        while (session.canUndo()) {
            session.undo();
        }
        assertEquals(texteInitial, session.text(),
                "défaire toutes les étapes restaure le texte initial");

        while (session.canRedo()) {
            session.redo();
        }
        assertEquals(texteFinal, session.text(),
                "refaire toutes les étapes restaure le texte final");
    }
}
