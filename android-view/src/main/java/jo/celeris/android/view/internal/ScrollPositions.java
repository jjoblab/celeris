package jo.celeris.android.view.internal;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Map;

import jo.celeris.core.text.TextSession;

/**
 * Le parc des positions de défilement par session (feuille de route
 * des améliorations, C2 — « le retour d'onglet ramenait en haut du
 * document ») : la position de la session quittée s'ARCHIVE sous
 * elle, la session revenue la RETROUVE — clampée par
 * {@code ScrollGeometry} à la pose (le document a pu raccourcir en
 * arrière-plan, la position archivée dépasse alors son contenu).
 *
 * <p>Le modèle est le {@link RowPark} (C1) : la mémoire de la vue
 * vit PAR SESSION, la bascule se fait au changement d'onglet
 * (archive sous la session quittée, restauration pour celle qui
 * arrive), le plafond LRU borne la mémoire (les onglets ouverts
 * d'un éditeur réel), la libération de la vue vide tout. La langue
 * signe la hauteur de contenu (retour à la ligne différent) :
 * {@link #forget(TextSession)} retire la position archivée d'un
 * document dont la langue change — le retour ne resservira jamais
 * une position périmée.</p>
 */
public final class ScrollPositions {

    /** Le plafond de positions archivées (C2) : au-delà, la moins
     *  récemment revenue part. */
    public static final int PARKED_LIMIT = 8;

    /** La position archivée d'une session : les deux axes, en
     *  pixels — le clamp de la pose les ramène dans le contenu. */
    public record Position(float scrollX, float scrollY) { }

    private final Map<TextSession, Position> archivees =
            new IdentityHashMap<>();
    /** L'ordre LRU : la tête est la plus récemment revenue, la
     *  queue la prochaine à partir. */
    private final Deque<TextSession> ordre = new ArrayDeque<>();

    /**
     * Archive la position de la session quittée et rend celle de la
     * session revenue — NEUTRE (zéro, zéro) si inconnue (première
     * ouverture ou position chassée du plafond).
     *
     * <p>La session revenue se rafraîchit en tête de l'ordre LRU ;
     * un dépassement du plafond chasse la plus ancienne. La position
     * rendue n'est PAS clampée ici : la pose la rabat dans le
     * contenu du document revenu ({@code preparer()} s'en charge au
     * premier dessin) — la position archivée d'un document édité en
     * arrière-plan reste VALIDE en arithmétique, le clamp la rend
     * légitime.</p>
     *
     * @param quittee la session qui quitte l'écran (null pour
     *                aucune — le premier montage), null déjà couvert
     * @param x       la position horizontale de la session quittée
     * @param y       la position verticale de la session quittée
     * @param revenue la session qui prend l'écran
     * @return la position de la session revenue, NEUTRE si inconnue
     */
    public Position toggle(
            @org.jspecify.annotations.Nullable TextSession quittee,
            float x, float y, TextSession revenue) {
        if (quittee != null) {
            archivees.put(quittee, new Position(x, y));
            ordre.remove(quittee);
            ordre.addFirst(quittee);
        }
        Position revenuee = archivees.remove(revenue);
        evictAtCeiling();
        return revenuee != null ? revenuee : new Position(0f, 0f);
    }

    /** Chasse les positions au-delà du plafond — les moins
     *  récemment revenues partent d'abord. */
    private void evictAtCeiling() {
        while (archivees.size() > PARKED_LIMIT) {
            TextSession ancienne = ordre.pollLast();
            if (ancienne == null) {
                break;
            }
            archivees.remove(ancienne);
        }
    }

    /** Oublie la position archivée d'une session (C2) : la langue a
     *  changé sous le document — la hauteur de contenu change avec
     *  elle (retour à la ligne différent), la position archivée
     *  serait périmée. */
    public void forget(TextSession morte) {
        archivees.remove(morte);
        ordre.remove(morte);
    }

    /** Vide TOUT le parc — la vue qui meurt ne retient le document
     *  d'aucun onglet. */
    public void clearAll() {
        archivees.clear();
        ordre.clear();
    }

    /** Le nombre de positions archivées — lecture du banc (C2). */
    public int size() {
        return archivees.size();
    }
}
