package jo.celeris.core.text;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

/**
 * Propriétés jqwik de la session de texte : l'oracle naïf est un
 * {@link StringBuilder} miroir (ADR-003, ADR-010) — défaire toutes les
 * étapes restaure le texte initial, les refaire toutes restaure le texte
 * final, quelle que soit la suite d'éditions (coalescence comprise). La
 * randomisation est déterministe, rejouable à l'identique à partir de la
 * graine (exigence de la mutation).
 */
class TextSessionProprietesTest {

    private static final String TEXTE_INITIAL = "ligne initiale\nseconde ligne\n";

    /**
     * Défaire puis refaire toutes les étapes restaure les textes : suite
     * aléatoire d'éditions appliquées à la session et à un miroir
     * {@code StringBuilder} ; l'annulation intégrale revient au texte
     * initial, le refaire intégral revient au texte final.
     */
    @Property(tries = 250)
    void defairePuisRefaireToutesLesEtapes_restaureLesTextes(@ForAll long graine) {
        Random rng = new Random(graine);
        TextSession session = TextSession.of(TEXTE_INITIAL, () -> 0L);
        StringBuilder miroir = new StringBuilder(TEXTE_INITIAL);
        int nombreEditions = rng.nextInt(25) + 1;
        for (int i = 0; i < nombreEditions; i++) {
            int depart = rng.nextInt(session.length() + 1);
            int fin = Math.min(depart + rng.nextInt(7), session.length());
            String texte = texteAleatoire(rng, 9);
            session.replaceRange(depart, fin, texte);
            miroir.replace(depart, fin, texte);
        }
        String texteFinal = miroir.toString();
        assertEquals(texteFinal, session.text(),
                "l'oracle naïf suit chaque édition (graine " + graine + ")");

        while (session.canUndo()) {
            session.undo();
        }
        assertEquals(TEXTE_INITIAL, session.text(),
                "défaire toutes les étapes restaure le texte initial (graine " + graine + ")");

        while (session.canRedo()) {
            session.redo();
        }
        assertEquals(texteFinal, session.text(),
                "refaire toutes les étapes restaure le texte final (graine " + graine + ")");
    }

    /**
     * Mélange de lettres minuscules, chiffres, sauts de ligne, espaces et
     * caractères accentués — les insertions vides sont permises (édition
     * sans effet admise par replaceRange).
     */
    private static String texteAleatoire(Random rng, int longueurMax) {
        int longueur = rng.nextInt(longueurMax + 1);
        StringBuilder sb = new StringBuilder(longueur);
        for (int i = 0; i < longueur; i++) {
            int choix = rng.nextInt(16);
            if (choix < 10) {
                sb.append((char) ('a' + rng.nextInt(26)));
            } else if (choix < 12) {
                sb.append((char) ('0' + rng.nextInt(10)));
            } else if (choix < 13) {
                sb.append('\n');
            } else if (choix < 14) {
                sb.append(' ');
            } else if (choix < 15) {
                sb.append("é");
            } else {
                sb.append("🌍");
            }
        }
        return sb.toString();
    }
}
