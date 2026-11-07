package jo.celeris.core.ai;

import java.util.List;

import jo.celeris.core.text.TextEdit;

/**
 * Une prédiction d'édition (F-02, ADR-012) : le modèle prédit une
 * opération, pas un texte — une liste d'éditions qui s'applique comme
 * une transaction ordinaire, donc annulable et diffable.
 *
 * <p>La prédiction ne s'applique JAMAIS d'elle-même : le cœur ne donne
 * au modèle aucune session à toucher. L'hôte décide — l'action explicite
 * d'un utilisateur, la revue d'un agent — puis applique par la voie de
 * transaction unique. L'instantané de référence ({@code revision})
 * doit correspondre au document au moment de l'application : sinon le
 * refus est explicite, jamais de fusion silencieuse.</p>
 *
 * @param description la description courte de l'opération prédite
 * @param edits       les éditions prédites, jamais vides
 * @param revision    la révision du document contre laquelle la
 *                    prédiction a été calculée
 */
public record PredictionEdition(String description, List<TextEdit> edits, int revision) {

    /**
     * Valide la prédiction et la fige : description, éditions, révision.
     *
     * @throws IllegalArgumentException si la description est vide, si les
     *         éditions sont vides ou si la révision est négative
     */
    public PredictionEdition {
        if (description.isEmpty()) {
            throw new IllegalArgumentException("prediction description is empty");
        }
        if (edits.isEmpty()) {
            throw new IllegalArgumentException("prediction edits are empty");
        }
        if (revision < 0) {
            throw new IllegalArgumentException("revision must be >= 0: " + revision);
        }
        edits = List.copyOf(edits);
    }
}
