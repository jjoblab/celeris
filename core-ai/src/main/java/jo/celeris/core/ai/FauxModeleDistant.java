package jo.celeris.core.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import org.jspecify.annotations.Nullable;

import jo.celeris.core.syntax.JetonAnnulation;

/**
 * Le modèle distant de référence (ADR-012, F-08) : le même contrat que
 * le local, la livraison en moins — elle ne part que par l'exécuteur
 * injecté, qui joue le réseau et décide quand la réponse arrive.
 *
 * <p>L'avis au départ de la requête est l'avis servi — le réseau ne
 * change pas d'avis en route. Le jeton se relit au moment de livrer :
 * un vol mort en route ne livre rien, où qu'il en soit (F-09). Le
 * journal des requêtes reçues suit la même discipline que le modèle
 * local — les exclusions de contexte (F-07) s'y liront pareil.</p>
 *
 * <p>Modèle d'exécution (ADR-007) : l'exécuteur est fourni par l'hôte —
 * le cœur n'en crée aucun. Les tests y mettent une pompe manuelle, le
 * temps devient déterministe ; la démonstration y met un fil différé,
 * le voyage devient visible.</p>
 */
public final class FauxModeleDistant implements ModeleTexteFantome, ModelePrediction {

    private final List<RequeteModele> requetes = new ArrayList<>();
    private final Executor reseau;
    private @Nullable SuggestionFantome avisFantome;
    private @Nullable PredictionEdition avisPrediction;

    /**
     * Un modèle distant qui livre par l'exécuteur donné.
     *
     * @param reseau l'exécuteur des livraisons — injecté par l'hôte
     */
    public FauxModeleDistant(Executor reseau) {
        this.reseau = reseau;
    }

    /**
     * Fixe l'avis servi aux requêtes de texte fantôme.
     *
     * @param suggestion la suggestion à servir, ou {@code null} pour
     *                   l'aveu qu'il n'y en a pas
     */
    public void fantome(@Nullable SuggestionFantome suggestion) {
        this.avisFantome = suggestion;
    }

    /**
     * Fixe l'avis servi aux requêtes de prédiction.
     *
     * @param prediction la prédiction à servir, ou {@code null} pour
     *                   l'aveu qu'il n'y en a pas
     */
    public void prediction(@Nullable PredictionEdition prediction) {
        this.avisPrediction = prediction;
    }

    /**
     * Le journal des requêtes reçues, dans l'ordre — fantôme et
     * prédiction confondues.
     *
     * @return une copie figée du journal
     */
    public List<RequeteModele> requetes() {
        return List.copyOf(requetes);
    }

    @Override
    public void completer(RequeteModele requete, ModeleTexteFantome.Recepteur recepteur) {
        requetes.add(requete);
        JetonAnnulation jeton = requete.jeton();
        if (jeton.estAnnule()) {
            return;
        }
        SuggestionFantome avis = avisFantome;
        reseau.execute(() -> {
            if (jeton.estAnnule()) {
                return;
            }
            if (avis == null) {
                recepteur.aucune();
            } else {
                recepteur.suggestion(avis);
            }
        });
    }

    @Override
    public void predire(RequeteModele requete, ModelePrediction.Recepteur recepteur) {
        requetes.add(requete);
        JetonAnnulation jeton = requete.jeton();
        if (jeton.estAnnule()) {
            return;
        }
        PredictionEdition avis = avisPrediction;
        reseau.execute(() -> {
            if (jeton.estAnnule()) {
                return;
            }
            if (avis == null) {
                recepteur.aucune();
            } else {
                recepteur.prediction(avis);
            }
        });
    }
}
