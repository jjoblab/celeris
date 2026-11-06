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
final class FauxVcsTest {

    private static FauxVcs fauxPrecharge() {
        Map<Integer, MarqueVcs> marques = Map.of(
                3, MarqueVcs.MODIFIE,
                4, MarqueVcs.MODIFIE,
                7, MarqueVcs.AJOUTE);
        List<PlageLignes> supprimees = List.of(new PlageLignes(9, 10));
        List<AnnotationBlame> blame = List.of(
                new AnnotationBlame(1, "r1", "amine", "creation"),
                new AnnotationBlame(2, "r1", "amine", "creation"),
                new AnnotationBlame(3, "r2", "sofia", "correction"),
                new AnnotationBlame(7, "r2", "sofia", "ajout"));
        List<RevisionVcs> revisions = List.of(
                new RevisionVcs("r2", "sofia", "2026-11-05", "correction"),
                new RevisionVcs("r1", "amine", "2026-11-01", "creation"));
        return new FauxVcs(marques, supprimees, blame, revisions);
    }

    /** Les marques se servent par ligne, inchangée hors des plages. */
    @Test
    void marquesParLigne() {
        FauxVcs vcs = fauxPrecharge();
        assertEquals(MarqueVcs.MODIFIE, vcs.etatLignes(CheminDoc.de("/a")).marque(3));
        assertEquals(MarqueVcs.MODIFIE, vcs.etatLignes(CheminDoc.de("/a")).marque(4));
        assertEquals(MarqueVcs.AJOUTE, vcs.etatLignes(CheminDoc.de("/a")).marque(7));
        assertEquals(MarqueVcs.INCHANGE, vcs.etatLignes(CheminDoc.de("/a")).marque(1));
        assertEquals(MarqueVcs.INCHANGE, vcs.etatLignes(CheminDoc.de("/a")).marque(5));
    }

    /** Les plages supprimées se servent intactes : bornes inclusives. */
    @Test
    void plagesSupprimeesIntactes() {
        EtatLignesVcs etat = fauxPrecharge().etatLignes(CheminDoc.de("/a"));
        assertEquals(1, etat.supprimees().size());
        assertEquals(9, etat.supprimees().get(0).debut());
        assertEquals(10, etat.supprimees().get(0).fin());
    }

    /** Le blame sert une annotation par ligne fournie, dans l'ordre. */
    @Test
    void blameParLigne() {
        List<AnnotationBlame> blame = fauxPrecharge().blame(CheminDoc.de("/a"));
        assertEquals(4, blame.size());
        assertEquals(3, blame.get(2).ligne());
        assertEquals("sofia", blame.get(2).auteur());
        assertEquals("r2", blame.get(2).revision());
    }

    /** L'historique descend les révisions fournies, la plus récente en tête. */
    @Test
    void historiqueDescent() {
        List<RevisionVcs> revisions = fauxPrecharge().historique(CheminDoc.de("/a"));
        assertEquals(2, revisions.size());
        assertEquals("r2", revisions.get(0).id());
        assertEquals("2026-11-05", revisions.get(0).date());
    }

    /** Sans données, l'état est vide et muet : aucune marque, aucune ligne supprimée. */
    @Test
    void etatVideParDefaut() {
        FauxVcs vide = new FauxVcs();
        EtatLignesVcs etat = vide.etatLignes(CheminDoc.de("/a"));
        assertEquals(MarqueVcs.INCHANGE, etat.marque(1));
        assertEquals(MarqueVcs.INCHANGE, etat.marque(100));
        assertEquals(0, etat.supprimees().size());
        assertEquals(0, vide.blame(CheminDoc.de("/a")).size());
        assertEquals(0, vide.historique(CheminDoc.de("/a")).size());
    }
}
