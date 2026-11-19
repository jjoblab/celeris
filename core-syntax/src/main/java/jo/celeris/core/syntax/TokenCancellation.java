package jo.celeris.core.syntax;

/**
 * Jeton d'annulation d'un travail d'analyse (ADR-007) : tout travail
 * long — lexicographie complète, complétion de l'arbre — accepte un
 * jeton et l'interroge à ses points de contrôle ; annuler coûte moins
 * cher que terminer. Le drapeau vit sous le verrou intrinsèque : pas
 * de volatile ni d'atomique au cœur (la règle est vérifiée par le test
 * d'architecture du module), le moniteur suffit — l'annulation vient
 * d'un autre fil que celui qui travaille.
 *
 * <p>Le jeton ne porte pas de raison : supplanté par une édition ou
 * fermé par l'hôte, le travail s'arrête de la même façon —
 * {@link AnalysisCancelled} au prochain point de contrôle.</p>
 */
public final class TokenCancellation {

    private boolean cancelled;

    /** Un jeton actif. */
    public TokenCancellation() {
        this.cancelled = false;
    }

    /** Annule le travail associé : idempotent. */
    public synchronized void annuler() {
        cancelled = true;
    }

    /** Vrai quand le travail associé doit s'arrêter au prochain point de contrôle. */
    public synchronized boolean isCancelled() {
        return cancelled;
    }

    /**
     * Point de contrôle : lève si le travail est annulé, sinon passe.
     *
     * @throws AnalysisCancelled si le travail a été annulé
     */
    public void check() {
        if (isCancelled()) {
            throw new AnalysisCancelled();
        }
    }
}
