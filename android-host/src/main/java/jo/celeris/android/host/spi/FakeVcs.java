package jo.celeris.android.host.spi;

import java.util.List;
import java.util.Map;

/**
 * Le faux VCS de référence (E-02) : sert les données qu'on lui donne,
 * déterministe, pour les tests et les exemples d'intégration. Aucun calcul de
 * diff — les marques sont déclarées à la construction, les mêmes pour
 * tout fichier interrogé (les tests visent le contrat, pas un dépôt
 * réel).
 */
public final class FakeVcs implements IntegrationVcs {

    private final VcsLinesState servedState;

    private final List<AnnotationBlame> blameServi;

    private final List<RevisionVcs> historiqueServi;

    /**
     * Construit le faux muet : tout fichier rend l'état vide.
     */
    public FakeVcs() {
        this(Map.of(), List.of(), List.of(), List.of());
    }

    /**
     * Construit le faux préchargé.
     *
     * @param marks    la marque de chaque ligne modifiée ou ajoutée
     * @param supprimees les plages supprimées
     * @param blame      les annotations de blame, dans l'ordre des lignes
     * @param revisions  l'historique, la plus récente en tête
     */
    public FakeVcs(Map<Integer, VcsMark> marks, List<LineRange> supprimees,
            List<AnnotationBlame> blame, List<RevisionVcs> revisions) {
        this.servedState = VcsLinesState.of(marks, supprimees);
        this.blameServi = List.copyOf(blame);
        this.historiqueServi = List.copyOf(revisions);
    }

    @Override
    public VcsLinesState linesState(CheminDoc fichier) {
        return servedState;
    }

    @Override
    public List<AnnotationBlame> blame(CheminDoc fichier) {
        return blameServi;
    }

    @Override
    public List<RevisionVcs> historique(CheminDoc fichier) {
        return historiqueServi;
    }
}
