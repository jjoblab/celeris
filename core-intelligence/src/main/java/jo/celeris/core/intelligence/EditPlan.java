package jo.celeris.core.intelligence;

import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.Transaction;

/**
 * Le plan d'un geste d'édition intelligent (M5) : la transaction à
 * appliquer par la voie unique (ADR-003) et la sélection d'après le
 * geste, explicite — la projection automatique ne peut pas deviner que
 * le curseur vit entre les deux caractères d'une paire insérée. La
 * session applique le plan par
 * {@code apply(transaction, selection)} : l'annulation restitue la
 * sélection d'avant, le refaire la sélection explicite (A-13).
 *
 * @param transaction la transaction du geste, éventuellement vide (le
 *                    saut par-dessus un fermant n'édite pas)
 * @param selection   la sélection d'après, dans les coordonnées du
 *                    document résultant
 */
public record EditPlan(Transaction transaction, MultiSelection selection) {

    /**
     * Constructeur canonique : aucune composante nulle.
     */
    public EditPlan {
        java.util.Objects.requireNonNull(transaction, "transaction");
        java.util.Objects.requireNonNull(selection, "selection");
    }
}
