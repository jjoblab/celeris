package jo.celeris.android.view;

import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * Les marques d'intégration de la gouttière (D-03, D-04, D-05) : les
 * marques de modification VCS par ligne, les points d'arrêt posés et
 * désactivés, la ligne courante de l'exécution en pause et les lignes
 * de sa pile — l'état immuable de la vue, alimenté par l'adaptateur de
 * l'hôte, consommé par les marques et la peinture de gouttière.
 *
 * <p>Les lignes sont numérotées à partir de zéro, convention de la vue
 * et de l'arbre ; les contrats de l'hôte numérotent à partir de un —
 * la conversion vit dans l'adaptateur. Une instance vide ne porte
 * aucune marque : la coulisse des marqueurs ne s'élargit pas pour
 * elle.</p>
 */
public final class IntegrationMarks {

    /** Les marques vides : aucune ligne marquée, aucun arrêt, aucune pause. */
    public static final IntegrationMarks EMPTY =
            new IntegrationMarks(Map.of(), Map.of(), null, Set.of());

    private final Map<Integer, MarkLine> vcsMarks;

    private final Map<Integer, BreakpointState> breakpoints;

    private final @Nullable Integer currentLine;

    private final Set<Integer> stackLines;

    private IntegrationMarks(Map<Integer, MarkLine> vcsMarks,
            Map<Integer, BreakpointState> breakpoints, @Nullable Integer currentLine,
            Set<Integer> stackLines) {
        this.vcsMarks = Map.copyOf(vcsMarks);
        this.breakpoints = Map.copyOf(breakpoints);
        this.currentLine = currentLine;
        this.stackLines = Set.copyOf(stackLines);
    }

    /**
     * Les marques d'intégration depuis leurs sources.
     *
     * @param vcsMarks    la marque de modification de chaque ligne
     *                      marquée (les absentes sont inchangées)
     * @param breakpoints        l'état d'arrêt de chaque ligne porteuse (les
     *                      absentes n'en portent pas)
     * @param currentLine la ligne courante de l'exécution en pause,
     *                      null hors pause
     * @param stackLines    les lignes des cadres de la pile courante
     * @return les marques correspondantes
     */
    public static IntegrationMarks of(Map<Integer, MarkLine> vcsMarks,
            Map<Integer, BreakpointState> breakpoints, @Nullable Integer currentLine,
            Set<Integer> stackLines) {
        if (vcsMarks.isEmpty() && breakpoints.isEmpty() && currentLine == null
                && stackLines.isEmpty()) {
            return EMPTY;
        }
        return new IntegrationMarks(vcsMarks, breakpoints, currentLine, stackLines);
    }

    /** Vrai quand aucune marque, aucun arrêt ni repère ne se peint. */
    public boolean estVide() {
        return vcsMarks.isEmpty() && breakpoints.isEmpty() && currentLine == null
                && stackLines.isEmpty();
    }

    /**
     * La marque de modification d'une ligne : inchangée si non marquée.
     *
     * @param line la ligne, numérotée à partir de zéro
     * @return la marque de la ligne, jamais null
     */
    public MarkLine marqueVcs(int line) {
        return vcsMarks.getOrDefault(line, MarkLine.INCHANGE);
    }

    /**
     * L'état d'arrêt d'une ligne : aucun si non porteuse.
     *
     * @param line la ligne, numérotée à partir de zéro
     * @return l'état d'arrêt, jamais null
     */
    public BreakpointState stop(int line) {
        return breakpoints.getOrDefault(line, BreakpointState.AUCUN);
    }

    /**
     * La ligne courante de l'exécution en pause.
     *
     * @return la ligne, numérotée à partir de zéro, ou null hors pause
     */
    public @Nullable Integer currentLine() {
        return currentLine;
    }

    /**
     * Vrai quand la ligne porte un cadre de la pile courante.
     *
     * @param line la ligne, numérotée à partir de zéro
     * @return vrai si la ligne est dans la pile
     */
    public boolean inStack(int line) {
        return stackLines.contains(line);
    }
}
