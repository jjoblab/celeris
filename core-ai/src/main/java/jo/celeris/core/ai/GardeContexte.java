package jo.celeris.core.ai;

/**
 * Le garde du contexte (F-07, ADR-012) : un chemin exclu ne part
 * jamais vers un modèle — la requête elle-même reste au sol, le
 * journal du modèle en témoigne.
 *
 * <p>Le garde se glisse entre la session et le modèle branché par
 * l'hôte : il porte le chemin du document servi et les exclusions
 * lues du fichier du dépôt. Le chemin exclu ne produit aucun départ —
 * pas de requête, donc pas de réponse : le fantôme reste absent, la
 * prédiction ne vient pas, exactement comme devant un modèle qui ne
 * répond pas. Le chemin admis traverse tel quel, le garde ne touche à
 * rien d'autre.</p>
 */
public final class GardeContexte implements ModeleTexteFantome, ModelePrediction {

    private final ModeleTexteFantome fantome;
    private final ModelePrediction prediction;
    private final String chemin;
    private final ExclusionsContexte exclusions;

    /**
     * Le garde d'un document unique : les deux contrats du modèle se
     * gardent ensemble, fantôme et prédiction.
     *
     * @param fantome    le modèle de complétion branché par l'hôte
     * @param prediction le modèle de prédiction branché par l'hôte
     * @param chemin     le chemin du document servi, jamais envoyé tel
     *                   quel — consulté contre les exclusions
     * @param exclusions les exclusions de contexte du dépôt
     */
    public GardeContexte(ModeleTexteFantome fantome, ModelePrediction prediction,
            String chemin, ExclusionsContexte exclusions) {
        this.fantome = fantome;
        this.prediction = prediction;
        this.chemin = chemin;
        this.exclusions = exclusions;
    }

    @Override
    public void completer(RequeteModele requete,
            ModeleTexteFantome.Recepteur recepteur) {
        if (exclusions.estExclu(chemin)) {
            return;
        }
        fantome.completer(requete, recepteur);
    }

    @Override
    public void predire(RequeteModele requete,
            ModelePrediction.Recepteur recepteur) {
        if (exclusions.estExclu(chemin)) {
            return;
        }
        prediction.predire(requete, recepteur);
    }
}
