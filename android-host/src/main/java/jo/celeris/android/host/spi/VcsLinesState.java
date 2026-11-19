package jo.celeris.android.host.spi;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * L'état des lignes d'un fichier côté VCS (E-02, D-03) : la marque par
 * ligne — inchangée par défaut — et les plages supprimées depuis la
 * révision de référence.
 */
public final class VcsLinesState {

    private static final VcsLinesState EMPTY =
            new VcsLinesState(Map.of(), List.of());

    private final Map<Integer, VcsMark> marks;

    private final List<LineRange> supprimees;

    private VcsLinesState(Map<Integer, VcsMark> marks,
            List<LineRange> supprimees) {
        this.marks = marks;
        this.supprimees = supprimees;
    }

    /**
     * L'état vide : aucune marque, aucune suppression.
     *
     * @return l'état muet
     */
    public static VcsLinesState vide() {
        return EMPTY;
    }

    /**
     * L'état depuis les marques par ligne et les plages supprimées.
     *
     * @param marks     la marque de chaque ligne modifiée ou ajoutée
     * @param supprimees  les plages de lignes supprimées
     * @return l'état correspondant
     */
    public static VcsLinesState of(Map<Integer, VcsMark> marks,
            List<LineRange> supprimees) {
        return new VcsLinesState(Map.copyOf(marks), List.copyOf(supprimees));
    }

    /**
     * La marque d'une ligne : inchangée si aucune marque enregistrée.
     *
     * @param line la ligne, numérotée à partir de un
     * @return la marque de la ligne, jamais null
     */
    public VcsMark mark(int line) {
        return marks.getOrDefault(line, VcsMark.INCHANGE);
    }

    /**
     * Les lignes marquées et leur marque — la matière première de la
     * peinture de gouttière (D-03), qui parcourt sans sonner ligne à
     * ligne.
     *
     * @return les marques par ligne (numérotées à partir de un), non
     *         modifiables
     */
    public Map<Integer, VcsMark> marks() {
        return marks;
    }

    /**
     * Les plages supprimées, dans l'ordre fourni.
     *
     * @return les plages, non modifiables
     */
    public List<LineRange> supprimees() {
        return supprimees;
    }

    @Override
    public boolean equals(Object autre) {
        return autre instanceof VcsLinesState state
                && state.marks.equals(marks) && state.supprimees.equals(supprimees);
    }

    @Override
    public int hashCode() {
        return Objects.hash(marks, supprimees);
    }
}
