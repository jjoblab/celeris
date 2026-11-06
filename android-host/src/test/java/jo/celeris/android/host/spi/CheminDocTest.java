package jo.celeris.android.host.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests du chemin de document (E-01) : écriture normalisée, résolution
 * du relatif, refus de la traversée — le socle du système de fichiers
 * virtuel, jamais contournable par un chemin forgé.
 */
final class CheminDocTest {

    /** L'écriture se normalise : barres redondantes et « . » disparaissent. */
    @Test
    void ecritureNormalisee() {
        assertEquals("/", CheminDoc.racine().ecriture());
        assertEquals("/a/b", CheminDoc.de("/a//b").ecriture());
        assertEquals("/a/b", CheminDoc.de("/a/./b").ecriture());
        assertEquals(List.of("a", "b"), CheminDoc.de("/a/b").segments());
        assertEquals("b", CheminDoc.de("/a/b").nom());
        assertEquals("", CheminDoc.racine().nom());
        assertTrue(CheminDoc.racine().estRacine());
    }

    /** Le « .. » qui reste dans les bornes se résout ; celui qui sort est refusé. */
    @Test
    void traverseeBorneeOuRefusee() {
        assertEquals("/b", CheminDoc.de("/a/../b").ecriture());
        assertEquals("/", CheminDoc.de("/a/..").ecriture());
        assertThrows(IllegalArgumentException.class,
                () -> CheminDoc.de("/a/../.."),
                "sortir de la racine par « .. » : refusé");
        assertThrows(IllegalArgumentException.class,
                () -> CheminDoc.de("../a"),
                "chemin relatif forgé : refusé");
    }

    /** La résolution du relatif passe par la même normalisation et les mêmes refus. */
    @Test
    void resolutionRelativeMemeDiscipline() {
        assertEquals("/a/b/c", CheminDoc.de("/a").resoudre("b/c").ecriture());
        assertEquals("/a/c", CheminDoc.de("/a").resoudre("b/../c").ecriture());
        assertThrows(IllegalArgumentException.class,
                () -> CheminDoc.de("/a").resoudre("../../c"));
    }

    /** Le parent monte d'un cran, la racine est son propre parent. */
    @Test
    void parentMonteDUnCran() {
        assertEquals("/a", CheminDoc.de("/a/b").parent().ecriture());
        assertEquals("/", CheminDoc.de("/a").parent().ecriture());
        assertEquals("/", CheminDoc.racine().parent().ecriture());
    }

    /** Le chemin vide ou blanc est refusé, jamais silencieusement racine. */
    @Test
    void cheminVideRefuse() {
        assertThrows(IllegalArgumentException.class, () -> CheminDoc.de(""));
        assertThrows(IllegalArgumentException.class, () -> CheminDoc.de("   "));
    }
}
