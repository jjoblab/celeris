package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Les caractères invisibles (A-19, M5) : la matérialisation des blancs
 * — l'espace finale toujours, la tabulation toujours, l'espace fine
 * (insécable, étroite) toujours, l'espace interne en mode « tout », la
 * fin de ligne sur demande. Le modèle rend les marques, le peintre les
 * dessine.
 */
class InvisibleCharsTest {

    /** L'espace finale et la tabulation se matérialisent toujours. */
    @Test
    void espaceFinaleEtTabulationToujours() {
        List<InvisibleChars.Mark> marks =
                InvisibleChars.marks("a b\t ", false, false);
        // tabulation (colonne 3) et espace finale (colonne 4) — pas l'espace interne
        assertEquals(2, marks.size());
        assertEquals(InvisibleChars.Kind.TABULATION, marks.get(0).sorte());
        assertEquals(3, marks.get(0).column());
        assertEquals(InvisibleChars.Kind.ESPACE, marks.get(1).sorte());
        assertEquals(4, marks.get(1).column());
    }

    /** L'espace interne se matérialise en mode « tout ». */
    @Test
    void espaceInterneEnModeTout() {
        List<InvisibleChars.Mark> marks =
                InvisibleChars.marks("a b", true, false);
        assertEquals(1, marks.size());
        assertEquals(1, marks.get(0).column());
        assertEquals(InvisibleChars.Kind.ESPACE, marks.get(0).sorte());
    }

    /** L'espace fine — insécable ou étroite — se matérialise toujours. */
    @Test
    void espaceFineToujours() {
        // « a » + espace insécable (U+00A0) + « b » + étroite (U+202F) + « c »
        List<InvisibleChars.Mark> marks =
                InvisibleChars.marks("a\u00a0b\u202fc", false, false);
        assertEquals(2, marks.size());
        assertEquals(InvisibleChars.Kind.ESPACE_FINE, marks.get(0).sorte());
        assertEquals(1, marks.get(0).column());
        assertEquals(InvisibleChars.Kind.ESPACE_FINE, marks.get(1).sorte());
        assertEquals(3, marks.get(1).column());
    }

    /** La fin de ligne se matérialise sur demande. */
    @Test
    void finDeLigneSurDemande() {
        List<InvisibleChars.Mark> marks =
                InvisibleChars.marks("ab", false, true);
        assertEquals(1, marks.size());
        assertEquals(InvisibleChars.Kind.FIN_DE_LIGNE, marks.get(0).sorte());
        assertEquals(2, marks.get(0).column());
    }

    /** Une ligne sans blanc ne rend que sa fin, sur demande. */
    @Test
    void ligneSansBlanc() {
        assertEquals(0, InvisibleChars.marks("ab", false, false).size());
    }
}
