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
        assertEquals("/", CheminDoc.racine().writing());
        assertEquals("/a/b", CheminDoc.of("/a//b").writing());
        assertEquals("/a/b", CheminDoc.of("/a/./b").writing());
        assertEquals(List.of("a", "b"), CheminDoc.of("/a/b").segments());
        assertEquals("b", CheminDoc.of("/a/b").nom());
        assertEquals("", CheminDoc.racine().nom());
        assertTrue(CheminDoc.racine().estRacine());
    }

    /** Le « .. » qui reste dans les bornes se résout ; celui qui sort est refusé. */
    @Test
    void traverseeBorneeOuRefusee() {
        assertEquals("/b", CheminDoc.of("/a/../b").writing());
        assertEquals("/", CheminDoc.of("/a/..").writing());
        assertThrows(IllegalArgumentException.class,
                () -> CheminDoc.of("/a/../.."),
                "sortir de la racine par « .. » : refusé");
        assertThrows(IllegalArgumentException.class,
                () -> CheminDoc.of("../a"),
                "chemin relatif forgé : refusé");
    }

    /** La résolution du relatif passe par la même normalisation et les mêmes refus. */
    @Test
    void resolutionRelativeMemeDiscipline() {
        assertEquals("/a/b/c", CheminDoc.of("/a").resoudre("b/c").writing());
        assertEquals("/a/c", CheminDoc.of("/a").resoudre("b/../c").writing());
        assertThrows(IllegalArgumentException.class,
                () -> CheminDoc.of("/a").resoudre("../../c"));
    }

    /** Le parent monte d'un cran, la racine est son propre parent. */
    @Test
    void parentMonteDUnCran() {
        assertEquals("/a", CheminDoc.of("/a/b").parent().writing());
        assertEquals("/", CheminDoc.of("/a").parent().writing());
        assertEquals("/", CheminDoc.racine().parent().writing());
    }

    /** Le chemin vide ou blanc est refusé, jamais silencieusement racine. */
    @Test
    void cheminVideRefuse() {
        assertThrows(IllegalArgumentException.class, () -> CheminDoc.of(""));
        assertThrows(IllegalArgumentException.class, () -> CheminDoc.of("   "));
    }
}
