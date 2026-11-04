package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests des segments visuels bidirectionnels (rendu BiDi, ADR-008) :
 * une rangée se découpe en segments de niveau homogène rendus de
 * gauche à droite dans l'ordre visuel — la réorganisation L2 du cœur
 * (UAX #9), la discipline de rendu de la vue. Le texte LOGIQUE ne
 * bouge jamais : seules les positions de tracé changent (ADR-003 — la
 * vue peut se tromper sur l'affichage, jamais sur l'état).
 */
class VisualRunsTest {

    /** Une rangée purement gauche-à-droite tient en un segment. */
    @Test
    void runs_pureLtrIsOneSegment() {
        List<VisualRuns.Run> segments = VisualRuns.runs("abcdef");

        assertEquals(1, segments.size());
        assertEquals(0, segments.get(0).start());
        assertEquals(6, segments.get(0).end());
        assertTrue(!segments.get(0).rtl());
    }

    /** Latin, hébreu, latin : trois segments, l'hébreu au milieu. */
    @Test
    void runs_mixedLtrRtlLtr() {
        List<VisualRuns.Run> segments = VisualRuns.runs("a\u05d1\u05d2b");

        assertEquals(3, segments.size());
        assertEquals(0, segments.get(0).start());
        assertEquals(1, segments.get(0).end());
        assertTrue(!segments.get(0).rtl());
        assertEquals(1, segments.get(1).start());
        assertEquals(3, segments.get(1).end());
        assertTrue(segments.get(1).rtl(), "l'hébreu se lit de droite à gauche");
        assertEquals(3, segments.get(2).start());
        assertEquals(4, segments.get(2).end());
    }

    /**
     * Paragraphe à droite-à-gauche (premier caractère fort hébreu) :
     * le latin embarqué passe à GAUCHE de l'hébreu — l'ordre visuel
     * inverse les segments.
     */
    @Test
    void runs_rtlParagraphPutsEmbeddedLatinLeft() {
        List<VisualRuns.Run> segments = VisualRuns.runs("\u05d1\u05d2a");

        assertEquals(2, segments.size());
        assertEquals(2, segments.get(0).start());
        assertEquals(3, segments.get(0).end());
        assertTrue(!segments.get(0).rtl(), "le latin embarqué reste gauche-à-droite");
        assertEquals(0, segments.get(1).start());
        assertEquals(2, segments.get(1).end());
        assertTrue(segments.get(1).rtl());
    }

    /** Une rangée entièrement hébraïque tient en un segment droite-à-gauche. */
    @Test
    void runs_pureRtlIsOneSegment() {
        List<VisualRuns.Run> segments = VisualRuns.runs("\u05d0\u05d1\u05d2");

        assertEquals(1, segments.size());
        assertEquals(0, segments.get(0).start());
        assertEquals(3, segments.get(0).end());
        assertTrue(segments.get(0).rtl());
    }

    /** Rangée vide : aucun segment, rien à tracer. */
    @Test
    void runs_emptyRowYieldsNothing() {
        assertEquals(0, VisualRuns.runs("").size());
    }

    /** La détection RTL parcourt la rangée sans allouer. */
    @Test
    void runs_detectsRtlPresence() {
        assertTrue(VisualRuns.containsRtl("a\u05d1b"));
        assertTrue(!VisualRuns.containsRtl("abc"));
        assertTrue(!VisualRuns.containsRtl(""));
    }
}
