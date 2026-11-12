package jo.celeris.android.view;

import java.util.function.IntConsumer;

import org.jspecify.annotations.Nullable;

/**
 * Les rappels de l'hôte sur les gestes de la vue (M7.6, D-04 la
 * reprise) : l'appui long du texte y rend l'offset sélectionné — le
 * moment juste de la documentation au survol ; l'appui long de
 * gouttière y rend la ligne visée — le moment juste du point d'arrêt.
 * La vue ne fait que tenir les consommateurs et notifier : les états
 * d'exécution appartiennent à l'hôte.
 *
 * <p>Porteur mince : la vue y délègue ses poseurs publics, ses
 * collaborateurs de toucher y notifient — le plafond de la vue
 * (ADR-002) respire.</p>
 */
final class RappelsHote {

    /** Le consommateur d'appui long de l'hôte (M7.6) — le moment du survol. */
    private volatile @Nullable IntConsumer surAppuiLong;

    /** Le consommateur de bascule d'arrêt de l'hôte (D-04) — l'appui long de gouttière. */
    private volatile @Nullable IntConsumer surBasculeArret;

    /**
     * Pose (ou coupe) le consommateur d'appui long.
     *
     * @param consommateur le consommateur d'offset, null pour couper
     */
    void poserAppuiLong(@Nullable IntConsumer consommateur) {
        surAppuiLong = consommateur;
    }

    /** Notifie l'hôte de l'appui long — la sélection est déjà posée. */
    void notifierAppuiLong(int offset) {
        IntConsumer consommateur = surAppuiLong;
        if (consommateur != null) {
            consommateur.accept(offset);
        }
    }

    /**
     * Pose (ou coupe) le consommateur de bascule d'arrêt.
     *
     * @param consommateur le consommateur de ligne, null pour couper
     */
    void poserBasculeArret(@Nullable IntConsumer consommateur) {
        surBasculeArret = consommateur;
    }

    /** Notifie l'hôte de la bascule d'arrêt — la gouttière est visée. */
    void notifierBasculeArret(int ligne) {
        IntConsumer consommateur = surBasculeArret;
        if (consommateur != null) {
            consommateur.accept(ligne);
        }
    }
}
