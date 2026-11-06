package jo.celeris.android.host.spi;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * L'état des lignes d'un fichier côté VCS (E-02, D-03) : la marque par
 * ligne — inchangée par défaut — et les plages supprimées depuis la
 * révision de référence.
 */
public final class EtatLignesVcs {

    private static final EtatLignesVcs VIDE =
            new EtatLignesVcs(Map.of(), List.of());

    private final Map<Integer, MarqueVcs> marques;

    private final List<PlageLignes> supprimees;

    private EtatLignesVcs(Map<Integer, MarqueVcs> marques,
            List<PlageLignes> supprimees) {
        this.marques = marques;
        this.supprimees = supprimees;
    }

    /**
     * L'état vide : aucune marque, aucune suppression.
     *
     * @return l'état muet
     */
    public static EtatLignesVcs vide() {
        return VIDE;
    }

    /**
     * L'état depuis les marques par ligne et les plages supprimées.
     *
     * @param marques     la marque de chaque ligne modifiée ou ajoutée
     * @param supprimees  les plages de lignes supprimées
     * @return l'état correspondant
     */
    public static EtatLignesVcs de(Map<Integer, MarqueVcs> marques,
            List<PlageLignes> supprimees) {
        return new EtatLignesVcs(Map.copyOf(marques), List.copyOf(supprimees));
    }

    /**
     * La marque d'une ligne : inchangée si aucune marque enregistrée.
     *
     * @param ligne la ligne, numérotée à partir de un
     * @return la marque de la ligne, jamais null
     */
    public MarqueVcs marque(int ligne) {
        return marques.getOrDefault(ligne, MarqueVcs.INCHANGE);
    }

    /**
     * Les lignes marquées et leur marque — la matière première de la
     * peinture de gouttière (D-03), qui parcourt sans sonner ligne à
     * ligne.
     *
     * @return les marques par ligne (numérotées à partir de un), non
     *         modifiables
     */
    public Map<Integer, MarqueVcs> marques() {
        return marques;
    }

    /**
     * Les plages supprimées, dans l'ordre fourni.
     *
     * @return les plages, non modifiables
     */
    public List<PlageLignes> supprimees() {
        return supprimees;
    }

    @Override
    public boolean equals(Object autre) {
        return autre instanceof EtatLignesVcs etat
                && etat.marques.equals(marques) && etat.supprimees.equals(supprimees);
    }

    @Override
    public int hashCode() {
        return Objects.hash(marques, supprimees);
    }
}
