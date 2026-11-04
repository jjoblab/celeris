package jo.celeris.core.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

/**
 * Propriétés jqwik du remplacement (mode littéral, casse exacte) :
 * l'oracle est {@link String#replace(CharSequence, CharSequence)} — le
 * remplacement littéral du JDK remplace chaque occurrence non recouverte
 * de gauche à droite, exactement comme la passe du moteur. Les éditions
 * du résultat, ajustées en deltas cumulés, doivent reconstruire le même
 * texte par la voie de {@link Document} ; par la session, le remplacement
 * doit toujours s'annuler intégralement. La randomisation est
 * déterministe, rejouable à l'identique à partir de la graine.
 */
class RemplacementProprietesTest {

    /** Moteur à horloge figée : aucun budget ne peut être dépassé. */
    private static final FindReplace MOTEUR = FindReplace.of(() -> 0L);

    /**
     * Le remplacement littéral coïncide avec l'oracle du JDK, et les
     * éditions du résultat (offsets du texte d'origine, ordre du document)
     * reconstruisent le même texte une fois ajustées en deltas cumulés.
     */
    @Property(tries = 250)
    void leRemplacementLitteralCoincideAvecLOracle(@ForAll long graine) {
        Random rng = new Random(graine);
        String texte = texteAleatoire(rng, 40);
        String requete = texteAleatoire(rng, 3);
        String remplacement = texteAleatoire(rng, 4);

        FindReplace.ReplaceResult passe = MOTEUR.replaceAll(texte, requete, remplacement,
                new FindOptions());
        assertEquals(texte.replace(requete, remplacement), passe.text(),
                "oracle String.replace (graine " + graine + ")");

        Document doc = Document.of(texte);
        int delta = 0;
        for (TextEdit edition : passe.edits()) {
            doc = doc.replace(edition.start() + delta, edition.end() + delta,
                    edition.inserted());
            delta += edition.inserted().length() - edition.removed().length();
        }
        assertEquals(passe.text(), doc.text(),
                "les éditions en deltas cumulés reconstruisent le texte (graine " + graine + ")");
    }

    /** Par la session, tout remplacement s'annule intégralement en UNE étape. */
    @Property(tries = 100)
    void leRemplacementParLaSessionSannuleIntegral(@ForAll long graine) {
        Random rng = new Random(graine);
        String texte = texteAleatoire(rng, 30);
        String requete = texteAleatoire(rng, 3);
        TextSession session = TextSession.of(texte, () -> 0L);
        session.replaceAll(requete, "XY", new FindOptions());
        if (session.canUndo()) {
            assertEquals(1, session.undoDepth(),
                    "un remplacement global = UNE étape (graine " + graine + ")");
            assertTrue(session.undo());
            assertEquals(texte, session.text(),
                    "défaire restaure le texte d'origine (graine " + graine + ")");
        } else {
            assertEquals(texte, session.text(),
                    "aucune occurrence : le texte est inchangé (graine " + graine + ")");
        }
    }

    /** Texte aléatoire sur un alphabet de trois lettres (longueur au moins 1). */
    private static String texteAleatoire(Random rng, int longueurMax) {
        int longueur = rng.nextInt(longueurMax) + 1;
        StringBuilder sb = new StringBuilder(longueur);
        for (int i = 0; i < longueur; i++) {
            sb.append("abc".charAt(rng.nextInt(3)));
        }
        return sb.toString();
    }
}
