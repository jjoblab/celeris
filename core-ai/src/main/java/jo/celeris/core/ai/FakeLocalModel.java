package jo.celeris.core.ai;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Le modèle local de référence (ADR-012, option A) : déterministe,
 * scripté, sans réseau — la preuve que le contrat se vit sans
 * fournisseur.
 *
 * <p>L'avis courant sert toutes les requêtes jusqu'à ce qu'il change :
 * le test fixe l'avis, demande, observe. Chaque requête reçue entre au
 * journal — le jour des exclusions de contexte (F-07), c'est ce journal
 * qui prouvera qu'un chemin exclu ne part jamais vers un modèle. La
 * livraison est synchrone, sur le fil de l'appelant : le modèle local
 * n'a pas de voyage à faire.</p>
 */
public final class FakeLocalModel implements GhostTextModel, PredictionModel {

    private final List<ModelRequest> requetes = new ArrayList<>();
    private @Nullable GhostSuggestion avisGhost;
    private @Nullable PredictionEdit avisPrediction;

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
        if (requete.token().isCancelled()) {
            return;
        }
        GhostSuggestion avis = avisGhost;
        if (avis == null) {
            receiver.aucune();
        } else {
            receiver.suggestion(avis);
        }
    }

    @Override
    public void predire(ModelRequest requete, PredictionModel.Receiver receiver) {
        requetes.add(requete);
        if (requete.token().isCancelled()) {
            return;
        }
        PredictionEdit avis = avisPrediction;
        if (avis == null) {
            receiver.aucune();
        } else {
            receiver.prediction(avis);
        }
    }
}
