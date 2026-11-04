package jo.celeris.core.text;

import java.util.function.LongSupplier;

/**
 * Vue garde-fou du texte parcouru par le moteur regex — portage du dépôt
 * de départ (garde-fou D-05), visible du paquet pour ses tests.
 *
 * <p>Elle vérifie l'échéance — et l'interruption du thread — pendant le
 * backtracking : une expression à retour sur soi-même catastrophique ne
 * fige plus le thread appelant, la correspondance est interrompue au-delà
 * du budget par {@link FindReplace.RegexBudgetExceededException}. Le
 * contrôle est échantillonné (un contrôle tous les
 * {@value #CHECK_EVERY} accès) : le surcoût est constant et négligeable,
 * l'échéance reste respectée à l'échelle de la milliseconde.</p>
 *
 * <p>L'horloge est injectée en millisecondes croissantes (ADR-007) : le
 * moteur n'appelle jamais l'horloge système — l'hôte fournit la sienne,
 * les tests obtiennent un temps déterministe.</p>
 */
final class GuardedCharSequence implements CharSequence {

    /** Fréquence d'échantillonnage du contrôle d'échéance. */
    static final int CHECK_EVERY = 1024;

    private final CharSequence delegate;
    private final LongSupplier horloge;
    private final long echeanceMillis;

    /** Contrôles échantillonnés depuis le dernier relevé d'échéance. */
    private int controlesDepuisDernier;

    GuardedCharSequence(CharSequence delegate, long budgetMillis, LongSupplier horloge) {
        this.delegate = delegate;
        this.horloge = horloge;
        this.echeanceMillis = horloge.getAsLong() + budgetMillis;
    }

    /** Interruption ou budget dépassé : la correspondance est interrompue. */
    private void controlerEcheance() {
        // Interruption : la recherche est annulée proprement — le statut
        // est consommé une fois pour toutes.
        if (Thread.interrupted()) {
            throw new FindReplace.RegexBudgetExceededException("search interrupted");
        }
        if (horloge.getAsLong() > echeanceMillis) {
            throw new FindReplace.RegexBudgetExceededException(
                    "regex too costly: budget of " + FindReplace.REGEX_BUDGET_MS
                            + " ms exceeded");
        }
    }

    @Override
    public int length() {
        if (++controlesDepuisDernier >= CHECK_EVERY) {
            controlesDepuisDernier = 0;
            controlerEcheance();
        }
        return delegate.length();
    }

    @Override
    public char charAt(int index) {
        if (++controlesDepuisDernier >= CHECK_EVERY) {
            controlesDepuisDernier = 0;
            controlerEcheance();
        }
        return delegate.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        if (++controlesDepuisDernier >= CHECK_EVERY) {
            controlesDepuisDernier = 0;
            controlerEcheance();
        }
        return delegate.subSequence(start, end);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
