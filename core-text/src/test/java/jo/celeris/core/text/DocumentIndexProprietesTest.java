package jo.celeris.core.text;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

/**
 * Propriétés jqwik des invariants croisés document ↔ index de lignes
 * (ADR-003 : l'index vit à côté de la rope et la transaction le maintient).
 * L'oracle naïf est un {@link StringBuilder} miroir dont l'index de lignes
 * est recalculé par balayage après chaque édition : nombre de lignes,
 * débuts et fins de ligne, contenu de chaque ligne, conversion
 * offset → ligne sur offsets échantillonnés. L'invariant est vérifié aussi
 * après l'annulation intégrale puis le refaire intégral — le rejeu passe
 * par la même voie de mutation, l'index doit suivre (PIEGE-07). La
 * randomisation est déterministe, rejouable à l'identique à partir de la
 * graine.
 */
class DocumentIndexProprietesTest {

    private static final String TEXTE_INITIAL = "ligne initiale\nseconde ligne\n";

    /**
     * Chaque édition maintient l'index en phase avec l'oracle, et
     * l'annulation puis le refaire intégraux le maintiennent aussi.
     */
    @Property(tries = 250)
    void lIndexDeLignesResteEnPhaseAvecLOracle(@ForAll long graine) {
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
            verifierIndex(session.document(), miroir, rng, graine);
        }

        while (session.canUndo()) {
            session.undo();
        }
        assertEquals(TEXTE_INITIAL, session.text(),
                "défaire toutes les étapes restaure le texte initial (graine " + graine + ")");
        verifierIndex(session.document(), new StringBuilder(TEXTE_INITIAL), rng, graine);

        while (session.canRedo()) {
            session.redo();
        }
        assertEquals(miroir.toString(), session.text(),
                "refaire toutes les étapes restaure le texte final (graine " + graine + ")");
        verifierIndex(session.document(), miroir, rng, graine);
    }

    /**
     * Vérifie les invariants croisés entre l'instantané et le miroir :
     * l'index de lignes de l'instantané doit coïncider, entrée par entrée,
     * avec l'index recalculé par balayage du texte du miroir.
     */
    private static void verifierIndex(Document doc, StringBuilder miroir, Random rng, long graine) {
        String texte = miroir.toString();
        int[] attendus = indexParBalayage(texte);

        assertEquals(attendus.length, doc.lineCount(),
                "nombre de lignes (graine " + graine + ")");
        for (int ligne = 0; ligne < attendus.length; ligne++) {
            assertEquals(attendus[ligne], doc.lineStart(ligne),
                    "début de la ligne " + ligne + " (graine " + graine + ")");
            int finAttendue = ligne + 1 < attendus.length
                    ? attendus[ligne + 1] - 1
                    : texte.length();
            assertEquals(finAttendue, doc.lineEnd(ligne),
                    "fin de la ligne " + ligne + " (graine " + graine + ")");
            assertEquals(texte.substring(attendus[ligne], finAttendue), doc.lineText(ligne),
                    "contenu de la ligne " + ligne + " (graine " + graine + ")");
        }

        // Conversion offset → ligne sur offsets échantillonnés, plus les
        // bornes : l'oracle compte les sauts de ligne strictement avant
        // l'offset.
        for (int essai = 0; essai < 10; essai++) {
            int offset = rng.nextInt(texte.length() + 1);
            assertEquals(ligneOracle(texte, offset), doc.lineForOffset(offset),
                    "offset " + offset + " (graine " + graine + ")");
        }
        assertEquals(0, doc.lineForOffset(-1), "offset négatif clampé (graine " + graine + ")");
        assertEquals(doc.lineCount() - 1, doc.lineForOffset(texte.length() + 7),
                "offset au-delà clampé (graine " + graine + ")");
    }

    /** Index de référence : 0, puis l'offset qui suit chaque saut de ligne. */
    private static int[] indexParBalayage(String texte) {
        int[] temporaires = new int[texte.length() + 1];
        int nombre = 1;
        temporaires[0] = 0;
        for (int i = 0; i < texte.length(); i++) {
            if (texte.charAt(i) == '\n') {
                temporaires[nombre++] = i + 1;
            }
        }
        int[] result = new int[nombre];
        System.arraycopy(temporaires, 0, result, 0, nombre);
        return result;
    }

    /** Ligne oracle d'un offset : les sauts de ligne strictement avant lui. */
    private static int ligneOracle(String texte, int offset) {
        int ligne = 0;
        for (int i = 0; i < offset && i < texte.length(); i++) {
            if (texte.charAt(i) == '\n') {
                ligne++;
            }
        }
        return ligne;
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
