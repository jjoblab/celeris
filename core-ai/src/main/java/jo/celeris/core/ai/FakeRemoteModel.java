package jo.celeris.core.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import org.jspecify.annotations.Nullable;

import jo.celeris.core.syntax.TokenCancellation;

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
 * temps devient déterministe ; l'exemplenstration y met un fil différé,
 * le voyage devient visible.</p>
 */
public final class FakeRemoteModel implements GhostTextModel, PredictionModel {

    private final List<ModelRequest> requetes = new ArrayList<>();
    private final Executor reseau;
    private @Nullable GhostSuggestion avisGhost;
    private @Nullable PredictionEdit avisPrediction;

    /**
     * Un modèle distant qui livre par l'exécuteur donné.
     *
     * @param reseau l'exécuteur des livraisons — injecté par l'hôte
     */
    public FakeRemoteModel(Executor reseau) {
        this.reseau = reseau;
    }

    /**
     * Fixe l'avis servi aux requêtes de texte fantôme.
     *
     * @param suggestion la suggestion à servir, ou {@code null} pour
     *                   l'aveu qu'il n'y en a pas
     */
    public void ghost(@Nullable GhostSuggestion suggestion) {
        this.avisGhost = suggestion;
    }

    /**
     * Fixe l'avis servi aux requêtes de prédiction.
     *
     * @param prediction la prédiction à servir, ou {@code null} pour
     *                   l'aveu qu'il n'y en a pas
     */
    public void prediction(@Nullable PredictionEdit prediction) {
        this.avisPrediction = prediction;
    }

    /**
     * Le journal des requêtes reçues, dans l'ordre — fantôme et
     * prédiction confondues.
     *
     * @return une copie figée du journal
     */
    public List<ModelRequest> requetes() {
        return List.copyOf(requetes);
    }

    @Override
    public void completer(ModelRequest requete, GhostTextModel.Receiver receiver) {
        requetes.add(requete);
        TokenCancellation token = requete.token();
        if (token.isCancelled()) {
            return;
        }
        GhostSuggestion avis = avisGhost;
        reseau.execute(() -> {
            if (token.isCancelled()) {
                return;
            }
            if (avis == null) {
                receiver.aucune();
            } else {
                receiver.suggestion(avis);
            }
        });
    }

    @Override
    public void predire(ModelRequest requete, PredictionModel.Receiver receiver) {
        requetes.add(requete);
        TokenCancellation token = requete.token();
        if (token.isCancelled()) {
            return;
        }
        PredictionEdit avis = avisPrediction;
        reseau.execute(() -> {
            if (token.isCancelled()) {
                return;
            }
            if (avis == null) {
                receiver.aucune();
            } else {
                receiver.prediction(avis);
            }
        });
    }
}
