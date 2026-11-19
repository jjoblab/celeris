package jo.celeris.android.host.spi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * Le faux débogueur de référence (E-03) : les arrêts vivent en mémoire,
 * la pause et la pile se simulent explicitement — déterministe, pour
 * les tests et les exemples d'intégration.
 */
public final class FakeDebugger implements IntegrationDebogueur {

    private final List<Breakpoint> breakpoints = new ArrayList<>();

    private final Map<Integer, List<InlineValue>> inlineValues = new HashMap<>();

    private final Map<Integer, Map<String, String>> survols = new HashMap<>();

    private @Nullable BreakpointLine pause;

    private List<StackFrame> pile = List.of();

    @Override
    public void placeBreakpoint(CheminDoc fichier, int line) {
        Breakpoint candidat = new Breakpoint(fichier, line, true);
        if (!breakpoints.contains(candidat)) {
            breakpoints.add(candidat);
        }
    }

    @Override
    public void removeBreakpoint(CheminDoc fichier, int line) {
        breakpoints.removeIf(stop -> stop.fichier().equals(fichier)
                && stop.line() == line);
    }

    @Override
    public void toggleBreakpoint(CheminDoc fichier, int line) {
        for (int i = 0; i < breakpoints.size(); i++) {
            Breakpoint stop = breakpoints.get(i);
            if (stop.fichier().equals(fichier) && stop.line() == line) {
                breakpoints.set(i, stop.bascule());
                return;
            }
        }
    }

    @Override
    public List<Breakpoint> breakpoints() {
        return List.copyOf(breakpoints);
    }

    @Override
    public @Nullable BreakpointLine currentLine() {
        return pause;
    }

    @Override
    public List<StackFrame> currentStack() {
        return pile;
    }

    @Override
    public String evaluerAuSurvol(CheminDoc fichier, int line, int column,
            String expression) {
        if (pause == null) {
            throw new IllegalStateException("not paused");
        }
        if (!survolAutorise(fichier, line)) {
            throw new IllegalStateException("line outside the current frame");
        }
        Map<String, String> parExpression = survols.getOrDefault(line, Map.of());
        return parExpression.getOrDefault(expression, "");
    }

    @Override
    public List<InlineValue> inlineValues(int line) {
        return List.copyOf(inlineValues.getOrDefault(line, List.of()));
    }

    /**
     * Simule la pause de l'exécution au lieu donné, avec sa pile : les
     * valeurs en ligne d'une pause d'avant se retirent — chaque pause
     * recommence à zéro, l'hôte y re-sert ses valeurs fraîches.
     *
     * @param fichier le fichier de la pause
     * @param line   la ligne de la pause
     * @param pile    la pile d'appels, du plus récent au plus ancien
     */
    public void simulerPause(CheminDoc fichier, int line, List<StackFrame> pile) {
        this.pause = new BreakpointLine(fichier, line);
        this.pile = List.copyOf(pile);
        inlineValues.clear();
    }

    /** Simule la reprise : plus de ligne courante ni de pile. */
    public void simulerReprise() {
        this.pause = null;
        this.pile = List.of();
    }

    /**
     * Enregistre la valeur d'une expression pour les survols d'une
     * ligne.
     *
     * @param line      la ligne du survol
     * @param expression l'expression évaluée
     * @param value     la valeur à rendre
     */
    public void enregistrerSurvol(int line, String expression, String value) {
        survols.computeIfAbsent(line, k -> new HashMap<>())
                .put(expression, value);
    }

    /**
     * Enregistre une valeur en marge de ligne.
     *
     * @param line      la ligne de la valeur
     * @param expression l'expression affichée
     * @param value     la valeur affichée
     */
    public void recordInlineValue(int line, String expression, String value) {
        inlineValues.computeIfAbsent(line, k -> new ArrayList<>())
                .add(new InlineValue(line, expression, value));
    }

    private boolean survolAutorise(CheminDoc fichier, int line) {
        if (pause == null) {
            return false;
        }
        if (pause.fichier().equals(fichier) && pause.line() == line) {
            return true;
        }
        for (StackFrame cadre : pile) {
            if (cadre.fichier().equals(fichier.nom()) && cadre.line() == line) {
                return true;
            }
        }
        return false;
    }
}
