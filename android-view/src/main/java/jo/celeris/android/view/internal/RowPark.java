package jo.celeris.android.view.internal;

import jo.celeris.android.view.RowLayoutCache;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Map;

import jo.celeris.core.text.TextSession;

/**
 * Le parc des mises en page par session (feuille de route des
 * améliorations, C1 — le retour du propriétaire : « à chaque
 * changement d'onglet, impression d'un retard dans la coloration ») :
 * la mesure d'une rangée appartient AU DOCUMENT, comme la
 * tokenisation du {@link EnginePark} — le stockage du cache de la
 * session quittée se PARQUE sous elle, une session revenue RETROUVE
 * ses mises en page et ne re-mesure rien.
 *
 * <p>Le retard venait de la purge globale à chaque pose de session
 * ({@code reinitialiserScene} vidait le cache ENTIER, toutes
 * sessions comprises) : le retour sur un onglet re-payait la mesure
 * de chaque rangée visible — l'image retarde d'autant. La clé
 * ({@link RowKey}) porte la révision du document mais pas la
 * session : deux documents peuvent y entrer en collision (même
 * révision, même rangée), d'où la purge prudente d'alors (PIEGE-02
 * par construction). Le parc lève la collision AUTOUR de la clé :
 * l'instance du cache vit avec la vue (les couches du pipeline la
 * portent), son STOCKAGE bascule par session — détaché à la
 * sortie, adopté au retour, la révision continue d'invalider DANS
 * un document.</p>
 *
 * <p>Le parc ne grandit pas sans borne : les stockages parqués
 * suivent une politique LRU ({@value #PLAFOND_PARQUES}, la fenêtre
 * des onglets d'un éditeur réel), chaque stockage reste plafonné à
 * {@link RowLayoutCache#CEILING} entrées. {@link #clearAll()}
 * vide le parc à la libération de la vue — la vue qui meurt ne
 * retient le document d'aucun onglet.</p>
 */
public final class RowPark {

    /** Le plafond de stockages parqués (C1) : au-delà, le moins
     *  récemment utilisé part. */
    public static final int PARKED_LIMIT = 8;

    private final Map<TextSession, RowLayoutCache.Stockage> parques =
            new IdentityHashMap<>();
    /** L'ordre LRU : la tête est la plus récemment revenue, la
     *  queue la prochaine à partir. */
    private final Deque<TextSession> ordre = new ArrayDeque<>();

    /**
     * Parque le stockage de la session quittée — la session revenue
     * adopte le sien, ou un stockage NEUF si le document n'en avait
     * jamais eu (première ouverture).
     *
     * <p>La session revenue se rafraîchit en tête de l'ordre LRU ;
     * un dépassement du plafond chasse la plus ancienne. Le
     * stockage revenu porte les rangées valides À LA RÉVISION du
     * document parqué — le document a pu ÉDITER en arrière-plan :
     * ses clés périmées s'invalident par révision comme à l'écran
     * (l'épissure et l'invalidation par révision font leur travail
     * DANS le stockage), les autres survivent.</p>
     *
     * @param quittee  la session qui quitte l'écran (null pour
     *                 aucune — le premier montage)
     * @param courant  le stockage détaché de la session quittée
     * @param revenue  la session qui prend l'écran
     * @return le stockage de la session revenue, NEUF si inconnu
     */
    public RowLayoutCache.Stockage toggle(
            @org.jspecify.annotations.Nullable TextSession quittee,
            RowLayoutCache.Stockage courant, TextSession revenue) {
        if (quittee != null) {
            parques.put(quittee, courant);
            ordre.remove(quittee);
            ordre.addFirst(quittee);
        }
        RowLayoutCache.Stockage revenu = parques.remove(revenue);
        evictAtCeiling();
        return revenu != null ? revenu : new RowLayoutCache.Stockage();
    }

    /** Chasse les stockages au-delà du plafond — les moins
     *  récemment utilisés partent d'abord. */
    private void evictAtCeiling() {
        while (parques.size() > PARKED_LIMIT) {
            TextSession ancienne = ordre.pollLast();
            if (ancienne == null) {
                break;
            }
            parques.remove(ancienne);
        }
    }

    /** Oublie le stockage parqué d'une session (C1) : la langue a
     *  changé sous le document — les mises en page parquées sont
     *  périmées par la langue (les portées signent le tracé), le
     *  retour de la session repartira d'un stockage NEUF. */
    public void forget(TextSession morte) {
        parques.remove(morte);
        ordre.remove(morte);
    }

    /** Vide TOUT le parc — la vue qui meurt ne retient le document
     *  d'aucun onglet. */
    public void clearAll() {
        parques.clear();
        ordre.clear();
    }

    /** Le nombre de stockages parqués — lecture du banc (C1). */
    public int size() {
        return parques.size();
    }
}
