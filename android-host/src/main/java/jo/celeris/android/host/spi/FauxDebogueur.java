package jo.celeris.android.host.spi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * Le faux débogueur de référence (E-03) : les arrêts vivent en mémoire,
 * la pause et la pile se simulent explicitement — déterministe, pour
 * les tests et la démonstration.
 */
public final class FauxDebogueur implements IntegrationDebogueur {

    private final List<Arret> arrets = new ArrayList<>();

    private final Map<Integer, List<ValeurEnLigne>> valeursEnLigne = new HashMap<>();

    private final Map<Integer, Map<String, String>> survols = new HashMap<>();

    private @Nullable LigneArret pause;

    private List<CadrePile> pile = List.of();

    @Override
    public void poserArret(CheminDoc fichier, int ligne) {
        Arret candidat = new Arret(fichier, ligne, true);
        if (!arrets.contains(candidat)) {
            arrets.add(candidat);
        }
    }

    @Override
    public void retirerArret(CheminDoc fichier, int ligne) {
        arrets.removeIf(arret -> arret.fichier().equals(fichier)
                && arret.ligne() == ligne);
    }

    @Override
    public void basculerArret(CheminDoc fichier, int ligne) {
        for (int i = 0; i < arrets.size(); i++) {
            Arret arret = arrets.get(i);
            if (arret.fichier().equals(fichier) && arret.ligne() == ligne) {
                arrets.set(i, arret.bascule());
                return;
            }
        }
    }

    @Override
    public List<Arret> arrets() {
        return List.copyOf(arrets);
    }

    @Override
    public @Nullable LigneArret ligneCourante() {
        return pause;
    }

    @Override
    public List<CadrePile> pileCourante() {
        return pile;
    }

    @Override
    public String evaluerAuSurvol(CheminDoc fichier, int ligne, int colonne,
            String expression) {
        if (pause == null) {
            throw new IllegalStateException("not paused");
        }
        if (!survolAutorise(fichier, ligne)) {
            throw new IllegalStateException("line outside the current frame");
        }
        Map<String, String> parExpression = survols.getOrDefault(ligne, Map.of());
        return parExpression.getOrDefault(expression, "");
    }

    @Override
    public List<ValeurEnLigne> valeursEnLigne(int ligne) {
        return List.copyOf(valeursEnLigne.getOrDefault(ligne, List.of()));
    }

    /**
     * Simule la pause de l'exécution au lieu donné, avec sa pile : les
     * valeurs en ligne d'une pause d'avant se retirent — chaque pause
     * recommence à zéro, l'hôte y re-sert ses valeurs fraîches.
     *
     * @param fichier le fichier de la pause
     * @param ligne   la ligne de la pause
     * @param pile    la pile d'appels, du plus récent au plus ancien
     */
    public void simulerPause(CheminDoc fichier, int ligne, List<CadrePile> pile) {
        this.pause = new LigneArret(fichier, ligne);
        this.pile = List.copyOf(pile);
        valeursEnLigne.clear();
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
     * @param ligne      la ligne du survol
     * @param expression l'expression évaluée
     * @param valeur     la valeur à rendre
     */
    public void enregistrerSurvol(int ligne, String expression, String valeur) {
        survols.computeIfAbsent(ligne, k -> new HashMap<>())
                .put(expression, valeur);
    }

    /**
     * Enregistre une valeur en marge de ligne.
     *
     * @param ligne      la ligne de la valeur
     * @param expression l'expression affichée
     * @param valeur     la valeur affichée
     */
    public void enregistrerValeurEnLigne(int ligne, String expression, String valeur) {
        valeursEnLigne.computeIfAbsent(ligne, k -> new ArrayList<>())
                .add(new ValeurEnLigne(ligne, expression, valeur));
    }

    private boolean survolAutorise(CheminDoc fichier, int ligne) {
        if (pause == null) {
            return false;
        }
        if (pause.fichier().equals(fichier) && pause.ligne() == ligne) {
            return true;
        }
        for (CadrePile cadre : pile) {
            if (cadre.fichier().equals(fichier.nom()) && cadre.ligne() == ligne) {
                return true;
            }
        }
        return false;
    }
}
