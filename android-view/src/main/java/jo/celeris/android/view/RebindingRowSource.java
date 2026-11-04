package jo.celeris.android.view;

import org.jspecify.annotations.Nullable;

/**
 * Source déléguée réassignable : la vue y échange la source de rangées
 * courante à chaque reconstruction de projection — changement de
 * session, de révision, de budget de coupure.
 *
 * <p>La couche de texte est construite une fois au pipeline : sans cette
 * indirection, elle capturerait la source du premier jour et servirait
 * le contenu d'avant sous la révision d'après — le cache invalidé
 * serait recomblé de mises en page périmées. La réassignation est le
 * seul moment de mutation, hors du chemin de dessin ; la consultation
 * délègue sans allouer (PIEGE-15).</p>
 */
public final class RebindingRowSource implements RowSource {

    private @Nullable RowSource deleguee;

    /**
     * Construit la source déléguée sur la source initiale.
     *
     * @param initiale la source servie jusqu'à la première réassignation
     */
    public RebindingRowSource(RowSource initiale) {
        this.deleguee = initiale;
    }

    /**
     * Réassigne la source servie.
     *
     * @param source la nouvelle source de rangées
     */
    public void set(RowSource source) {
        this.deleguee = source;
    }

    @Override
    public RowLayout layoutOf(RowKey key) {
        RowSource locale = deleguee;
        if (locale == null) {
            throw new IllegalStateException("rebound source unset");
        }
        return locale.layoutOf(key);
    }
}
