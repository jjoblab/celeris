package jo.celeris.android.host.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Tests du faux VCS (E-02, D-03) : marques par plage de lignes et
 * annotations de blame par ligne — le contrat que la gouttière
 * consommera, servi par l'implémentation de référence déterministe.
 */
final class FakeVcsTest {

    private static FakeVcs fauxPrecharge() {
        Map<Integer, VcsMark> marks = Map.of(
                3, VcsMark.MODIFIE,
                4, VcsMark.MODIFIE,
                7, VcsMark.AJOUTE);
        List<LineRange> supprimees = List.of(new LineRange(9, 10));
        List<AnnotationBlame> blame = List.of(
                new AnnotationBlame(1, "r1", "amine", "creation"),
                new AnnotationBlame(2, "r1", "amine", "creation"),
                new AnnotationBlame(3, "r2", "sofia", "correction"),
                new AnnotationBlame(7, "r2", "sofia", "ajout"));
        List<RevisionVcs> revisions = List.of(
                new RevisionVcs("r2", "sofia", "2026-11-05", "correction"),
                new RevisionVcs("r1", "amine", "2026-11-01", "creation"));
        return new FakeVcs(marks, supprimees, blame, revisions);
    }

    /** Les marques se servent par ligne, inchangée hors des plages. */
    @Test
    void marquesParLigne() {
        FakeVcs vcs = fauxPrecharge();
        assertEquals(VcsMark.MODIFIE, vcs.linesState(CheminDoc.of("/a")).mark(3));
        assertEquals(VcsMark.MODIFIE, vcs.linesState(CheminDoc.of("/a")).mark(4));
        assertEquals(VcsMark.AJOUTE, vcs.linesState(CheminDoc.of("/a")).mark(7));
        assertEquals(VcsMark.INCHANGE, vcs.linesState(CheminDoc.of("/a")).mark(1));
        assertEquals(VcsMark.INCHANGE, vcs.linesState(CheminDoc.of("/a")).mark(5));
    }

    /** Les plages supprimées se servent intactes : bornes inclusives. */
    @Test
    void plagesSupprimeesIntactes() {
        VcsLinesState state = fauxPrecharge().linesState(CheminDoc.of("/a"));
        assertEquals(1, state.supprimees().size());
        assertEquals(9, state.supprimees().get(0).start());
        assertEquals(10, state.supprimees().get(0).end());
    }

    /** Le blame sert une annotation par ligne fournie, dans l'ordre. */
    @Test
    void blameParLigne() {
        List<AnnotationBlame> blame = fauxPrecharge().blame(CheminDoc.of("/a"));
        assertEquals(4, blame.size());
        assertEquals(3, blame.get(2).line());
        assertEquals("sofia", blame.get(2).auteur());
        assertEquals("r2", blame.get(2).revision());
    }

    /** L'historique descend les révisions fournies, la plus récente en tête. */
    @Test
    void historiqueDescent() {
        List<RevisionVcs> revisions = fauxPrecharge().historique(CheminDoc.of("/a"));
        assertEquals(2, revisions.size());
        assertEquals("r2", revisions.get(0).id());
        assertEquals("2026-11-05", revisions.get(0).date());
    }

    /** Sans données, l'état est vide et muet : aucune marque, aucune ligne supprimée. */
    @Test
    void etatVideParDefaut() {
        FakeVcs vide = new FakeVcs();
        VcsLinesState state = vide.linesState(CheminDoc.of("/a"));
        assertEquals(VcsMark.INCHANGE, state.mark(1));
        assertEquals(VcsMark.INCHANGE, state.mark(100));
        assertEquals(0, state.supprimees().size());
        assertEquals(0, vide.blame(CheminDoc.of("/a")).size());
        assertEquals(0, vide.historique(CheminDoc.of("/a")).size());
    }
}
