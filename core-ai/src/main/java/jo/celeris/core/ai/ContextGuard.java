package jo.celeris.core.ai;

import java.util.function.BooleanSupplier;

/**
 * Le garde du contexte (F-07, F-10, ADR-012) : un chemin exclu ne part
 * jamais vers un modèle, un consentement absent ne part pas non plus —
 * la requête elle-même reste au sol, le journal du modèle en témoigne.
 *
 * <p>Le garde se glisse entre la session et le modèle branché par
 * l'hôte : il porte le chemin du document servi, les exclusions lues
 * du fichier du dépôt et l'état de consentement (F-10 — relu à chaque
 * départ, l'activation explicite de l'utilisateur reste maîtresse).
 * Le refus ne produit aucun départ — pas de requête, donc pas de
 * réponse : le fantôme reste absent, la prédiction ne vient pas,
 * exactement comme devant un modèle qui ne répond pas. Le chemin admis
 * et consenti traverse tel quel, le garde ne touche à rien
 * d'autre.</p>
 */
public final class ContextGuard implements GhostTextModel, PredictionModel {

    private final GhostTextModel ghost;
    private final PredictionModel prediction;
    private final String chemin;
    private final ContextExclusions exclusions;
    private final BooleanSupplier consentement;

    /**
     * Le garde d'un document unique, sans état de consentement : la
     * demande traverse — l'hôte qui ne gère pas de consentement le
     * garde ailleurs (l'activation explicite reste sa règle, ADR-012).
     *
     * @param ghost    le modèle de complétion branché par l'hôte
     * @param prediction le modèle de prédiction branché par l'hôte
     * @param chemin     le chemin du document servi, consulté contre
     *                   les exclusions
     * @param exclusions les exclusions de contexte du dépôt
     */
    public ContextGuard(GhostTextModel ghost, PredictionModel prediction,
            String chemin, ContextExclusions exclusions) {
        this(ghost, prediction, chemin, exclusions, () -> true);
    }

    /**
     * Le garde d'un document unique, avec l'état de consentement
     * (F-10) : relu à chaque départ — l'activation explicite de
     * l'utilisateur, jamais une constante figée à la construction.
     *
     * @param ghost     le modèle de complétion branché par l'hôte
     * @param prediction  le modèle de prédiction branché par l'hôte
     * @param chemin      le chemin du document servi, consulté contre
     *                    les exclusions
     * @param exclusions  les exclusions de contexte du dépôt
     * @param consentement l'état de consentement, consulté à chaque
     *                    départ
     */
    public ContextGuard(GhostTextModel ghost, PredictionModel prediction,
            String chemin, ContextExclusions exclusions,
            BooleanSupplier consentement) {
        this.ghost = ghost;
        this.prediction = prediction;
        this.chemin = chemin;
        this.exclusions = exclusions;
        this.consentement = consentement;
    }

    @Override
    public void completer(ModelRequest requete,
            GhostTextModel.Receiver receiver) {
        if (!consentement.getAsBoolean()) {
            return;
        }
        if (exclusions.isExcluded(chemin)) {
            return;
        }
        ghost.completer(requete, receiver);
    }

    @Override
    public void predire(ModelRequest requete,
            PredictionModel.Receiver receiver) {
        if (!consentement.getAsBoolean()) {
            return;
        }
        if (exclusions.isExcluded(chemin)) {
            return;
        }
        prediction.predire(requete, receiver);
    }
}
