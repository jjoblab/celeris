package jo.celeris.android.host.popups;

import java.util.List;

import jo.celeris.android.widgets.PopupController;
import jo.celeris.android.widgets.PopupManager;

/**
 * Le repositionneur des popups ouvertes (le satellite de l'avis de
 * fenêtre, 2026-11-14) : au défilement, au zoom et au clavier, chaque
 * surface OUVERTE se re-pose — la complétion au caret, le fil
 * d'Ariane à sa ligne, la bande d'erreurs à ses murs, la barre de
 * sélection et le menu d'intentions à leurs ancres ; les surfaces de
 * consultation se repositionnent SANS condition (le régime du genre).
 * La complexité quitte l'orchestrateur, les gestes restent les siens.
 */
final class Repositionneur {

    private final PopupManager popups;
    private final Runnable compl;
    private final Runnable fil;
    private final Runnable bande;
    private final Runnable barre;
    private final Runnable intentions;
    private final List<Runnable> consultations;

    /**
     * Construit le repositionneur sur les gestes de l'orchestrateur.
     *
     * @param popups       le coordinateur des popups (qui vit ?)
     * @param compl        re-pose la complétion (ouverte)
     * @param fil          rafraîchit le fil d'Ariane (ouvert)
     * @param bande        rafraîchit la bande d'erreurs (ouverte)
     * @param barre        re-pose la barre de sélection (ouverte)
     * @param intentions   re-pose le menu d'intentions (ouvert)
     * @param consultations les repositionnements inconditionnels de la
     *                     consultation (usages, hiérarchie, gabarits…)
     */
    Repositionneur(PopupManager popups, Runnable compl, Runnable fil,
            Runnable bande, Runnable barre, Runnable intentions,
            List<Runnable> consultations) {
        this.popups = popups;
        this.compl = compl;
        this.fil = fil;
        this.bande = bande;
        this.barre = barre;
        this.intentions = intentions;
        this.consultations = List.copyOf(consultations);
    }

    /** Re-pose chaque surface ouverte ; la consultation suit toujours. */
    void tout() {
        if (popups.estOuvert(PopupController.PopupKind.COMPLETION_LIST)) {
            compl.run();
        }
        if (popups.estOuvert(PopupController.PopupKind.BREADCRUMB)) {
            fil.run();
        }
        if (popups.estOuvert(PopupController.PopupKind.ERROR_BAND)) {
            bande.run();
        }
        if (popups.estOuvert(PopupController.PopupKind.SELECTION_BAR)) {
            barre.run();
        }
        if (popups.estOuvert(PopupController.PopupKind.INTENTIONS_MENU)) {
            intentions.run();
        }
        for (Runnable repositionner : consultations) {
            repositionner.run();
        }
    }
}
