package jo.celeris.android.view.internal.exec;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * L'exécuteur RETENU du travail d'analyse (M7.5, §9 — le réglage de
 * lenteur livré) : chaque soumission annule la précédente en attente
 * et reprogramme après le délai — la frappe rapide ne paie plus une
 * passe d'analyse par caractère, la publication arrive au calme du
 * doigt. Le délai se règle à chaud : zéro rend la main immédiate.
 *
 * <p>Le créneau d'attente vit DANS l'instance (audit coloration, H2) :
 * une retenue par MOTEUR — {@link #pourMoteur()} en dérive une par
 * document, partageant l'exécuteur de base, le minuteur et le délai ;
 * l'ouverture rapide d'un second onglet ne supprime plus la tâche du
 * premier. La PREMIÈRE soumission d'une instance part direct (H1) :
 * la retenue sert la frappe, jamais l'ouverture — 250 ms de moins
 * avant la première couleur.</p>
 *
 * <p>L'annulation est sûre par construction (ADR-007) : chaque tâche
 * soumise porte la révision la plus récente et son jeton supplanté se
 * rend au premier point de contrôle — la tâche retirée n'aurait rien
 * publié.</p>
 *
 * <p>Pur et testable sans appareil : le minuteur s'injecte (le banc y
 * capture les tâches sans horloge réelle), le délai se lit et se pose.</p>
 */
public final class RetainedExecutor implements Executor {

    /** Le minuteur injecté : planifier une tâche rend SA commande
     *  d'annulation — le créneau vit chez l'appelant, jamais dans le
     *  minuteur (une retenue par moteur, un minuteur par vue). */
    public interface Delayer {

        /**
         * Planifie la tâche après le délai et rend la commande
         * d'annulation de CETTE planification.
         *
         * @param delayMs le délai, en millisecondes
         * @param task    la tâche à exécuter au terme du délai
         * @return la commande qui retire la planification
         */
        Runnable schedule(long delayMs, Runnable task);

        /**
         * Éteint le minuteur (chantier C2) : le fil démon qui porte les
         * reprogrammations meurt. L'implantation de banc, qui ne pose
         * aucun fil, ne fait rien.
         */
        void shutdown();
    }

    /**
     * Le minuteur de production : un planificateur démon à fil unique,
     * SANS état — chaque planification rend sa propre commande
     * d'annulation, le même minuteur sert toutes les retenues de la
     * vue (une par moteur).
     *
     * @return le minuteur branché sur le planificateur démon
     */
    public static Delayer daemonDelayer() {
        ScheduledExecutorService minuteur =
                Executors.newSingleThreadScheduledExecutor(future -> {
                    Thread thread = new Thread(future, "celeris-analyse-retenu");
                    thread.setDaemon(true);
                    return thread;
                });
        return new Delayer() {
            @Override
            public Runnable schedule(long delayMs, Runnable task) {
                ScheduledFuture<?> planifiee = minuteur.schedule(task, delayMs,
                        TimeUnit.MILLISECONDS);
                return () -> planifiee.cancel(false);
            }

            @Override
            public void shutdown() {
                minuteur.shutdownNow();
            }
        };
    }

    /** Le délai partagé entre la retenue et toutes ses dérivées par
     *  moteur : le réglage à chaud de la vue les atteint toutes. */
    private static final class DelaiPartage {

        private volatile long ms;

        DelaiPartage(long ms) {
            this.ms = Math.max(0L, ms);
        }

        void poser(long ms) {
            this.ms = Math.max(0L, ms);
        }
    }

    private final Executor base;
    private final Delayer delayer;
    private final DelaiPartage delai;

    /** L'annulation de la planifiée en cours — LE créneau de CETTE
     *  instance, jamais partagé (H2 : une retenue par moteur). */
    private Runnable annulation;

    /** Vrai jusqu'à la première soumission — elle part direct (H1) :
     *  la retenue sert la frappe, jamais l'ouverture. */
    private boolean premiereSoumission = true;

    /**
     * Construit l'exécuteur retenu.
     *
     * @param base    l'exécuteur du travail d'analyse (fil unique démon)
     * @param delayer le minuteur des reprogrammations
     * @param delayMs le délai de retenue, en millisecondes (0 : immédiat)
     */
    public RetainedExecutor(Executor base, Delayer delayer, long delayMs) {
        this.base = base;
        this.delayer = delayer;
        this.delai = new DelaiPartage(delayMs);
    }

    private RetainedExecutor(Executor base, Delayer delayer, DelaiPartage delai) {
        this.base = base;
        this.delayer = delayer;
        this.delai = delai;
    }

    /**
     * La retenue d'UN moteur (audit H2) : même base, même minuteur,
     * même délai — créneau et première soumission neufs. La vue en
     * dérive une par document : les ouvertures rapides ne se
     * suppriment plus les unes les autres, le réglage du délai reste
     * celui de la vue.
     *
     * @return la retenue du moteur, prête pour sa première ouverture
     */
    public RetainedExecutor pourMoteur() {
        return new RetainedExecutor(base, delayer, delai);
    }

    /** Le délai courant, en millisecondes (0 : immédiat). */
    public long delayMs() {
        return delai.ms;
    }

    /**
     * Pose le délai de retenue — à chaud, l'hôte y règle sa lenteur
     * d'analyse ; zéro rend la main immédiate. Le réglage atteint
     * toutes les retenues dérivées par moteur (le délai est partagé).
     *
     * @param delayMs le délai, en millisecondes, jamais négatif
     */
    public void setDelayMs(long delayMs) {
        delai.poser(delayMs);
    }

    /**
     * La soumission : la première part DIRECT au travail (H1 :
     * l'ouverture ne patiente pas) ; au délai nul, la main passe
     * immédiatement ; sinon la planifiée se retire et la nouvelle se
     * programme — la rafale ne laisse partir que la DERNIÈRE tâche,
     * au calme, vers l'exécuteur de base.
     */
    @Override
    public synchronized void execute(Runnable command) {
        boolean premiere = premiereSoumission;
        premiereSoumission = false;
        if (delai.ms <= 0L || premiere) {
            annuler();
            base.execute(command);
            return;
        }
        annuler();
        annulation = delayer.schedule(delai.ms, () -> base.execute(command));
    }

    /** Retire la planifiée en cours, s'il y en a une. */
    private void annuler() {
        Runnable locale = annulation;
        annulation = null;
        if (locale != null) {
            locale.run();
        }
    }
}
