package jo.celeris.android.view;

/**
 * Source de mises en page de rangées : la fabrique consultée quand le
 * cache manque une rangée.
 *
 * <p>Le pré-calcul asynchrone hors fil d'interface (ADR-006) s'y branchera ;
 * en attendant, la source synchrone de l'hôte suffit — le chemin de
 * dessin ne la consulte qu'au premier affichage d'une rangée, jamais en
 * régime établi.</p>
 */
public interface RowSource {

    /**
     * Calcule la mise en page demandée par la clé.
     *
     * @param key la clé de la rangée manquante
     * @return la mise en page de la rangée
     */
    RowLayout layoutOf(RowKey key);
}
