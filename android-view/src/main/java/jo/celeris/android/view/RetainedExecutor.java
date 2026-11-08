package jo.celeris.android.view;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * L'exécuteur RETENU du travail d'analyse (M7.5, §9 — le réglage de
 * lenteur livré) : chaque soumission annule la précédente en attente
 * et reprogramme après le délai — la frappe rapide ne paie plus une
 * passe d'analyse par caractère, la publication arrive au calme du
 * doigt. Le délai se règle à chaud : zéro rend la main immédiate.
 *
 * <p>L'annulation est sûre par construction (ADR-007) : chaque tâche
 * soumise porte la révision la plus récente et son jeton supplanté se
 * rend au premier point de contrôle — la tâche retirée n'aurait rien
 * publié. Le retard vit dans un minuteur démon à fil unique ; le
 * travail part vers l'exécuteur de base, fil unique démon lui aussi.</p>
 *
 * <p>Pur et testable sans appareil : le minuteur s'injecte (le banc y
 * capture les tâches sans horloge réelle), le délai se lit et se pose.</p>
 */
final class RetainedExecutor implements Executor {

    /** Le minuteur injecté : planifier une tâche, retirer la planifiée. */
    interface Delayer {

        /**
         * Planifie la tâche après le délai.
         *
         * @param delayMs le délai, en millisecondes
         * @param task    la tâche à exécuter au terme du délai
         */
        void schedule(long delayMs, Runnable task);

        /** Retire la tâche planifiée, s'il y en a une. */
        void cancelPending();
    }

    /**
     * Le minuteur de production : un planificateur démon à fil unique,
     * la tâche planifiée se retire au fil d'une référence atomique.
     *
     * @return le minuteur branché sur le planificateur démon
     */
    static Delayer daemonDelayer() {
        ScheduledExecutorService minuteur =
                Executors.newSingleThreadScheduledExecutor(future -> {
                    Thread fil = new Thread(future, "celeris-analyse-retenu");
                    fil.setDaemon(true);
                    return fil;
                });
        AtomicReference<ScheduledFuture<?>> planifiee = new AtomicReference<>();
        return new Delayer() {
            @Override
            public void schedule(long delayMs, Runnable task) {
                planifiee.set(minuteur.schedule(task, delayMs,
                        TimeUnit.MILLISECONDS));
            }

            @Override
            public void cancelPending() {
                ScheduledFuture<?> enAttente = planifiee.getAndSet(null);
                if (enAttente != null) {
                    enAttente.cancel(false);
                }
            }
        };
    }

    private final Executor base;
    private final Delayer delayer;
    private volatile long delayMs;

    /**
     * Construit l'exécuteur retenu.
     *
     * @param base    l'exécuteur du travail d'analyse (fil unique démon)
     * @param delayer le minuteur des reprogrammations
     * @param delayMs le délai de retenue, en millisecondes (0 : immédiat)
     */
    RetainedExecutor(Executor base, Delayer delayer, long delayMs) {
        this.base = base;
        this.delayer = delayer;
        this.delayMs = Math.max(0L, delayMs);
    }

    /** Le délai courant, en millisecondes (0 : immédiat). */
    long delayMs() {
        return delayMs;
    }

    /**
     * Pose le délai de retenue — à chaud, l'hôte y règle sa lenteur
     * d'analyse ; zéro rend la main immédiate.
     *
     * @param delayMs le délai, en millisecondes, jamais négatif
     */
    void setDelayMs(long delayMs) {
        this.delayMs = Math.max(0L, delayMs);
    }

    /**
     * La soumission : au délai nul, la main passe immédiatement ;
     * sinon la planifiée se retire et la nouvelle se programme — la
     * rafale ne laisse partir que la DERNIÈRE tâche, au calme, vers
     * l'exécuteur de base.
     */
    @Override
    public synchronized void execute(Runnable command) {
        if (delayMs <= 0L) {
            base.execute(command);
            return;
        }
        delayer.cancelPending();
        delayer.schedule(delayMs, () -> base.execute(command));
    }
}
