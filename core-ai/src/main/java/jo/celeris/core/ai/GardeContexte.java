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
public final class GardeContexte implements ModeleTexteFantome, ModelePrediction {

    private final ModeleTexteFantome fantome;
    private final ModelePrediction prediction;
    private final String chemin;
    private final ExclusionsContexte exclusions;
    private final BooleanSupplier consentement;

    /**
     * Le garde d'un document unique, sans état de consentement : la
     * demande traverse — l'hôte qui ne gère pas de consentement le
     * garde ailleurs (l'activation explicite reste sa règle, ADR-012).
     *
     * @param fantome    le modèle de complétion branché par l'hôte
     * @param prediction le modèle de prédiction branché par l'hôte
     * @param chemin     le chemin du document servi, consulté contre
     *                   les exclusions
     * @param exclusions les exclusions de contexte du dépôt
     */
    public GardeContexte(ModeleTexteFantome fantome, ModelePrediction prediction,
            String chemin, ExclusionsContexte exclusions) {
        this(fantome, prediction, chemin, exclusions, () -> true);
    }

    /**
     * Le garde d'un document unique, avec l'état de consentement
     * (F-10) : relu à chaque départ — l'activation explicite de
     * l'utilisateur, jamais une constante figée à la construction.
     *
     * @param fantome     le modèle de complétion branché par l'hôte
     * @param prediction  le modèle de prédiction branché par l'hôte
     * @param chemin      le chemin du document servi, consulté contre
     *                    les exclusions
     * @param exclusions  les exclusions de contexte du dépôt
     * @param consentement l'état de consentement, consulté à chaque
     *                    départ
     */
    public GardeContexte(ModeleTexteFantome fantome, ModelePrediction prediction,
            String chemin, ExclusionsContexte exclusions,
            BooleanSupplier consentement) {
        this.fantome = fantome;
        this.prediction = prediction;
        this.chemin = chemin;
        this.exclusions = exclusions;
        this.consentement = consentement;
    }

    @Override
    public void completer(RequeteModele requete,
            ModeleTexteFantome.Recepteur recepteur) {
        if (!consentement.getAsBoolean()) {
            return;
        }
        if (exclusions.estExclu(chemin)) {
            return;
        }
        fantome.completer(requete, recepteur);
    }

    @Override
    public void predire(RequeteModele requete,
            ModelePrediction.Recepteur recepteur) {
        if (!consentement.getAsBoolean()) {
            return;
        }
        if (exclusions.estExclu(chemin)) {
            return;
        }
        prediction.predire(requete, recepteur);
    }
}
