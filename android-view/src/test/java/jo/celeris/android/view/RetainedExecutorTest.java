package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * L'exécuteur RETENU du travail d'analyse (M7.5, §9 — le réglage de
 * lenteur livré) : la rafale ne laisse partir que la DERNIÈRE tâche,
 * au calme du délai ; le délai nul rend la main immédiate ; le délai se
 * règle à chaud. Pur, sans horloge réelle — le minuteur s'injecte.
 */
class RetainedExecutorTest {

    /** Le minuteur d'essai : capture la dernière tâche planifiée. */
    private static final class MinuteurEssai implements RetainedExecutor.Delayer {

        private Runnable planifiee;
        private long dernierDelaiMs = -1L;
        private int annulations;

        @Override
        public void schedule(long delayMs, Runnable task) {
            planifiee = task;
            dernierDelaiMs = delayMs;
        }

        @Override
        public void cancelPending() {
            if (planifiee != null) {
                annulations++;
                planifiee = null;
            }
        }

        /** Exécute la tâche planifiée, s'il y en a une. */
        void expire() {
            Runnable tache = planifiee;
            planifiee = null;
            if (tache != null) {
                tache.run();
            }
        }
    }

    /** L'exécuteur de base d'essai : enregistre les passages. */
    private static final class BaseEssai implements java.util.concurrent.Executor {

        private final List<String> passages = new ArrayList<>();

        @Override
        public void execute(Runnable command) {
            command.run();
            passages.add("passage");
        }
    }

    /**
     * La rafale ne laisse partir que la dernière tâche : trois
     * soumissions successives, deux annulations, une seule part.
     */
    @Test
    void rafaleNeLaissePartirQueLaDerniere() {
        MinuteurEssai minuteur = new MinuteurEssai();
        BaseEssai base = new BaseEssai();
        RetainedExecutor executeur = new RetainedExecutor(base, minuteur, 250L);

        executeur.execute(() -> { });
        executeur.execute(() -> { });
        executeur.execute(() -> { });

        assertEquals(2, minuteur.annulations,
                "chaque nouvelle soumission retire la planifiée");
        assertEquals(250L, minuteur.dernierDelaiMs,
                "le délai se programme tel quel");
        minuteur.expire();
        assertEquals(1, base.passages.size(),
                "une seule tâche part, vers l'exécuteur de base");
    }

    /** Le délai nul rend la main immédiate : rien ne se programme. */
    @Test
    void delaiNulRendLaMainImmediate() {
        MinuteurEssai minuteur = new MinuteurEssai();
        BaseEssai base = new BaseEssai();
        RetainedExecutor executeur = new RetainedExecutor(base, minuteur, 0L);

        executeur.execute(() -> { });

        assertEquals(1, base.passages.size(), "la tâche part immédiatement");
        assertEquals(-1L, minuteur.dernierDelaiMs, "rien ne se programme");
    }

    /** Le délai se règle à chaud : la soumission suivante le suit. */
    @Test
    void delaiSeRegleAChaud() {
        MinuteurEssai minuteur = new MinuteurEssai();
        RetainedExecutor executeur = new RetainedExecutor(
                new BaseEssai(), minuteur, 250L);

        assertEquals(250L, executeur.delayMs());
        executeur.setDelayMs(600L);
        assertEquals(600L, executeur.delayMs());
        executeur.execute(() -> { });
        assertEquals(600L, minuteur.dernierDelaiMs,
                "la reprogrammation suit le nouveau délai");
        // le délai négatif se borne : jamais de retenue négative
        executeur.setDelayMs(-5L);
        assertEquals(0L, executeur.delayMs());
        assertTrue(executeur.delayMs() >= 0L);
    }
}
