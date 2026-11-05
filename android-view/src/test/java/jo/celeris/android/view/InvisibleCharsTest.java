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
        List<InvisibleChars.Marque> marques =
                InvisibleChars.marques("a b\t ", false, false);
        // tabulation (colonne 3) et espace finale (colonne 4) — pas l'espace interne
        assertEquals(2, marques.size());
        assertEquals(InvisibleChars.Sorte.TABULATION, marques.get(0).sorte());
        assertEquals(3, marques.get(0).colonne());
        assertEquals(InvisibleChars.Sorte.ESPACE, marques.get(1).sorte());
        assertEquals(4, marques.get(1).colonne());
    }

    /** L'espace interne se matérialise en mode « tout ». */
    @Test
    void espaceInterneEnModeTout() {
        List<InvisibleChars.Marque> marques =
                InvisibleChars.marques("a b", true, false);
        assertEquals(1, marques.size());
        assertEquals(1, marques.get(0).colonne());
        assertEquals(InvisibleChars.Sorte.ESPACE, marques.get(0).sorte());
    }

    /** L'espace fine — insécable ou étroite — se matérialise toujours. */
    @Test
    void espaceFineToujours() {
        // « a » + espace insécable (U+00A0) + « b » + étroite (U+202F) + « c »
        List<InvisibleChars.Marque> marques =
                InvisibleChars.marques("a\u00a0b\u202fc", false, false);
        assertEquals(2, marques.size());
        assertEquals(InvisibleChars.Sorte.ESPACE_FINE, marques.get(0).sorte());
        assertEquals(1, marques.get(0).colonne());
        assertEquals(InvisibleChars.Sorte.ESPACE_FINE, marques.get(1).sorte());
        assertEquals(3, marques.get(1).colonne());
    }

    /** La fin de ligne se matérialise sur demande. */
    @Test
    void finDeLigneSurDemande() {
        List<InvisibleChars.Marque> marques =
                InvisibleChars.marques("ab", false, true);
        assertEquals(1, marques.size());
        assertEquals(InvisibleChars.Sorte.FIN_DE_LIGNE, marques.get(0).sorte());
        assertEquals(2, marques.get(0).colonne());
    }

    /** Une ligne sans blanc ne rend que sa fin, sur demande. */
    @Test
    void ligneSansBlanc() {
        assertEquals(0, InvisibleChars.marques("ab", false, false).size());
    }
}
