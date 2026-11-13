package jo.celeris.android.view;

import java.util.function.IntConsumer;
import java.util.function.IntPredicate;

import org.jspecify.annotations.Nullable;

/**
 * Les rappels de l'hôte sur les gestes de la vue (M7.6, D-04 la
 * reprise) : le double-tap du texte y rend son verdict (le color
 * picker d'un littéral, la documentation au symbole — le
 * redressement du 2026-11-13 retire le survol de l'appui long : il
 * entrait en conflit avec la barre de sélection, seul le double-clic
 * et le « i » de la barre l'ouvrent) ; l'appui long de gouttière y
 * rend la ligne visée — le moment juste du point d'arrêt. Le chantier
 * d'unification (2026-11-13) y pose le VERDICT DES TOUCHES
 * MATÉRIELLES : l'hôte y délègue la navigation clavier de ses popups
 * (PIEGE-27 : la vue garde le focus, la touche remonte à l'hôte AVANT
 * la table de raccourcis — jamais la composition IME ne s'y sert).
 * La vue ne fait que tenir les consommateurs et notifier : les états
 * d'exécution appartiennent à l'hôte.
 *
 * <p>Porteur mince : la vue y délègue ses poseurs publics, ses
 * collaborateurs de toucher y notifient — le plafond de la vue
 * (ADR-002) respire.</p>
 */
final class RappelsHote {

    /** Le verdict de double-tap de l'hôte — le moment du color picker
     *  d'un littéral de couleur, par exemple. */
    private volatile @Nullable IntPredicate surDoubleTap;

    /** Le consommateur de bascule d'arrêt de l'hôte (D-04) — l'appui long de gouttière. */
    private volatile @Nullable IntConsumer surBasculeArret;

    /** Le verdict des touches matérielles de l'hôte — la navigation
     *  clavier de ses popups, déléguée sans jamais le focus. */
    private volatile @Nullable IntPredicate surTouchePopup;

    /**
     * Pose (ou coupe) le verdict de double-tap.
     *
     * @param consommateur le verdict d'offset, null pour couper
     */
    void poserDoubleTap(@Nullable IntPredicate consommateur) {
        surDoubleTap = consommateur;
    }

    /**
     * Notifie l'hôte du double-tap : vrai quand l'hôte CONSOMME le
     * geste (la cascade du tap simple ne suit pas), faux quand il le
     * laisse passer.
     */
    boolean notifierDoubleTap(int offset) {
        IntPredicate consommateur = surDoubleTap;
        return consommateur != null && consommateur.test(offset);
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

    /**
     * Pose (ou coupe) le verdict des touches matérielles de l'hôte.
     *
     * @param consommateur le verdict de touche, null pour couper
     */
    void poserTouchePopup(@Nullable IntPredicate consommateur) {
        surTouchePopup = consommateur;
    }

    /**
     * Notifie l'hôte de la touche matérielle : vrai quand l'hôte la
     * CONSOMME pour ses popups — la composition IME en cours la rend
     * à la vie normale de l'éditeur, jamais une frappe composée ne
     * sert une carte.
     */
    boolean consommerTouche(CompositionState clavier, int keyCode) {
        IntPredicate consommateur = surTouchePopup;
        return consommateur != null && !clavier.isComposing()
                && consommateur.test(keyCode);
    }
}
