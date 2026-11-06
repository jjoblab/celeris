package jo.celeris.android.host.spi;

import java.util.List;
import java.util.Map;

/**
 * Le faux VCS de référence (E-02) : sert les données qu'on lui donne,
 * déterministe, pour les tests et la démonstration. Aucun calcul de
 * diff — les marques sont déclarées à la construction, les mêmes pour
 * tout fichier interrogé (les tests visent le contrat, pas un dépôt
 * réel).
 */
public final class FauxVcs implements IntegrationVcs {

    private final EtatLignesVcs etatServi;

    private final List<AnnotationBlame> blameServi;

    private final List<RevisionVcs> historiqueServi;

    /**
     * Construit le faux muet : tout fichier rend l'état vide.
     */
    public FauxVcs() {
        this(Map.of(), List.of(), List.of(), List.of());
    }

    /**
     * Construit le faux préchargé.
     *
     * @param marques    la marque de chaque ligne modifiée ou ajoutée
     * @param supprimees les plages supprimées
     * @param blame      les annotations de blame, dans l'ordre des lignes
     * @param revisions  l'historique, la plus récente en tête
     */
    public FauxVcs(Map<Integer, MarqueVcs> marques, List<PlageLignes> supprimees,
            List<AnnotationBlame> blame, List<RevisionVcs> revisions) {
        this.etatServi = EtatLignesVcs.de(marques, supprimees);
        this.blameServi = List.copyOf(blame);
        this.historiqueServi = List.copyOf(revisions);
    }

    @Override
    public EtatLignesVcs etatLignes(CheminDoc fichier) {
        return etatServi;
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
