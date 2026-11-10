package jo.celeris.core.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests du bossage de chameau (A-04) : les frontières UAX #29 de
 * {@code Word} raffinées par les transitions de casse et de nature —
 * le critère de la matrice s'y vérifie mot pour mot
 * (« monCompteurDeLignes » se coupe en 4 mots).
 */
class CamelWordTest {

    /** A-04 — le critère de la matrice, mot pour mot. */
    @Test
    void camelHumpsCutIdentifiers() {
        assertEquals(List.of(0, 3, 11, 13, 19), CamelWord.boundaries("monCompteurDeLignes"));
    }

    /** A-04 — la course de majuscules se termine sur sa dernière majuscule. */
    @Test
    void upperCaseRunEndsOnItsLastUpper() {
        assertEquals(List.of(0, 4, 10), CamelWord.boundaries("HTTPServer"));
        assertEquals(List.of(0, 5), CamelWord.boundaries("HTTPS"));
        // La course terminée par une minuscule coupe à sa dernière majuscule.
        assertEquals(List.of(0, 4, 6), CamelWord.boundaries("HTTPSx"));
    }

    /** A-04 — lettres et chiffres du script commun alternent. */
    @Test
    void lettersAndAsciiDigitsAlternate() {
        assertEquals(List.of(0, 3, 6, 9), CamelWord.boundaries("abc123def"));
        assertEquals(List.of(0, 3, 4, 11), CamelWord.boundaries("UTF8Support"));
    }

    /** A-04 — la base UAX #29 reste entière : rien ne recolle. */
    @Test
    void uaxBoundariesStayUntouched() {
        assertEquals(List.of(0, 5, 6, 7, 12, 13), CamelWord.boundaries("Hello, world!"));
        // Les tirets bas prolongent le mot, les séparateurs médians aussi.
        assertEquals(List.of(0, 12), CamelWord.boundaries("mot_de_passe"));
        assertEquals(List.of(0, 4), CamelWord.boundaries("3.14"));
        assertEquals(List.of(0, 7), CamelWord.boundaries("foo:bar"));
        assertEquals(List.of(0, 9), CamelWord.boundaries("l'éditeur"));
        // L'accent suit la casse : « monÉditeur » bosse sur le É.
        assertEquals(List.of(0, 3, 10), CamelWord.boundaries("monÉditeur"));
    }

    /** A-04 — les écritures sans casse ne changent d'un cran (CJK, kana, arabe). */
    @Test
    void caselessScriptsStayPut() {
        assertEquals(List.of(0, 1, 2), CamelWord.boundaries("安心"));
        assertEquals(List.of(0, 4), CamelWord.boundaries("カタカナ"));
        assertEquals(List.of(0, 4), CamelWord.boundaries("سلام"));
        // Le chameau latin voisin ne perturbe pas le sinogramme.
        assertEquals(List.of(0, 3, 4, 5), CamelWord.boundaries("mon安心"));
    }

    /** A-04 — les chiffres d'un autre script restent des chiffres de mot. */
    @Test
    void nonAsciiDigitsDoNotHump() {
        // Le chiffre arabe-indien ٣ n'est pas du script commun : le mot
        // reste entier, sans bosse.
        assertEquals(List.of(0, 3), CamelWord.boundaries("a٣b"));
    }

    /** A-04 — les frontières avancent par bossages successifs. */
    @Test
    void nextBoundaryWalksHumpByHump() {
        String text = "monCompteurDeLignes HTTPServer";
        // 0 → 3 (mon) → 11 (Compteur) → 13 (De) → 19 (Lignes) → 20 ( ) → 24 (HTTP)…
        assertEquals(3, CamelWord.nextBoundary(text, 0));
        assertEquals(11, CamelWord.nextBoundary(text, 3));
        assertEquals(13, CamelWord.nextBoundary(text, 11));
        assertEquals(19, CamelWord.nextBoundary(text, 13));
        assertEquals(20, CamelWord.nextBoundary(text, 19));
        assertEquals(24, CamelWord.nextBoundary(text, 20));
        assertEquals(30, CamelWord.nextBoundary(text, 24));
        assertEquals(30, CamelWord.nextBoundary(text, 30));
    }

    /** A-04 — les frontières reculent par bossages successifs. */
    @Test
    void previousBoundaryWalksHumpByHump() {
        String text = "monCompteurDeLignes";
        assertEquals(13, CamelWord.previousBoundary(text, 19));
        assertEquals(11, CamelWord.previousBoundary(text, 13));
        assertEquals(3, CamelWord.previousBoundary(text, 11));
        assertEquals(0, CamelWord.previousBoundary(text, 3));
        assertEquals(0, CamelWord.previousBoundary(text, 0));
        // La course de majuscules vue depuis l'intérieur : la frontière
        // avant le S final se lit sur le « e » qui suit.
        assertEquals(4, CamelWord.previousBoundary("HTTPServer", 5));
        assertEquals(4, CamelWord.previousBoundary("HTTPServer", 10));
        assertEquals(0, CamelWord.previousBoundary("HTTPS", 5));
    }

    /** A-04 — next et previous se répondent frontière à frontière. */
    @Test
    void nextAndPreviousAgreeOnEveryBoundary() {
        String text = "a monCompteurDeLignes b1c HTTPServer 3.14 x1Y2";
        List<Integer> boundaries = CamelWord.boundaries(text);
        assertEquals(0, boundaries.get(0));
        assertEquals(text.length(), boundaries.get(boundaries.size() - 1));
        for (int k = 1; k < boundaries.size(); k++) {
            assertEquals(boundaries.get(k),
                    CamelWord.nextBoundary(text, boundaries.get(k - 1)),
                    "next depuis " + boundaries.get(k - 1));
            assertEquals(boundaries.get(k - 1),
                    CamelWord.previousBoundary(text, boundaries.get(k)),
                    "previous depuis " + boundaries.get(k));
        }
    }

    /** A-04 — les positions hors bornes se refusent comme partout ailleurs. */
    @Test
    void outOfBoundsPositionsAreRejected() {
        assertThrows(IndexOutOfBoundsException.class,
                () -> CamelWord.nextBoundary("abc", -1));
        assertThrows(IndexOutOfBoundsException.class,
                () -> CamelWord.nextBoundary("abc", 4));
        assertThrows(IndexOutOfBoundsException.class,
                () -> CamelWord.previousBoundary("abc", 4));
        assertThrows(IndexOutOfBoundsException.class,
                () -> CamelWord.previousBoundary("abc", -1));
    }
}
