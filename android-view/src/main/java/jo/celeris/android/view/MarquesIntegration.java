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
public final class MarquesIntegration {

    /** Les marques vides : aucune ligne marquée, aucun arrêt, aucune pause. */
    public static final MarquesIntegration VIDE =
            new MarquesIntegration(Map.of(), Map.of(), null, Set.of());

    private final Map<Integer, MarqueLigne> marquesVcs;

    private final Map<Integer, EtatArret> arrets;

    private final @Nullable Integer ligneCourante;

    private final Set<Integer> lignesPile;

    private MarquesIntegration(Map<Integer, MarqueLigne> marquesVcs,
            Map<Integer, EtatArret> arrets, @Nullable Integer ligneCourante,
            Set<Integer> lignesPile) {
        this.marquesVcs = Map.copyOf(marquesVcs);
        this.arrets = Map.copyOf(arrets);
        this.ligneCourante = ligneCourante;
        this.lignesPile = Set.copyOf(lignesPile);
    }

    /**
     * Les marques d'intégration depuis leurs sources.
     *
     * @param marquesVcs    la marque de modification de chaque ligne
     *                      marquée (les absentes sont inchangées)
     * @param arrets        l'état d'arrêt de chaque ligne porteuse (les
     *                      absentes n'en portent pas)
     * @param ligneCourante la ligne courante de l'exécution en pause,
     *                      null hors pause
     * @param lignesPile    les lignes des cadres de la pile courante
     * @return les marques correspondantes
     */
    public static MarquesIntegration of(Map<Integer, MarqueLigne> marquesVcs,
            Map<Integer, EtatArret> arrets, @Nullable Integer ligneCourante,
            Set<Integer> lignesPile) {
        if (marquesVcs.isEmpty() && arrets.isEmpty() && ligneCourante == null
                && lignesPile.isEmpty()) {
            return VIDE;
        }
        return new MarquesIntegration(marquesVcs, arrets, ligneCourante, lignesPile);
    }

    /** Vrai quand aucune marque, aucun arrêt ni repère ne se peint. */
    public boolean estVide() {
        return marquesVcs.isEmpty() && arrets.isEmpty() && ligneCourante == null
                && lignesPile.isEmpty();
    }

    /**
     * La marque de modification d'une ligne : inchangée si non marquée.
     *
     * @param ligne la ligne, numérotée à partir de zéro
     * @return la marque de la ligne, jamais null
     */
    public MarqueLigne marqueVcs(int ligne) {
        return marquesVcs.getOrDefault(ligne, MarqueLigne.INCHANGE);
    }

    /**
     * L'état d'arrêt d'une ligne : aucun si non porteuse.
     *
     * @param ligne la ligne, numérotée à partir de zéro
     * @return l'état d'arrêt, jamais null
     */
    public EtatArret arret(int ligne) {
        return arrets.getOrDefault(ligne, EtatArret.AUCUN);
    }

    /**
     * La ligne courante de l'exécution en pause.
     *
     * @return la ligne, numérotée à partir de zéro, ou null hors pause
     */
    public @Nullable Integer ligneCourante() {
        return ligneCourante;
    }

    /**
     * Vrai quand la ligne porte un cadre de la pile courante.
     *
     * @param ligne la ligne, numérotée à partir de zéro
     * @return vrai si la ligne est dans la pile
     */
    public boolean dansPile(int ligne) {
        return lignesPile.contains(ligne);
    }
}
