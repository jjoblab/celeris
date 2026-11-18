package jo.celeris.android.view;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
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
 * <p>Les trois événements y sont MULTIPLES (ADR-0022, phase 2, T3 —
 * point 13 de la revue) : le kit et l'hôte s'abonnent chacun, sans
 * s'écraser. Les poseurs 1.x restent en compatibilité.</p>
 *
 * <p>Porteur mince : la vue y délègue ses poseurs publics, ses
 * collaborateurs de toucher y notifient — le plafond de la vue
 * (ADR-002) respire.</p>
 */
final class RappelsHote {

    /** Le verdict de double-tap de l'hôte — le moment du color picker
     *  d'un littéral de couleur, par exemple. Voie 1.x. */
    private volatile @Nullable IntPredicate surDoubleTap;

    /** Les verdicts 2.0 du double-tap — multiples (T3). */
    private final List<IntPredicate> verdictsDoubleTap =
            new CopyOnWriteArrayList<>();

    /** Le consommateur de bascule d'arrêt de l'hôte (D-04) — l'appui
     *  long de gouttière. Voie 1.x. */
    private volatile @Nullable IntConsumer surBasculeBreakpoint;

    /** Les consommateurs 2.0 de bascule d'arrêt — multiples (T3). */
    private final List<IntConsumer> consommateursBascule =
            new CopyOnWriteArrayList<>();

    /** Le verdict des touches matérielles de l'hôte — la navigation
     *  clavier de ses popups, déléguée sans jamais le focus. Voie 1.x. */
    private volatile @Nullable IntPredicate surTouchePopup;

    /** Les verdicts 2.0 des touches matérielles — multiples (T3). */
    private final List<IntPredicate> verdictsTouchePopup =
            new CopyOnWriteArrayList<>();

    /**
     * Pose (ou coupe) le verdict de double-tap. Voie 1.x à emplacement
     * unique : la voie 2.0 est {@link #addDoubleTapListener}.
     *
     * @param consommateur le verdict d'offset, null pour couper
     */
    void poserDoubleTap(@Nullable IntPredicate consommateur) {
        surDoubleTap = consommateur;
    }

    /**
     * Abonne un verdict de double-tap (voie 2.0, T3).
     *
     * @param verdict le verdict d'offset, jamais null
     */
    void addDoubleTapListener(IntPredicate verdict) {
        verdictsDoubleTap.add(Objects.requireNonNull(verdict));
    }

    /**
     * Désabonne un verdict de double-tap (voie 2.0).
     *
     * @param verdict le verdict retiré
     */
    void removeDoubleTapListener(IntPredicate verdict) {
        verdictsDoubleTap.remove(verdict);
    }

    /**
     * Notifie le double-tap : vrai quand AU MOINS UN abonné CONSOMME
     * le geste (la cascade du tap simple ne suit pas), faux quand
     * tous le laissent passer.
     */
    boolean notifierDoubleTap(int offset) {
        boolean consomme = false;
        IntPredicate consommateur = surDoubleTap;
        if (consommateur != null) {
            consomme |= consommateur.test(offset);
        }
        for (IntPredicate verdict : verdictsDoubleTap) {
            consomme |= verdict.test(offset);
        }
        return consomme;
    }

    /**
     * Pose (ou coupe) le consommateur de bascule d'arrêt. Voie 1.x à
     * emplacement unique : la voie 2.0 est
     * {@link #addBreakpointToggleListener}.
     *
     * @param consommateur le consommateur de ligne, null pour couper
     */
    void poserBasculeBreakpoint(@Nullable IntConsumer consommateur) {
        surBasculeBreakpoint = consommateur;
    }

    /**
     * Abonne un consommateur de bascule d'arrêt (voie 2.0, T3).
     *
     * @param consommateur le consommateur de ligne, jamais null
     */
    void addBreakpointToggleListener(IntConsumer consommateur) {
        consommateursBascule.add(Objects.requireNonNull(consommateur));
    }

    /**
     * Désabonne un consommateur de bascule d'arrêt (voie 2.0).
     *
     * @param consommateur le consommateur retiré
     */
    void removeBreakpointToggleListener(IntConsumer consommateur) {
        consommateursBascule.remove(consommateur);
    }

    /** Notifie la bascule d'arrêt — la gouttière est visée, CHAQUE
     *  abonné rend sa ligne. */
    void notifierBasculeBreakpoint(int ligne) {
        IntConsumer consommateur = surBasculeBreakpoint;
        if (consommateur != null) {
            consommateur.accept(ligne);
        }
        for (IntConsumer abonne : consommateursBascule) {
            abonne.accept(ligne);
        }
    }

    /**
     * Pose (ou coupe) le verdict des touches matérielles de l'hôte.
     * Voie 1.x à emplacement unique : la voie 2.0 est
     * {@link #addPopupKeyListener}.
     *
     * @param consommateur le verdict de touche, null pour couper
     */
    void poserTouchePopup(@Nullable IntPredicate consommateur) {
        surTouchePopup = consommateur;
    }

    /**
     * Abonne un verdict des touches matérielles (voie 2.0, T3).
     *
     * @param verdict le verdict de touche, jamais null
     */
    void addPopupKeyListener(IntPredicate verdict) {
        verdictsTouchePopup.add(Objects.requireNonNull(verdict));
    }

    /**
     * Désabonne un verdict des touches matérielles (voie 2.0).
     *
     * @param verdict le verdict retiré
     */
    void removePopupKeyListener(IntPredicate verdict) {
        verdictsTouchePopup.remove(verdict);
    }

    /**
     * Notifie la touche matérielle : vrai quand AU MOINS UN abonné la
     * CONSOMME pour ses popups — la composition IME en cours la rend
     * à la vie normale de l'éditeur, jamais une frappe composée ne
     * sert une carte.
     */
    boolean consommerTouche(CompositionState clavier, int keyCode) {
        if (clavier.isComposing()) {
            return false;
        }
        boolean consomme = false;
        IntPredicate consommateur = surTouchePopup;
        if (consommateur != null) {
            consomme |= consommateur.test(keyCode);
        }
        for (IntPredicate verdict : verdictsTouchePopup) {
            consomme |= verdict.test(keyCode);
        }
        return consomme;
    }
}
